package llc.devlab.app.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import llc.devlab.app.model.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Page<PurchaseOrder> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "lines", "lines.product"})
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<PurchaseOrder> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"owner", "lines", "lines.product"})
    @Query("select o from PurchaseOrder o where o.id = :id and o.owner.id = :ownerId")
    Optional<PurchaseOrder> findDetailedByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"owner", "lines", "lines.product"})
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<PurchaseOrder> findDetailedByIdForUpdate(@Param("id") Long id);
}
