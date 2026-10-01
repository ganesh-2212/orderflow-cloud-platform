package com.orderflow.incident.dto.request;

import com.orderflow.incident.entity.IncidentCategory;
import com.orderflow.incident.entity.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateIncidentRequest {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    private String resourceId;

    @NotNull(message = "category is required")
    private IncidentCategory category;

    private IncidentSeverity severity;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be less than 255 characters")
    private String title;

    @NotBlank(message = "description is required")
    @Size(max = 1000, message = "description must be less than 1000 characters")
    private String description;

    private String errorCode;

    @Size(max = 1000, message = "errorMessage must be less than 1000 characters")
    private String errorMessage;

    public CreateIncidentRequest() {}

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }

    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
