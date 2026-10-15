package com.medisense.ingestion;

import com.medisense.domain.entity.ManualReviewQueueEntity;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QualityGate1ServiceTest {

    @Mock
    private ManualReviewQueueRepository manualReviewQueueRepository;

    @InjectMocks
    private QualityGate1Service qualityGate1Service;

    @Test
    void testHappyPathValidation() {
        String longText = "clinical guideline ".repeat(100);
        ParsedDocument doc = ParsedDocument.builder()
                .documentName("guidelines.pdf")
                .rawText(longText)
                .extractionConfidence(0.95)
                .tokenCount(600)
                .build();

        assertDoesNotThrow(() -> qualityGate1Service.validateDocument(doc));
    }

    @Test
    void testLowConfidenceRejection() {
        ParsedDocument doc = ParsedDocument.builder()
                .documentName("low_conf.pdf")
                .rawText("sample content ".repeat(100))
                .extractionConfidence(0.70)
                .tokenCount(600)
                .build();

        DocumentQualityException ex = assertThrows(DocumentQualityException.class,
                () -> qualityGate1Service.validateDocument(doc));
        assertTrue(ex.getMessage().contains("Extraction confidence"));
        verify(manualReviewQueueRepository).save(any(ManualReviewQueueEntity.class));
    }

    @Test
    void testLowTokenCountRejection() {
        ParsedDocument doc = ParsedDocument.builder()
                .documentName("short.pdf")
                .rawText("Short clinical document with few words.")
                .extractionConfidence(0.95)
                .tokenCount(100)
                .build();

        DocumentQualityException ex = assertThrows(DocumentQualityException.class,
                () -> qualityGate1Service.validateDocument(doc));
        assertTrue(ex.getMessage().contains("token count"));
        verify(manualReviewQueueRepository).save(any(ManualReviewQueueEntity.class));
    }

    @Test
    void testFormularyTablePatternRejection() {
        String formularyText = "Drug Name\tDosage\tFrequency\tRoute\n" +
                "Atorvastatin\t10mg\tOnce daily\tOral\n" +
                "sample content ".repeat(100);

        ParsedDocument doc = ParsedDocument.builder()
                .documentName("formulary.pdf")
                .rawText(formularyText)
                .extractionConfidence(0.95)
                .tokenCount(600)
                .build();

        DocumentQualityException ex = assertThrows(DocumentQualityException.class,
                () -> qualityGate1Service.validateDocument(doc));
        assertTrue(ex.getMessage().contains("forbidden drug formulary table patterns"));
        verify(manualReviewQueueRepository).save(any(ManualReviewQueueEntity.class));
    }
}
