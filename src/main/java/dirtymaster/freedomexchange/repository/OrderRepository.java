package dirtymaster.freedomexchange.repository;

import dirtymaster.freedomexchange.dto.SummedOrder;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Order findTopRateByCurrencyToSellAndCurrencyToBuy(Currency currencyToSell, Currency currencyToBuy);

    @Query("""
            SELECT new dirtymaster.freedomexchange.dto.SummedOrder(o.rate, SUM(o.amount))
            FROM Order o 
            WHERE o.currencyToSell = :currencyToSell AND o.currencyToBuy = :currencyToBuy
            GROUP BY o.currencyToSell, o.currencyToBuy, o.rate
            ORDER BY o.rate ASC
            LIMIT :limit""")
    List<SummedOrder> findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateAsc(
            @Param("currencyToSell") Currency currencyToSell,
            @Param("currencyToBuy") Currency currencyToBuy, @Param("limit") int limit);

    @Query("""
            SELECT new dirtymaster.freedomexchange.dto.SummedOrder(o.rate, SUM(o.amount))
            FROM Order o 
            WHERE o.currencyToSell = :currencyToSell AND o.currencyToBuy = :currencyToBuy
            GROUP BY o.currencyToSell, o.currencyToBuy, o.rate
            ORDER BY o.rate DESC
            LIMIT :limit""")
    List<SummedOrder> findTop50SummedByBaseCurrencyAndQuoteCurrencyOrderByRateDesc(
            @Param("currencyToSell") Currency currencyToSell,
            @Param("currencyToBuy") Currency currencyToBuy, @Param("limit") int limit);
}
