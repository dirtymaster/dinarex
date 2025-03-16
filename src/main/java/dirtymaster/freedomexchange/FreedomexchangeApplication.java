package dirtymaster.freedomexchange;

import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.OrderRepository;
import dirtymaster.freedomexchange.repository.UserRepository;
import dirtymaster.freedomexchange.service.ActiveService;
import dirtymaster.freedomexchange.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

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


//    private final Random random = new Random();
//    @PostConstruct
//    public void init() {
//        if (!authService.userExists("admin@gmail.com")) {
//            authService.registerUser("admin@gmail.com", "admin");
//            activeService.changeActive("admin@gmail.com", Currency.RUB, new BigDecimal(10000), BigDecimal.ZERO);
//            activeService.changeActive("admin@gmail.com", Currency.RSD, new BigDecimal(1000), BigDecimal.ZERO);
//            activeService.changeActive("admin@gmail.com", Currency.EUR, new BigDecimal(100), BigDecimal.ZERO);
//        }
//
//        orderRepository.deleteAll();
//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RSD);
//                order.setCurrencyToBuy(Currency.RUB);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(1.25 + i * 0.0001));
//                order.setCreatedAt(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//        }
//
//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RUB);
//                order.setCurrencyToBuy(Currency.RSD);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(0.86 - i * 0.0001));
//                order.setCreatedAt(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//        }
//
//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.EUR);
//                order.setCurrencyToBuy(Currency.RUB);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(0.01 + i * 0.000001));
//                order.setCreatedAt(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//        }
//
//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < random.nextInt(10); ++j) {
//                Order order = new Order();
//                order.setCreator("admin@gmail.com");
//                order.setCurrencyToSell(Currency.RUB);
//                order.setCurrencyToBuy(Currency.EUR);
//                order.setTotalAmountToSell(new BigDecimal(random.nextInt(1000)));
//                order.setCompletedAmountToSell(BigDecimal.ZERO);
//                order.setOrderType(OrderType.LIMIT);
//                order.setRate(new BigDecimal(90 - i * 0.01));
//                order.setCreatedAt(LocalDateTime.now());
//                orderRepository.save(order);
//            }
//        }
//    }
}
