package dirtymaster.freedomexchange.service;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Active;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import dirtymaster.freedomexchange.repository.OrderRepository;
import org.junit.jupiter.api.AfterAll;
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
        authService.deleteUser("currentuser@gmail.com");
        authService.deleteUser("creator@gmail.com");

        authService.registerUser("currentuser@gmail.com", "password");
        authService.registerUser("creator@gmail.com", "password");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("currentuser@gmail.com", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    void afterAll() {
        authService.deleteUser("currentuser@gmail.com");
        authService.deleteUser("creator@gmail.com");
    }

    @BeforeEach
    void beforeEach() {
        Active currentUserActive = activeRepository.findByUsernameAndCurrency("currentuser@gmail.com", Currency.RUB);
        currentUserActive.setAmount(BigDecimal.valueOf(1000));
        activeRepository.save(currentUserActive);
        Active creatorActive = activeRepository.findByUsernameAndCurrency("creator@gmail.com", Currency.EUR);
        creatorActive.setAmount(BigDecimal.valueOf(1000));
        activeRepository.save(creatorActive);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        activeRepository.deleteAll();
    }

    @Test
    void processOrder() {
        Order limitOrder1 = orderRepository.save(orderService.createOrder(
                Currency.EUR, Currency.RUB, OrderType.LIMIT, new BigDecimal(1), new BigDecimal(100)));
        Order limitOrder2 = orderRepository.save(orderService.createOrder(
                Currency.EUR, Currency.RUB, OrderType.LIMIT, new BigDecimal(1), new BigDecimal(90)));
        Order limitOrder3 = orderRepository.save(orderService.createOrder(
                Currency.EUR, Currency.RUB, OrderType.LIMIT, new BigDecimal(1), new BigDecimal(85)));

        orderRepository.saveAll(List.of(limitOrder1, limitOrder2, limitOrder3));

        // текущий пользователь продает RUB и покупает EUR
                                                                                // рублей продает
        Order order = orderService.processOrder(Currency.RUB, Currency.EUR, OrderType.MARKET, BigDecimal.valueOf(175), null);
        System.out.println();
    }
}
