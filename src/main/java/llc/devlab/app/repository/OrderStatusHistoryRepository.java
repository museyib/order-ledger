package llc.devlab.app.repository;

import java.util.List;

import llc.devlab.app.model.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByTransitionAtAsc(Long orderId);
}
