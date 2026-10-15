package com.medisense.audit;

import com.medisense.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class AbuseDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(AbuseDetectionJob.class);

    public static final String ABUSE_USER_FLAG_PREFIX = "abuse:flag:";
    public static final String ABUSE_IP_FLAG_PREFIX = "abuse:ip:";

    private final AuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    public AbuseDetectionJob(AuditLogRepository auditLogRepository,
                             StringRedisTemplate redisTemplate,
                             AuditLogService auditLogService) {
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void detectAbusePatterns() {
        log.info("Running automated abuse detection pattern analysis job...");

        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);

        // Pattern 1: User with > 10 QUERY_CLASSIFIED_PRESCRIPTIVE events in 1 hour
        try {
            List<Object[]> abusiveUsers = auditLogRepository.findAbusiveUsers(oneHourAgo, 10L);
            for (Object[] row : abusiveUsers) {
                UUID userId = (UUID) row[0];
                Long count = (Long) row[1];

                log.warn("ABUSE DETECTED: User {} generated {} prescriptive queries in 1 hour.", userId, count);
                flagUserInRedis(userId, 24);

                // Log CRITICAL audit event
                auditLogService.logSync(AuditEventBuilder.of(AuditEvent.QUERY_CLASSIFIED_PRESCRIPTIVE)
                        .user(userId)
                        .severity(Severity.CRITICAL)
                        .detail("abuse_reason", "Excessive prescriptive query attempts (>10/hr)")
                        .detail("prescriptive_count", count));
            }
        } catch (Exception e) {
            log.error("Error evaluating user abuse patterns: {}", e.getMessage());
        }

        // Pattern 2: IP with > 50 RATE_LIMIT_EXCEEDED events in 1 hour
        try {
            List<Object[]> abusiveIps = auditLogRepository.findAbusiveIps(oneHourAgo, 50L);
            for (Object[] row : abusiveIps) {
                String ipAddress = (String) row[0];
                Long count = (Long) row[1];

                log.warn("ABUSE DETECTED: IP {} breached rate limits {} times in 1 hour.", ipAddress, count);
                flagIpInRedis(ipAddress, 1);

                // Log CRITICAL audit event
                auditLogService.logSync(AuditEventBuilder.of(AuditEvent.RATE_LIMIT_EXCEEDED)
                        .ip(ipAddress)
                        .severity(Severity.CRITICAL)
                        .detail("abuse_reason", "Excessive rate limit violations (>50/hr)")
                        .detail("violation_count", count));
            }
        } catch (Exception e) {
            log.error("Error evaluating IP abuse patterns: {}", e.getMessage());
        }
    }

    private void flagUserInRedis(UUID userId, int hoursTtl) {
        if (redisTemplate != null) {
            try {
                String key = ABUSE_USER_FLAG_PREFIX + userId;
                redisTemplate.opsForValue().set(key, "FLAGGED_ABUSIVE", Duration.ofHours(hoursTtl));
            } catch (Exception e) {
                log.error("Failed to set user abuse flag in Redis: {}", e.getMessage());
            }
        }
    }

    private void flagIpInRedis(String ipAddress, int hoursTtl) {
        if (redisTemplate != null) {
            try {
                String key = ABUSE_IP_FLAG_PREFIX + ipAddress;
                redisTemplate.opsForValue().set(key, "FLAGGED_ABUSIVE", Duration.ofHours(hoursTtl));
            } catch (Exception e) {
                log.error("Failed to set IP abuse flag in Redis: {}", e.getMessage());
            }
        }
    }
}
