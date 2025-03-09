package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MyController {
    private final OrderService orderService;

    @GetMapping("/")
    public String index(Model model) {
        BigDecimal rubRsdRate = orderService.getRate(Currency.RUB, Currency.RSD);
        BigDecimal rsdRubRate = orderService.getRate(Currency.RSD, Currency.RUB);
        model.addAttribute("rubRsdRate", rubRsdRate);
        model.addAttribute("rsdRubRate", rsdRubRate);
        return "index";
    }
}
