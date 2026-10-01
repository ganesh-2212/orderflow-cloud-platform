package com.orderflow.incident.dto.request;

import com.orderflow.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateIncidentStatusRequest {
    
    @NotNull(message = "status is required")
    private IncidentStatus status;

    public UpdateIncidentStatusRequest() {}

    public UpdateIncidentStatusRequest(IncidentStatus status) {
        this.status = status;
    }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
}
