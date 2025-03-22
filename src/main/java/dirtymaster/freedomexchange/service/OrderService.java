package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.config.OrdersConfig;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.dto.SortingType;
import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.exception.LowLiquidityException;
import dirtymaster.freedomexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.Monetary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final ActiveService activeService;
    private final OrdersConfig ordersConfig;

    public Map<String, Object> getOrders(Currency currencyToSell, Currency currencyToBuy) {
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
        return orderRepository.findTopRateByCurrencyToSellAndCurrencyToBuy(Monetary.getCurrency(first.name()), Monetary.getCurrency(second.name())).getRate();
    }

    private List<SummedOrder> get50Orders(Currency currencyToSell, Currency currencyToBuy, SortingType sortingType) {
        List<SummedOrder> summedOrders;
        if (sortingType == SortingType.ASC) {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(
                    currencyToSell.name(), currencyToBuy.name(), 50);
        } else {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                    currencyToSell.name(), currencyToBuy.name(), 50);
        }
        return summedOrders;
    }

    @Transactional
    public Order processOrder(Currency currencyCurrentUserSelling, Currency currencyCurrentUserBuying, OrderType orderType,
                            BigDecimal amountCurrentUserSelling, BigDecimal rate) {
        boolean isMarket = orderType == OrderType.MARKET;
        if (!isMarket && rate == null) {
            throw new IllegalArgumentException("Rate is required for limit order");
        }

        Order newOrder = createOrder(currencyCurrentUserSelling, currencyCurrentUserBuying, orderType, amountCurrentUserSelling, rate);
        if (newOrder.getActiveToSell().getAmount().compareTo(amountCurrentUserSelling) < 0) {
            throw new IllegalArgumentException("Not enough funds");
        }

        BigDecimal invertedRate = invertValue(rate);
        List<Order> orders = isMarket ?
                orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                        false, Monetary.getCurrency(currencyCurrentUserBuying.name()), Monetary.getCurrency(currencyCurrentUserSelling.name()))
                : orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyAndRateGreaterThanEqualOrderByRateDesc(
                        false, Monetary.getCurrency(currencyCurrentUserBuying.name()), Monetary.getCurrency(currencyCurrentUserSelling.name()), invertedRate);
        if (!orders.isEmpty()) {
            // Сумма в существующих ордерах в валюте, которую продает текущий пользователь.
            BigDecimal summedAmountInCurrencyCurrentUserSelling = BigDecimal.ZERO;
            List<Order> selectedOrders = new ArrayList<>();
            for (Order order : orders) {
                if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) >= 0) {
                    break;
                }
                if (isMarket && !selectedOrders.isEmpty()) {
                    BigDecimal ordersRateRatio = selectedOrders.getFirst().getRate().divide(order.getRate(), 20, RoundingMode.HALF_UP);
                    if (ordersRateRatio.compareTo(ordersConfig.getLowLiquidityRatio()) < 0) {
                        throw new LowLiquidityException("ordersRateRatio: %s, lowLiquidityRatio: %s".formatted(ordersRateRatio, ordersConfig.getLowLiquidityRatio()));
                    }
                }

                selectedOrders.add(order);
                BigDecimal notCompletedInAmountCurrentUserSelling = order.getNotCompletedAmountInCurrency(currencyCurrentUserSelling);
                summedAmountInCurrencyCurrentUserSelling = summedAmountInCurrencyCurrentUserSelling.add(notCompletedInAmountCurrentUserSelling);
            }

            // Если сумма в существующих ордерах меньше, чем в новом
            if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) < 0) {
                BigDecimal totalWeightedRate = BigDecimal.ZERO;
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Order order : selectedOrders) {
                    BigDecimal notCompletedAmount = order.getNotCompletedAmountInCurrency(order.getCurrencyToSell());
                    totalWeightedRate = totalWeightedRate.add(order.getRate().multiply(notCompletedAmount));
                    totalAmount = totalAmount.add(notCompletedAmount);
                }
                BigDecimal averageRate = totalWeightedRate.divide(totalAmount, 20, RoundingMode.HALF_UP);
                selectedOrders.forEach(this::successfulComplete);

                newOrder.setCompletedAmountToSell(summedAmountInCurrencyCurrentUserSelling);
                newOrder.getActiveToBuy().addAmount(summedAmountInCurrencyCurrentUserSelling.multiply(averageRate));

                if (isMarket) {
                    newOrder.getActiveToSell().subtractAmount(summedAmountInCurrencyCurrentUserSelling);
                    newOrder.setCompleted(true);
                } else {
                    newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
                }
            // Если сумма в существующих ордерах точно равна сумме в новом
            } else if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) == 0) {
                selectedOrders.forEach(this::successfulComplete);
                successfulComplete(newOrder);

                newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
            // Если сумма в существующих ордерах превышает сумму в новом
            } else {
                Order lastOrder = selectedOrders.getLast();
                BigDecimal amountMatchedForLastOrder = summedAmountInCurrencyCurrentUserSelling.subtract(amountCurrentUserSelling);
                lastOrder.setNotCompletedAmountInCurrency(amountMatchedForLastOrder, currencyCurrentUserSelling);
                lastOrder.getActiveToBuy().addAmount(amountMatchedForLastOrder);

                IntStream.range(0, selectedOrders.size() - 1)
                        .forEach(i -> successfulComplete(selectedOrders.get(i)));

                successfulComplete(newOrder);

                newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
            }
            orderRepository.saveAll(selectedOrders);
        } else if (isMarket) {
            throw new RuntimeException("Order book is empty");
        } else {
            newOrder.getActiveToSell().subtractAmount(amountCurrentUserSelling);
        }
        return orderRepository.save(newOrder);
    }

    public Order createOrder(Currency currencyToSell, Currency currencyToBuy, OrderType orderType,
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
        if (summedOrders.getFirst().getRate().compareTo(BigDecimal.ONE) < 0) {
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
