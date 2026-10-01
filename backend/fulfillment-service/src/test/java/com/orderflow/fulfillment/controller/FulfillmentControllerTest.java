package com.orderflow.fulfillment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.fulfillment.dto.request.CreateFulfillmentRequest;
import com.orderflow.fulfillment.dto.request.UpdateFulfillmentStatusRequest;
import com.orderflow.fulfillment.dto.response.FulfillmentResponse;
import com.orderflow.fulfillment.entity.Fulfillment;
import com.orderflow.fulfillment.entity.FulfillmentStatus;
import com.orderflow.fulfillment.exception.FulfillmentNotFoundException;
import com.orderflow.fulfillment.exception.InvalidFulfillmentTransitionException;
import com.orderflow.fulfillment.service.FulfillmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FulfillmentController.class)
class FulfillmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FulfillmentService fulfillmentService;

    @Test
    void createFulfillment_ValidRequest_Returns201() throws Exception {
        CreateFulfillmentRequest request = new CreateFulfillmentRequest(100L, "WH-001");
        Fulfillment fulfillment = new Fulfillment(100L, "WH-001");
        FulfillmentResponse response = new FulfillmentResponse(fulfillment);

        when(fulfillmentService.createFulfillment(any(CreateFulfillmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/fulfillments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("WAREHOUSE_PROCESSING"));
    }

    @Test
    void createFulfillment_InvalidRequest_Returns400() throws Exception {
        CreateFulfillmentRequest request = new CreateFulfillmentRequest(-1L, ""); // Invalid

        mockMvc.perform(post("/api/v1/fulfillments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getFulfillmentById_Existing_Returns200() throws Exception {
        Fulfillment fulfillment = new Fulfillment(100L, "WH-001");
        FulfillmentResponse response = new FulfillmentResponse(fulfillment);

        when(fulfillmentService.getFulfillmentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/fulfillments/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    void getFulfillmentById_Missing_Returns404() throws Exception {
        when(fulfillmentService.getFulfillmentById(99L)).thenThrow(new FulfillmentNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/fulfillments/99")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateFulfillmentStatus_ValidTransition_Returns200() throws Exception {
        UpdateFulfillmentStatusRequest request = new UpdateFulfillmentStatusRequest(FulfillmentStatus.READY_FOR_SHIPPING);
        Fulfillment fulfillment = new Fulfillment(100L, "WH-001");
        fulfillment.setStatus(FulfillmentStatus.READY_FOR_SHIPPING);
        FulfillmentResponse response = new FulfillmentResponse(fulfillment);

        when(fulfillmentService.updateFulfillmentStatus(eq(1L), eq(FulfillmentStatus.READY_FOR_SHIPPING))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/fulfillments/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_SHIPPING"));
    }

    @Test
    void updateFulfillmentStatus_InvalidTransition_Returns409() throws Exception {
        UpdateFulfillmentStatusRequest request = new UpdateFulfillmentStatusRequest(FulfillmentStatus.WAREHOUSE_PROCESSING);

        when(fulfillmentService.updateFulfillmentStatus(eq(1L), eq(FulfillmentStatus.WAREHOUSE_PROCESSING)))
                .thenThrow(new InvalidFulfillmentTransitionException("Invalid transition"));

        mockMvc.perform(patch("/api/v1/fulfillments/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_FULFILLMENT_TRANSITION"));
    }
}
