package com.orderflow.incident.service;

import com.orderflow.incident.entity.IncidentCategory;
import com.orderflow.incident.entity.IncidentSeverity;
import org.springframework.stereotype.Component;

@Component
public class IncidentSeverityClassifier {

    public IncidentSeverity classify(IncidentCategory category) {
        if (category == null) {
            return IncidentSeverity.MEDIUM;
        }

        return switch (category) {
            case SECURITY_FINDING, DATABASE_ERROR, SERVICE_UNAVAILABLE, FULFILLMENT_FAILURE -> IncidentSeverity.HIGH;
            case TIMEOUT, UNKNOWN, INVENTORY_FAILURE -> IncidentSeverity.MEDIUM;
            case VALIDATION_FAILURE, PERFORMANCE_DEGRADATION -> IncidentSeverity.LOW;
        };
    }
}
