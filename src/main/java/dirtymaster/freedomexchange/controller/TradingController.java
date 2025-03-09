package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.config.CommissionConfig;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.dto.TradeRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trading")
@RequiredArgsConstructor
public class TradingController {
    private final ActiveService activeService;
    private final OrderService orderService;
    private final CommissionConfig commissionConfig;

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
                commissionConfig.getMarketOrderCommission() : commissionConfig.getLimitOrderCommission();
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

        List<SummedOrder> sellOrders = orderService.get50Orders(currencyToSell, currencyToBuy, true);
        List<SummedOrder> buyOrders = orderService.get50Orders(currencyToBuy, currencyToSell, true);
        boolean valuesInverted = orderService.invertValues(sellOrders);
        if (!valuesInverted) {
            orderService.invertValues(buyOrders);
        }
        model.addAttribute("sellOrders", sellOrders);
        model.addAttribute("buyOrders", buyOrders);
        model.addAttribute("valuesInverted", valuesInverted);

        return "trading";
    }

    @GetMapping("/{currencyToSell}/{currencyToBuy}/orders")
    @ResponseBody
    public Map<String, List<SummedOrder>> getOrders(@PathVariable Currency currencyToSell,
                                                    @PathVariable Currency currencyToBuy) {
        List<SummedOrder> sellOrders = orderService.get50Orders(currencyToSell, currencyToBuy, true);
        List<SummedOrder> buyOrders = orderService.get50Orders(currencyToBuy, currencyToSell, true);
        boolean valuesInverted = orderService.invertValues(sellOrders);
        if (!valuesInverted) {
            orderService.invertValues(buyOrders);
        }

        Map<String, List<SummedOrder>> response = new HashMap<>();
        response.put("sellOrders", sellOrders);
        response.put("buyOrders", buyOrders);

        return response;
    }

    @GetMapping("/{currency}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Currency currency) {
        BigDecimal balance = activeService.getActiveAmountByCurrency(currency);
        return ResponseEntity.ok(balance);
    }

//    @PostMapping("/{currencyToSell}/{currencyToBuy}")
//    public ResponseEntity<String> executeTrade(@PathVariable String currencyToSell, @PathVariable String currencyToBuy,
//                                               @RequestBody TradeRequest tradeRequest) {
//        if (tradeRequest.getOrderType() == OrderType.MARKET) {
//
//        }
//    }
}
