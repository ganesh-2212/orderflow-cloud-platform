package com.orderflow.inventory.dto.response;

import com.orderflow.inventory.entity.Inventory;
import java.time.LocalDateTime;

public class InventoryResponse {

    private Long id;
    private String productId;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryResponse(Inventory inventory) {
        this.id = inventory.getId();
        this.productId = inventory.getProductId();
        this.productName = inventory.getProductName();
        this.availableQuantity = inventory.getAvailableQuantity();
        this.reservedQuantity = inventory.getReservedQuantity();
        this.createdAt = inventory.getCreatedAt();
        this.updatedAt = inventory.getUpdatedAt();
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
