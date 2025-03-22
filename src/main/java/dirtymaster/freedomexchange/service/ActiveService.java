package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.constant.CurrencyUnitConstants;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import org.javamoney.moneta.Money;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
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
                                .blockedMonetaryAmount(BigDecimal.ZERO)
                                .build())
                .toList();
        activeRepository.saveAll(activesToSave);
    }

    public void changeActive(CurrencyUnit currency, Money amount) {
        changeActive(currency, amount, null);
    }

    public void changeActive(CurrencyUnit currency, Money amount, Money blockedAmount) {
        String username = authService.getUsernameOrNull();
        Active active = activeRepository.findByUsernameAndCurrency(username, currency);
        active.setMonetaryAmount(amount);
        if (blockedAmount != null) {
            active.setBlockedMonetaryAmount(blockedAmount);
        }
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
