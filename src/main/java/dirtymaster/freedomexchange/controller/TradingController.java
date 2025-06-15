package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.constant.CurrencyUnitConstants;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.service.ActiveService;
import dirtymaster.freedomexchange.service.TradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {
    private final TradingService tradingService;
    private final ActiveService activeService;

    @GetMapping
    public String tradingPage() {
        return "redirect:/trading/RUB/RSD";
    }

    @GetMapping("/{currencyToSell}/{currencyToBuy}")
    public String tradingPage(@PathVariable CurrencyUnit currencyToSell, @PathVariable CurrencyUnit currencyToBuy,
                              @RequestParam(defaultValue = "MARKET") OrderType orderType,
                              Model model) {
        List<CurrencyUnit> availableCurrencies = CurrencyUnitConstants.availableCurrencies;
        if (currencyToSell == currencyToBuy) {
            currencyToBuy = currencyToSell.getCurrencyCode().equals(availableCurrencies.get(0).getCurrencyCode())
                    ? availableCurrencies.get(1) : availableCurrencies.get(0);
            return "redirect:/trading/%s/%s".formatted(currencyToSell, currencyToBuy);
        }
        tradingService.getTradePageModel(currencyToSell, currencyToBuy, orderType).forEach(model::addAttribute);

        return "trading";
    }

    @GetMapping("/{currency}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable CurrencyUnit currency) {
        BigDecimal balance = activeService.getActiveAmountByCurrency(currency);
        return ResponseEntity.ok(balance);
    }
}
