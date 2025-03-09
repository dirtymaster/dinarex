package dirtymaster.freedomexchange.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {
    private BigDecimal amountToSell;
    private OrderType orderType;
    private BigDecimal rate;
}
