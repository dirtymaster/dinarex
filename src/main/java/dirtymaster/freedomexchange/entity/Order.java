package dirtymaster.freedomexchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Currency currencyToSell;

    @Enumerated(EnumType.STRING)
    private Currency currencyToBuy;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * rate = 1 currencyToSell / 1 currencyToBuy
     * 1 currencyToSell = 1 currencyToBuy * rate
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal rate;

    private boolean active;
}
