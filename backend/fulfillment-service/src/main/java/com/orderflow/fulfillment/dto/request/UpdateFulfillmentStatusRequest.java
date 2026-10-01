package com.orderflow.fulfillment.dto.request;

import com.orderflow.fulfillment.entity.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateFulfillmentStatusRequest {

    @NotNull(message = "status is required")
    private FulfillmentStatus status;

    public UpdateFulfillmentStatusRequest() {}

    public UpdateFulfillmentStatusRequest(FulfillmentStatus status) {
        this.status = status;
    }

    public FulfillmentStatus getStatus() { return status; }
    public void setStatus(FulfillmentStatus status) { this.status = status; }
}
