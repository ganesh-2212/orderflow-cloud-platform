package com.orderflow.order.service;

import com.orderflow.order.dto.request.CreateOrderRequest;
import com.orderflow.order.dto.response.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.client.FulfillmentClient;
import com.orderflow.order.client.FulfillmentClientException;
import com.orderflow.order.client.IncidentClient;
import com.orderflow.order.client.IncidentRequest;
import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.InventoryClientException;
import com.orderflow.order.dto.request.CreateOrderRequest;
import com.orderflow.order.dto.request.OrderItemRequest;
import com.orderflow.order.dto.response.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private FulfillmentClient fulfillmentClient;
    
    @Mock
    private IncidentClient incidentClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private OrderService orderService;

    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = new Order("CUST-1001", OrderStatus.CREATED, new BigDecimal("2499.00"), "INR");
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        orderService = new OrderService(orderRepository, inventoryClient, fulfillmentClient, incidentClient, meterRegistry);
        mockOrder.addItem(new OrderItem(mockOrder, "PROD-1001", 2));
    }

    @Test
    void createOrder_CompleteWorkflow_StatusConfirmed() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1001", new BigDecimal("2499.00"), "INR", 
                List.of(new OrderItemRequest("PROD-1001", 2)));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = (Order) i.getArguments()[0];
            try {
                var field = Order.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(o, 1L);
            } catch (Exception ignored) {}
            return o; 
        });

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("CUST-1001", response.getCustomerId());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(inventoryClient, times(1)).reserveStock("PROD-1001", 2);
        verify(fulfillmentClient, times(1)).createFulfillment(anyLong(), eq("WH-001"));
        verify(incidentClient, never()).reportIncident(any(IncidentRequest.class));
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrder_InventoryFail_StatusFailedAndCompensates_NoFulfillmentCalled() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1001", new BigDecimal("2499.00"), "INR", 
                List.of(new OrderItemRequest("PROD-1001", 2), new OrderItemRequest("PROD-1002", 3)));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = (Order) i.getArguments()[0];
            try {
                var field = Order.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(o, 1L);
            } catch (Exception ignored) {}
            return o;
        });
        
        // First item succeeds, second fails
        doNothing().when(inventoryClient).reserveStock("PROD-1001", 2);
        doThrow(new InventoryClientException("Insufficient stock")).when(inventoryClient).reserveStock("PROD-1002", 3);

        OrderResponse response = orderService.createOrder(request);

        assertEquals(OrderStatus.FAILED, response.getStatus());
        verify(inventoryClient, times(1)).reserveStock("PROD-1001", 2);
        verify(inventoryClient, times(1)).reserveStock("PROD-1002", 3);
        verify(inventoryClient, times(1)).releaseStock("PROD-1001", 2); // Compensation!
        verify(fulfillmentClient, never()).createFulfillment(anyLong(), anyString());
        verify(incidentClient, times(1)).reportIncident(any(IncidentRequest.class));
    }

    @Test
    void createOrder_FulfillmentFail_CompensatesInventoryAndFailsOrder() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1001", new BigDecimal("2499.00"), "INR", 
                List.of(new OrderItemRequest("PROD-1001", 2)));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = (Order) i.getArguments()[0];
            try {
                var field = Order.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(o, 1L);
            } catch (Exception ignored) {}
            return o;
        });
        
        doNothing().when(inventoryClient).reserveStock("PROD-1001", 2);
        doThrow(new FulfillmentClientException("Fulfillment failed")).when(fulfillmentClient).createFulfillment(anyLong(), anyString());

        OrderResponse response = orderService.createOrder(request);

        assertEquals(OrderStatus.FAILED, response.getStatus());
        verify(inventoryClient, times(1)).reserveStock("PROD-1001", 2);
        verify(fulfillmentClient, times(1)).createFulfillment(anyLong(), eq("WH-001"));
        verify(inventoryClient, times(1)).releaseStock("PROD-1001", 2); // Compensation because fulfillment failed
        verify(incidentClient, times(1)).reportIncident(any(IncidentRequest.class)); // Reports fulfillment failure
    }
    
    @Test
    void createOrder_CompensationFail_ReportsSecondIncident() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1001", new BigDecimal("2499.00"), "INR", 
                List.of(new OrderItemRequest("PROD-1001", 2), new OrderItemRequest("PROD-1002", 3)));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = (Order) i.getArguments()[0];
            try {
                var field = Order.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(o, 1L);
            } catch (Exception ignored) {}
            return o;
        });
        
        doNothing().when(inventoryClient).reserveStock("PROD-1001", 2);
        doThrow(new InventoryClientException("Inventory failed")).when(inventoryClient).reserveStock("PROD-1002", 3);
        doThrow(new RuntimeException("Compensation failed")).when(inventoryClient).releaseStock("PROD-1001", 2);

        OrderResponse response = orderService.createOrder(request);

        assertEquals(OrderStatus.FAILED, response.getStatus());
        // Reports two incidents: one for inventory failing, one for compensation failing
        verify(incidentClient, times(2)).reportIncident(any(IncidentRequest.class));
    }

    @Test
    void getOrderById_Successfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals("CUST-1001", response.getCustomerId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_ThrowsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository, times(1)).findById(99L);
    }

    @Test
    void updateOrderStatus_Successfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, mockOrder.getStatus()); // Verify mutation
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(mockOrder);
    }
}
