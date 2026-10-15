package com.medisense.audit;

import com.medisense.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AbuseDetectionJobTest {

    private AbuseDetectionJob abuseDetectionJob;
    private AuditLogRepository auditLogRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuditLogService auditLogService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        auditLogService = mock(AuditLogService.class);
        abuseDetectionJob = new AbuseDetectionJob(auditLogRepository, redisTemplate, auditLogService);
    }

    @Test
    void testDetectAbusePatternsFlagsAbusiveUserAndIpInRedis() {
        UUID abusiveUserId = UUID.randomUUID();
        Object[] userRow = new Object[]{abusiveUserId, 15L};

        String abusiveIp = "203.0.113.5";
        Object[] ipRow = new Object[]{abusiveIp, 60L};

        when(auditLogRepository.findAbusiveUsers(any(OffsetDateTime.class), eq(10L)))
                .thenReturn(List.<Object[]>of(userRow));
        when(auditLogRepository.findAbusiveIps(any(OffsetDateTime.class), eq(50L)))
                .thenReturn(List.<Object[]>of(ipRow));

        abuseDetectionJob.detectAbusePatterns();

        // Verifies user flagged in Redis for 24h
        verify(valueOperations).set(eq("abuse:flag:" + abusiveUserId), eq("FLAGGED_ABUSIVE"), eq(Duration.ofHours(24)));

        // Verifies IP flagged in Redis for 1h
        verify(valueOperations).set(eq("abuse:ip:" + abusiveIp), eq("FLAGGED_ABUSIVE"), eq(Duration.ofHours(1)));

        // Verifies CRITICAL audit events logged
        verify(auditLogService, times(2)).logSync(any());
    }
}
