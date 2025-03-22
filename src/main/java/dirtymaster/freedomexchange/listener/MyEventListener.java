package dirtymaster.freedomexchange.listener;

import dirtymaster.freedomexchange.entity.Order;
import dirtymaster.freedomexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyEventListener {
    private final OrderRepository orderRepository;

//    @Async
//    @EventListener(ApplicationReadyEvent.class)
//    public void runAsyncTask() {
//        //TODO удалить в реальном приложении
//        System.out.println("Асинхронная операция запущена...");
//        for (Order order : orderRepository.findAll()) {
//            orderRepository.delete(order);
//            System.out.println("aboba");
//            try {
//                Thread.sleep(1000); // Имитация долгой операции
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        System.out.println("Асинхронная операция завершена.");
//    }
}
