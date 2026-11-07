package llc.devlab.app.repository;

import java.util.Optional;

import llc.devlab.app.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
