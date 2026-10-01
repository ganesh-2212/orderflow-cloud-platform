package com.orderflow.order.service;

import com.orderflow.order.entity.AuditLog;
import com.orderflow.order.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logAudit(String username, String action, String resourceType, String resourceId, String outcome) {
        AuditLog auditLog = new AuditLog(username, action, resourceType, resourceId, outcome);
        auditRepository.save(auditLog);
        log.info("Audit event: user={}, action={}, outcome={}", username, action, outcome);
    }
    
    public List<AuditLog> getAllAuditLogs() {
        return auditRepository.findAll();
    }
}
