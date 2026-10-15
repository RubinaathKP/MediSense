package com.medisense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.AuditLogEntity;
import com.medisense.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DlqRetryJobTest {

    private DlqRetryJob dlqRetryJob;
    private AuditLogRepository auditLogRepository;
    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOperations;
    private AuditLogService auditLogService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        ObjectMapper objectMapper = new ObjectMapper();
        auditLogService = new AuditLogService(auditLogRepository, redisTemplate, objectMapper);
        dlqRetryJob = new DlqRetryJob(auditLogRepository, redisTemplate, objectMapper, auditLogService);
    }

    @Test
    void testDlqRetryJobProcessesRedisItemsSuccessfully() throws Exception {
        AuditLogEntity entity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .eventType(AuditEvent.AUTH_LOGIN_FAILURE)
                .ipAddress("192.168.1.1")
                .severity(Severity.WARN)
                .build();

        String payload = new ObjectMapper().writeValueAsString(entity);

        when(listOperations.leftPop(AuditLogService.DLQ_REDIS_KEY))
                .thenReturn(payload)
                .thenReturn(null);

        dlqRetryJob.processDlqItems();

        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
}
