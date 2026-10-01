package com.orderflow.incident.service;

import com.orderflow.incident.dto.request.CreateIncidentRequest;
import com.orderflow.incident.dto.response.IncidentResponse;
import com.orderflow.incident.entity.Incident;
import com.orderflow.incident.entity.IncidentCategory;
import com.orderflow.incident.entity.IncidentSeverity;
import com.orderflow.incident.entity.IncidentStatus;
import com.orderflow.incident.exception.IncidentNotFoundException;
import com.orderflow.incident.exception.InvalidIncidentTransitionException;
import com.orderflow.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentKeyGenerator keyGenerator;

    @Mock
    private IncidentSeverityClassifier classifier;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private IncidentService incidentService;

    private Incident mockIncident;

    @BeforeEach
    void setUp() {
        mockIncident = new Incident();
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        incidentService = new IncidentService(incidentRepository, keyGenerator, classifier, meterRegistry);
        mockIncident.setIncidentKey("INC-20260928-00001");
        mockIncident.setServiceName("order-service");
        mockIncident.setCategory(IncidentCategory.FULFILLMENT_FAILURE);
        mockIncident.setSeverity(IncidentSeverity.HIGH);
        mockIncident.setStatus(IncidentStatus.OPEN);
        mockIncident.setRetryCount(0);
        
        try {
            var field = Incident.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(mockIncident, 1L);
        } catch (Exception ignored) {}
    }

    @Test
    void createIncident_Successfully() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setServiceName("order-service");
        request.setCategory(IncidentCategory.FULFILLMENT_FAILURE);
        request.setTitle("Fail");
        request.setDescription("Desc");

        when(keyGenerator.generate()).thenReturn("INC-20260928-00001");
        when(classifier.classify(any())).thenReturn(IncidentSeverity.HIGH);
        when(incidentRepository.save(any(Incident.class))).thenReturn(mockIncident);

        IncidentResponse response = incidentService.createIncident(request);

        assertNotNull(response);
        assertEquals("INC-20260928-00001", response.getIncidentKey());
        assertEquals(IncidentSeverity.HIGH, response.getSeverity());
    }

    @Test
    void getIncidentById_Successfully() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        IncidentResponse response = incidentService.getIncidentById(1L);
        assertEquals(1L, response.getId());
    }

    @Test
    void getIncidentByKey_Successfully() {
        when(incidentRepository.findByIncidentKey("INC-20260928-00001")).thenReturn(Optional.of(mockIncident));
        IncidentResponse response = incidentService.getIncidentByKey("INC-20260928-00001");
        assertEquals("INC-20260928-00001", response.getIncidentKey());
    }

    @Test
    void listIncidents_Successfully() {
        when(incidentRepository.findAll()).thenReturn(List.of(mockIncident));
        List<IncidentResponse> list = incidentService.getAllIncidents();
        assertEquals(1, list.size());
    }

    @Test
    void filterByStatus_Successfully() {
        when(incidentRepository.findByStatus(IncidentStatus.OPEN)).thenReturn(List.of(mockIncident));
        List<IncidentResponse> list = incidentService.getIncidentsByStatus(IncidentStatus.OPEN);
        assertEquals(1, list.size());
    }

    @Test
    void updateStatus_OpenToInvestigating_Successfully() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        when(incidentRepository.save(any())).thenReturn(mockIncident);

        IncidentResponse res = incidentService.updateIncidentStatus(1L, IncidentStatus.INVESTIGATING);
        assertEquals(IncidentStatus.INVESTIGATING, res.getStatus());
    }
    
    @Test
    void updateStatus_InvestigatingToMitigating_Successfully() {
        mockIncident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        when(incidentRepository.save(any())).thenReturn(mockIncident);

        IncidentResponse res = incidentService.updateIncidentStatus(1L, IncidentStatus.MITIGATING);
        assertEquals(IncidentStatus.MITIGATING, res.getStatus());
    }

    @Test
    void updateStatus_OpenToResolved_SetsResolvedAt() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        when(incidentRepository.save(any())).thenReturn(mockIncident);

        IncidentResponse res = incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, res.getStatus());
        assertNotNull(mockIncident.getResolvedAt());
    }

    @Test
    void updateStatus_InvalidTransition_ThrowsException() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        assertThrows(InvalidIncidentTransitionException.class, 
                () -> incidentService.updateIncidentStatus(1L, IncidentStatus.CLOSED));
    }

    @Test
    void remediation_IncrementsRetryAndChangesToMitigating() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));
        when(incidentRepository.save(any())).thenReturn(mockIncident);

        IncidentResponse res = incidentService.recordRemediationAttempt(1L, "RETRY");
        assertEquals(1, res.getRetryCount());
        assertEquals(IncidentStatus.MITIGATING, res.getStatus());
    }

    @Test
    void remediation_MaxAttempts_ThrowsException() {
        mockIncident.setRetryCount(3);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(mockIncident));

        assertThrows(IllegalStateException.class, 
                () -> incidentService.recordRemediationAttempt(1L, "RETRY"));
    }

    @Test
    void incidentNotFound_ThrowsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IncidentNotFoundException.class, 
                () -> incidentService.getIncidentById(99L));
    }
}
