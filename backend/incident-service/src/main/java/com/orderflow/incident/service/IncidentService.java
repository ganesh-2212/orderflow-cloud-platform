package com.orderflow.incident.service;

import com.orderflow.incident.dto.request.CreateIncidentRequest;
import com.orderflow.incident.dto.response.IncidentResponse;
import com.orderflow.incident.entity.Incident;
import com.orderflow.incident.entity.IncidentSeverity;
import com.orderflow.incident.entity.IncidentStatus;
import com.orderflow.incident.exception.IncidentNotFoundException;
import com.orderflow.incident.exception.InvalidIncidentTransitionException;
import com.orderflow.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentKeyGenerator incidentKeyGenerator;
    private final IncidentSeverityClassifier severityClassifier;
    private final MeterRegistry meterRegistry;
    private final Counter createdCounter;
    private final Counter resolvedCounter;
    private final Counter remediationAttemptsCounter;

    public IncidentService(IncidentRepository incidentRepository, 
                           IncidentKeyGenerator incidentKeyGenerator, 
                           IncidentSeverityClassifier severityClassifier,
                           MeterRegistry meterRegistry) {
        this.incidentRepository = incidentRepository;
        this.incidentKeyGenerator = incidentKeyGenerator;
        this.severityClassifier = severityClassifier;
        this.meterRegistry = meterRegistry;
        this.createdCounter = meterRegistry.counter("orderflow.incidents.created");
        this.resolvedCounter = meterRegistry.counter("orderflow.incidents.resolved");
        this.remediationAttemptsCounter = meterRegistry.counter("orderflow.incidents.remediation.attempts");
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        log.info("Creating incident for service: {}", request.getServiceName());
        
        Incident incident = new Incident();
        incident.setIncidentKey(incidentKeyGenerator.generate());
        incident.setServiceName(request.getServiceName());
        incident.setResourceId(request.getResourceId());
        incident.setCategory(request.getCategory());
        
        IncidentSeverity severity = request.getSeverity() != null ? 
                request.getSeverity() : severityClassifier.classify(request.getCategory());
        incident.setSeverity(severity);
        
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setErrorCode(request.getErrorCode());
        incident.setErrorMessage(request.getErrorMessage());

        Incident savedIncident = incidentRepository.save(incident);
        this.createdCounter.increment();
        log.info("Incident created: key={}, service={}", savedIncident.getIncidentKey(), savedIncident.getServiceName());
        
        return new IncidentResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return new IncidentResponse(incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id)));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentByKey(String key) {
        return new IncidentResponse(incidentRepository.findByIncidentKey(key)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with key: " + key)));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(IncidentResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsByStatus(IncidentStatus status) {
        return incidentRepository.findByStatus(status).stream()
                .map(IncidentResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public IncidentResponse updateIncidentStatus(Long id, IncidentStatus newStatus) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        IncidentStatus oldStatus = incident.getStatus();
        validateTransition(oldStatus, newStatus);
        
        incident.setStatus(newStatus);
        
        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
            this.resolvedCounter.increment();
            log.info("Incident resolved: id={}", id);
        }

        Incident savedIncident = incidentRepository.save(incident);
        log.info("Incident status transition: id={}, from={}, to={}", id, oldStatus, newStatus);
        
        return new IncidentResponse(savedIncident);
    }

    @Transactional
    public IncidentResponse recordRemediationAttempt(Long id, String action) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getRetryCount() >= 3) {
            log.error("Maximum remediation attempts reached: incidentId={}", id);
            throw new IllegalStateException("Maximum remediation attempts reached");
        }

        incident.setRetryCount(incident.getRetryCount() + 1);
        
        if (incident.getStatus() == IncidentStatus.OPEN || incident.getStatus() == IncidentStatus.INVESTIGATING) {
            incident.setStatus(IncidentStatus.MITIGATING);
            log.info("Incident status transition: id={}, from={}, to={}", id, incident.getStatus(), IncidentStatus.MITIGATING);
        }

        log.info("Remediation attempt: incidentKey={}, attempt={}, action={}", incident.getIncidentKey(), incident.getRetryCount(), action);
        
        this.remediationAttemptsCounter.increment();
        return new IncidentResponse(incidentRepository.save(incident));
    }

    @Transactional(readOnly = true)
    public com.orderflow.incident.dto.response.IncidentStatisticsResponse getStatistics() {
        com.orderflow.incident.dto.response.IncidentStatisticsResponse stats = new com.orderflow.incident.dto.response.IncidentStatisticsResponse();
        
        stats.setTotal(incidentRepository.count());
        stats.setOpen(incidentRepository.countByStatus(IncidentStatus.OPEN));
        stats.setInvestigating(incidentRepository.countByStatus(IncidentStatus.INVESTIGATING));
        stats.setMitigating(incidentRepository.countByStatus(IncidentStatus.MITIGATING));
        stats.setResolved(incidentRepository.countByStatus(IncidentStatus.RESOLVED));
        stats.setClosed(incidentRepository.countByStatus(IncidentStatus.CLOSED));
        
        stats.setCritical(incidentRepository.countBySeverity(com.orderflow.incident.entity.IncidentSeverity.CRITICAL));
        stats.setHigh(incidentRepository.countBySeverity(com.orderflow.incident.entity.IncidentSeverity.HIGH));
        stats.setMedium(incidentRepository.countBySeverity(com.orderflow.incident.entity.IncidentSeverity.MEDIUM));
        stats.setLow(incidentRepository.countBySeverity(com.orderflow.incident.entity.IncidentSeverity.LOW));
        
        stats.setRemediationAttempts(incidentRepository.sumRetryCount());
        
        return stats;
    }

    private void validateTransition(IncidentStatus current, IncidentStatus next) {
        if (current == next) return;

        boolean valid = switch (current) {
            case OPEN -> next == IncidentStatus.INVESTIGATING || next == IncidentStatus.RESOLVED;
            case INVESTIGATING -> next == IncidentStatus.MITIGATING || next == IncidentStatus.RESOLVED;
            case MITIGATING -> next == IncidentStatus.RESOLVED;
            case RESOLVED -> next == IncidentStatus.CLOSED;
            case CLOSED -> false; // Terminal state
        };

        if (!valid) {
            throw new InvalidIncidentTransitionException(
                    String.format("Cannot transition incident from %s to %s", current, next)
            );
        }
    }
}
