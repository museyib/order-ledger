package llc.devlab.app.state;

import llc.devlab.app.model.enums.OrderStatus;

public interface OrderState {

    OrderStatus status();

    TransitionDecision transition(OrderStatus targetStatus);
}
