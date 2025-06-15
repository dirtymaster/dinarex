package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RSD;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainPageController {
    private final OrderService orderService;

    @GetMapping("/")
    public String index(Model model) {
        orderService.getIndexPageModel().forEach(model::addAttribute);
        return "index";
    }
}
