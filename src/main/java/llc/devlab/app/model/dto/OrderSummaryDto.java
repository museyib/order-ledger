package llc.devlab.app.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

import llc.devlab.app.model.enums.OrderStatus;

public record OrderSummaryDto(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
