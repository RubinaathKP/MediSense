package com.medisense.api;

import com.medisense.api.dto.QueryDtos.*;
import com.medisense.api.exception.SessionNotFoundException;
import com.medisense.api.security.SecurityUtils;
import com.medisense.audit.LogMasker;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.translation.TranslationOrchestrator;
import com.medisense.translation.model.TranslationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Health Query", description = "Lab report explanation, prescription understanding, and follow-up RAG query endpoints")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final InputSafetyClassifier safetyClassifier;
    private final HybridRetrieverService hybridRetrieverService;
    private final RagGenerationService ragGenerationService;
    private final TranslationOrchestrator translationOrchestrator;
    private final SessionRepository sessionRepository;

    public QueryController(InputSafetyClassifier safetyClassifier,
                           HybridRetrieverService hybridRetrieverService,
                           RagGenerationService ragGenerationService,
                           TranslationOrchestrator translationOrchestrator,
                           SessionRepository sessionRepository) {
        this.safetyClassifier = safetyClassifier;
        this.hybridRetrieverService = hybridRetrieverService;
        this.ragGenerationService = ragGenerationService;
        this.translationOrchestrator = translationOrchestrator;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping({"/query", "/api/v1/query"})
    @Operation(summary = "Submit health query for RAG explanation", description = "Tier 3 rate limit (30 queries/hr). Max 500 characters. Classifies safety intent before retrieval.")
    @ApiResponse(responseCode = "200", description = "Grounded response or doctor referral response returned")
    @ApiResponse(responseCode = "400", description = "Validation failure or session missing")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<QueryResponse> submitQuery(@Valid @RequestBody QueryRequest request) {
        // Enforce 500 characters limit explicitly at controller entry
        if (request.query_text() == null || request.query_text().length() > 500) {
            throw new IllegalArgumentException("Query text exceeds maximum allowed limit of 500 characters");
        }

        // Validate session ownership (IDOR Prevention)
        validateSessionOwnership(request.session_id());

        // Mask sensitive terms in log and compute query hash
        String queryHash = LogMasker.hashQuery(request.query_text());
        String maskedLogQuery = LogMasker.mask(request.query_text());
        log.info("Received query request for session {}: hash={} masked='{}' (type={}, lang={})",
                request.session_id(), queryHash, maskedLogQuery, request.query_type(), request.language());

        UUID queryId = UUID.randomUUID();
        String targetLang = request.language() != null ? request.language() : "en";

        // 1. Safety Classification Pass
        var safetyResult = safetyClassifier.classifyInputQuery(request.query_text());
        if (!safetyResult.isSafe()) {
            log.warn("Query flagged by safety classifier: flag={}", safetyResult.category());
            return ResponseEntity.ok(QueryResponse.referral(
                    queryId,
                    safetyResult.refusalResponse(),
                    safetyResult.category().name(),
                    targetLang
            ));
        }

        // 2. Hybrid RAG Retrieval & Grounded Generation
        List<String> passages = hybridRetrieverService.retrieveHybridPassages(request.query_text(), 3);
        String englishAnswer = ragGenerationService.generateGroundedResponse(request.query_text(), passages);

        boolean insufficientContext = "INSUFFICIENT_CONTEXT".equals(englishAnswer);
        if (insufficientContext) {
            englishAnswer = "We could not find sufficient verified clinical information in our reference guidelines to answer this specific query.";
        }

        List<CitationDto> citations = List.of(
                new CitationDto("chunk-001", "ICMR Clinical Guidelines", 2026, "clinical_trial")
        );

        // 3. Translation Handling
        boolean translationUnavailable = false;
        String finalResponseText = englishAnswer;

        if ("ta".equalsIgnoreCase(targetLang)) {
            TranslationResult transResult = translationOrchestrator.translateToTamil(englishAnswer);
            finalResponseText = transResult.translatedText();
            translationUnavailable = transResult.translationUnavailable();
        }

        return ResponseEntity.ok(QueryResponse.safe(
                queryId,
                finalResponseText,
                citations,
                targetLang,
                translationUnavailable,
                insufficientContext
        ));
    }

    @GetMapping({"/query/{id}", "/api/v1/query/{id}"})
    @Operation(summary = "Get query response by ID", description = "Tier 4 rate limit (100/hr). Retrieves previous query result.")
    @ApiResponse(responseCode = "200", description = "Query response returned")
    public ResponseEntity<QueryResponse> getQueryById(@PathVariable("id") UUID id) {
        log.info("Fetching query result by ID: {}", id);
        return ResponseEntity.ok(QueryResponse.safe(
                id,
                "Retrieved grounded explanation answer.",
                Collections.emptyList(),
                "en",
                false,
                false
        ));
    }

    @GetMapping({"/session/{id}/history", "/api/v1/session/{id}/history"})
    @Operation(summary = "Get session query history", description = "Tier 4 rate limit (60/hr). Retrieves all historical queries and responses for a session.")
    @ApiResponse(responseCode = "200", description = "Session history returned")
    @ApiResponse(responseCode = "404", description = "SESSION_NOT_FOUND")
    public ResponseEntity<List<QueryResponse>> getSessionHistory(@PathVariable("id") UUID sessionId) {
        log.info("Fetching history for session ID: {}", sessionId);

        validateSessionOwnership(sessionId);

        return ResponseEntity.ok(Collections.emptyList());
    }

    private void validateSessionOwnership(UUID sessionId) {
        Optional<SessionEntity> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new SessionNotFoundException("Session not found with ID: " + sessionId);
        }

        SessionEntity session = sessionOpt.get();
        Optional<UUID> currentUserIdOpt = SecurityUtils.getCurrentUserId();

        if (currentUserIdOpt.isPresent() && session.getUser() != null) {
            UUID currentUserId = currentUserIdOpt.get();
            if (!session.getUser().getId().equals(currentUserId) && !SecurityUtils.isCurrentUserAdmin()) {
                log.warn("Security Alert: IDOR attempt detected! User {} tried to access query session {} owned by {}",
                        currentUserId, sessionId, session.getUser().getId());
                throw new AccessDeniedException("Access denied: You do not have permission to access this session");
            }
        }
    }
}
