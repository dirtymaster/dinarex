package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RSD;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainPageController {
    private final OrderService orderService;

    @GetMapping("/")
    public String index(Model model) {
        BigDecimal rubRsdRate = orderService.findFirstByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(RUB, RSD);
        BigDecimal rsdRubRate = orderService.findTopRateByCurrencyToSellAndCurrencyToBuy(RSD, RUB);
        model.addAttribute("rubRsdRate", rubRsdRate.setScale(6, RoundingMode.HALF_UP));
        model.addAttribute("rsdRubRate", rsdRubRate.setScale(6, RoundingMode.HALF_UP));
        return "index";
    }
}
