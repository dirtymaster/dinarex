package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.config.OrdersConfig;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.dto.SortingType;
import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.exception.LowLiquidityException;
import dirtymaster.freedomexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RSD;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final ActiveService activeService;
    private final OrdersConfig ordersConfig;

    public Map<String, Object> getIndexPageModel() {
        Map<String, Object> modelMap = new HashMap<>();
        BigDecimal rubRsdRate = findFirstByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(RUB, RSD);
        BigDecimal rsdRubRate = findTopRateByCurrencyToSellAndCurrencyToBuy(RSD, RUB);
        modelMap.put("rubRsdRate", rubRsdRate.setScale(6, RoundingMode.HALF_UP));
        modelMap.put("rsdRubRate", rsdRubRate.setScale(6, RoundingMode.HALF_UP));
        return modelMap;
    }

    public Map<String, Object> getOrders(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy) {
        List<SummedOrder> sellOrders = get50Orders(currencyToSell, currencyToBuy, SortingType.ASC);
        List<SummedOrder> buyOrders = get50Orders(currencyToBuy, currencyToSell, SortingType.ASC);
        boolean ratesNormalized = normalizeRates(sellOrders);
        if (!ratesNormalized) {
            normalizeRates(buyOrders);
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("sellOrders", sellOrders);
        responseMap.put("buyOrders", buyOrders);
        responseMap.put("ratesNormalized", ratesNormalized);

        return responseMap;
    }

    private BigDecimal findTopRateByCurrencyToSellAndCurrencyToBuy(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy) {
        BigDecimal rate = orderRepository.findTopRateByCurrencyToSellAndCurrencyToBuy(currencyToSell, currencyToBuy).getRate();
        return rate.compareTo(BigDecimal.ONE) < 0 ? invertValue(rate) : rate;
    }

    private BigDecimal findFirstByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy) {
        BigDecimal rate = orderRepository.findFirstByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(currencyToSell, currencyToBuy).getRate();
        return rate.compareTo(BigDecimal.ONE) < 0 ? invertValue(rate) : rate;
    }

    private List<SummedOrder> get50Orders(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy, SortingType sortingType) {
        List<SummedOrder> summedOrders;
        if (sortingType == SortingType.ASC) {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(
                    currencyToSell.getCurrencyCode(), currencyToBuy.getCurrencyCode(), 50);
        } else {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                    currencyToSell.getCurrencyCode(), currencyToBuy.getCurrencyCode(), 50);
        }
        return summedOrders;
    }

    @Transactional
    public Order processOrder(CurrencyUnit currencyCurrentUserSelling, CurrencyUnit currencyCurrentUserBuying,
                              OrderType orderType, BigDecimal amountCurrentUserSelling, BigDecimal rate) {

        validateInput(orderType, rate);

        boolean isMarket = orderType == OrderType.MARKET;
        Order newOrder = createOrder(currencyCurrentUserSelling, currencyCurrentUserBuying, orderType, amountCurrentUserSelling, rate);
        validateFunds(newOrder, currencyCurrentUserSelling, amountCurrentUserSelling);

        List<Order> matchedOrders = findMatchingOrders(isMarket, currencyCurrentUserBuying, currencyCurrentUserSelling, rate);
        if (!matchedOrders.isEmpty()) {
            return matchOrders(isMarket, newOrder, matchedOrders, currencyCurrentUserSelling, amountCurrentUserSelling);
        } else if (isMarket) {
            throw new RuntimeException("Order book is empty");
        } else {
            newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
        }

        return orderRepository.save(newOrder);
    }

    private void validateInput(OrderType orderType, BigDecimal rate) {
        if (orderType != OrderType.MARKET && rate == null) {
            throw new IllegalArgumentException("Rate is required for limit order");
        }
    }

    private void validateFunds(Order newOrder, CurrencyUnit currencyCurrentUserSelling, BigDecimal amountCurrentUserSelling) {
        if (newOrder.getActiveToSell().getMonetaryAmount().isLessThan(Money.of(amountCurrentUserSelling, currencyCurrentUserSelling))) {
            throw new IllegalArgumentException("Not enough funds");
        }
    }

    private List<Order> findMatchingOrders(boolean isMarket, CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy, BigDecimal rate) {
        BigDecimal invertedRate = invertValue(rate);
        return isMarket
                ? orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, currencyToBuy, currencyToSell)
                : orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyAndRateGreaterThanEqualOrderByRateDesc(
                false, currencyToBuy, currencyToSell, invertedRate);
    }

    private Order matchOrders(boolean isMarket, Order newOrder, List<Order> orders,
                              CurrencyUnit currencyCurrentUserSelling, BigDecimal amountCurrentUserSelling) {

        List<Order> selectedOrders = selectMatchingOrders(isMarket, orders, currencyCurrentUserSelling, amountCurrentUserSelling);
        BigDecimal matchedAmount = calculateMatchedAmount(selectedOrders, currencyCurrentUserSelling);

        if (matchedAmount.compareTo(amountCurrentUserSelling) < 0) {
            processPartialMatch(newOrder, selectedOrders, matchedAmount, amountCurrentUserSelling, isMarket);
        } else if (matchedAmount.compareTo(amountCurrentUserSelling) == 0) {
            processFullMatch(newOrder, selectedOrders, amountCurrentUserSelling);
        } else {
            processExcessMatch(newOrder, selectedOrders, matchedAmount, amountCurrentUserSelling, currencyCurrentUserSelling);
        }

        orderRepository.saveAll(selectedOrders);
        return newOrder;
    }

    private List<Order> selectMatchingOrders(boolean isMarket, List<Order> orders,
                                             CurrencyUnit currencyCurrentUserSelling, BigDecimal amountToMatch) {
        List<Order> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Order order : orders) {
            if (total.compareTo(amountToMatch) >= 0) break;

            if (isMarket && !selected.isEmpty()) {
                BigDecimal rateRatio = selected.get(0).getRate().divide(order.getRate(), 20, RoundingMode.HALF_UP);
                if (rateRatio.compareTo(ordersConfig.getLowLiquidityRatio()) < 0) {
                    throw new LowLiquidityException("ordersRateRatio: %s, lowLiquidityRatio: %s".formatted(rateRatio, ordersConfig.getLowLiquidityRatio()));
                }
            }

            selected.add(order);
            total = total.add(order.getNotCompletedAmountInCurrency(currencyCurrentUserSelling));
        }

        return selected;
    }

    private BigDecimal calculateMatchedAmount(List<Order> selectedOrders, CurrencyUnit currencyCurrentUserSelling) {
        return selectedOrders.stream()
                .map(o -> o.getNotCompletedAmountInCurrency(currencyCurrentUserSelling))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void processPartialMatch(Order newOrder, List<Order> selectedOrders, BigDecimal matchedAmount,
                                     BigDecimal requestedAmount, boolean isMarket) {
        BigDecimal totalWeightedRate = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Order order : selectedOrders) {
            BigDecimal notCompleted = order.getNotCompletedAmountInCurrency(order.getCurrencyToSell());
            totalWeightedRate = totalWeightedRate.add(order.getRate().multiply(notCompleted));
            totalAmount = totalAmount.add(notCompleted);
        }

        BigDecimal averageRate = totalWeightedRate.divide(totalAmount, 20, RoundingMode.HALF_UP);

        selectedOrders.forEach(this::successfulComplete);
        newOrder.setCompletedAmountToSell(matchedAmount);
        newOrder.getActiveToBuy().addAmount(matchedAmount.multiply(averageRate));

        if (isMarket) {
            newOrder.getActiveToSell().subtractAmount(matchedAmount);
            newOrder.setCompleted(true);
        } else {
            newOrder.getActiveToSell().subtractAmount(requestedAmount);
        }
    }

    private void processFullMatch(Order newOrder, List<Order> selectedOrders, BigDecimal amountCurrentUserSelling) {
        selectedOrders.forEach(this::successfulComplete);
        successfulComplete(newOrder);
        newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
    }

    private void processExcessMatch(Order newOrder, List<Order> selectedOrders, BigDecimal matchedAmount,
                                    BigDecimal requestedAmount, CurrencyUnit currencyCurrentUserSelling) {

        Order lastOrder = selectedOrders.get(selectedOrders.size() - 1);
        BigDecimal excess = matchedAmount.subtract(requestedAmount);

        lastOrder.setNotCompletedAmountInCurrency(excess, currencyCurrentUserSelling);
        lastOrder.getActiveToBuy().addAmount(excess);

        IntStream.range(0, selectedOrders.size() - 1)
                .forEach(i -> successfulComplete(selectedOrders.get(i)));

        successfulComplete(newOrder);
        newOrder.getActiveToSell().subtractAmount(requestedAmount);
    }


    public Order createOrder(CurrencyUnit currencyToSell, CurrencyUnit currencyToBuy, OrderType orderType,
                             BigDecimal totalAmountToBuy, BigDecimal rate) {
        String username = authService.getUsernameOrNull();
        if (username == null) {
            //TODO 403
            throw new RuntimeException();
        }
        Order order = new Order();
        order.setCreator(username);
        order.setCurrencyToSell(currencyToSell);
        order.setCurrencyToBuy(currencyToBuy);
        order.setTotalAmountToSell(totalAmountToBuy);
        order.setCompletedAmountToSell(BigDecimal.ZERO);
        order.setCompleted(false);
        order.setOrderType(orderType);
        if (orderType == OrderType.LIMIT) {
            order.setRate(rate);
        }
        order.setCreatedAt(LocalDateTime.now());
        Active activeCurrentUserSelling = activeService.findByCurrency(currencyToSell);
        order.setActiveToSell(activeCurrentUserSelling);
        Active activeCurrentUserBuying = activeService.findByCurrency(currencyToBuy);
        order.setActiveToBuy(activeCurrentUserBuying);
        return order;
    }

    private void successfulComplete(Order order) {
        BigDecimal notCompletedAmount = order.getNotCompletedAmountInCurrency(order.getCurrencyToBuy());
        order.setCompleted(true);
        order.setCompletedAmountToSell(order.getTotalAmountToSell());
        order.setCompletedAt(LocalDateTime.now());

        order.getActiveToBuy().addAmount(notCompletedAmount);
    }

    private boolean normalizeRates(List<SummedOrder> summedOrders) {
        if (summedOrders.get(0).getRate().compareTo(BigDecimal.ONE) < 0) {
            summedOrders.forEach(summedOrder ->
                    summedOrder.setRate(invertValue(summedOrder.getRate())));
            return true;
        }
        return false;
    }

    private BigDecimal invertValue(BigDecimal value) {
        return BigDecimal.ONE.divide(value, 20, RoundingMode.HALF_UP);
    }
}
