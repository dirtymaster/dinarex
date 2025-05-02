package dirtymaster.freedomexchange;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.ActiveRepository;
import dirtymaster.freedomexchange.repository.OrderRepository;
import dirtymaster.freedomexchange.repository.UserRepository;
import dirtymaster.freedomexchange.service.ActiveService;
import dirtymaster.freedomexchange.service.AuthService;
import dirtymaster.freedomexchange.service.OrderService;
import jakarta.annotation.PostConstruct;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.EUR;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RSD;
import static dirtymaster.freedomexchange.constant.CurrencyUnitConstants.RUB;

@SpringBootApplication
@EnableConfigurationProperties
public class FreedomexchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreedomexchangeApplication.class, args);
    }

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ActiveRepository activeRepository;

    private final Random random = new Random();

    @PostConstruct
    public void init() {
        orderRepository.deleteAll();
        activeRepository.deleteAll();
        authService.deleteUser("admin@gmail.com");
        authService.registerUser("admin@gmail.com", "admin");
        UserDetails user = new User(
                "admin@gmail.com",
                "admin",  // пароль можно закодировать через PasswordEncoder
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        activeService.changeActive(Money.of(new BigDecimal(10000), RUB));
        activeService.changeActive(Money.of(new BigDecimal(1000), RSD));
        activeService.changeActive(Money.of(new BigDecimal(100), EUR));



        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RSD);
//                order.setCurrencyToBuy(Currency.RUB);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(1.25 + i * 0.0001));
//                order.setCreatedAt(LocalDateTime.now());
                Order order = orderService.createOrder(RSD, RUB, OrderType.LIMIT, new BigDecimal(random.nextInt(1000)), new BigDecimal(1.25 + i * 0.0001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RUB);
//                order.setCurrencyToBuy(Currency.RSD);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(0.86 - i * 0.0001));
//                order.setCreatedAt(LocalDateTime.now());
                Order order = orderService.createOrder(RUB, RSD, OrderType.LIMIT, new BigDecimal(random.nextInt(1000)), new BigDecimal(0.86 - i * 0.0001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.EUR);
//                order.setCurrencyToBuy(Currency.RUB);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(0.01 + i * 0.000001));
//                order.setCreatedAt(LocalDateTime.now());
                Order order = orderService.createOrder(EUR, RUB, OrderType.LIMIT, new BigDecimal(random.nextInt(1000)), new BigDecimal(0.01 + i * 0.000001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RUB);
//                order.setCurrencyToBuy(Currency.EUR);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(90 - i * 0.01));
//                order.setCreatedAt(LocalDateTime.now());
                Order order = orderService.createOrder(RUB, EUR, OrderType.LIMIT, new BigDecimal(random.nextInt(1000)), new BigDecimal(90 - i * 0.01));
                orderRepository.save(order);
            }
        }
    }
}
