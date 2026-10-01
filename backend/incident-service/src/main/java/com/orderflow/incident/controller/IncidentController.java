package com.orderflow.incident.controller;

import com.orderflow.incident.dto.request.CreateIncidentRequest;
import com.orderflow.incident.dto.request.RecordRemediationRequest;
import com.orderflow.incident.dto.request.UpdateIncidentStatusRequest;
import com.orderflow.incident.dto.response.IncidentResponse;
import com.orderflow.incident.entity.IncidentStatus;
import com.orderflow.incident.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{incidentKey}")
    public ResponseEntity<IncidentResponse> getIncidentByKey(@PathVariable String incidentKey) {
        IncidentResponse response = incidentService.getIncidentByKey(incidentKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<com.orderflow.incident.dto.response.IncidentStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(incidentService.getStatistics());
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<IncidentResponse>> getIncidentsByStatus(@PathVariable IncidentStatus status) {
        return ResponseEntity.ok(incidentService.getIncidentsByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request) {
        IncidentResponse response = incidentService.updateIncidentStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/remediation")
    public ResponseEntity<IncidentResponse> recordRemediation(
            @PathVariable Long id,
            @Valid @RequestBody RecordRemediationRequest request) {
        IncidentResponse response = incidentService.recordRemediationAttempt(id, request.getAction());
        return ResponseEntity.ok(response);
    }
}
