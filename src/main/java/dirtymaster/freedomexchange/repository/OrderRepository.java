package dirtymaster.freedomexchange.repository;

import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Order findTopRateByCurrencyToSellAndCurrencyToBuy(Currency currencyToSell, Currency currencyToBuy);

    @Query(value = """
            SELECT o.rate, SUM(o.total_amount - o.completed_amount) 
            FROM orders o
            WHERE o.completed = false
              AND o.order_type = 'LIMIT'
              AND o.currency_to_sell = :currencyToSell 
              AND o.currency_to_buy = :currencyToBuy
            GROUP BY o.rate
            ORDER BY o.rate ASC
            LIMIT :limit""",
            nativeQuery = true)
    List<SummedOrder> findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateAsc(
            @Param("currencyToSell") String currencyToSell,
            @Param("currencyToBuy") String currencyToBuy, @Param("limit") int limit);

    @Query(value = """
            SELECT o.rate, SUM(o.total_amount - o.completed_amount) 
            FROM orders o
            WHERE o.completed = false
              AND o.order_type = 'LIMIT'
              AND o.currency_to_sell = :currencyToSell 
              AND o.currency_to_buy = :currencyToBuy
            GROUP BY o.rate
            ORDER BY o.rate DESC
            LIMIT :limit""",
            nativeQuery = true)
    List<SummedOrder> findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateDesc(
            @Param("currencyToSell") String currencyToSell,
            @Param("currencyToBuy") String currencyToBuy, @Param("limit") int limit);

    @Query("""
            SELECT o
            FROM Order o
            WHERE o.currencyToSell = :currencyToSell AND o.currencyToBuy = :currencyToBuy
            GROUP BY o.rate
            ORDER BY o.rate DESC""")
    List<Order> findByBaseCurrencyAndQuoteCurrencyOrderByRateDesc(
            @Param("currencyToSell") Currency currencyToSell,
            @Param("currencyToBuy") Currency currencyToBuy);

    List<Order> findByCompletedAndCurrencyToSellAndCurrencyToBuyAndRateGreaterThanOrderByRateDesc(
            boolean completed, Currency currencyToSell, Currency currencyToBuy, BigDecimal rate);

    List<Order> findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(
            boolean completed, Currency currencyToSell, Currency currencyToBuy);
}
