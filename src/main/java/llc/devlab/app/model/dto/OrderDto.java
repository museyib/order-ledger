package llc.devlab.app.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import llc.devlab.app.model.enums.OrderStatus;

public record OrderDto(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineDto> lines
) {
}
