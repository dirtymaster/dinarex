package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.config.OrdersConfig;
import dirtymaster.freedomexchange.constant.CurrencyUnitConstants;
import dirtymaster.freedomexchange.dto.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradingService {
    private final ActiveService activeService;
    private final OrdersConfig ordersConfig;

    public Map<String, Object> getTradePageModel(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy, OrderType orderType) {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("orderType", orderType.name());
        BigDecimal orderCommission = orderType == OrderType.MARKET ?
                ordersConfig.getMarketOrderCommission() : ordersConfig.getLimitOrderCommission();
        modelMap.put("orderCommission", orderCommission);
        List<CurrencyUnit> availableCurrencies = CurrencyUnitConstants.availableCurrencies;
        List<String> currencyNames = availableCurrencies.stream()
                .map(CurrencyUnit::getCurrencyCode)
                .toList();
        modelMap.put("currenciesToSell", currencyNames);
        modelMap.put("activeCurrencyToSell", currencyToSell);
        modelMap.put("activeCurrencyToBuy", currencyToBuy);
        modelMap.put("currenciesToBuy",
                CurrencyUnitConstants.availableCurrencies.stream()
                        .filter(c -> !c.equals(currencyToSell))
                        .toList());

        // Get user balances
        BigDecimal currencyToSellBalance = activeService.getActiveAmountByCurrency(currencyToSell);
        modelMap.put("currencyToSellBalance", currencyToSellBalance);
        BigDecimal currencyToBuyBalance = activeService.getActiveAmountByCurrency(currencyToBuy);
        modelMap.put("currencyToBuyBalance", currencyToBuyBalance);

        return modelMap;
    }
}
