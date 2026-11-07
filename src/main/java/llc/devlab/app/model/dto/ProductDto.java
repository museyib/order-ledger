package llc.devlab.app.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal unitPrice,
        Integer availableStock,
        Instant createdAt,
        Instant updatedAt
) {
}
