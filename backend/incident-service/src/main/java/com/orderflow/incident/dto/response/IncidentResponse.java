package com.orderflow.incident.dto.response;

import com.orderflow.incident.entity.Incident;
import com.orderflow.incident.entity.IncidentCategory;
import com.orderflow.incident.entity.IncidentSeverity;
import com.orderflow.incident.entity.IncidentStatus;
import java.time.LocalDateTime;

public class IncidentResponse {
    private Long id;
    private String incidentKey;
    private String serviceName;
    private String resourceId;
    private IncidentCategory category;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String title;
    private String description;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public IncidentResponse(Incident incident) {
        this.id = incident.getId();
        this.incidentKey = incident.getIncidentKey();
        this.serviceName = incident.getServiceName();
        this.resourceId = incident.getResourceId();
        this.category = incident.getCategory();
        this.severity = incident.getSeverity();
        this.status = incident.getStatus();
        this.title = incident.getTitle();
        this.description = incident.getDescription();
        this.errorCode = incident.getErrorCode();
        this.errorMessage = incident.getErrorMessage();
        this.retryCount = incident.getRetryCount();
        this.resolutionNotes = incident.getResolutionNotes();
        this.createdAt = incident.getCreatedAt();
        this.updatedAt = incident.getUpdatedAt();
        this.resolvedAt = incident.getResolvedAt();
    }

    public Long getId() { return id; }
    public String getIncidentKey() { return incidentKey; }
    public String getServiceName() { return serviceName; }
    public String getResourceId() { return resourceId; }
    public IncidentCategory getCategory() { return category; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public String getResolutionNotes() { return resolutionNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
