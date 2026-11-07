package llc.devlab.app.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import llc.devlab.app.model.enums.OrderStatus;

public record TransitionOrderRequest(
        @NotNull OrderStatus targetStatus,
        @Size(max = 255) String reason
) {
}
