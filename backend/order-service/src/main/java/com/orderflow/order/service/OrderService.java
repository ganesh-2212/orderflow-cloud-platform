package com.orderflow.order.service;

import com.orderflow.order.dto.request.CreateOrderRequest;
import com.orderflow.order.dto.response.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.OrderNotFoundException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final FulfillmentClient fulfillmentClient;
    private final IncidentClient incidentClient;
    private final MeterRegistry meterRegistry;
    private final Counter ordersCreatedCounter;
    private final Counter ordersFailedCounter;

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient, 
                        FulfillmentClient fulfillmentClient, IncidentClient incidentClient,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.fulfillmentClient = fulfillmentClient;
        this.incidentClient = incidentClient;
        this.meterRegistry = meterRegistry;
        this.ordersCreatedCounter = meterRegistry.counter("orderflow.orders.created");
        this.ordersFailedCounter = meterRegistry.counter("orderflow.orders.failed");
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        Order order = new Order(
                request.getCustomerId(),
                OrderStatus.CREATED,
                request.getTotalAmount(),
                request.getCurrency()
        );
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            order.addItem(new OrderItem(order, itemRequest.getProductId(), itemRequest.getQuantity()));
        }

        // Save order and items in CREATED state
        Order savedOrder = orderRepository.save(order);
        this.ordersCreatedCounter.increment();
        log.info("Order created locally with ID: {} in CREATED state", savedOrder.getId());

        // Process inventory reservations
        List<OrderItemRequest> successfulReservations = new ArrayList<>();
        boolean reservationFailed = false;

        for (OrderItemRequest item : request.getItems()) {
            try {
                inventoryClient.reserveStock(item.getProductId(), item.getQuantity());
                successfulReservations.add(item);
            } catch (InventoryClientException ex) {
                log.error("Inventory reservation failed for product {}. Initiating compensation...", item.getProductId());
                reservationFailed = true;
                break; // Stop reserving further items
            }
        }

        if (reservationFailed) {
            log.info("Starting compensation: orderId={}", savedOrder.getId());
            for (OrderItemRequest successItem : successfulReservations) {
                try {
                    inventoryClient.releaseStock(successItem.getProductId(), successItem.getQuantity());
                    log.info("Inventory compensation completed: orderId={}, productId={}", savedOrder.getId(), successItem.getProductId());
                } catch (Exception ex) {
                    log.error("Inventory compensation failed: orderId={}, productId={}", savedOrder.getId(), successItem.getProductId(), ex);
                    reportIncident(
                        savedOrder.getId(), 
                        "INVENTORY_FAILURE", 
                        "HIGH", 
                        "Inventory compensation failed", 
                        "Failed to release stock during compensation", 
                        "INVENTORY_COMPENSATION_FAILED", 
                        ex.getMessage()
                    );
                }
            }
            savedOrder.setStatus(OrderStatus.FAILED);
            this.ordersFailedCounter.increment();
            log.info("Order {} marked as FAILED due to inventory issues", savedOrder.getId());
            
            reportIncident(
                savedOrder.getId(), 
                "INVENTORY_FAILURE", 
                "HIGH", 
                "Inventory reservation failed", 
                "Failed to reserve inventory for one or more items", 
                "INVENTORY_RESERVATION_FAILED", 
                "Downstream inventory service failed"
            );
            
        } else {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
            meterRegistry.counter("orderflow.orders.confirmed").increment();
            log.info("Order confirmed: orderId={}", savedOrder.getId());
            
            // Phase 6: Fulfillment Creation
            try {
                log.info("Creating fulfillment: orderId={}", savedOrder.getId());
                fulfillmentClient.createFulfillment(savedOrder.getId(), "WH-001");
                log.info("Fulfillment created: orderId={}", savedOrder.getId());
            } catch (FulfillmentClientException ex) {
                log.error("Fulfillment creation failed: orderId={}", savedOrder.getId());
                log.info("Starting compensation for fulfillment failure: orderId={}", savedOrder.getId());
                
                // Release inventory
                for (OrderItemRequest successItem : successfulReservations) {
                    try {
                        inventoryClient.releaseStock(successItem.getProductId(), successItem.getQuantity());
                        log.info("Inventory compensation completed: orderId={}, productId={}", savedOrder.getId(), successItem.getProductId());
                    } catch (Exception releaseEx) {
                        log.error("Inventory compensation failed during fulfillment failure recovery: orderId={}, productId={}", savedOrder.getId(), successItem.getProductId(), releaseEx);
                        reportIncident(
                            savedOrder.getId(), 
                            "INVENTORY_FAILURE", 
                            "HIGH", 
                            "Inventory compensation failed", 
                            "Failed to release stock after fulfillment error", 
                            "INVENTORY_COMPENSATION_FAILED", 
                            releaseEx.getMessage()
                        );
                    }
                }
                
                savedOrder.setStatus(OrderStatus.FAILED);
                this.ordersFailedCounter.increment();
                log.info("Order {} marked as FAILED due to fulfillment issues", savedOrder.getId());
                
                reportIncident(
                    savedOrder.getId(), 
                    "FULFILLMENT_FAILURE", 
                    "HIGH", 
                    "Fulfillment creation failed", 
                    "Failed to create fulfillment record", 
                    "FULFILLMENT_CREATION_FAILED", 
                    ex.getMessage()
                );
            }
        }

        savedOrder = orderRepository.save(savedOrder);
        return new OrderResponse(savedOrder);
    }
    
    private void reportIncident(Long orderId, String category, String severity, String title, String description, String errorCode, String errorMessage) {
        IncidentRequest req = new IncidentRequest();
        req.setServiceName("order-service");
        req.setResourceId("ORDER-" + orderId);
        req.setCategory(category);
        req.setSeverity(severity);
        req.setTitle(title);
        req.setDescription(description);
        req.setErrorCode(errorCode);
        req.setErrorMessage(errorMessage);
        incidentClient.reportIncident(req);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Retrieving order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", id);
                    return new OrderNotFoundException("Order with id " + id + " was not found");
                });
        return new OrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Retrieving all orders");
        return orderRepository.findAll().stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Updating status for order ID: {} to {}", id, newStatus);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", id);
                    return new OrderNotFoundException("Order with id " + id + " was not found");
                });
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully for ID: {}", id);
        return new OrderResponse(updatedOrder);
    }
}
