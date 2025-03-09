package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActiveService {
    private final ActiveRepository activeRepository;

    public BigDecimal getActiveAmountByCurrency(Currency currency) {
        String username = getUsernameOrNull();
        if (!"anonymousUser".equals(username)) {
            return activeRepository.findByUsernameAndCurrency(username, currency).getAmount();
        }
        return BigDecimal.ZERO;
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

    public void changeActive(String username, Currency currency, BigDecimal amount, BigDecimal blockedAmount) {
        Active active = activeRepository.findByUsernameAndCurrency(username, currency);
        active.setAmount(amount);
        active.setBlockedAmount(blockedAmount);
        activeRepository.save(active);
    }

    private String getUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}
