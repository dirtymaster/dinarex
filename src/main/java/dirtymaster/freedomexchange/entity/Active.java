package dirtymaster.freedomexchange.entity;

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

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal amount;

    private BigDecimal blockedAmount;

    public void subtractAmount(BigDecimal amount) {
        this.amount = this.amount.subtract(amount);
    }

    public void addAmount(BigDecimal amount) {
        this.amount = this.amount.add(amount);
    }
}
