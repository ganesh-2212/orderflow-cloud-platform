package com.orderflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dto.request.CreateOrderRequest;
import com.orderflow.order.dto.request.UpdateOrderStatusRequest;
import com.orderflow.order.dto.response.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.service.OrderService;
import com.orderflow.order.dto.request.OrderItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.orderflow.order.security.JwtService;
import com.orderflow.order.security.JwtAuthenticationEntryPoint;
import com.orderflow.order.security.CustomAccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    void createOrder_ValidRequest_Returns201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("CUST-1001", new BigDecimal("2499.00"), "INR", List.of(new OrderItemRequest("PROD-1001", 2)));
        Order order = new Order("CUST-1001", OrderStatus.CREATED, new BigDecimal("2499.00"), "INR");
        OrderResponse response = new OrderResponse(order);

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST-1001"));
    }

    @Test
    void createOrder_InvalidRequest_Returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("", new BigDecimal("-10.00"), "IN", List.of());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getOrder_ExistingId_Returns200() throws Exception {
        Order order = new Order("CUST-1001", OrderStatus.CREATED, new BigDecimal("2499.00"), "INR");
        OrderResponse response = new OrderResponse(order);

        when(orderService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-1001"));
    }

    @Test
    void getOrder_NonExistingId_Returns404() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/orders/99")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void updateOrderStatus_ValidStatus_Returns200() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        Order order = new Order("CUST-1001", OrderStatus.CONFIRMED, new BigDecimal("2499.00"), "INR");
        OrderResponse response = new OrderResponse(order);

        when(orderService.updateOrderStatus(eq(1L), any(OrderStatus.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_InvalidStatus_Returns400() throws Exception {
        String invalidJson = "{\"status\": \"INVALID_STATUS_XYZ\"}";

        mockMvc.perform(patch("/api/v1/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }
}
