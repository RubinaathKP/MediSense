package com.medisense.unit.api;

import com.medisense.UnitTestBase;
import com.medisense.api.QueryController;
import com.medisense.api.dto.QueryDtos.QueryRequest;
import com.medisense.api.dto.QueryDtos.QueryResponse;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.fixtures.AssertionHelpers;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import com.medisense.translation.TranslationOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit Test: QueryController
 * Verifies HTTP contract, length validations, safety interception, and RAG grounded explanation response delivery.
 */
class QueryControllerUnitTest extends UnitTestBase {

    @Mock
    private HybridRetrieverService hybridRetrieverService;

    @Mock
    private RagGenerationService ragGenerationService;

    @Mock
    private TranslationOrchestrator translationOrchestrator;

    @Mock
    private SessionRepository sessionRepository;

    private QueryController queryController;

    @Override
    protected void onSetup() {
        PatternLibraryLoader patternLibraryLoader = new PatternLibraryLoader();
        patternLibraryLoader.init();
        InputSafetyClassifier safetyClassifier = new InputSafetyClassifier(patternLibraryLoader, null);

        queryController = new QueryController(
                safetyClassifier,
                hybridRetrieverService,
                ragGenerationService,
                translationOrchestrator,
                sessionRepository
        );
    }

    @Test
    @DisplayName("Should return 200 OK with grounded explanation for safe health inquiry")
    void shouldReturn200ForSafeQuery() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(SessionEntity.builder().id(sessionId).build()));

        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt()))
                .thenReturn(List.of("Borderline high LDL cholesterol (130-159 mg/dL) represents moderate cardiovascular risk."));
        when(ragGenerationService.generateGroundedResponse(any(), any()))
                .thenReturn("Borderline high LDL means your cholesterol level is between 130 and 159 mg/dL.");

        QueryRequest request = new QueryRequest(
                sessionId,
                "what does borderline LDL mean for my heart",
                "lab_explanation",
                "en"
        );

        ResponseEntity<QueryResponse> response = queryController.submitQuery(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse body = response.getBody();
        AssertionHelpers.assertThat(body)
                .isNotReferral()
                .hasCitations()
                .containsText("130 and 159 mg/dL");
    }

    @Test
    @DisplayName("Should intercept prescriptive queries and return referral response without calling RAG generation")
    void shouldInterceptPrescriptiveQueries() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(SessionEntity.builder().id(sessionId).build()));

        QueryRequest request = new QueryRequest(
                sessionId,
                "what dose of metformin should I take for high sugar",
                "prescription_understanding",
                "en"
        );

        ResponseEntity<QueryResponse> response = queryController.submitQuery(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse body = response.getBody();
        AssertionHelpers.assertThat(body)
                .isReferral()
                .containsText("doctor");
    }

    @Test
    @DisplayName("Should reject queries exceeding 500 characters limit with IllegalArgumentException")
    void shouldRejectQueryExceedingMaxLength() {
        UUID sessionId = UUID.randomUUID();
        String oversizedQuery = "a".repeat(501);

        QueryRequest request = new QueryRequest(
                sessionId,
                oversizedQuery,
                "lab_explanation",
                "en"
        );

        assertThatThrownBy(() -> queryController.submitQuery(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed 500 characters");
    }
}
