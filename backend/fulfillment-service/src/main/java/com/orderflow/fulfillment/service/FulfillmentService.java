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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

    private final FulfillmentRepository fulfillmentRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final IncidentClient incidentClient;
    private final MeterRegistry meterRegistry;
    private final Counter createdCounter;
    private final Counter shippedCounter;
    private final Counter deliveredCounter;
    private final Counter failedCounter;

    public FulfillmentService(FulfillmentRepository fulfillmentRepository, TrackingNumberGenerator trackingNumberGenerator, IncidentClient incidentClient, MeterRegistry meterRegistry) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.trackingNumberGenerator = trackingNumberGenerator;
        this.incidentClient = incidentClient;
        this.meterRegistry = meterRegistry;
        this.createdCounter = meterRegistry.counter("orderflow.fulfillments.created");
        this.shippedCounter = meterRegistry.counter("orderflow.fulfillments.shipped");
        this.deliveredCounter = meterRegistry.counter("orderflow.fulfillments.delivered");
        this.failedCounter = meterRegistry.counter("orderflow.fulfillments.failed");
    }

    @Transactional
    public FulfillmentResponse createFulfillment(CreateFulfillmentRequest request) {
        log.info("Creating fulfillment for order: {}", request.getOrderId());
        
        if (fulfillmentRepository.existsByOrderId(request.getOrderId())) {
            log.error("Fulfillment already exists for order: {}", request.getOrderId());
            
            reportIncident(
                "ORDER-" + request.getOrderId(),
                "FULFILLMENT_FAILURE",
                "HIGH",
                "Duplicate fulfillment creation attempt",
                "Order already has an active fulfillment",
                "DUPLICATE_FULFILLMENT"
            );
            this.failedCounter.increment();
            throw new FulfillmentAlreadyExistsException("Fulfillment already exists for order " + request.getOrderId());
        }

        try {
            Fulfillment fulfillment = new Fulfillment(request.getOrderId(), request.getWarehouseId());
            Fulfillment savedFulfillment = fulfillmentRepository.save(fulfillment);
            this.createdCounter.increment();
            log.info("Fulfillment created successfully with ID: {} for order: {}", savedFulfillment.getId(), savedFulfillment.getOrderId());
            
            return new FulfillmentResponse(savedFulfillment);
        } catch (Exception ex) {
            log.error("Unexpected error creating fulfillment for order {}: {}", request.getOrderId(), ex.getMessage());
            reportIncident(
                "ORDER-" + request.getOrderId(),
                "DATABASE_ERROR",
                "CRITICAL",
                "Fulfillment persistence failed",
                "Unexpected persistence failure: " + ex.getMessage(),
                "FULFILLMENT_PERSISTENCE_FAILED"
            );
            this.failedCounter.increment();
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public FulfillmentResponse getFulfillmentById(Long id) {
        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new FulfillmentNotFoundException("Fulfillment with ID " + id + " not found"));
        return new FulfillmentResponse(fulfillment);
    }

    @Transactional(readOnly = true)
    public FulfillmentResponse getFulfillmentByOrderId(Long orderId) {
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new FulfillmentNotFoundException("Fulfillment for order " + orderId + " not found"));
        return new FulfillmentResponse(fulfillment);
    }

    @Transactional(readOnly = true)
    public List<FulfillmentResponse> getAllFulfillments() {
        return fulfillmentRepository.findAll().stream()
                .map(FulfillmentResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public FulfillmentResponse updateFulfillmentStatus(Long id, FulfillmentStatus newStatus) {
        log.info("Updating fulfillment {} status to {}", id, newStatus);
        
        Fulfillment fulfillment = fulfillmentRepository.findById(id)
                .orElseThrow(() -> new FulfillmentNotFoundException("Fulfillment with ID " + id + " not found"));

        validateTransition(fulfillment.getStatus(), newStatus, fulfillment);

        fulfillment.setStatus(newStatus);
        
        if (newStatus == FulfillmentStatus.SHIPPED && fulfillment.getTrackingNumber() == null) {
            try {
                String trackingNumber = trackingNumberGenerator.generate(fulfillment.getId());
                fulfillment.setTrackingNumber(trackingNumber);
                log.info("Generated tracking number {} for fulfillment {}", trackingNumber, id);
            } catch (Exception ex) {
                log.error("Failed to generate tracking number for fulfillment {}: {}", id, ex.getMessage());
                reportIncident(
                    "FULFILLMENT-" + id,
                    "SERVICE_UNAVAILABLE",
                    "HIGH",
                    "Tracking generation failed",
                    "Failed to generate tracking number for fulfillment",
                    "TRACKING_GENERATION_FAILED"
                );
                throw ex;
            }
        }

        try {
            Fulfillment savedFulfillment = fulfillmentRepository.save(fulfillment);
            
            if (newStatus == FulfillmentStatus.SHIPPED) {
                this.shippedCounter.increment();
            } else if (newStatus == FulfillmentStatus.DELIVERED) {
                this.deliveredCounter.increment();
            } else if (newStatus == FulfillmentStatus.FAILED) {
                this.failedCounter.increment();
            }

            log.info("Successfully updated fulfillment {} to status {}", id, newStatus);
            return new FulfillmentResponse(savedFulfillment);
        } catch (Exception ex) {
            log.error("Unexpected error saving fulfillment {} status transition: {}", id, ex.getMessage());
            reportIncident(
                "FULFILLMENT-" + id,
                "DATABASE_ERROR",
                "HIGH",
                "Fulfillment status update failed",
                "Unexpected persistence failure during status update: " + ex.getMessage(),
                "FULFILLMENT_UPDATE_FAILED"
            );
            this.failedCounter.increment();
            throw ex;
        }
    }

    private void validateTransition(FulfillmentStatus currentStatus, FulfillmentStatus newStatus, Fulfillment fulfillment) {
        if (newStatus == FulfillmentStatus.FAILED) {
            return; // Can transition to FAILED from any state
        }
        
        boolean isValid = switch (currentStatus) {
            case WAREHOUSE_PROCESSING -> newStatus == FulfillmentStatus.READY_FOR_SHIPPING;
            case READY_FOR_SHIPPING -> newStatus == FulfillmentStatus.SHIPPED;
            case SHIPPED -> newStatus == FulfillmentStatus.DELIVERED;
            default -> false;
        };

        if (!isValid) {
            log.error("Invalid transition attempt from {} to {}", currentStatus, newStatus);
            
            reportIncident(
                "FULFILLMENT-" + fulfillment.getId(),
                "VALIDATION_FAILURE",
                "MEDIUM",
                "Invalid fulfillment state transition",
                "Attempted to transition fulfillment from " + currentStatus + " to " + newStatus,
                "INVALID_FULFILLMENT_TRANSITION"
            );
            this.failedCounter.increment();
            
            throw new InvalidFulfillmentTransitionException(
                    String.format("Cannot transition fulfillment from %s to %s", currentStatus, newStatus)
            );
        }
    }
    
    private void reportIncident(String resourceId, String category, String severity, String title, String description, String errorCode) {
        IncidentRequest req = new IncidentRequest();
        req.setServiceName("fulfillment-service");
        req.setResourceId(resourceId);
        req.setCategory(category);
        req.setSeverity(severity);
        req.setTitle(title);
        req.setDescription(description);
        req.setErrorCode(errorCode);
        incidentClient.reportIncident(req);
    }
}
