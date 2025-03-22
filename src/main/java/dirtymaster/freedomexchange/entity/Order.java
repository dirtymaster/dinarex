package dirtymaster.freedomexchange.entity;

import dirtymaster.freedomexchange.dto.OrderType;
import io.hypersistence.utils.hibernate.type.money.CurrencyUnitType;
import io.hypersistence.utils.hibernate.type.money.MonetaryAmountType;
import jakarta.persistence.AttributeOverride;
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
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
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
//    @Enumerated(EnumType.STRING)
    @Column(name = "currency_to_sell")
    @Type(CurrencyUnitType.class)
    private CurrencyUnit currencyToSell;

    /**
     * Валюта, которую пользователь хочет купить
     */
//    @Enumerated(EnumType.STRING)
    @Column(name = "currency_to_buy")
    @Type(CurrencyUnitType.class)
    private CurrencyUnit currencyToBuy;

    /**
     * Количество валюты, которую пользователь хочет продать
     */
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "total_amount_to_sell")
    )
    @AttributeOverride(
            name = "currency",
            column = @Column(name = "currency_to_sell", insertable=false, updatable=false)
    )
    @CompositeType(MonetaryAmountType.class)
    private MonetaryAmount totalAmountToSell;

    /**
     * Количество валюты, которая уже продана в рамках ордера
     */
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "completed_amount_to_sell")
    )
    @AttributeOverride(
            name = "currency",
            column = @Column(name = "currency_to_sell", insertable=false, updatable=false)
    )
    @CompositeType(MonetaryAmountType.class)
    private MonetaryAmount completedAmountToSell;

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

    public void setTotalAmountToSell(MonetaryAmount totalAmountToSell) {
        this.totalAmountToSell = totalAmountToSell;
    }

    public void setCompletedAmountToSell(MonetaryAmount completedAmountToSell) {
        this.completedAmountToSell = completedAmountToSell;
    }

    public void setCompletedAmountToSell(BigDecimal completedAmountToSell) {
        this.completedAmountToSell = Money.of(completedAmountToSell, currencyToSell);
    }

    public void setTotalAmountToSell(BigDecimal totalAmountToSell) {
        this.totalAmountToSell = Money.of(totalAmountToSell, currencyToSell);
    }

    public BigDecimal getNotCompletedAmountInCurrency(CurrencyUnit currency) {
        if (currency.equals(currencyToSell)) {
            return getNotCompletedAmountToSell();
        } else if (currency.equals(currencyToBuy)) {
            return getNotCompletedAmountToBuy();
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    public void setNotCompletedAmountInCurrency(BigDecimal notCompletedAmount, CurrencyUnit currency) {
        if (currency.equals(currencyToSell)) {
//            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmount);
            completedAmountToSell = totalAmountToSell.subtract(Money.of(notCompletedAmount, currencyToSell));
        } else if (currency.equals(currencyToBuy)) {
            BigDecimal notCompletedAmountToSell = notCompletedAmount.multiply(rate);
//            this.completedAmountToSell = totalAmountToSell.subtract(notCompletedAmountToSell);
            completedAmountToSell = totalAmountToSell.subtract(Money.of(notCompletedAmountToSell, currencyToSell));
        } else {
            throw new IllegalArgumentException("Unknown currency: " + currency);
        }
    }

    private BigDecimal getNotCompletedAmountToSell() {
//        return totalAmountToSell.subtract(completedAmountToSell);
        return ((Money) totalAmountToSell.subtract(completedAmountToSell)).getNumberStripped();
    }

    private BigDecimal getNotCompletedAmountToBuy() {
        return getNotCompletedAmountToSell().divide(rate, 20, RoundingMode.HALF_UP);
    }
}
