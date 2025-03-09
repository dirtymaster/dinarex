package dirtymaster.freedomexchange.repository;

import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActiveRepository extends JpaRepository<Active, UUID> {
    Active findByUsernameAndCurrency(String username, Currency currency);
    boolean existsByUsername(String username);
}
