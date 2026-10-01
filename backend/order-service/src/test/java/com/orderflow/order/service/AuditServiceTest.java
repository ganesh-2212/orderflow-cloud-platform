package com.orderflow.order.service;

import com.orderflow.order.entity.AuditLog;
import com.orderflow.order.repository.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void logAudit_SavesLogSuccessfully() {
        auditService.logAudit("admin", "LOGIN", "USER", "admin", "SUCCESS");
        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getAllAuditLogs_ReturnsLogs() {
        AuditLog log1 = new AuditLog("admin", "LOGIN", "USER", "admin", "SUCCESS");
        when(auditRepository.findAll()).thenReturn(List.of(log1));

        List<AuditLog> logs = auditService.getAllAuditLogs();
        assertEquals(1, logs.size());
        assertEquals("admin", logs.get(0).getUsername());
    }
}
