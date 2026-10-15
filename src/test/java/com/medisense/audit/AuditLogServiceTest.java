package com.medisense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.AuditLogEntity;
import com.medisense.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogService auditLogService;
    private AuditLogRepository auditLogRepository;
    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOperations;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        auditLogService = new AuditLogService(auditLogRepository, redisTemplate, new ObjectMapper());
    }

    @Test
    void testLogAsyncSavesEntityToRepository() {
        UUID userId = UUID.randomUUID();
        AuditEventBuilder builder = AuditEventBuilder.of(AuditEvent.QUERY_RECEIVED)
                .user(userId)
                .ip("192.168.1.50")
                .queryText("what does LDL mean")
                .severity(Severity.INFO);

        auditLogService.logSync(builder);

        verify(auditLogRepository).save(argThat(entity ->
                entity.getEventType() == AuditEvent.QUERY_RECEIVED &&
                entity.getUserId().equals(userId) &&
                entity.getIpAddress().equals("192.168.1.50") &&
                entity.getEventDetail().contains("query_text_hash")
        ));
    }

    @Test
    void testLogFallbackToDlqOnDatabaseFailure() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("Database connection timeout"));

        AuditEventBuilder builder = AuditEventBuilder.of(AuditEvent.RATE_LIMIT_EXCEEDED)
                .ip("10.0.0.1")
                .severity(Severity.WARN);

        auditLogService.saveEntityWithDlqFallback(builder.build());

        // Verifies fallback pushed to Redis DLQ
        verify(listOperations).rightPush(eq(AuditLogService.DLQ_REDIS_KEY), anyString());
    }

    @Test
    void testPrivacyGuardrailStripsSensitiveHealthMetadata() {
        AuditEventBuilder builder = AuditEventBuilder.of(AuditEvent.REPORT_VALUE_CONFIRMED)
                .detail("drug_name", "Atorvastatin")
                .detail("lab_value", "145 mg/dL")
                .detail("safe_category", "lipid_management");

        AuditLogEntity entity = builder.build();

        assertTrue(entity.getEventDetail().contains("[STRIPPED_PRIVACY_PROTECTED]"));
        assertTrue(entity.getEventDetail().contains("lipid_management"));
        assertFalse(entity.getEventDetail().contains("Atorvastatin"));
        assertFalse(entity.getEventDetail().contains("145 mg/dL"));
    }
}
