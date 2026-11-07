package llc.devlab.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import llc.devlab.app.exception.BadRequestException;
import llc.devlab.app.exception.ForbiddenException;
import llc.devlab.app.model.dto.CreateOrderRequest;
import llc.devlab.app.model.dto.OrderDto;
import llc.devlab.app.model.dto.OrderLineRequest;
import llc.devlab.app.model.dto.TransitionOrderRequest;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.entity.Product;
import llc.devlab.app.model.enums.AccountRole;
import llc.devlab.app.model.enums.OrderStatus;
import llc.devlab.app.repository.AccountRepository;
import llc.devlab.app.repository.OrderLineRepository;
import llc.devlab.app.repository.OrderStatusHistoryRepository;
import llc.devlab.app.repository.ProductRepository;
import llc.devlab.app.repository.PurchaseOrderRepository;
import llc.devlab.app.repository.RefreshTokenRepository;
import llc.devlab.app.security.AccountPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDomainData() {
        orderStatusHistoryRepository.deleteAll();
        orderLineRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        productRepository.deleteAll();
        refreshTokenRepository.deleteAll();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderReducesStockAndAppendsCreatedHistory() {
        Account customer = createAccount("buyer@example.com", AccountRole.USER);
        Product product = createProduct("SKU-100", new BigDecimal("15.50"), 10);
        authenticate(customer);

        OrderDto order = orderService.create(new CreateOrderRequest(List.of(new OrderLineRequest(product.getId(), 2))));

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        assertThat(order.totalAmount()).isEqualByComparingTo("31.00");
        assertThat(order.lines()).hasSize(1);
        assertThat(reloaded.getAvailableStock()).isEqualTo(8);
        assertThat(orderStatusHistoryRepository.findByOrderIdOrderByTransitionAtAsc(order.id()))
                .extracting(history -> history.getNewStatus().name())
                .containsExactly("CREATED");
    }

    @Test
    void createOrderRejectsDuplicateLinesWithoutPartialChanges() {
        Account customer = createAccount("duplicate@example.com", AccountRole.USER);
        Product product = createProduct("SKU-200", new BigDecimal("7.25"), 9);
        authenticate(customer);

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(List.of(
                new OrderLineRequest(product.getId(), 1),
                new OrderLineRequest(product.getId(), 2)
        )))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate product lines");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isEqualTo(9);
        assertThat(purchaseOrderRepository.count()).isZero();
    }

    @Test
    void cancellationRestocksInventoryAndAppendsHistory() {
        Account customer = createAccount("cancel@example.com", AccountRole.USER);
        Product product = createProduct("SKU-300", new BigDecimal("5.00"), 4);
        authenticate(customer);

        OrderDto order = orderService.create(new CreateOrderRequest(List.of(new OrderLineRequest(product.getId(), 3))));
        OrderDto cancelled = orderService.transition(order.id(), new TransitionOrderRequest(OrderStatus.CANCELLED, "Customer changed mind"));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isEqualTo(4);
        assertThat(orderStatusHistoryRepository.findByOrderIdOrderByTransitionAtAsc(order.id()))
                .extracting(history -> history.getNewStatus().name())
                .containsExactly("CREATED", "CANCELLED");
    }

    @Test
    void nonAdminCannotMarkOwnOrderAsPaid() {
        Account customer = createAccount("transition@example.com", AccountRole.USER);
        Product product = createProduct("SKU-400", new BigDecimal("11.00"), 5);
        authenticate(customer);

        OrderDto order = orderService.create(new CreateOrderRequest(List.of(new OrderLineRequest(product.getId(), 1))));

        assertThatThrownBy(() -> orderService.transition(order.id(), new TransitionOrderRequest(OrderStatus.PAID, "Trying to self-pay")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void concurrentOrderPlacementDoesNotOversellStock() throws Exception {
        Account customerOne = createAccount("concurrency-one@example.com", AccountRole.USER);
        Account customerTwo = createAccount("concurrency-two@example.com", AccountRole.USER);
        Product product = createProduct("SKU-500", new BigDecimal("22.00"), 1);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(createOrderTask(customerOne, product.getId(), startGate));
            Future<String> second = executor.submit(createOrderTask(customerTwo, product.getId(), startGate));

            startGate.countDown();

            List<String> results = List.of(first.get(), second.get());
            long successes = results.stream().filter("SUCCESS"::equals).count();

            assertThat(successes).isEqualTo(1);
            assertThat(results).anyMatch(result -> result.contains(BadRequestException.class.getSimpleName()));
            assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isZero();
            assertThat(purchaseOrderRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> createOrderTask(Account account, Long productId, CountDownLatch startGate) {
        return () -> {
            startGate.await();
            authenticate(account);
            try {
                orderService.create(new CreateOrderRequest(List.of(new OrderLineRequest(productId, 1))));
                return "SUCCESS";
            } catch (Exception exception) {
                return exception.getClass().getSimpleName();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private void authenticate(Account account) {
        AccountPrincipal principal = AccountPrincipal.from(account);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private Account createAccount(String email, AccountRole role) {
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("encoded-password")
                .displayName(email)
                .role(role)
                .build());
    }

    private Product createProduct(String sku, BigDecimal unitPrice, int stock) {
        return productRepository.save(Product.builder()
                .sku(sku)
                .name(sku)
                .description("Test product")
                .unitPrice(unitPrice)
                .availableStock(stock)
                .build());
    }
}
