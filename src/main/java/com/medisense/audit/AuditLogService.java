package com.medisense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.AuditLogEntity;
import com.medisense.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    public static final String DLQ_REDIS_KEY = "audit:dlq";

    private final AuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Queue<String> localDlq = new ConcurrentLinkedQueue<>();

    public AuditLogService(AuditLogRepository auditLogRepository,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("auditTaskExecutor")
    public void logAsync(AuditEventBuilder builder) {
        AuditLogEntity entity = builder.build();
        saveEntityWithDlqFallback(entity);
    }

    public void logSync(AuditEventBuilder builder) {
        AuditLogEntity entity = builder.build();
        saveEntityWithDlqFallback(entity);
    }

    public void saveEntityWithDlqFallback(AuditLogEntity entity) {
        try {
            auditLogRepository.save(entity);
            log.debug("Logged audit event: {} severity={}", entity.getEventType(), entity.getSeverity());
        } catch (Exception e) {
            log.error("CRITICAL: Failed to write audit log entry to database! Pushing to Dead Letter Queue (DLQ). Error: {}", e.getMessage());
            pushToDlq(entity);
        }
    }

    private void pushToDlq(AuditLogEntity entity) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(entity);
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForList().rightPush(DLQ_REDIS_KEY, jsonPayload);
                    log.info("Successfully pushed failed audit event to Redis DLQ: {}", entity.getEventType());
                    return;
                } catch (Exception ex) {
                    log.error("Redis DLQ push failed, storing in memory: {}", ex.getMessage());
                }
            }
            localDlq.add(jsonPayload);
        } catch (Exception ex) {
            log.error("Serialization failure while pushing to DLQ: {}", ex.getMessage(), ex);
        }
    }

    public Queue<String> getLocalDlq() {
        return localDlq;
    }
}
