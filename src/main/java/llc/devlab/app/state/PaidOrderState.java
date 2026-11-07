package llc.devlab.app.state;

import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class PaidOrderState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.PAID;
    }

    @Override
    public TransitionDecision transition(OrderStatus targetStatus) {
        return switch (targetStatus) {
            case SHIPPED -> new TransitionDecision(OrderStatus.PAID, OrderStatus.SHIPPED, false);
            case CANCELLED -> new TransitionDecision(OrderStatus.PAID, OrderStatus.CANCELLED, true);
            default -> throw new ConflictException("Transition from PAID to " + targetStatus + " is not allowed.");
        };
    }
}
