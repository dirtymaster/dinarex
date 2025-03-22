package dirtymaster.freedomexchange.repository;

import dirtymaster.freedomexchange.entity.Active;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.money.CurrencyUnit;
import java.util.UUID;

public interface ActiveRepository extends JpaRepository<Active, UUID> {
    Active findByUsernameAndCurrency(String username, CurrencyUnit currency);
    boolean existsByUsername(String username);
}
