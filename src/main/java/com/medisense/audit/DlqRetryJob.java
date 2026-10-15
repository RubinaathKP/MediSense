package com.medisense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.AuditLogEntity;
import com.medisense.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;

@Component
public class DlqRetryJob {

    private static final Logger log = LoggerFactory.getLogger(DlqRetryJob.class);

    private final AuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public DlqRetryJob(AuditLogRepository auditLogRepository,
                       StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       AuditLogService auditLogService) {
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void processDlqItems() {
        log.info("Executing scheduled DLQ retry job for audit logs...");

        // 1. Process Redis DLQ
        if (redisTemplate != null) {
            try {
                String payload;
                int retriedCount = 0;
                while ((payload = redisTemplate.opsForList().leftPop(AuditLogService.DLQ_REDIS_KEY)) != null) {
                    processPayload(payload);
                    retriedCount++;
                }
                if (retriedCount > 0) {
                    log.info("Successfully reprocessed {} audit items from Redis DLQ.", retriedCount);
                }
            } catch (Exception e) {
                log.error("Error processing Redis DLQ: {}", e.getMessage());
            }
        }

        // 2. Process Local Memory DLQ Fallback
        Queue<String> localQueue = auditLogService.getLocalDlq();
        int localCount = 0;
        while (!localQueue.isEmpty()) {
            String payload = localQueue.poll();
            if (payload != null) {
                processPayload(payload);
                localCount++;
            }
        }
        if (localCount > 0) {
            log.info("Successfully reprocessed {} audit items from local memory DLQ.", localCount);
        }
    }

    private void processPayload(String payload) {
        try {
            AuditLogEntity entity = objectMapper.readValue(payload, AuditLogEntity.class);
            // Reset logId so database assigns fresh PK if needed
            entity.setLogId(null);
            auditLogRepository.save(entity);
            log.debug("DLQ retry success for event: {}", entity.getEventType());
        } catch (Exception e) {
            log.error("DLQ retry failed for item: {}. Returning to DLQ.", e.getMessage());
            // Put back into Redis if possible
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForList().rightPush(AuditLogService.DLQ_REDIS_KEY, payload);
                } catch (Exception ex) {
                    log.error("Failed to re-queue item in Redis DLQ: {}", ex.getMessage());
                }
            }
        }
    }
}
