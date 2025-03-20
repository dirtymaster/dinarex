package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import dirtymaster.freedomexchange.repository.OrderRepository;
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

    @BeforeAll
    void beforeAll() {
        orderRepository.deleteAll();
        activeRepository.deleteAll();
        authService.deleteUser("userSellingEur@gmail.com");
        authService.deleteUser("userBuyingEur@gmail.com");
    }

    @BeforeEach
    void setUp() {
        authService.registerUser("userSellingEur@gmail.com", "password");
        authService.registerUser("userBuyingEur@gmail.com", "password");
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        activeRepository.deleteAll();
        authService.deleteUser("userSellingEur@gmail.com");
        authService.deleteUser("userBuyingEur@gmail.com");
    }

    @Test
    void createLimitOrderWhenOrderBookIsEmpty() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(10));
        activeRepository.save(userSellingEurActiveEur);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("userSellingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));

        // checks
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(9));
        Order order = orderRepository.findAll().get(0);
        assertThat(order)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userSellingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(1));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
                    assertThat(o.getCompletedAt()).isNull();
                });
    }

    @Test
    void matchLimitOrderWithExistingOrderInBook() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(2));
        activeRepository.save(userSellingEurActiveEur);
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        userBuyingEurActiveRub.setAmount(BigDecimal.valueOf(100));
        activeRepository.save(userBuyingEurActiveRub);

        UsernamePasswordAuthenticationToken auth1 =
                new UsernamePasswordAuthenticationToken("userSellingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth1);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.ONE);

        UsernamePasswordAuthenticationToken auth2 =
                new UsernamePasswordAuthenticationToken("userBuyingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth2);
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1));
        userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userSellingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(1));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(1));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userBuyingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });
    }

    @Test
    void partiallyMatchLimitOrderDueToInsufficientVolume() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(1));
        activeRepository.save(userSellingEurActiveEur);
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        userBuyingEurActiveRub.setAmount(BigDecimal.valueOf(100));
        activeRepository.save(userBuyingEurActiveRub);

        UsernamePasswordAuthenticationToken auth1 =
                new UsernamePasswordAuthenticationToken("userSellingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth1);
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(1), BigDecimal.valueOf(0.01));
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        UsernamePasswordAuthenticationToken auth2 =
                new UsernamePasswordAuthenticationToken("userBuyingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth2);
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(50), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userSellingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(1));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isNull();
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userBuyingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(50));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(50));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });
    }

    @Test
    void completeOneLimitOrderBySecondBigger() {
        Active userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        userSellingEurActiveEur.setAmount(BigDecimal.valueOf(1));
        activeRepository.save(userSellingEurActiveEur);
        Active userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        userBuyingEurActiveRub.setAmount(BigDecimal.valueOf(100));
        activeRepository.save(userBuyingEurActiveRub);

        UsernamePasswordAuthenticationToken auth1 =
                new UsernamePasswordAuthenticationToken("userSellingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth1);
        // userSellingEur sells 0.5 EUR
        orderService.processOrder(Currency.EUR, Currency.RUB, OrderType.LIMIT, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.01));
        userSellingEurActiveEur = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.EUR);
        assertThat(userSellingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));

        UsernamePasswordAuthenticationToken auth2 =
                new UsernamePasswordAuthenticationToken("userBuyingEur@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth2);
        // userBuyingEur sells 100 RUB
        orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.LIMIT, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        // checks
        Active userSellingEurActiveRub = activeRepository.findByUsernameAndCurrency("userSellingEur@gmail.com", Currency.RUB);
        assertThat(userSellingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        Active userBuyingEurActiveEur = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.EUR);
        assertThat(userBuyingEurActiveEur.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        userBuyingEurActiveRub = activeRepository.findByUsernameAndCurrency("userBuyingEur@gmail.com", Currency.RUB);
        assertThat(userBuyingEurActiveRub.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Order orderSellingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(true, Currency.EUR, Currency.RUB).get(0);
        assertThat(orderSellingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userSellingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.EUR);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.RUB);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
                    assertThat(o.isCompleted()).isTrue();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(0.01));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                });

        Order orderBuyingEur = orderRepository.findByCompletedAndCurrencyToSellAndCurrencyToBuyOrderByRateDesc(false, Currency.RUB, Currency.EUR).get(0);
        assertThat(orderBuyingEur)
                .satisfies(o -> {
                    assertThat(o.getCreator()).isEqualTo("userBuyingEur@gmail.com");
                    assertThat(o.getCurrencyToSell()).isEqualTo(Currency.RUB);
                    assertThat(o.getCurrencyToBuy()).isEqualTo(Currency.EUR);
                    assertThat(o.getTotalAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCompletedAmountToSell()).isEqualByComparingTo(BigDecimal.valueOf(50));
                    assertThat(o.isCompleted()).isFalse();
                    assertThat(o.getOrderType()).isEqualTo(OrderType.LIMIT);
                    assertThat(o.getRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(o.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
                    assertThat(o.getCompletedAt()).isNull();
                });
    }
}
