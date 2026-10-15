package com.medisense.domain.repository;

import com.medisense.audit.AuditEvent;
import com.medisense.audit.Severity;
import com.medisense.domain.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.createdAt <= :dateTo) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findWithFilters(
            @Param("eventType") AuditEvent eventType,
            @Param("userId") UUID userId,
            @Param("severity") Severity severity,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLogEntity a WHERE a.severity IN (com.medisense.audit.Severity.WARN, com.medisense.audit.Severity.CRITICAL) AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findSecurityEventsSince(@Param("since") OffsetDateTime since);

    @Query("SELECT a.userId, COUNT(a) FROM AuditLogEntity a WHERE a.eventType = com.medisense.audit.AuditEvent.QUERY_CLASSIFIED_PRESCRIPTIVE AND a.createdAt >= :since AND a.userId IS NOT NULL GROUP BY a.userId HAVING COUNT(a) > :threshold")
    List<Object[]> findAbusiveUsers(@Param("since") OffsetDateTime since, @Param("threshold") long threshold);

    @Query("SELECT a.ipAddress, COUNT(a) FROM AuditLogEntity a WHERE a.eventType = com.medisense.audit.AuditEvent.RATE_LIMIT_EXCEEDED AND a.createdAt >= :since GROUP BY a.ipAddress HAVING COUNT(a) > :threshold")
    List<Object[]> findAbusiveIps(@Param("since") OffsetDateTime since, @Param("threshold") long threshold);
}
