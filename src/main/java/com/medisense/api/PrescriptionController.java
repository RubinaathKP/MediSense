package com.medisense.api;

import com.medisense.domain.dto.HealthQueryRequest;
import com.medisense.domain.dto.PrescriptionDto;
import com.medisense.domain.dto.PrescriptionUnderstandingResponse;
import com.medisense.domain.enums.SafetyDecision;
import com.medisense.safety.InputSafetyClassifier;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionController.class);

    private final InputSafetyClassifier safetyClassifier;

    public PrescriptionController(InputSafetyClassifier safetyClassifier) {
        this.safetyClassifier = safetyClassifier;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<PrescriptionDto>> uploadPrescription(@RequestParam("file") MultipartFile file) {
        log.info("Received prescription upload: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/understand")
    public ResponseEntity<PrescriptionUnderstandingResponse> understandPrescription(@Valid @RequestBody HealthQueryRequest request) {
        log.info("Received request for prescription understanding query: {}", request.getQueryText());

        var classification = safetyClassifier.classifyQuery(request.getQueryText());
        if (classification.decision() != SafetyDecision.PASSED) {
            return ResponseEntity.ok(PrescriptionUnderstandingResponse.builder()
                    .mechanismOfActionExplanation(classification.refusalResponse())
                    .safetyDecision(classification.decision())
                    .citations(Collections.emptyList())
                    .build());
        }

        return ResponseEntity.ok(PrescriptionUnderstandingResponse.builder()
                .brandName("Extracted Brand")
                .genericName("Extracted Generic")
                .drugClass("Pharmacological Class")
                .mechanismOfActionExplanation("This drug class works by inhibiting specific target receptors.")
                .indicationExplanation("Generally indicated for managing hypertension.")
                .plainLanguageDosing("Take once daily in the morning after meals.")
                .safetyDecision(SafetyDecision.PASSED)
                .citations(Collections.emptyList())
                .build());
    }
}
