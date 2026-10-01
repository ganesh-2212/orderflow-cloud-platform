package com.orderflow.incident.repository;

import com.orderflow.incident.entity.Incident;
import com.orderflow.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import com.orderflow.incident.entity.IncidentSeverity;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByIncidentKey(String incidentKey);
    Optional<Incident> findTopByIncidentKeyStartingWithOrderByIncidentKeyDesc(String prefix);
    List<Incident> findByStatus(IncidentStatus status);
    
    long countByStatus(IncidentStatus status);
    long countBySeverity(IncidentSeverity severity);
    
    @Query("SELECT COALESCE(SUM(i.retryCount), 0) FROM Incident i")
    long sumRetryCount();
}
