package llc.devlab.app.model.dto;

import java.math.BigDecimal;

public record OrderLineDto(
        Long productId,
        String productSku,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
}
