package llc.devlab.app.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotEmpty @Valid List<OrderLineRequest> lines
) {
}
