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
            summedOrders = orderRepository.findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateAsc(
                    currencyToSell.name(), currencyToBuy.name(), 50);
        } else {
            summedOrders = orderRepository.findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateDesc(
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
        Active activeCurrentUserSelling = activeService.findByCurrency(currencyCurrentUserSelling);
        if (activeCurrentUserSelling.getAmount().compareTo(amountCurrentUserSelling) < 0) {
            throw new IllegalArgumentException("Not enough funds");
        }
        Active activeCurrentUserBuying = activeService.findByCurrency(currencyCurrentUserBuying);
        //TODO плюсовать пользователю баланс купленного актива
        BigDecimal amountToBuy = BigDecimal.ZERO;

        boolean isMarket = orderType == OrderType.MARKET;
        if (!isMarket && rate == null) {
            throw new IllegalArgumentException("Rate is required for limit order");
        }
        Order newOrder = createOrder(currencyCurrentUserSelling, currencyCurrentUserBuying, orderType, amountCurrentUserSelling, rate);
        List<Order> ordersToSave = new ArrayList<>();
        ordersToSave.add(newOrder);

        List<Order> orders = isMarket ?
                orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
                        false, currencyCurrentUserBuying, currencyCurrentUserSelling)
                : orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyAndRateGreaterThanOrderByRateDesc(
                        false, currencyCurrentUserBuying, currencyCurrentUserSelling, rate);
        if (!orders.isEmpty()) {
            // Сумма в существующих ордерах в валюте, которую продает текущий пользователь.
            BigDecimal summedAmountInCurrencyCurrentUserSelling = BigDecimal.ZERO;
            List<Order> selectedOrders = new ArrayList<>();
            for (Order order : orders) {
                if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) > 0) {
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
                selectedOrders.forEach(Order::complete);
                if (isMarket) {
                    newOrder.setCompleted(true);
                }
                newOrder.setCompletedAmountToSell(summedAmountInCurrencyCurrentUserSelling);

                activeCurrentUserSelling.subtractAmount(summedAmountInCurrencyCurrentUserSelling);
            // Если сумма в существующих ордерах точно равна сумме в новом
            } else if (summedAmountInCurrencyCurrentUserSelling.compareTo(amountCurrentUserSelling) == 0) {
                selectedOrders.forEach(Order::complete);
                newOrder.complete();

                activeCurrentUserSelling.subtractAmount(amountCurrentUserSelling);
            // Если сумма в существующих ордерах превышает сумму в новом
            } else {
                Order lastOrder = selectedOrders.getLast();
                BigDecimal amountMatchedForLastOrder = summedAmountInCurrencyCurrentUserSelling.subtract(amountCurrentUserSelling);
                lastOrder.setNotCompletedAmount(amountMatchedForLastOrder, currencyCurrentUserSelling);
                IntStream.range(0, selectedOrders.size() - 1)
                        .forEach(i -> selectedOrders.get(i).complete());

                newOrder.complete();

                activeCurrentUserSelling.subtractAmount(amountCurrentUserSelling);
            }
            ordersToSave.addAll(selectedOrders);
        } else if (isMarket) {
            throw new RuntimeException("Order book is empty");
        }
        orderRepository.saveAll(ordersToSave);
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

    private void completeOrder(Order order) {
        order.setCompletedAmountToSell(order.getTotalAmountToSell());
        order.setCompleted(true);
        order.setCompletedAt(LocalDateTime.now());

        Active active = activeService.findByUsernameAndCurrency(order.getCreator(), order.getCurrencyToBuy());

    }

    private boolean invertValues(List<SummedOrder> summedOrders) {
        if (summedOrders.getFirst().getRate().compareTo(BigDecimal.ONE) < 0) {
            summedOrders.forEach(summedOrder ->
                    summedOrder.setRate(BigDecimal.ONE.divide(summedOrder.getRate(), 6, RoundingMode.CEILING)));
            return true;
        }
        return false;
    }
}
