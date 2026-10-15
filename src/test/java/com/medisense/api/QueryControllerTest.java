package com.medisense.api;

import com.medisense.api.dto.QueryDtos.*;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import com.medisense.translation.TranslationOrchestrator;
import com.medisense.translation.model.TranslationResult;
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

class QueryControllerTest {

    private QueryController controller;
    private InputSafetyClassifier safetyClassifier;
    private HybridRetrieverService hybridRetrieverService;
    private RagGenerationService ragGenerationService;
    private TranslationOrchestrator translationOrchestrator;
    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        PatternLibraryLoader patternLoader = new PatternLibraryLoader();
        patternLoader.init();
        safetyClassifier = new InputSafetyClassifier(patternLoader, null);

        hybridRetrieverService = mock(HybridRetrieverService.class);
        ragGenerationService = mock(RagGenerationService.class);
        translationOrchestrator = mock(TranslationOrchestrator.class);
        sessionRepository = mock(SessionRepository.class);

        when(sessionRepository.findById(any()))
                .thenAnswer(inv -> java.util.Optional.of(com.medisense.domain.entity.SessionEntity.builder().id(inv.getArgument(0)).build()));

        controller = new QueryController(
                safetyClassifier,
                hybridRetrieverService,
                ragGenerationService,
                translationOrchestrator,
                sessionRepository
        );
    }

    @Test
    void testSubmitQuerySafeResponse() {
        UUID sessionId = UUID.randomUUID();
        QueryRequest request = new QueryRequest(sessionId, "what does borderline LDL mean", "lab_explanation", "en");

        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt())).thenReturn(List.of("Borderline LDL passage"));
        when(ragGenerationService.generateGroundedResponse(any(), any())).thenReturn("Borderline LDL indicates 130-159 mg/dL.");

        ResponseEntity<QueryResponse> response = controller.submitQuery(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().is_referral());
        assertEquals("Borderline LDL indicates 130-159 mg/dL.", response.getBody().response_text());
        assertFalse(response.getBody().citations().isEmpty());
    }

    @Test
    void testSubmitQueryFlaggedPrescriptiveResponse() {
        UUID sessionId = UUID.randomUUID();
        QueryRequest request = new QueryRequest(sessionId, "what dose of metformin should I take", "prescription_explanation", "en");

        ResponseEntity<QueryResponse> response = controller.submitQuery(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().is_referral());
        assertEquals("PRESCRIPTIVE", response.getBody().classifier_flag());
        assertTrue(response.getBody().citations().isEmpty());
    }

    @Test
    void testSubmitQueryTamilTranslation() {
        UUID sessionId = UUID.randomUUID();
        QueryRequest request = new QueryRequest(sessionId, "what does borderline LDL mean", "lab_explanation", "ta");

        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt())).thenReturn(List.of("Borderline LDL passage"));
        when(ragGenerationService.generateGroundedResponse(any(), any())).thenReturn("Borderline LDL indicates 130-159 mg/dL.");
        when(translationOrchestrator.translateToTamil(any())).thenReturn(TranslationResult.success("வரம்புநிலை LDL விளக்குகிறது", "orig", "en", "ta"));

        ResponseEntity<QueryResponse> response = controller.submitQuery(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ta", response.getBody().language_out());
        assertEquals("வரம்புநிலை LDL விளக்குகிறது", response.getBody().response_text());
    }
}
