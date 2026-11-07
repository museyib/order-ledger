package llc.devlab.app.state;

import llc.devlab.app.model.enums.OrderStatus;

public record TransitionDecision(
        OrderStatus from,
        OrderStatus to,
        boolean releaseInventory
) {
}
