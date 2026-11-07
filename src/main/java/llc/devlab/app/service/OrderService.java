package llc.devlab.app.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.exception.BadRequestException;
import llc.devlab.app.exception.ForbiddenException;
import llc.devlab.app.exception.ResourceNotFoundException;
import llc.devlab.app.mapper.OrderMapper;
import llc.devlab.app.model.dto.CreateOrderRequest;
import llc.devlab.app.model.dto.OrderDto;
import llc.devlab.app.model.dto.OrderLineRequest;
import llc.devlab.app.model.dto.OrderStatusHistoryDto;
import llc.devlab.app.model.dto.OrderSummaryDto;
import llc.devlab.app.model.dto.PageResponse;
import llc.devlab.app.model.dto.TransitionOrderRequest;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.entity.OrderLine;
import llc.devlab.app.model.entity.Product;
import llc.devlab.app.model.entity.PurchaseOrder;
import llc.devlab.app.model.enums.AccountRole;
import llc.devlab.app.model.enums.OrderStatus;
import llc.devlab.app.repository.AccountRepository;
import llc.devlab.app.repository.OrderStatusHistoryRepository;
import llc.devlab.app.repository.ProductRepository;
import llc.devlab.app.repository.PurchaseOrderRepository;
import llc.devlab.app.security.AccountPrincipal;
import llc.devlab.app.security.SecurityUtils;
import llc.devlab.app.state.OrderStateResolver;
import llc.devlab.app.state.TransitionDecision;
import llc.devlab.app.util.PageResponseFactory;
import llc.devlab.app.util.PageableFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "totalAmount", "orderNumber");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AccountRepository accountRepository;
    private final OrderMapper orderMapper;
    private final OrderHistoryRecorder orderHistoryRecorder;
    private final OrderStateResolver orderStateResolver;
    private final PageableFactory pageableFactory;
    private final PageResponseFactory pageResponseFactory;

    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        AccountPrincipal principal = SecurityUtils.currentPrincipal();
        Account owner = accountRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated account was not found."));

        Map<Long, Integer> quantitiesByProductId = normalizeLines(request.lines());
        List<Long> productIds = List.copyOf(quantitiesByProductId.keySet());
        List<Product> lockedProducts = productRepository.findAllByIdsForUpdate(productIds);

        if (lockedProducts.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more requested products were not found.");
        }

        Map<Long, Product> productsById = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        PurchaseOrder order = PurchaseOrder.builder()
                .owner(owner)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineRequest lineRequest : request.lines()) {
            Product product = productsById.get(lineRequest.productId());
            if (product.getAvailableStock() < lineRequest.quantity()) {
                throw new BadRequestException("Insufficient stock for product " + product.getSku() + ".");
            }

            product.setAvailableStock(product.getAvailableStock() - lineRequest.quantity());
            BigDecimal lineTotal = product.getUnitPrice().multiply(BigDecimal.valueOf(lineRequest.quantity()));
            total = total.add(lineTotal);

            order.addLine(OrderLine.builder()
                    .product(product)
                    .productSkuSnapshot(product.getSku())
                    .productNameSnapshot(product.getName())
                    .unitPriceSnapshot(product.getUnitPrice())
                    .quantity(lineRequest.quantity())
                    .lineTotal(lineTotal)
                    .build());
        }

        order.setTotalAmount(total);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        orderHistoryRecorder.append(savedOrder, null, OrderStatus.CREATED, principal.email(), "Order created.");
        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getById(long orderId) {
        return orderMapper.toDto(findAccessibleOrder(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> list(int page, int size, List<String> sort) {
        AccountPrincipal principal = SecurityUtils.currentPrincipal();
        Pageable pageable = pageableFactory.create(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"), ALLOWED_SORT_FIELDS);
        Page<PurchaseOrder> result = isAdmin(principal)
                ? purchaseOrderRepository.findAll(pageable)
                : purchaseOrderRepository.findByOwnerId(principal.id(), pageable);
        return pageResponseFactory.from(result, orderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDto> getHistory(long orderId) {
        PurchaseOrder order = findAccessibleOrder(orderId);
        return orderStatusHistoryRepository.findByOrderIdOrderByTransitionAtAsc(order.getId())
                .stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional
    public OrderDto transition(long orderId, TransitionOrderRequest request) {
        AccountPrincipal principal = SecurityUtils.currentPrincipal();
        PurchaseOrder order = purchaseOrderRepository.findDetailedByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found."));

        enforceAccess(principal, order, request.targetStatus());
        TransitionDecision decision = orderStateResolver.transition(order.getStatus(), request.targetStatus());
        if (decision.releaseInventory()) {
            releaseInventory(order);
        }

        order.setStatus(decision.to());
        orderHistoryRecorder.append(order, decision.from(), decision.to(), principal.email(), request.reason());
        return orderMapper.toDto(order);
    }

    private PurchaseOrder findAccessibleOrder(long orderId) {
        AccountPrincipal principal = SecurityUtils.currentPrincipal();
        return isAdmin(principal)
                ? purchaseOrderRepository.findDetailedById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found."))
                : purchaseOrderRepository.findDetailedByIdAndOwnerId(orderId, principal.id())
                        .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found."));
    }

    private void enforceAccess(AccountPrincipal principal, PurchaseOrder order, OrderStatus targetStatus) {
        boolean owner = order.getOwner().getId().equals(principal.id());
        boolean admin = isAdmin(principal);

        if (targetStatus == OrderStatus.CANCELLED && (owner || admin)) {
            return;
        }
        if (admin) {
            return;
        }

        throw new ForbiddenException("You do not have permission to perform this transition.");
    }

    private void releaseInventory(PurchaseOrder order) {
        Map<Long, Product> lockedProducts = productRepository.findAllByIdsForUpdate(
                        order.getLines().stream().map(line -> line.getProduct().getId()).toList()
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        order.getLines().forEach(line -> {
            Product product = lockedProducts.get(line.getProduct().getId());
            product.setAvailableStock(product.getAvailableStock() + line.getQuantity());
        });
    }

    private Map<Long, Integer> normalizeLines(List<OrderLineRequest> lines) {
        Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (OrderLineRequest line : lines) {
            Integer existing = normalized.putIfAbsent(line.productId(), line.quantity());
            if (existing != null) {
                throw new BadRequestException("Duplicate product lines are not allowed: " + line.productId());
            }
        }
        return normalized;
    }

    private boolean isAdmin(AccountPrincipal principal) {
        return AccountRole.ADMIN.name().equals(principal.role());
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
