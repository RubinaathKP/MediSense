package com.medisense.integration;

import com.medisense.IntegrationTestBase;
import com.medisense.api.QueryController;
import com.medisense.api.dto.QueryDtos.QueryRequest;
import com.medisense.api.dto.QueryDtos.QueryResponse;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.domain.repository.UserRepository;
import com.medisense.fixtures.AssertionHelpers;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import com.medisense.translation.TranslationOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Integration Test: QueryFlowIntegrationTest
 * Validates full inquiry lifecycle from HTTP request through guardrail classification,
 * RAG retrieval, grounded answer generation, and database session validation.
 */
class QueryFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @MockBean
    private HybridRetrieverService hybridRetrieverService;

    @MockBean
    private RagGenerationService ragGenerationService;

    @MockBean
    private TranslationOrchestrator translationOrchestrator;

    private QueryController queryController;

    @Override
    protected void onIntegrationSetup() {
        PatternLibraryLoader patternLoader = new PatternLibraryLoader();
        patternLoader.init();
        InputSafetyClassifier safetyClassifier = new InputSafetyClassifier(patternLoader, null);

        queryController = new QueryController(
                safetyClassifier,
                hybridRetrieverService,
                ragGenerationService,
                translationOrchestrator,
                sessionRepository
        );
    }

    @Test
    @DisplayName("Should successfully execute end-to-end query flow for grounded lab explanation")
    void shouldExecuteEndToEndQueryFlow() {
        UserEntity user = userRepository.save(TestDataBuilder.aUser().email("flow.patient@example.com").build());
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        QueryRequest request = new QueryRequest(
                session.getId(),
                "what does borderline LDL cholesterol mean for heart health",
                "lab_explanation",
                "en"
        );

        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt()))
                .thenReturn(List.of("Borderline high LDL cholesterol (130-159 mg/dL) represents moderate cardiovascular risk."));
        when(ragGenerationService.generateGroundedResponse(any(), any()))
                .thenReturn("Borderline high LDL means your cholesterol level is between 130 and 159 mg/dL.");

        ResponseEntity<QueryResponse> response = queryController.submitQuery(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse body = response.getBody();
        AssertionHelpers.assertThat(body)
                .isNotReferral()
                .hasCitations()
                .containsText("130 and 159 mg/dL");
    }

    @Test
    @DisplayName("Should safeguard prescriptive queries by intercepting before RAG generation")
    void shouldInterceptPrescriptiveQueriesInIntegrationFlow() {
        UserEntity user = userRepository.save(TestDataBuilder.aUser().email("flow.rx@example.com").build());
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        QueryRequest request = new QueryRequest(
                session.getId(),
                "should I take 10mg atorvastatin daily for borderline LDL",
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
}
