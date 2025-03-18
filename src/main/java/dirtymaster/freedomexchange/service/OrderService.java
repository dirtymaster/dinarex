package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.exception.LowLiquidityException;
import dirtymaster.freedomexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Map<String, Object> getOrders(Currency currencyToSell, Currency currencyToBuy) {
        List<SummedOrder> sellOrders = get50Orders(currencyToSell, currencyToBuy, true);
        List<SummedOrder> buyOrders = get50Orders(currencyToBuy, currencyToSell, true);
        boolean valuesInverted = invertValues(sellOrders);
        if (!valuesInverted) {
            invertValues(buyOrders);
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("sellOrders", sellOrders);
        responseMap.put("buyOrders", buyOrders);
        responseMap.put("valuesInverted", valuesInverted);

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
        return orderRepository.findTopRateByCurrencyToSellAndCurrencyToBuy(first, second).getRate();
    }

    private List<SummedOrder> get50Orders(Currency currencyToSell, Currency currencyToBuy, boolean asc) {
        List<SummedOrder> summedOrders;
        if (asc) {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateAsc(
                    currencyToSell.name(), currencyToBuy.name(), 50);
        } else {
            summedOrders = orderRepository.findTop50SummedByCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                    currencyToSell.name(), currencyToBuy.name(), 50);
        }
        return summedOrders;
    }

    /**
     * При создании ордера, у пользователя отнимаются активы на полную сумму, которую он продает.
     * И
     *
     * @param currencyCurrentUserSelling
     * @param currencyCurrentUserBuying
     * @param orderType
     * @param amountCurrentUserSelling
     * @param rate
     * @return
     */
    @Transactional
    public Order processOrder(Currency currencyCurrentUserSelling, Currency currencyCurrentUserBuying, OrderType orderType,
                            BigDecimal amountCurrentUserSelling, BigDecimal rate) {
        boolean isMarket = orderType == OrderType.MARKET;
        if (!isMarket && rate == null) {
            throw new IllegalArgumentException("Rate is required for limit order");
        }

        Active activeCurrentUserSelling = activeService.findByCurrency(currencyCurrentUserSelling);
        if (activeCurrentUserSelling.getAmount().compareTo(amountCurrentUserSelling) < 0) {
            throw new IllegalArgumentException("Not enough funds");
        }

        Order newOrder = createOrder(currencyCurrentUserSelling, currencyCurrentUserBuying, orderType, amountCurrentUserSelling, rate);

        BigDecimal invertedRate = invertValue(rate);
        List<Order> orders = isMarket ?
                orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                        false, currencyCurrentUserBuying, currencyCurrentUserSelling)
                : orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyAndRateGreaterThanEqualOrderByRateDesc(
                        false, currencyCurrentUserBuying, currencyCurrentUserSelling, invertedRate);
        if (!orders.isEmpty()) {
            // Сумма в существующих ордерах в валюте, которую продает текущий пользователь.
            BigDecimal summedAmountInCurrencyCurrentUserSelling = BigDecimal.ZERO;
            List<Order> selectedOrders = new ArrayList<>();
            for (Order order : orders) {
                if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) >= 0) {
                    break;
                }
                if (isMarket && !selectedOrders.isEmpty()) {
                    //TODO делать первое на второе или второе на первое?
                    BigDecimal ordersRateRatio = selectedOrders.getFirst().getRate().divide(order.getRate(), 6, RoundingMode.CEILING);
                    //TODO вынести в properties
                    if (ordersRateRatio.compareTo(BigDecimal.valueOf(0.8)) < 0) {
                        throw new LowLiquidityException("Low liquidity");
                    }
                }

                selectedOrders.add(order);
                BigDecimal notCompletedInAmountCurrentUserSelling = order.getNotCompletedAmountInCurrency(currencyCurrentUserSelling);
                summedAmountInCurrencyCurrentUserSelling = summedAmountInCurrencyCurrentUserSelling.add(notCompletedInAmountCurrentUserSelling);
            }

            // Если сумма в существующих ордерах меньше, чем в новом
            if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) < 0) {
                selectedOrders.forEach(this::successfulComplete);

                BigDecimal totalWeightedRate = BigDecimal.ZERO;
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Order order : selectedOrders) {
                    BigDecimal notCompletedAmount = order.getNotCompletedAmountInCurrency(order.getCurrencyToSell());
                    totalWeightedRate = totalWeightedRate.add(order.getRate().multiply(notCompletedAmount));
                    totalAmount = totalAmount.add(notCompletedAmount);
                }
                BigDecimal averageRate = totalWeightedRate.divide(totalAmount, 6, RoundingMode.HALF_UP);

                if (isMarket) {
                    newOrder.setCompleted(true);
                }
                newOrder.setCompletedAmountToSell(summedAmountInCurrencyCurrentUserSelling);
                Active activeCurrentUserBuying = activeService.findByCurrency(currencyCurrentUserBuying);
                activeCurrentUserBuying.addAmount(summedAmountInCurrencyCurrentUserSelling.multiply(averageRate));
                activeService.save(activeCurrentUserBuying);

                activeCurrentUserSelling.subtractAmount(summedAmountInCurrencyCurrentUserSelling);
            // Если сумма в существующих ордерах точно равна сумме в новом
            } else if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) == 0) {
                selectedOrders.forEach(this::successfulComplete);
                successfulComplete(newOrder);

                activeCurrentUserSelling.subtractAmount(amountCurrentUserSelling);
            // Если сумма в существующих ордерах превышает сумму в новом
            } else {
                Order lastOrder = selectedOrders.getLast();
                BigDecimal amountMatchedForLastOrder = summedAmountInCurrencyCurrentUserSelling.subtract(amountCurrentUserSelling);
                lastOrder.setNotCompletedAmount(amountMatchedForLastOrder, currencyCurrentUserSelling);
                Active active = activeService.findByUsernameAndCurrency(lastOrder.getCreator(), lastOrder.getCurrencyToBuy());
                active.addAmount(amountMatchedForLastOrder);
                activeService.save(active);

                IntStream.range(0, selectedOrders.size() - 1)
                        .forEach(i -> successfulComplete(selectedOrders.get(i)));

                successfulComplete(newOrder);

                activeCurrentUserSelling.subtractAmount(amountCurrentUserSelling);
            }
            orderRepository.saveAll(selectedOrders);
        } else if (isMarket) {
            throw new RuntimeException("Order book is empty");
        } else {
            activeCurrentUserSelling.subtractAmount(amountCurrentUserSelling);
        }
        orderRepository.save(newOrder);
        activeService.save(activeCurrentUserSelling);
        return newOrder;
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
        return order;
    }

    private void successfulComplete(Order order) {
        BigDecimal notCompletedAmount = order.getNotCompletedAmountInCurrency(order.getCurrencyToBuy());
        order.setCompleted(true);
        order.setCompletedAmountToSell(order.getTotalAmountToSell());
        order.setCompletedAt(LocalDateTime.now());

        Active active = activeService.findByUsernameAndCurrency(order.getCreator(), order.getCurrencyToBuy());
        active.addAmount(notCompletedAmount);
        activeService.save(active);
    }

    private boolean invertValues(List<SummedOrder> summedOrders) {
        if (summedOrders.getFirst().getRate().compareTo(BigDecimal.ONE) < 0) {
            summedOrders.forEach(summedOrder ->
                    summedOrder.setRate(invertValue(summedOrder.getRate())));
            return true;
        }
        return false;
    }

    private BigDecimal invertValue(BigDecimal value) {
        return BigDecimal.ONE.divide(value, 6, RoundingMode.CEILING);
    }
}
