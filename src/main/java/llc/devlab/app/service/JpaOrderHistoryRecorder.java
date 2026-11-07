package llc.devlab.app.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.model.entity.OrderStatusHistory;
import llc.devlab.app.model.entity.PurchaseOrder;
import llc.devlab.app.model.enums.OrderStatus;
import llc.devlab.app.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaOrderHistoryRecorder implements OrderHistoryRecorder {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Override
    public void append(
            PurchaseOrder order,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            String actorEmail,
            String reason
    ) {
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .transitionAt(Instant.now())
                .actorEmail(actorEmail)
                .reason(reason)
                .build());
    }
}
