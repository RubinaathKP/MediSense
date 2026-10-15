package com.medisense.api;

import com.medisense.api.dto.QueryDtos.*;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import com.medisense.translation.TranslationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryFlowIntegrationTest {

    private QueryController queryController;
    private HybridRetrieverService hybridRetrieverService;
    private RagGenerationService ragGenerationService;

    @BeforeEach
    void setUp() {
        PatternLibraryLoader patternLoader = new PatternLibraryLoader();
        patternLoader.init();
        InputSafetyClassifier safetyClassifier = new InputSafetyClassifier(patternLoader, null);

        hybridRetrieverService = mock(HybridRetrieverService.class);
        ragGenerationService = mock(RagGenerationService.class);
        TranslationOrchestrator translationOrchestrator = mock(TranslationOrchestrator.class);
        SessionRepository sessionRepository = mock(SessionRepository.class);
        when(sessionRepository.findById(any()))
                .thenAnswer(inv -> java.util.Optional.of(com.medisense.domain.entity.SessionEntity.builder().id(inv.getArgument(0)).build()));

        queryController = new QueryController(
                safetyClassifier,
                hybridRetrieverService,
                ragGenerationService,
                translationOrchestrator,
                sessionRepository
        );
    }

    @Test
    void testFullQueryFlowFromPostQueryToGroundedResponseDelivery() {
        UUID sessionId = UUID.randomUUID();
        QueryRequest request = new QueryRequest(
                sessionId,
                "what does borderline LDL mean for my heart health",
                "lab_explanation",
                "en"
        );

        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt()))
                .thenReturn(List.of("Borderline high LDL cholesterol (130-159 mg/dL) indicates elevated risk."));
        when(ragGenerationService.generateGroundedResponse(any(), any()))
                .thenReturn("Borderline high LDL means your cholesterol level is between 130 and 159 mg/dL.");

        ResponseEntity<QueryResponse> response = queryController.submitQuery(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        QueryResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.is_referral());
        assertNotNull(body.query_id());
        assertTrue(body.response_text().contains("130 and 159 mg/dL"));
        assertFalse(body.citations().isEmpty());
        assertEquals("ICMR Clinical Guidelines", body.citations().get(0).source_name());
    }
}
