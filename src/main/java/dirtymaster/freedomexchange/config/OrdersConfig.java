package dirtymaster.freedomexchange.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties("orders")
@Data
public class OrdersConfig {
    private BigDecimal marketOrderCommission;
    private BigDecimal limitOrderCommission;
    private BigDecimal lowLiquidityRatio;
}
