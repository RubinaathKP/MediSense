package com.medisense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.AuditLogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuditEventBuilder {

    private static final Logger log = LoggerFactory.getLogger(AuditEventBuilder.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuditEvent eventType;
    private UUID sessionId;
    private UUID userId;
    private UUID queryId;
    private String consentGrantId;
    private String ipAddress = "0.0.0.0";
    private Severity severity = Severity.INFO;
    private final Map<String, Object> details = new HashMap<>();

    private AuditEventBuilder(AuditEvent eventType) {
        this.eventType = eventType;
    }

    public static AuditEventBuilder of(AuditEvent eventType) {
        return new AuditEventBuilder(eventType);
    }

    public AuditEventBuilder session(UUID sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public AuditEventBuilder user(UUID userId) {
        this.userId = userId;
        return this;
    }

    public AuditEventBuilder queryId(UUID queryId) {
        this.queryId = queryId;
        return this;
    }

    public AuditEventBuilder consentGrant(String consentGrantId) {
        this.consentGrantId = consentGrantId;
        return this;
    }

    public AuditEventBuilder ip(String ipAddress) {
        if (ipAddress != null && !ipAddress.isBlank()) {
            this.ipAddress = ipAddress;
        }
        return this;
    }

    public AuditEventBuilder severity(Severity severity) {
        if (severity != null) {
            this.severity = severity;
        }
        return this;
    }

    public AuditEventBuilder queryText(String plaintextQuery) {
        if (plaintextQuery != null && !plaintextQuery.isBlank()) {
            String hash = hashSha256(plaintextQuery);
            this.details.put("query_text_hash", hash);
        }
        return this;
    }

    public AuditEventBuilder detail(String key, Object value) {
        if (key == null || value == null) return this;
        String lowerKey = key.toLowerCase();
        // Privacy guardrail: prohibit embedding drug names, dosage values, or raw health data
        if (lowerKey.contains("drug_name") || lowerKey.contains("dosage") || lowerKey.contains("lab_value") || lowerKey.contains("raw_text")) {
            log.warn("Privacy Guardrail: Blocked attempt to include sensitive health metadata '{}' in audit detail", key);
            this.details.put(key, "[STRIPPED_PRIVACY_PROTECTED]");
            return this;
        }
        this.details.put(key, value);
        return this;
    }

    public AuditLogEntity build() {
        String detailJson = null;
        if (!details.isEmpty()) {
            try {
                detailJson = MAPPER.writeValueAsString(details);
            } catch (Exception e) {
                log.error("Failed to serialize audit detail map: {}", e.getMessage());
                detailJson = "{}";
            }
        }

        return AuditLogEntity.builder()
                .eventType(eventType)
                .sessionId(sessionId)
                .userId(userId)
                .queryId(queryId)
                .consentGrantId(consentGrantId)
                .ipAddress(ipAddress)
                .eventDetail(detailJson)
                .severity(severity)
                .build();
    }

    private static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm unavailable: {}", e.getMessage());
            return "HASH_ERROR";
        }
    }
}
