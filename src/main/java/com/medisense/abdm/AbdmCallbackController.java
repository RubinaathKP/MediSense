package com.medisense.abdm;

import com.medisense.abdm.parser.FhirBundleParser;
import com.medisense.domain.entity.ConsentRequestEntity;
import com.medisense.domain.repository.ConsentRequestRepository;
import com.medisense.domain.repository.LabReportValueRepository;
import com.medisense.domain.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/abdm/callback")
public class AbdmCallbackController {

    private static final Logger log = LoggerFactory.getLogger(AbdmCallbackController.class);

    private final ConsentRequestRepository consentRequestRepository;
    private final HealthRecordFetchService healthRecordFetchService;
    private final AbdmDataDecryptor dataDecryptor;
    private final FhirBundleParser fhirBundleParser;
    private final LabReportValueRepository labReportValueRepository;
    private final PrescriptionRepository prescriptionRepository;

    public AbdmCallbackController(ConsentRequestRepository consentRequestRepository,
                                  HealthRecordFetchService healthRecordFetchService,
                                  AbdmDataDecryptor dataDecryptor,
                                  FhirBundleParser fhirBundleParser,
                                  LabReportValueRepository labReportValueRepository,
                                  PrescriptionRepository prescriptionRepository) {
        this.consentRequestRepository = consentRequestRepository;
        this.healthRecordFetchService = healthRecordFetchService;
        this.dataDecryptor = dataDecryptor;
        this.fhirBundleParser = fhirBundleParser;
        this.labReportValueRepository = labReportValueRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @PostMapping("/consent/notify")
    public ResponseEntity<Map<String, String>> handleConsentNotification(
            @RequestHeader(value = "X-CM-ID", required = false) String cmId,
            @RequestBody Map<String, Object> body) {

        log.info("Received ABDM consent notification callback. CM-ID: {}, Body keys: {}", cmId, body.keySet());

        // Respond 202 Accepted immediately without blocking the callback
        CompletableFuture.runAsync(() -> processConsentNotificationAsync(body));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "message", "Consent notification received and queuing for processing."));
    }

    @Async
    @SuppressWarnings("unchecked")
    public void processConsentNotificationAsync(Map<String, Object> body) {
        try {
            Map<String, Object> notification = (Map<String, Object>) body.get("notification");
            if (notification == null) return;

            String consentRequestId = (String) notification.get("consentRequestId");
            String status = (String) notification.get("status");

            log.info("Async processing consent callback: consentRequestId={}, status={}", consentRequestId, status);

            Optional<ConsentRequestEntity> entityOpt = consentRequestRepository.findByConsentRequestId(consentRequestId);
            if (entityOpt.isPresent()) {
                ConsentRequestEntity entity = entityOpt.get();
                entity.setStatus(status);
                entity.setUpdatedAt(OffsetDateTime.now());

                if ("GRANTED".equalsIgnoreCase(status)) {
                    List<Map<String, Object>> artefacts = (List<Map<String, Object>>) notification.get("consentArtefacts");
                    if (artefacts != null && !artefacts.isEmpty()) {
                        String artefactId = (String) artefacts.get(0).get("id");
                        entity.setConsentArtefactId(artefactId);
                        consentRequestRepository.save(entity);

                        log.info("Consent GRANTED. Triggering HealthRecordFetchService for artefactId: {}", artefactId);
                        healthRecordFetchService.fetchHealthRecords(artefactId);
                    } else {
                        consentRequestRepository.save(entity);
                    }
                } else if ("DENIED".equalsIgnoreCase(status)) {
                    consentRequestRepository.save(entity);
                    log.warn("Consent DENIED by patient for consentRequestId: {}. Notifying session to fallback to PDF upload.", consentRequestId);
                } else {
                    consentRequestRepository.save(entity);
                }
            } else {
                log.warn("Consent notification received for untracked consentRequestId: {}", consentRequestId);
            }
        } catch (Exception e) {
            log.error("Error during async consent notification processing: {}", e.getMessage(), e);
        }
    }

    @PostMapping("/health-info")
    public ResponseEntity<Map<String, String>> handleHealthInfoTransfer(
            @RequestHeader(value = "X-CM-ID", required = false) String cmId,
            @RequestBody Map<String, Object> body) {

        log.info("Received ABDM health-info callback. CM-ID: {}, Body keys: {}", cmId, body.keySet());

        // Respond 202 Accepted immediately without blocking the callback
        CompletableFuture.runAsync(() -> processHealthInfoTransferAsync(body));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "message", "Health information payload received and queuing for processing."));
    }

    @Async
    @SuppressWarnings("unchecked")
    public void processHealthInfoTransferAsync(Map<String, Object> body) {
        try {
            String transactionId = (String) body.get("transactionId");
            List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entries");

            log.info("Async processing health info transfer: transactionId={}, entries={}",
                    transactionId, entries != null ? entries.size() : 0);

            if (entries != null) {
                for (Map<String, Object> entry : entries) {
                    String encryptedData = (String) entry.get("content");
                    String dhPublicKey = (String) entry.get("dhPublicKey");
                    String nonce = (String) entry.get("nonce");

                    if (encryptedData != null) {
                        String decryptedFhirJson = dataDecryptor.decryptHealthInformation(encryptedData, dhPublicKey, nonce);
                        FhirBundleParser.ParsedFhirHealthData parsedData = fhirBundleParser.parseBundle(decryptedFhirJson);

                        if (!parsedData.labReportValues().isEmpty()) {
                            labReportValueRepository.saveAll(parsedData.labReportValues());
                            log.info("Persisted {} canonical lab report values from ABDM FHIR bundle.", parsedData.labReportValues().size());
                        }

                        if (!parsedData.prescriptions().isEmpty()) {
                            prescriptionRepository.saveAll(parsedData.prescriptions());
                            log.info("Persisted {} canonical prescriptions from ABDM FHIR bundle.", parsedData.prescriptions().size());
                        }
                    }
                }
            }

            if (transactionId != null) {
                consentRequestRepository.findByTransactionId(transactionId).ifPresent(entity -> {
                    entity.setStatus("COMPLETED");
                    entity.setUpdatedAt(OffsetDateTime.now());
                    consentRequestRepository.save(entity);
                });
            }

        } catch (Exception e) {
            log.error("Error during async health info transfer processing: {}", e.getMessage(), e);
        }
    }
}
