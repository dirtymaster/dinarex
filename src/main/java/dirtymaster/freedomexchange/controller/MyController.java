package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.money.Monetary;
import java.math.BigDecimal;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RSD;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MyController {
    private final OrderService orderService;

    @GetMapping("/")
    public String index(Model model) {
        BigDecimal rubRsdRate = orderService.getRate(RUB, RSD);
        BigDecimal rsdRubRate = orderService.getRate(RSD, RUB);
        model.addAttribute("rubRsdRate", rubRsdRate);
        model.addAttribute("rsdRubRate", rsdRubRate);
        return "index";
    }
}
