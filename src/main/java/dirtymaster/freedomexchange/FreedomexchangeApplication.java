package dirtymaster.freedomexchange;

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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
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
    private final Random random = new Random();
    @PostConstruct
    public void init() {
        orderRepository.deleteAll();
        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
                Order order = new Order();
                order.setCurrencyToSell(Currency.RSD);
                order.setCurrencyToBuy(Currency.RUB);
                order.setAmount(new BigDecimal(random.nextInt(1000)));
                order.setRate(new BigDecimal(1.25 + i * 0.0001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
                Order order = new Order();
                order.setCurrencyToSell(Currency.RUB);
                order.setCurrencyToBuy(Currency.RSD);
                order.setAmount(new BigDecimal(random.nextInt(1000)));
                order.setRate(new BigDecimal(0.86 - i * 0.0001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
                Order order = new Order();
                order.setCurrencyToSell(Currency.EUR);
                order.setCurrencyToBuy(Currency.RUB);
                order.setAmount(new BigDecimal(random.nextInt(1000)));
                order.setRate(new BigDecimal(0.01 + i * 0.000001));
                orderRepository.save(order);
            }
        }

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < random.nextInt(10); ++j) {
                Order order = new Order();
                order.setCurrencyToSell(Currency.RUB);
                order.setCurrencyToBuy(Currency.EUR);
                order.setAmount(new BigDecimal(random.nextInt(1000)));
                order.setRate(new BigDecimal(90 - i * 0.01));
                orderRepository.save(order);
            }
        }

        if (!authService.userExists("admin@gmail.com")) {
            authService.registerUser("admin@gmail.com", "admin");
            activeService.changeActive("admin@gmail.com", Currency.RUB, new BigDecimal(10000), BigDecimal.ZERO);
            activeService.changeActive("admin@gmail.com", Currency.RSD, new BigDecimal(1000), BigDecimal.ZERO);
            activeService.changeActive("admin@gmail.com", Currency.EUR, new BigDecimal(100), BigDecimal.ZERO);
        }
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runAsyncTask() {
        System.out.println("Асинхронная операция запущена...");
        for (Order order : orderRepository.findAll()) {
            orderRepository.delete(order);
            System.out.println("aboba");
            try {
                Thread.sleep(1000); // Имитация долгой операции
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Асинхронная операция завершена.");
    }
}
