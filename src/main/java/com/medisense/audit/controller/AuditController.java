package com.medisense.audit.controller;

import com.medisense.audit.AuditEvent;
import com.medisense.audit.AuditQueryService;
import com.medisense.audit.Severity;
import com.medisense.domain.entity.AuditLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/internal/audit", "/api/v1/internal/audit"})
@Tag(name = "Admin Audit Log Query", description = "Admin-only audit event inspection, filtering, and security event detection")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/events")
    @Operation(summary = "Get filtered audit events", description = "Rate limit: 10/hr/adminKey. Returns paginated list of audit entries.")
    @ApiResponse(responseCode = "200", description = "Paginated audit logs returned")
    public ResponseEntity<Page<AuditLogEntity>> getAuditEvents(
            @RequestParam(value = "event_type", required = false) AuditEvent eventType,
            @RequestParam(value = "user_id", required = false) UUID userId,
            @RequestParam(value = "severity", required = false) Severity severity,
            @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("Received request for filtered audit log events");
        Page<AuditLogEntity> results = auditQueryService.getAuditEvents(eventType, userId, severity, dateFrom, dateTo, page, size);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/security-events")
    @Operation(summary = "Get security events from last 24 hours", description = "Returns all WARN and CRITICAL audit events from last 24 hours.")
    @ApiResponse(responseCode = "200", description = "Security events returned")
    public ResponseEntity<List<AuditLogEntity>> getSecurityEvents() {
        log.info("Received request for last 24 hours security events");
        List<AuditLogEntity> events = auditQueryService.getRecentSecurityEvents();
        return ResponseEntity.ok(events);
    }
}
