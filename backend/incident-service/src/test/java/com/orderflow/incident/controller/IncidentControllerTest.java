package com.orderflow.incident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.incident.dto.request.CreateIncidentRequest;
import com.orderflow.incident.dto.request.RecordRemediationRequest;
import com.orderflow.incident.dto.request.UpdateIncidentStatusRequest;
import com.orderflow.incident.dto.response.IncidentResponse;
import com.orderflow.incident.entity.Incident;
import com.orderflow.incident.entity.IncidentCategory;
import com.orderflow.incident.entity.IncidentSeverity;
import com.orderflow.incident.entity.IncidentStatus;
import com.orderflow.incident.exception.IncidentNotFoundException;
import com.orderflow.incident.exception.InvalidIncidentTransitionException;
import com.orderflow.incident.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidentService incidentService;

    @Test
    void createIncident_Valid_Returns201() throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setServiceName("order");
        req.setCategory(IncidentCategory.UNKNOWN);
        req.setTitle("Fail");
        req.setDescription("Desc");

        Incident inc = new Incident();
        inc.setIncidentKey("INC-001");
        inc.setSeverity(IncidentSeverity.MEDIUM);
        IncidentResponse res = new IncidentResponse(inc);

        when(incidentService.createIncident(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentKey").value("INC-001"));
    }

    @Test
    void createIncident_Invalid_Returns400() throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest(); // Missing fields

        mockMvc.perform(post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncidentById_Returns200() throws Exception {
        Incident inc = new Incident();
        inc.setIncidentKey("INC-001");
        when(incidentService.getIncidentById(1L)).thenReturn(new IncidentResponse(inc));

        mockMvc.perform(get("/api/v1/incidents/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getIncidentById_NotFound_Returns404() throws Exception {
        when(incidentService.getIncidentById(99L)).thenThrow(new IncidentNotFoundException("Not found"));
        mockMvc.perform(get("/api/v1/incidents/99")).andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_Valid_Returns200() throws Exception {
        UpdateIncidentStatusRequest req = new UpdateIncidentStatusRequest(IncidentStatus.INVESTIGATING);
        Incident inc = new Incident();
        inc.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentService.updateIncidentStatus(eq(1L), eq(IncidentStatus.INVESTIGATING)))
                .thenReturn(new IncidentResponse(inc));

        mockMvc.perform(patch("/api/v1/incidents/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_InvalidTransition_Returns409() throws Exception {
        UpdateIncidentStatusRequest req = new UpdateIncidentStatusRequest(IncidentStatus.CLOSED);
        when(incidentService.updateIncidentStatus(eq(1L), eq(IncidentStatus.CLOSED)))
                .thenThrow(new InvalidIncidentTransitionException("Invalid"));

        mockMvc.perform(patch("/api/v1/incidents/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void recordRemediation_Valid_Returns200() throws Exception {
        RecordRemediationRequest req = new RecordRemediationRequest("RETRY");
        Incident inc = new Incident();
        inc.setRetryCount(1);
        when(incidentService.recordRemediationAttempt(1L, "RETRY")).thenReturn(new IncidentResponse(inc));

        mockMvc.perform(post("/api/v1/incidents/1/remediation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void recordRemediation_MaxLimit_Returns409() throws Exception {
        RecordRemediationRequest req = new RecordRemediationRequest("RETRY");
        when(incidentService.recordRemediationAttempt(1L, "RETRY"))
                .thenThrow(new IllegalStateException("Max attempts"));

        mockMvc.perform(post("/api/v1/incidents/1/remediation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
