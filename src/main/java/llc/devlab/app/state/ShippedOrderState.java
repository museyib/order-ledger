package llc.devlab.app.state;

import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class ShippedOrderState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.SHIPPED;
    }

    @Override
    public TransitionDecision transition(OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.COMPLETED) {
            return new TransitionDecision(OrderStatus.SHIPPED, OrderStatus.COMPLETED, false);
        }
        throw new ConflictException("Transition from SHIPPED to " + targetStatus + " is not allowed.");
    }
}
