package dirtymaster.freedomexchange.service.order;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import dirtymaster.freedomexchange.repository.OrderRepository;
import dirtymaster.freedomexchange.service.AuthService;
import dirtymaster.freedomexchange.service.OrderService;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderServiceTest {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ActiveRepository activeRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderService orderService;

    private static final String USER_SELLING_EUR = "userSellingEur@gmail.com";
    private static final String USER_BUYING_EUR = "userBuyingEur@gmail.com";
    private static final String PASSWORD = "password";
    private static final CurrencyUnit EUR = Monetary.getCurrency("EUR");
    private static final CurrencyUnit RUB = Monetary.getCurrency("RUB");

    @BeforeAll
    void beforeAll() {
        cleanDatabase();
    }

    @BeforeEach
    void setUp() {
        authService.registerUser(USER_SELLING_EUR, PASSWORD);
        authService.registerUser(USER_BUYING_EUR, PASSWORD);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        orderRepository.deleteAll();
        activeRepository.deleteAll();
        authService.deleteUser(USER_SELLING_EUR);
        authService.deleteUser(USER_BUYING_EUR);
    }

    @Test
    void createLimitOrderWhenOrderBookIsEmpty() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(10));
        activeRepository.save(userSellingEurActiveEur);
        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));

        // checks
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(9));
        Order order = orderRepository.findAll().get(0);
        assertThat(order)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.ZERO, EUR));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
                    assertThat(o.getCompletedAt()).isNull();
                });
    }

    @Test
    void matchLimitOrderWithExistingOrderInBook() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(2));
        activeRepository.save(userSellingEurActiveEur);
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.RUB);
        userBuyingEurActiveRub.setAmount(BigDecimal.valueOf(100));
        activeRepository.save(userBuyingEurActiveRub);

        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.ONE);

        authenticateUser(USER_BUYING_EUR);
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1));
        userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(100), RUB));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(100), RUB));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });
    }

    @Test
    void partiallyMatchLimitOrderDueToInsufficientVolume() {
        setActiveBalance(USER_SELLING_EUR, Currency.EUR, BigDecimal.valueOf(1));
        setActiveBalance(USER_BUYING_EUR, Currency.RUB, BigDecimal.valueOf(100));

        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        authenticateUser(USER_BUYING_EUR);
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(50), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isNull();
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });
    }

    @Test
    void completeOneLimitOrderBySecondBigger() {
        setActiveBalance(USER_SELLING_EUR, Currency.EUR, BigDecimal.valueOf(1));
        setActiveBalance(USER_BUYING_EUR, Currency.RUB, BigDecimal.valueOf(100));

        authenticateUser(USER_SELLING_EUR);
        // userSellingEur sells 0.5 EUR
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.01));
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));

        authenticateUser(USER_BUYING_EUR);
        // userBuyingEur sells 100 RUB
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(100), RUB));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isNull();
                });
    }

    private void authenticateUser(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, PASSWORD, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void setActiveBalance(String username, Currency currency, BigDecimal amount) {
        Active active = activeRepository.findByUsernameAndCurrency(username, currency);
        active.setAmount(amount);
        activeRepository.save(active);
    }
}
