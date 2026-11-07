package llc.devlab.app.state;

import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class CreatedOrderState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.CREATED;
    }

    @Override
    public TransitionDecision transition(OrderStatus targetStatus) {
        return switch (targetStatus) {
            case PAID -> new TransitionDecision(OrderStatus.CREATED, OrderStatus.PAID, false);
            case CANCELLED -> new TransitionDecision(OrderStatus.CREATED, OrderStatus.CANCELLED, true);
            default -> throw new ConflictException("Transition from CREATED to " + targetStatus + " is not allowed.");
        };
    }
}
