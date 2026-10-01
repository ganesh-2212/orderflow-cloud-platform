package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.request.CreateInventoryRequest;
import com.orderflow.inventory.dto.request.ReleaseInventoryRequest;
import com.orderflow.inventory.dto.request.ReserveInventoryRequest;
import com.orderflow.inventory.dto.response.InventoryResponse;
import com.orderflow.inventory.entity.Inventory;
import com.orderflow.inventory.exception.DuplicateProductException;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.InvalidStockReleaseException;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryService inventoryService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private Inventory mockInventory;

    @BeforeEach
    void setUp() {
        mockInventory = new Inventory("PROD-1001", "Laptop", 10);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        inventoryService = new InventoryService(inventoryRepository, meterRegistry);
    }

    @Test
    void createInventory_Successfully() {
        CreateInventoryRequest request = new CreateInventoryRequest("PROD-1001", "Laptop", 10);
        
        when(inventoryRepository.existsByProductId("PROD-1001")).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        InventoryResponse response = inventoryService.createInventory(request);

        assertNotNull(response);
        assertEquals("PROD-1001", response.getProductId());
        assertEquals(10, response.getAvailableQuantity());
        assertEquals(0, response.getReservedQuantity());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    void createInventory_ThrowsDuplicateProductException() {
        CreateInventoryRequest request = new CreateInventoryRequest("PROD-1001", "Laptop", 10);
        
        when(inventoryRepository.existsByProductId("PROD-1001")).thenReturn(true);

        assertThrows(DuplicateProductException.class, () -> inventoryService.createInventory(request));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void getInventory_Successfully() {
        when(inventoryRepository.findByProductId("PROD-1001")).thenReturn(Optional.of(mockInventory));

        InventoryResponse response = inventoryService.getInventoryByProductId("PROD-1001");

        assertNotNull(response);
        assertEquals("PROD-1001", response.getProductId());
    }

    @Test
    void getInventory_ThrowsNotFoundException() {
        when(inventoryRepository.findByProductId("MISSING")).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.getInventoryByProductId("MISSING"));
    }

    @Test
    void reserveStock_Successfully() {
        ReserveInventoryRequest request = new ReserveInventoryRequest("PROD-1001", 2);
        
        when(inventoryRepository.findByProductId("PROD-1001")).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        InventoryResponse response = inventoryService.reserveStock(request);

        assertNotNull(response);
        assertEquals(8, mockInventory.getAvailableQuantity());
        assertEquals(2, mockInventory.getReservedQuantity());
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    @Test
    void reserveStock_ThrowsInsufficientStockException() {
        ReserveInventoryRequest request = new ReserveInventoryRequest("PROD-1001", 20); // More than 10
        
        when(inventoryRepository.findByProductId("PROD-1001")).thenReturn(Optional.of(mockInventory));

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(request));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void releaseStock_Successfully() {
        mockInventory.setAvailableQuantity(8);
        mockInventory.setReservedQuantity(2);
        
        ReleaseInventoryRequest request = new ReleaseInventoryRequest("PROD-1001", 1);
        
        when(inventoryRepository.findByProductId("PROD-1001")).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);

        InventoryResponse response = inventoryService.releaseStock(request);

        assertNotNull(response);
        assertEquals(9, mockInventory.getAvailableQuantity());
        assertEquals(1, mockInventory.getReservedQuantity());
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    @Test
    void releaseStock_ThrowsInvalidStockReleaseException() {
        mockInventory.setAvailableQuantity(8);
        mockInventory.setReservedQuantity(2);
        
        ReleaseInventoryRequest request = new ReleaseInventoryRequest("PROD-1001", 5); // More than 2
        
        when(inventoryRepository.findByProductId("PROD-1001")).thenReturn(Optional.of(mockInventory));

        assertThrows(InvalidStockReleaseException.class, () -> inventoryService.releaseStock(request));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}
