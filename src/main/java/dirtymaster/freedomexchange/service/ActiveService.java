package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class ActiveService {
    private final ActiveRepository activeRepository;
    private final AuthService authService;

    public ActiveService(ActiveRepository activeRepository, @Lazy AuthService authService) {
        this.activeRepository = activeRepository;
        this.authService = authService;
    }

    public BigDecimal getActiveAmountByCurrency(Currency currency) {
        String username = authService.getUsernameOrNull();
        if (username != null) {
            return activeRepository.findByUsernameAndCurrency(username, currency).getAmount();
        }
        return BigDecimal.ZERO;
    }

    public Active findByUsernameAndCurrency(String username, Currency currency) {
        return activeRepository.findByUsernameAndCurrency(username, currency);
    }

    public void createAllZeroActives(String username) {
        if (activeRepository.existsByUsername(username)) {
            throw new IllegalStateException("User already has actives");
        }
        List<Active> activesToSave = Arrays.stream(Currency.values())
                .map(currency ->
                        Active.builder()
                                .username(username)
                                .currency(currency)
                                .amount(BigDecimal.ZERO)
                                .blockedAmount(BigDecimal.ZERO)
                                .build())
                .toList();
        activeRepository.saveAll(activesToSave);
    }

    public void changeActive(Currency currency, BigDecimal amount) {
        changeActive(currency, amount, null);
    }

    public void changeActive(Currency currency, BigDecimal amount, BigDecimal blockedAmount) {
        String username = authService.getUsernameOrNull();
        Active active = activeRepository.findByUsernameAndCurrency(username, currency);
        active.setAmount(amount);
        if (blockedAmount != null) {
            active.setBlockedAmount(blockedAmount);
        }
        activeRepository.save(active);
    }

    public Active findByCurrency(Currency currency) {
        String username = authService.getUsernameOrNull();
        return activeRepository.findByUsernameAndCurrency(username, currency);
    }

    public void saveActive(Active active) {
        activeRepository.save(active);
    }
}
