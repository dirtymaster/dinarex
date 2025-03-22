package dirtymaster.freedomexchange.service.order;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Active;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.EUR;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;
import static org.assertj.core.api.Assertions.assertThat;

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
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        userSellingEurActiveEur.setMonetaryAmount(Money.of(BigDecimal.valueOf(10), EUR));
        activeRepository.save(userSellingEurActiveEur);
        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(EUR, RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));

        // checks
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        assertThat(userSellingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(9), EUR));
        Order order = orderRepository.findAll().get(0);
        assertThat(order)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(RUB);
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
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        userSellingEurActiveEur.setMonetaryAmount(Money.of(BigDecimal.valueOf(2), EUR));
        activeRepository.save(userSellingEurActiveEur);
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, RUB);
        userBuyingEurActiveRub.setMonetaryAmount(Money.of(BigDecimal.valueOf(100), RUB));
        activeRepository.save(userBuyingEurActiveRub);

        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(EUR, RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        assertThat(userSellingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.ONE, EUR));

        authenticateUser(USER_BUYING_EUR);
        orderService.processOrder(RUB, EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, RUB);
        assertThat(userSellingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(100), RUB));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, EUR);
        assertThat(userBuyingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
        userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, RUB);
        assertThat(userBuyingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.zero(RUB));

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, EUR, RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, RUB, EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(EUR);
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
        setActiveBalance(USER_SELLING_EUR, EUR, BigDecimal.valueOf(1));
        setActiveBalance(USER_BUYING_EUR, RUB, BigDecimal.valueOf(100));

        authenticateUser(USER_SELLING_EUR);
        orderService.processOrder(EUR, RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        assertThat(userSellingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.zero(EUR));

        authenticateUser(USER_BUYING_EUR);
        orderService.processOrder(RUB, EUR, OrderType.LIMIT, BigDecimal.valueOf(50), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, RUB);
        assertThat(userSellingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, EUR);
        assertThat(userBuyingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, RUB);
        assertThat(userBuyingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, EUR, RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(1), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isNull();
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, RUB, EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(EUR);
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
        setActiveBalance(USER_SELLING_EUR, EUR, BigDecimal.valueOf(1));
        setActiveBalance(USER_BUYING_EUR, RUB, BigDecimal.valueOf(100));

        authenticateUser(USER_SELLING_EUR);
        // userSellingEur sells 0.5 EUR
        orderService.processOrder(EUR, RUB, OrderType.LIMIT, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.01));
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, EUR);
        assertThat(userSellingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));

        authenticateUser(USER_BUYING_EUR);
        // userBuyingEur sells 100 RUB
        orderService.processOrder(RUB, EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_SELLING_EUR, RUB);
        assertThat(userSellingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(50), RUB));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, EUR);
        assertThat(userBuyingEurActiveEur.getMonetaryAmount()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency(USER_BUYING_EUR, RUB);
        assertThat(userBuyingEurActiveRub.getMonetaryAmount()).isEqualTo(Money.zero(RUB));

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, EUR, RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_SELLING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.getCompletedAmountToSell()).isEqualTo(Money.of(BigDecimal.valueOf(0.5), EUR));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, RUB, EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo(USER_BUYING_EUR);
                    assertThat(o.getCurrencyToSell()).isEqualTo(RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(EUR);
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

    private void setActiveBalance(String username, CurrencyUnit currency, BigDecimal amount) {
        Active active = activeRepository.findByUsernameAndCurrency(username, currency);
        active.setMonetaryAmount(Money.of(amount, active.getCurrency()));
        activeRepository.save(active);
    }
}
