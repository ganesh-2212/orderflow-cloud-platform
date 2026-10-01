package com.orderflow.fulfillment.service;

import com.orderflow.fulfillment.dto.request.CreateFulfillmentRequest;
import com.orderflow.fulfillment.dto.response.FulfillmentResponse;
import com.orderflow.fulfillment.entity.Fulfillment;
import com.orderflow.fulfillment.entity.FulfillmentStatus;
import com.orderflow.fulfillment.client.IncidentClient;
import com.orderflow.fulfillment.client.IncidentRequest;
import com.orderflow.fulfillment.exception.FulfillmentAlreadyExistsException;
import com.orderflow.fulfillment.exception.FulfillmentNotFoundException;
import com.orderflow.fulfillment.exception.InvalidFulfillmentTransitionException;
import com.orderflow.fulfillment.repository.FulfillmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    @Mock
    private FulfillmentRepository fulfillmentRepository;

    @Mock
    private TrackingNumberGenerator trackingNumberGenerator;
    
    @Mock
    private IncidentClient incidentClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private FulfillmentService fulfillmentService;

    private Fulfillment mockFulfillment;

    @BeforeEach
    void setUp() {
        mockFulfillment = new Fulfillment(100L, "WH-001");
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        fulfillmentService = new FulfillmentService(fulfillmentRepository, trackingNumberGenerator, incidentClient, meterRegistry);
        // We simulate ID generation here
        try {
            var field = Fulfillment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(mockFulfillment, 1L);
        } catch (Exception ignored) {}
    }

    @Test
    void createFulfillment_Successfully() {
        CreateFulfillmentRequest request = new CreateFulfillmentRequest(100L, "WH-001");
        
        when(fulfillmentRepository.existsByOrderId(100L)).thenReturn(false);
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenReturn(mockFulfillment);

        FulfillmentResponse response = fulfillmentService.createFulfillment(request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("WH-001", response.getWarehouseId());
        assertEquals(FulfillmentStatus.WAREHOUSE_PROCESSING, response.getStatus());
        verify(fulfillmentRepository, times(1)).save(any(Fulfillment.class));
    }

    @Test
    void createFulfillment_ThrowsDuplicateException() {
        CreateFulfillmentRequest request = new CreateFulfillmentRequest(100L, "WH-001");
        
        when(fulfillmentRepository.existsByOrderId(100L)).thenReturn(true);

        assertThrows(FulfillmentAlreadyExistsException.class, () -> fulfillmentService.createFulfillment(request));
        verify(fulfillmentRepository, never()).save(any(Fulfillment.class));
        verify(incidentClient, times(1)).reportIncident(any(IncidentRequest.class));
    }

    @Test
    void getFulfillmentById_Successfully() {
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));

        FulfillmentResponse response = fulfillmentService.getFulfillmentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getFulfillmentByOrderId_Successfully() {
        when(fulfillmentRepository.findByOrderId(100L)).thenReturn(Optional.of(mockFulfillment));

        FulfillmentResponse response = fulfillmentService.getFulfillmentByOrderId(100L);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
    }

    @Test
    void updateFulfillmentStatus_WarehouseToReady_Successfully() {
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenReturn(mockFulfillment);

        FulfillmentResponse response = fulfillmentService.updateFulfillmentStatus(1L, FulfillmentStatus.READY_FOR_SHIPPING);

        assertEquals(FulfillmentStatus.READY_FOR_SHIPPING, response.getStatus());
        verify(trackingNumberGenerator, never()).generate(any());
    }

    @Test
    void updateFulfillmentStatus_ReadyToShipped_GeneratesTracking() {
        mockFulfillment.setStatus(FulfillmentStatus.READY_FOR_SHIPPING);
        
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));
        when(trackingNumberGenerator.generate(1L)).thenReturn("TRK-12345");
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenReturn(mockFulfillment);

        FulfillmentResponse response = fulfillmentService.updateFulfillmentStatus(1L, FulfillmentStatus.SHIPPED);

        assertEquals(FulfillmentStatus.SHIPPED, response.getStatus());
        assertEquals("TRK-12345", response.getTrackingNumber());
        verify(trackingNumberGenerator, times(1)).generate(1L);
    }

    @Test
    void updateFulfillmentStatus_ShippedToDelivered_Successfully() {
        mockFulfillment.setStatus(FulfillmentStatus.SHIPPED);
        mockFulfillment.setTrackingNumber("TRK-12345");
        
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenReturn(mockFulfillment);

        FulfillmentResponse response = fulfillmentService.updateFulfillmentStatus(1L, FulfillmentStatus.DELIVERED);

        assertEquals(FulfillmentStatus.DELIVERED, response.getStatus());
        assertEquals("TRK-12345", response.getTrackingNumber()); // Ensure it keeps tracking
    }

    @Test
    void updateFulfillmentStatus_InvalidTransition_ThrowsException() {
        mockFulfillment.setStatus(FulfillmentStatus.DELIVERED);
        
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));

        assertThrows(InvalidFulfillmentTransitionException.class, 
                () -> fulfillmentService.updateFulfillmentStatus(1L, FulfillmentStatus.WAREHOUSE_PROCESSING));
        verify(fulfillmentRepository, never()).save(any(Fulfillment.class));
        verify(incidentClient, times(1)).reportIncident(any(IncidentRequest.class));
    }

    @Test
    void updateFulfillmentStatus_ToFailed_Successfully() {
        // Can go to FAILED from WAREHOUSE_PROCESSING
        when(fulfillmentRepository.findById(1L)).thenReturn(Optional.of(mockFulfillment));
        when(fulfillmentRepository.save(any(Fulfillment.class))).thenReturn(mockFulfillment);

        FulfillmentResponse response = fulfillmentService.updateFulfillmentStatus(1L, FulfillmentStatus.FAILED);

        assertEquals(FulfillmentStatus.FAILED, response.getStatus());
    }

    @Test
    void missingFulfillment_ThrowsException() {
        when(fulfillmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FulfillmentNotFoundException.class, 
                () -> fulfillmentService.updateFulfillmentStatus(99L, FulfillmentStatus.READY_FOR_SHIPPING));
    }
}
