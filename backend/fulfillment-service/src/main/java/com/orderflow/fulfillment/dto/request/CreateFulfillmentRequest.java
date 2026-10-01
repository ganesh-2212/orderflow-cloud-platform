package com.orderflow.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateFulfillmentRequest {

    @NotNull(message = "orderId is required")
    @Positive(message = "orderId must be positive")
    private Long orderId;

    @NotBlank(message = "warehouseId is required")
    private String warehouseId;

    public CreateFulfillmentRequest() {}

    public CreateFulfillmentRequest(Long orderId, String warehouseId) {
        this.orderId = orderId;
        this.warehouseId = warehouseId;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
}
