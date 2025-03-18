package dirtymaster.freedomexchange.entity;

import dirtymaster.freedomexchange.dto.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
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
    @Column(name = "currency_to_sell")
    private Currency currencyToSell;

    /**
     * Валюта, которую пользователь хочет купить
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency_to_buy")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "creator", referencedColumnName = "username", insertable = false, updatable = false),
            @JoinColumn(name = "currency_to_sell", referencedColumnName = "currency", insertable = false, updatable = false)
    })
    private Active activeToSell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "creator", referencedColumnName = "username", insertable = false, updatable = false),
            @JoinColumn(name = "currency_to_buy", referencedColumnName = "currency", insertable = false, updatable = false)
    })
    private Active activeToBuy;

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

    public BigDecimal getNotCompletedAmountInCurrency(Currency currency) {
        if (currency == currencyToSell) {
            return getNotCompletedAmountToSell();
        } else if (currency == currencyToBuy) {
            return getNotCompletedAmountToBuy();
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public void setNotCompletedAmountInCurrency(BigDecimal notCompletedAmount, Currency currency) {
        if (currency == currencyToSell) {
            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmount);
        } else if (currency == currencyToBuy) {
            BigDecimal notCompletedAmountToSell = notCompletedAmount.multiply(rate);
            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmountToSell);
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    private BigDecimal getNotCompletedAmountToSell() {
        return totalAmountToSell.subtract(completedAmountToSell);
    }

    private BigDecimal getNotCompletedAmountToBuy() {
        return getNotCompletedAmountToSell().divide(rate, 6, RoundingMode.CEILING);
    }
}
