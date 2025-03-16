package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/{currencyToSell}/{currencyToBuy}/table")
    @ResponseBody
    public Map<String, Object> getOrders(@PathVariable Currency currencyToSell, @PathVariable Currency currencyToBuy) {
        return orderService.getOrders(currencyToSell, currencyToBuy);
    }

    @PostMapping("/{currencyToSell}/{currencyToBuy}")
    public String createOrder(@PathVariable Currency currencyToSell,
                              @PathVariable Currency currencyToBuy,
                              @RequestParam BigDecimal amountToSell,
                              @RequestParam OrderType orderType,
                              @RequestParam(required = false) BigDecimal rate) {
        orderService.processOrder(currencyToSell, currencyToBuy, orderType, amountToSell, rate);
        return "redirect:/trading/" + currencyToSell + "/" + currencyToBuy;
    }
}
