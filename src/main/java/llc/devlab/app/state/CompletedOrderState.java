package llc.devlab.app.state;

import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class CompletedOrderState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.COMPLETED;
    }

    @Override
    public TransitionDecision transition(OrderStatus targetStatus) {
        throw new ConflictException("COMPLETED orders are immutable.");
    }
}
