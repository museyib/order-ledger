package llc.devlab.app.state;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import llc.devlab.app.exception.BadRequestException;
import llc.devlab.app.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderStateResolver {

    private final List<OrderState> orderStates;
    private final Map<OrderStatus, OrderState> statesByStatus = new EnumMap<>(OrderStatus.class);

    public OrderStateResolver(List<OrderState> orderStates) {
        this.orderStates = orderStates;
    }

    @PostConstruct
    void init() {
        orderStates.forEach(state -> statesByStatus.put(state.status(), state));
    }

    public TransitionDecision transition(OrderStatus currentStatus, OrderStatus targetStatus) {
        OrderState state = statesByStatus.get(currentStatus);
        if (state == null) {
            throw new BadRequestException("Unsupported current order state: " + currentStatus);
        }
        return state.transition(targetStatus);
    }
}
