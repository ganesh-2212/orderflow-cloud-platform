package com.orderflow.fulfillment.dto.response;

import com.orderflow.fulfillment.entity.Fulfillment;
import com.orderflow.fulfillment.entity.FulfillmentStatus;
import java.time.LocalDateTime;

public class FulfillmentResponse {
    private Long id;
    private Long orderId;
    private FulfillmentStatus status;
    private String warehouseId;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FulfillmentResponse(Fulfillment fulfillment) {
        this.id = fulfillment.getId();
        this.orderId = fulfillment.getOrderId();
        this.status = fulfillment.getStatus();
        this.warehouseId = fulfillment.getWarehouseId();
        this.trackingNumber = fulfillment.getTrackingNumber();
        this.createdAt = fulfillment.getCreatedAt();
        this.updatedAt = fulfillment.getUpdatedAt();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentStatus getStatus() { return status; }
    public String getWarehouseId() { return warehouseId; }
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
