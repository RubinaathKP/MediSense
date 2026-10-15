package com.medisense.safety;

import com.medisense.audit.AuditEvent;
import com.medisense.audit.AuditEventBuilder;
import com.medisense.audit.AuditLogService;
import com.medisense.audit.Severity;
import com.medisense.safety.model.QuerySafetyCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SafetyPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SafetyPipelineOrchestrator.class);

    private final InputSafetyClassifier inputClassifier;
    private final OutputSafetyFilter outputFilter;
    private final CitationVerifier citationVerifier;
    private final AuditLogService auditLogService;

    public SafetyPipelineOrchestrator(InputSafetyClassifier inputClassifier,
                                      OutputSafetyFilter outputFilter,
                                      CitationVerifier citationVerifier,
                                      AuditLogService auditLogService) {
        this.inputClassifier = inputClassifier;
        this.outputFilter = outputFilter;
        this.citationVerifier = citationVerifier;
        this.auditLogService = auditLogService;
    }

    public record InputSafetyOutcome(
            boolean isSafe,
            QuerySafetyCategory category,
            String refusalResponse,
            String matchedPattern,
            double confidence
    ) {}

    public record PipelineResponse(
            boolean isSuccess,
            String finalResponseText,
            String refusalReason
    ) {}

    public InputSafetyOutcome processInputSafety(String queryText, String queryIdStr, String sessionIdStr) {
        log.info("Starting Stage 1 Input Safety Classification for queryId={}, sessionId={}", queryIdStr, sessionIdStr);
        var classification = inputClassifier.classifyInputQuery(queryText);

        if (auditLogService != null) {
            AuditEvent eventType = classification.isSafe() ? AuditEvent.QUERY_CLASSIFIED_SAFE : mapCategoryToEvent(classification.category());
            auditLogService.logAsync(AuditEventBuilder.of(eventType)
                    .session(parseUuid(sessionIdStr))
                    .queryId(parseUuid(queryIdStr))
                    .queryText(queryText)
                    .detail("matched_pattern", classification.matchedPattern())
                    .detail("confidence", classification.confidence())
                    .severity(classification.isSafe() ? Severity.INFO : Severity.WARN));
        }

        return new InputSafetyOutcome(
                classification.isSafe(),
                classification.category(),
                classification.refusalResponse(),
                classification.matchedPattern(),
                classification.confidence()
        );
    }

    public PipelineResponse processOutputSafety(String generatedText, List<String> retrievedChunkIds, String queryIdStr, String sessionIdStr) {
        log.info("Starting Stage 2 & 3 Output Safety & Citation Verification for queryId={}", queryIdStr);

        // Stage 2: Output Safety Filter
        var outputResult = outputFilter.filterGeneratedOutput(generatedText);
        if (!outputResult.isSafe()) {
            if (auditLogService != null) {
                auditLogService.logAsync(AuditEventBuilder.of(AuditEvent.OUTPUT_FILTER_TRIGGERED)
                        .session(parseUuid(sessionIdStr))
                        .queryId(parseUuid(queryIdStr))
                        .detail("flag_reason", outputResult.flagReason())
                        .severity(Severity.WARN));
            }
            log.warn("Stage 2 Output Safety Filter BLOCKED response for queryId={}", queryIdStr);
            return new PipelineResponse(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, outputResult.flagReason());
        }

        // Stage 3: Citation Verification
        var citationResult = citationVerifier.verifyCitations(generatedText, retrievedChunkIds);
        if (!citationResult.isGrounded()) {
            if (auditLogService != null) {
                auditLogService.logAsync(AuditEventBuilder.of(AuditEvent.CITATION_VERIFICATION_FAILED)
                        .session(parseUuid(sessionIdStr))
                        .queryId(parseUuid(queryIdStr))
                        .detail("hallucinated_count", citationResult.hallucinatedCitations() != null ? citationResult.hallucinatedCitations().size() : 0)
                        .severity(Severity.ERROR));
            }
            log.error("Stage 3 Citation Verifier BLOCKED response for queryId={} due to hallucinated citations: {}",
                    queryIdStr, citationResult.hallucinatedCitations());
            return new PipelineResponse(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, "HALLUCINATED_CITATION");
        }

        if (auditLogService != null) {
            auditLogService.logAsync(AuditEventBuilder.of(AuditEvent.CITATION_VERIFICATION_PASSED)
                    .session(parseUuid(sessionIdStr))
                    .queryId(parseUuid(queryIdStr))
                    .severity(Severity.INFO));
        }

        log.info("All 3 Safety Pipeline stages PASSED cleanly for queryId={}", queryIdStr);
        return new PipelineResponse(true, generatedText, null);
    }

    private AuditEvent mapCategoryToEvent(QuerySafetyCategory category) {
        if (category == null) return AuditEvent.QUERY_CLASSIFIED_SAFE;
        return switch (category) {
            case PRESCRIPTIVE -> AuditEvent.QUERY_CLASSIFIED_PRESCRIPTIVE;
            case DIAGNOSTIC -> AuditEvent.QUERY_CLASSIFIED_DIAGNOSTIC;
            case DRUG_INTERACTION -> AuditEvent.QUERY_CLASSIFIED_DRUG_INTERACTION;
            case EMERGENCY -> AuditEvent.QUERY_CLASSIFIED_EMERGENCY;
            default -> AuditEvent.QUERY_CLASSIFIED_SAFE;
        };
    }

    private UUID parseUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
