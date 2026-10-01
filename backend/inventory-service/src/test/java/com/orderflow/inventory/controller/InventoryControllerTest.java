package com.orderflow.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventory.dto.request.CreateInventoryRequest;
import com.orderflow.inventory.dto.request.ReleaseInventoryRequest;
import com.orderflow.inventory.dto.request.ReserveInventoryRequest;
import com.orderflow.inventory.dto.response.InventoryResponse;
import com.orderflow.inventory.entity.Inventory;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.service.InventoryService;
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

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void createInventory_ValidRequest_Returns201() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest("PROD-1001", "Laptop", 10);
        Inventory inventory = new Inventory("PROD-1001", "Laptop", 10);
        InventoryResponse response = new InventoryResponse(inventory);

        when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PROD-1001"))
                .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void createInventory_InvalidRequest_Returns400() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest("", "Laptop", -5);

        mockMvc.perform(post("/api/v1/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getInventory_ExistingProductId_Returns200() throws Exception {
        Inventory inventory = new Inventory("PROD-1001", "Laptop", 10);
        InventoryResponse response = new InventoryResponse(inventory);

        when(inventoryService.getInventoryByProductId("PROD-1001")).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/PROD-1001")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-1001"));
    }

    @Test
    void getInventory_MissingProductId_Returns404() throws Exception {
        when(inventoryService.getInventoryByProductId("MISSING")).thenThrow(new InventoryNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/inventory/MISSING")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void reserveStock_ValidRequest_Returns200() throws Exception {
        ReserveInventoryRequest request = new ReserveInventoryRequest("PROD-1001", 2);
        Inventory inventory = new Inventory("PROD-1001", "Laptop", 10);
        inventory.setAvailableQuantity(8);
        inventory.setReservedQuantity(2);
        InventoryResponse response = new InventoryResponse(inventory);

        when(inventoryService.reserveStock(any(ReserveInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(8))
                .andExpect(jsonPath("$.reservedQuantity").value(2));
    }

    @Test
    void reserveStock_InsufficientStock_Returns409() throws Exception {
        ReserveInventoryRequest request = new ReserveInventoryRequest("PROD-1001", 20);

        when(inventoryService.reserveStock(any(ReserveInventoryRequest.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        mockMvc.perform(post("/api/v1/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void releaseStock_ValidRequest_Returns200() throws Exception {
        ReleaseInventoryRequest request = new ReleaseInventoryRequest("PROD-1001", 1);
        Inventory inventory = new Inventory("PROD-1001", "Laptop", 10);
        inventory.setAvailableQuantity(9);
        inventory.setReservedQuantity(1);
        InventoryResponse response = new InventoryResponse(inventory);

        when(inventoryService.releaseStock(any(ReleaseInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(9))
                .andExpect(jsonPath("$.reservedQuantity").value(1));
    }
}
