package com.orderflow.incident.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RecordRemediationRequest {

    @NotBlank(message = "action is required")
    private String action;

    public RecordRemediationRequest() {}
    
    public RecordRemediationRequest(String action) {
        this.action = action;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
