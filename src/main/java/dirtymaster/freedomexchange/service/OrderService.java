package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public BigDecimal getRate(Currency currencyToSell, Currency currencyToBuy) {
        Currency first;
        Currency second;
        if (currencyToSell.compareTo(currencyToBuy) < 0) {
            first = currencyToSell;
            second = currencyToBuy;
        } else {
            first = currencyToBuy;
            second = currencyToSell;
        }
        return orderRepository.findTopRateByCurrencyToSellAndCurrencyToBuy(first, second).getRate();
    }

    public List<SummedOrder> get50Orders(Currency currencyToSell, Currency currencyToBuy, boolean asc) {
        List<SummedOrder> summedOrders;
        if (asc) {
            summedOrders = orderRepository.findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateAsc(currencyToSell, currencyToBuy, 50);
        } else {
            summedOrders = orderRepository.findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateDesc(currencyToSell, currencyToBuy, 50);
        }
        return summedOrders;
    }

    public boolean invertValues(List<SummedOrder> summedOrders) {
        if (summedOrders.getFirst().getRate().compareTo(BigDecimal.ONE) < 0) {
            summedOrders.forEach(summedOrder ->
                    summedOrder.setRate(BigDecimal.ONE.divide(summedOrder.getRate(), 6, RoundingMode.CEILING)));
            return true;
        }
        return false;
    }
}
