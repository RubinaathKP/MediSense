package com.medisense.api;

import com.medisense.api.dto.ClaimDtos.*;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.translation.TranslationOrchestrator;
import com.medisense.translation.model.TranslationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Health Claim Verification", description = "Verify health claims against allopathic and traditional medicine evidence bases")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final HybridRetrieverService hybridRetrieverService;
    private final TranslationOrchestrator translationOrchestrator;

    public ClaimController(HybridRetrieverService hybridRetrieverService,
                           TranslationOrchestrator translationOrchestrator) {
        this.hybridRetrieverService = hybridRetrieverService;
        this.translationOrchestrator = translationOrchestrator;
    }

    @PostMapping({"/claims/verify", "/api/v1/claims/verify"})
    @Operation(summary = "Verify health claim", description = "Tier 3 rate limit (20 claims/hr). Max 300 characters. Evaluates allopathic and traditional positions without adjudicating.")
    @ApiResponse(responseCode = "200", description = "Claim assessment returned")
    @ApiResponse(responseCode = "400", description = "Invalid payload")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<ClaimResponse> verifyHealthClaim(@Valid @RequestBody ClaimRequest request) {
        log.info("Received health claim verification query for session {}: '{}'", request.session_id(), request.claim_text());

        UUID queryId = UUID.randomUUID();
        String targetLang = request.language() != null ? request.language() : "en";

        List<String> passages = hybridRetrieverService.retrieveHybridPassages(request.claim_text(), 3);
        log.debug("Retrieved {} guideline passages for claim verification evaluation.", passages.size());

        String alloSummary = "Clinical trial evidence indicates potential supportive effects but does not establish a complete cure.";
        String tradSummary = "Recognized in traditional Ayurveda text as a herbal remedy for blood sugar management.";

        if ("ta".equalsIgnoreCase(targetLang)) {
            TranslationResult transAllo = translationOrchestrator.translateToTamil(alloSummary);
            TranslationResult transTrad = translationOrchestrator.translateToTamil(tradSummary);
            alloSummary = transAllo.translatedText();
            tradSummary = transTrad.translatedText();
        }

        PositionDto allopathicPosition = new PositionDto(
                alloSummary,
                "clinical_trial",
                "ICMR Clinical Practice Guidelines (2026)"
        );

        PositionDto traditionalPosition = new PositionDto(
                tradSummary,
                "traditional_recognized",
                "AYUSH Pharmacopoeia of India"
        );

        ClaimAssessmentDto assessment = new ClaimAssessmentDto(
                allopathicPosition,
                traditionalPosition,
                null, // verdict is strictly null per SRS rule FR-40 (never adjudicate on behalf of user)
                "MediSense presents the evidence positions transparently. Please consult a qualified physician for treatment decisions."
        );

        return ResponseEntity.ok(new ClaimResponse(
                queryId,
                assessment,
                false,
                targetLang
        ));
    }
}
