package dirtymaster.freedomexchange.entity;

import io.hypersistence.utils.hibernate.type.money.CurrencyUnitType;
import io.hypersistence.utils.hibernate.type.money.MonetaryAmountType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
import java.util.UUID;

@Entity
@Table(name = "actives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Active {
    @Id
    @GeneratedValue
    private UUID id;

    private String username;

    @Column(name = "currency")
    @Type(CurrencyUnitType.class)
    private CurrencyUnit currency;

    @AttributeOverride(
            name = "amount",
            column = @Column(name = "amount")
    )
    @AttributeOverride(
            name = "currency",
            column = @Column(name = "currency", insertable = false, updatable = false)
    )
    @CompositeType(MonetaryAmountType.class)
    private MonetaryAmount monetaryAmount;

    public void subtractAmount(BigDecimal amount) {
        this.monetaryAmount = this.monetaryAmount.subtract(Money.of(amount, this.currency));
    }

    public void addAmount(BigDecimal amount) {
        this.monetaryAmount = this.monetaryAmount.add(Money.of(amount, this.currency));
    }
}
