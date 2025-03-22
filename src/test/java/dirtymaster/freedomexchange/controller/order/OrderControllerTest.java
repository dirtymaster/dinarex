package dirtymaster.freedomexchange.controller.order;

import dirtymaster.freedomexchange.controller.OrderController;
import dirtymaster.freedomexchange.dto.OrderType;
import dirtymaster.freedomexchange.entity.Currency;
import dirtymaster.freedomexchange.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void getOrders_shouldReturnOrdersFromService() throws Exception {
        Currency currencyToSell = Currency.EUR;
        Currency currencyToBuy = Currency.RUB;

        when(orderService.getOrders(currencyToSell, currencyToBuy)).thenReturn(Map.of());

        // Act & Assert
        mockMvc.perform(get("/order/{currencyToSell}/{currencyToBuy}/table", currencyToSell, currencyToBuy))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{}"));

        verify(orderService, times(1)).getOrders(currencyToSell, currencyToBuy);
    }

    @Test
    void createOrder_withLimitOrder_shouldCallServiceAndRedirect() throws Exception {
        // Arrange
        Currency currencyToSell = Currency.EUR;
        Currency currencyToBuy = Currency.RUB;
        BigDecimal amountToSell = new BigDecimal("2.0");
        OrderType orderType = OrderType.LIMIT;
        BigDecimal rate = new BigDecimal("0.07");

        // Act & Assert
        mockMvc.perform(post("/order/{currencyToSell}/{currencyToBuy}", currencyToSell, currencyToBuy)
                        .param("amountToSell", amountToSell.toString())
                        .param("orderType", orderType.toString())
                        .param("rate", rate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trading/" + currencyToSell + "/" + currencyToBuy));

        verify(orderService, times(1)).processOrder(eq(currencyToSell), eq(currencyToBuy),
                eq(orderType), eq(amountToSell), eq(rate));
    }
}