package llc.devlab.app.repository;

import llc.devlab.app.model.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
}
