package llc.devlab.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import llc.devlab.app.model.dto.CreateOrderRequest;
import llc.devlab.app.model.dto.OrderLineRequest;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.entity.Product;
import llc.devlab.app.model.enums.AccountRole;
import llc.devlab.app.repository.AccountRepository;
import llc.devlab.app.repository.OrderLineRepository;
import llc.devlab.app.repository.OrderStatusHistoryRepository;
import llc.devlab.app.repository.ProductRepository;
import llc.devlab.app.repository.PurchaseOrderRepository;
import llc.devlab.app.repository.RefreshTokenRepository;
import llc.devlab.app.security.AccountPrincipal;

@SpringBootTest
@ActiveProfiles("test")
class OrderRollbackIT {

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

    @MockitoBean
    private OrderHistoryRecorder orderHistoryRecorder;

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
    void createOrderRollsBackWhenHistoryPersistenceFails() {
        Account customer = accountRepository.save(Account.builder()
                .email("rollback@example.com")
                .passwordHash("encoded-password")
                .displayName("Rollback User")
                .role(AccountRole.USER)
                .build());

        Product product = productRepository.save(Product.builder()
                .sku("SKU-ROLLBACK")
                .name("Rollback Product")
                .description("Rollback")
                .unitPrice(new BigDecimal("9.99"))
                .availableStock(5)
                .build());

        AccountPrincipal principal = AccountPrincipal.from(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        doThrow(new RuntimeException("History write failed"))
                .when(orderHistoryRecorder)
                .append(any(), any(), any(), anyString(), anyString());

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(List.of(new OrderLineRequest(product.getId(), 2)))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("History write failed");

        assertThat(purchaseOrderRepository.count()).isZero();
        assertThat(orderStatusHistoryRepository.count()).isZero();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getAvailableStock()).isEqualTo(5);
    }
}
