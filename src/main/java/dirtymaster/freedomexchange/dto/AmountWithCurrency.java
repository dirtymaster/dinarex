package dirtymaster.freedomexchange.dto;

import dirtymaster.freedomexchange.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AmountWithCurrency {
    private final Currency currency;
    private BigDecimal amount;

    public BigDecimal add(AmountWithCurrency other) {
        checkCurrency(other.getCurrency());
        return amount.add(other.getAmount());
    }

    public BigDecimal subtract(AmountWithCurrency other) {
        checkCurrency(other.getCurrency());
        return amount.subtract(other.getAmount());
    }

    private void checkCurrency(Currency currency) {
        if (!this.currency.equals(currency)) {
            throw new IllegalArgumentException("Currencies must be the same");
        }
    }
}
