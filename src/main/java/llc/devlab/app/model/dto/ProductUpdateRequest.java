package llc.devlab.app.model.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @NotNull @PositiveOrZero Integer availableStock
) {
}
