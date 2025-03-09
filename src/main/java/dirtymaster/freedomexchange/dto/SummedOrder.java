package dirtymaster.freedomexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SummedOrder {
    private BigDecimal rate;
    private BigDecimal summedAmount;
}
