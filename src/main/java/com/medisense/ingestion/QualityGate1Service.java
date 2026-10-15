package com.medisense.ingestion;

import com.medisense.domain.entity.ManualReviewQueueEntity;
import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.model.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
public class QualityGate1Service {

    private static final Logger log = LoggerFactory.getLogger(QualityGate1Service.class);

    private static final double CONFIDENCE_THRESHOLD = 0.85;
    private static final int MIN_TOKENS_THRESHOLD = 500;

    // Pattern detecting drug formulary tables (drug name adjacent to dosage/frequency/route columns)
    private static final Pattern FORMULARY_TABLE_PATTERN = Pattern.compile(
            "(?i).*(drug|medicine|brand|generic|active ingredient)\\b.*(dosage|dose|frequency|route|administer|schedule)\\b.*",
            Pattern.DOTALL
    );

    private final ManualReviewQueueRepository manualReviewQueueRepository;

    public QualityGate1Service(ManualReviewQueueRepository manualReviewQueueRepository) {
        this.manualReviewQueueRepository = manualReviewQueueRepository;
    }

    public void validateDocument(ParsedDocument document) {
        log.info("Stage 2 - QualityGate1: Validating document '{}'.", document.getDocumentName());

        // Rule 1: Extraction Confidence Check
        if (document.getExtractionConfidence() < CONFIDENCE_THRESHOLD) {
            String reason = String.format("Extraction confidence (%.2f) below threshold (%.2f)",
                    document.getExtractionConfidence(), CONFIDENCE_THRESHOLD);
            rejectDocument(document, reason);
        }

        // Rule 2: Minimum Token Count Check
        if (document.getTokenCount() < MIN_TOKENS_THRESHOLD) {
            String reason = String.format("Document token count (%d) below minimum required (%d)",
                    document.getTokenCount(), MIN_TOKENS_THRESHOLD);
            rejectDocument(document, reason);
        }

        // Rule 3: Drug Formulary Table Pattern Detection
        if (FORMULARY_TABLE_PATTERN.matcher(document.getRawText()).matches()) {
            String reason = "Document contains forbidden drug formulary table patterns (drug names adjacent to dosage/frequency/route columns)";
            rejectDocument(document, reason);
        }

        log.info("Stage 2 - QualityGate1 PASSED for document '{}'.", document.getDocumentName());
    }

    private void rejectDocument(ParsedDocument document, String reason) {
        log.warn("Stage 2 - QualityGate1 REJECTED document '{}'. Reason: {}", document.getDocumentName(), reason);

        try {
            ManualReviewQueueEntity rejectionRecord = ManualReviewQueueEntity.builder()
                    .documentName(document.getDocumentName())
                    .rejectionReason(reason)
                    .rejectionStage("QualityGate1")
                    .extractionConfidence(BigDecimal.valueOf(document.getExtractionConfidence()))
                    .tokenCount(document.getTokenCount())
                    .build();

            manualReviewQueueRepository.save(rejectionRecord);
            log.info("Persisted document '{}' to manual_review_queue DB table.", document.getDocumentName());
        } catch (Exception e) {
            log.error("Failed to persist rejection record to manual_review_queue DB table: {}", e.getMessage(), e);
        }

        throw new DocumentQualityException(reason, document.getExtractionConfidence(), document.getTokenCount());
    }
}
