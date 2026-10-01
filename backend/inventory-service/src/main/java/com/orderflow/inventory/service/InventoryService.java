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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final MeterRegistry meterRegistry;
    private final Counter reservationsCounter;
    private final Counter failuresCounter;
    private final Counter releasesCounter;

    public InventoryService(InventoryRepository inventoryRepository, MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.meterRegistry = meterRegistry;
        this.reservationsCounter = meterRegistry.counter("orderflow.inventory.reservations");
        this.failuresCounter = meterRegistry.counter("orderflow.inventory.failures");
        this.releasesCounter = meterRegistry.counter("orderflow.inventory.releases");
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        log.info("Creating inventory for product: {}", request.getProductId());
        
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            log.error("Product already exists: {}", request.getProductId());
            throw new DuplicateProductException("Product with ID " + request.getProductId() + " already exists");
        }

        Inventory inventory = new Inventory(
                request.getProductId(),
                request.getProductName(),
                request.getQuantity()
        );
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Inventory created for product {}", savedInventory.getProductId());
        
        return new InventoryResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(String productId) {
        log.info("Retrieving inventory for product: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.error("Inventory not found for product: {}", productId);
                    return new InventoryNotFoundException("Inventory not found for product " + productId);
                });
        return new InventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        log.info("Retrieving all inventory records");
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse reserveStock(ReserveInventoryRequest request) {
        log.info("Reserving {} units for product: {}", request.getQuantity(), request.getProductId());
        
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Inventory not found for product: {}", request.getProductId());
                    return new InventoryNotFoundException("Inventory not found for product " + request.getProductId());
                });

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            this.failuresCounter.increment();
            log.error("Insufficient stock for product {}. Available: {}, Requested: {}", 
                    request.getProductId(), inventory.getAvailableQuantity(), request.getQuantity());
            throw new InsufficientStockException("Insufficient stock for product " + request.getProductId());
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        
        Inventory updatedInventory = inventoryRepository.save(inventory);
        this.reservationsCounter.increment();
        log.info("Reserved {} units of product {}", request.getQuantity(), request.getProductId());
        
        return new InventoryResponse(updatedInventory);
    }

    @Transactional
    public InventoryResponse releaseStock(ReleaseInventoryRequest request) {
        log.info("Releasing {} units for product: {}", request.getQuantity(), request.getProductId());
        
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Inventory not found for product: {}", request.getProductId());
                    return new InventoryNotFoundException("Inventory not found for product " + request.getProductId());
                });

        if (inventory.getReservedQuantity() < request.getQuantity()) {
            this.failuresCounter.increment();
            log.error("Invalid release for product {}. Reserved: {}, Requested Release: {}", 
                    request.getProductId(), inventory.getReservedQuantity(), request.getQuantity());
            throw new InvalidStockReleaseException("Cannot release more stock than is reserved for product " + request.getProductId());
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - request.getQuantity());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());
        
        Inventory updatedInventory = inventoryRepository.save(inventory);
        this.releasesCounter.increment();
        log.info("Released {} units of product {}", request.getQuantity(), request.getProductId());
        
        return new InventoryResponse(updatedInventory);
    }
}
