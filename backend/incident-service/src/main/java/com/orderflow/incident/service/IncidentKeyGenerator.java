package com.orderflow.incident.service;

import com.orderflow.incident.repository.IncidentRepository;
import com.orderflow.incident.entity.Incident;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class IncidentKeyGenerator {
    
    private final IncidentRepository incidentRepository;
    
    public IncidentKeyGenerator(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public synchronized String generate() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "INC-" + datePart + "-";
        
        Optional<Incident> latestIncident = incidentRepository.findTopByIncidentKeyStartingWithOrderByIncidentKeyDesc(prefix);
        
        int nextSequence = 1;
        if (latestIncident.isPresent()) {
            String latestKey = latestIncident.get().getIncidentKey();
            String sequencePart = latestKey.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(sequencePart) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        
        return String.format("%s%05d", prefix, nextSequence);
    }
}
