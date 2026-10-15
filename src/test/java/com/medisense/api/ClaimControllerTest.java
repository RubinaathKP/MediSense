package com.medisense.api;

import com.medisense.api.dto.ClaimDtos.*;
import com.medisense.rag.retrieval.HybridRetrieverService;
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

class ClaimControllerTest {

    private ClaimController controller;
    private HybridRetrieverService hybridRetrieverService;
    private TranslationOrchestrator translationOrchestrator;

    @BeforeEach
    void setUp() {
        hybridRetrieverService = mock(HybridRetrieverService.class);
        translationOrchestrator = mock(TranslationOrchestrator.class);
        controller = new ClaimController(hybridRetrieverService, translationOrchestrator);
    }

    @Test
    void testVerifyHealthClaimReturnsBothPositionsWithoutAdjudication() {
        ClaimRequest request = new ClaimRequest(UUID.randomUUID(), "does bitter gourd juice cure diabetes completely", "en");
        when(hybridRetrieverService.retrieveHybridPassages(any(), anyInt())).thenReturn(List.of("Passage on bitter gourd"));

        ResponseEntity<ClaimResponse> response = controller.verifyHealthClaim(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().claim_assessment());
        assertNotNull(response.getBody().claim_assessment().allopathic_position());
        assertNotNull(response.getBody().claim_assessment().traditional_position());
        assertNull(response.getBody().claim_assessment().verdict()); // Verdict must be null per FR-40
        assertNotNull(response.getBody().claim_assessment().closing_note());
        assertFalse(response.getBody().is_referral());
    }
}
