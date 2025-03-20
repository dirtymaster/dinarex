package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.config.OrdersConfig;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.service.ActiveService;
import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;

@Controller
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {
    private final ActiveService activeService;
    private final OrderService orderService;
    private final OrdersConfig ordersConfig;

    @GetMapping
    public String tradingPage() {
        return "redirect:/trading/RUB/RSD";
    }

    @GetMapping("/{currencyToSell}/{currencyToBuy}")
    public String tradingPage(@PathVariable Currency currencyToSell, @PathVariable Currency currencyToBuy,
                              @RequestParam(defaultValue = "MARKET") OrderType orderType,
                              Model model) {
        if (currencyToSell == currencyToBuy) {
            Currency[] values = Currency.values();
            currencyToBuy = currencyToSell == values[0] ? values[1] : values[0];
            return "redirect:/trading/%s/%s".formatted(currencyToSell, currencyToBuy);
        }
        model.addAttribute("orderType", orderType.name());
        BigDecimal orderCommission = orderType == OrderType.MARKET ?
                ordersConfig.getMarketOrderCommission() : ordersConfig.getLimitOrderCommission();
        model.addAttribute("orderCommission", orderCommission);
        model.addAttribute("currenciesToSell", Currency.values());
        model.addAttribute("activeCurrencyToSell", currencyToSell);
        model.addAttribute("activeCurrencyToBuy", currencyToBuy);
        model.addAttribute("currenciesToBuy",
                Arrays.stream(Currency.values())
                        .filter(c -> c != currencyToSell)
                        .toList());

        // Get user balances
        BigDecimal currencyToSellBalance = activeService.getActiveAmountByCurrency(currencyToSell);
        model.addAttribute("currencyToSellBalance", currencyToSellBalance);
        BigDecimal currencyToBuyBalance = activeService.getActiveAmountByCurrency(currencyToBuy);
        model.addAttribute("currencyToBuyBalance", currencyToBuyBalance);

        orderService.getOrders(currencyToSell, currencyToBuy)
                .forEach(model::addAttribute);

        return "trading";
    }

    @GetMapping("/{currency}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Currency currency) {
        BigDecimal balance = activeService.getActiveAmountByCurrency(currency);
        return ResponseEntity.ok(balance);
    }
}
