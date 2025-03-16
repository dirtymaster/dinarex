package dirtymaster.freedomexchange.entity;

import dirtymaster.freedomexchange.dto.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Все ордера, которые находятся в таблице и completed = false, уже отминусованы в таблице actives
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Создатель ордера
     */
    private String creator;

    /**
     * Валюта, которую пользователь хочет продать
     */
    @Enumerated(EnumType.STRING)
    private Currency currencyToSell;

    /**
     * Валюта, которую пользователь хочет купить
     */
    @Enumerated(EnumType.STRING)
    private Currency currencyToBuy;

    /**
     * Количество валюты, которую пользователь хочет продать
     */
    @Column(precision = 19, scale = 6)
    private BigDecimal totalAmountToSell;

    /**
     * Количество валюты, которая уже продана в рамках ордера
     */
    @Column(precision = 19, scale = 6)
    private BigDecimal completedAmountToSell;

    /**
     * Признак завершенности ордера
     */
    private boolean completed;

    /**
     * Тип ордера (MARKET, LIMIT)
     */
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    /**
     * rate = 1 currencyToSell / 1 currencyToBuy
     * 1 currencyToSell = 1 currencyToBuy * rate
     * 1 currencyToBuy = 1 currencyToBuy / rate
     */
    @Column(precision = 19, scale = 6)
    private BigDecimal rate;

    /**
     * Время и дата создания ордера
     */
    private LocalDateTime createdAt;

    /**
     * Время и дата завершения ордера
     */
    private LocalDateTime completedAt;

    public BigDecimal getTotalAmountInCurrency(Currency currency) {
        if (currency == currencyToSell) {
            return totalAmountToSell;
        } else if (currency == currencyToBuy) {
            return getTotalAmountToBuy();
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public BigDecimal getCompletedAmountInCurrency(Currency currency) {
        if (currency == currencyToSell) {
            return completedAmountToSell;
        } else if (currency == currencyToBuy) {
            return getCompletedAmountToBuy();
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public BigDecimal getNotCompletedAmountInCurrency(Currency currency) {
        if (currency == currencyToSell) {
            return getNotCompletedAmountToSell();
        } else if (currency == currencyToBuy) {
            return getNotCompletedAmountToBuy();
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public void setCompletedAmount(BigDecimal completedAmount, Currency currency) {
        if (currency == currencyToSell) {
            completedAmountToSell = completedAmount;
        } else if (currency == currencyToBuy) {
            completedAmountToSell = completedAmount.multiply(rate);
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public void addCompletedAmount(BigDecimal completedAmountToAdd, Currency currency) {
        BigDecimal completedAmountToSell;
        if (currency == currencyToSell) {
            completedAmountToSell = this.completedAmountToSell.add(completedAmountToAdd);
        } else if (currency == currencyToBuy) {
            completedAmountToSell = this.completedAmountToSell.add(completedAmountToAdd.multiply(rate));
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
        if (completedAmountToSell.compareTo(this.totalAmountToSell) > 0) {
            throw new IllegalArgumentException("Completed amount cannot be greater than total amount");
        }
        this.completedAmountToSell = completedAmountToSell;
    }

    public void setNotCompletedAmount(BigDecimal notCompletedAmount, Currency currency) {
        if (currency == currencyToSell) {
            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmount);
        } else if (currency == currencyToBuy) {
            BigDecimal notCompletedAmountToSell = notCompletedAmount.divide(rate, 6, RoundingMode.CEILING);
            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmountToSell);
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public void complete() {
        completed = true;
        completedAmountToSell = totalAmountToSell;
    }

    private BigDecimal getNotCompletedAmountToSell() {
        return totalAmountToSell.subtract(completedAmountToSell);
    }

    private void setNotCompletedAmountToSell(BigDecimal notCompletedAmountToSell) {
        this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmountToSell);
    }

    private BigDecimal getNotCompletedAmountToBuy() {
        return getNotCompletedAmountToSell().multiply(rate);
    }

    private void setNotCompletedAmountToBuy(BigDecimal notCompletedAmountToBuy) {
        BigDecimal notCompletedAmountToSell = notCompletedAmountToBuy.multiply(rate);
        setNotCompletedAmountToSell(notCompletedAmountToSell);
    }

    private BigDecimal getTotalAmountToBuy() {
        return totalAmountToSell.divide(rate, 6, RoundingMode.CEILING);
    }

    private BigDecimal getCompletedAmountToBuy() {
        return completedAmountToSell.divide(rate, 6, RoundingMode.CEILING);
    }
}
