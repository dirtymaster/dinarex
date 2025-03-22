package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.constant.CurrencyUnitConstants;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import org.javamoney.moneta.Money;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ActiveService {
    private final ActiveRepository activeRepository;
    private final AuthService authService;

    public ActiveService(ActiveRepository activeRepository, @Lazy AuthService authService) {
        this.activeRepository = activeRepository;
        this.authService = authService;
    }

    public BigDecimal getActiveAmountByCurrency(CurrencyUnit currency) {
        String username = authService.getUsernameOrNull();
        if (username != null) {
            return ((Money) activeRepository.findByUsernameAndCurrency(username, currency).getMonetaryAmount()).getNumberStripped();
        }
        return BigDecimal.ZERO;
    }

    public Active findByUsernameAndCurrency(String username, CurrencyUnit currency) {
        return activeRepository.findByUsernameAndCurrency(username, currency);
    }

    public void createAllZeroActives(String username) {
        if (activeRepository.existsByUsername(username)) {
            throw new IllegalStateException("User already has actives");
        }
        List<Active> activesToSave = CurrencyUnitConstants.availableCurrencies.stream()
                .map(currency ->
                        Active.builder()
                                .username(username)
                                .currency(currency)
                                .monetaryAmount(Money.zero(currency))
                                .build())
                .toList();
        activeRepository.saveAll(activesToSave);
    }

    public void changeActive(Money amount) {
        String username = authService.getUsernameOrNull();
        Active active = activeRepository.findByUsernameAndCurrency(username, amount.getCurrency());
        active.setMonetaryAmount(amount);
        activeRepository.save(active);
    }

    public Active findByCurrency(CurrencyUnit currency) {
        String username = authService.getUsernameOrNull();
        return activeRepository.findByUsernameAndCurrency(username, currency);
    }

    public void save(Active active) {
        activeRepository.save(active);
    }
}
