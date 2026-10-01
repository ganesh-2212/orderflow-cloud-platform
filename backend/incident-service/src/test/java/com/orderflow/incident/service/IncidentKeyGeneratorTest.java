package com.orderflow.incident.service;

import com.orderflow.incident.repository.IncidentRepository;
import com.orderflow.incident.entity.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentKeyGeneratorTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentKeyGenerator keyGenerator;

    private String todayPrefix;

    @BeforeEach
    void setUp() {
        todayPrefix = "INC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
    }

    @Test
    void testNoExistingIncidentReturns00001() {
        when(incidentRepository.findTopByIncidentKeyStartingWithOrderByIncidentKeyDesc(todayPrefix))
                .thenReturn(Optional.empty());

        String key = keyGenerator.generate();
        assertEquals(todayPrefix + "00001", key);
    }

    @Test
    void testExisting00001Returns00002() {
        Incident existing = new Incident();
        existing.setIncidentKey(todayPrefix + "00001");

        when(incidentRepository.findTopByIncidentKeyStartingWithOrderByIncidentKeyDesc(todayPrefix))
                .thenReturn(Optional.of(existing));

        String key = keyGenerator.generate();
        assertEquals(todayPrefix + "00002", key);
    }

    @Test
    void testExistingMultipleReturnsNextSequence() {
        Incident existing = new Incident();
        existing.setIncidentKey(todayPrefix + "00099");

        when(incidentRepository.findTopByIncidentKeyStartingWithOrderByIncidentKeyDesc(todayPrefix))
                .thenReturn(Optional.of(existing));

        String key = keyGenerator.generate();
        assertEquals(todayPrefix + "00100", key);
    }
}
