package llc.devlab.app.repository;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;
import llc.devlab.app.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    //Optimistic locking is provided by @Version
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id asc")
    List<Product> findAllByIdsForUpdate(@Param("ids") Collection<Long> ids);
}
