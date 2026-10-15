package com.medisense.audit;

import com.medisense.domain.entity.AuditLogEntity;
import com.medisense.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditQueryService {

    private static final Logger log = LoggerFactory.getLogger(AuditQueryService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLogEntity> getAuditEvents(
            AuditEvent eventType,
            UUID userId,
            Severity severity,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            int page,
            int size) {

        log.info("Fetching audit log entries with filters: eventType={}, userId={}, severity={}", eventType, userId, severity);
        return auditLogRepository.findWithFilters(
                eventType,
                userId,
                severity,
                dateFrom,
                dateTo,
                PageRequest.of(page, Math.min(size, 100))
        );
    }

    public List<AuditLogEntity> getRecentSecurityEvents() {
        OffsetDateTime last24Hours = OffsetDateTime.now().minusHours(24);
        log.info("Fetching WARN and CRITICAL security events since {}", last24Hours);
        return auditLogRepository.findSecurityEventsSince(last24Hours);
    }
}
