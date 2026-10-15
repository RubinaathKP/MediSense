package com.medisense.abdm;

import com.medisense.domain.entity.ConsentRequestEntity;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.repository.ConsentRequestRepository;
import com.medisense.domain.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ConsentRequestService {

    private static final Logger log = LoggerFactory.getLogger(ConsentRequestService.class);

    @Value("${medisense.abdm.gateway-url:https://dev.abdm.gov.in/gateway}")
    private String gatewayUrl;

    @Value("${medisense.abdm.hiu-id:medisense-hiu-01}")
    private String hiuId;

    private final AbdmAuthService authService;
    private final ConsentRequestRepository consentRequestRepository;
    private final SessionRepository sessionRepository;
    private final RestTemplate restTemplate;

    public ConsentRequestService(AbdmAuthService authService,
                                 ConsentRequestRepository consentRequestRepository,
                                 SessionRepository sessionRepository,
                                 RestTemplate restTemplate) {
        this.authService = authService;
        this.consentRequestRepository = consentRequestRepository;
        this.sessionRepository = sessionRepository;
        this.restTemplate = restTemplate;
    }

    public String initiateConsentRequest(String abhaAddress, UUID sessionId) {
        log.info("Initiating ABDM M3 consent request for ABHA ID: {}, session: {}", abhaAddress, sessionId);

        String requestId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        Instant now = Instant.now();
        Instant fromDate = now.minus(30, ChronoUnit.DAYS);
        Instant toDate = now;
        Instant dataEraseAt = now.plus(1, ChronoUnit.DAYS);

        // Fetch SessionEntity if present
        SessionEntity sessionEntity = null;
        if (sessionId != null) {
            sessionEntity = sessionRepository.findById(sessionId).orElse(null);
            if (sessionEntity != null && sessionEntity.getExpiresAt() != null) {
                dataEraseAt = sessionEntity.getExpiresAt().toInstant();
            }
        }

        // Build canonical M3 consent request payload
        Map<String, Object> payload = Map.of(
                "requestId", requestId,
                "timestamp", timestamp,
                "consent", Map.of(
                        "purpose", Map.of("code", "CAREMGT", "text", "Care Management Explanations"),
                        "patient", Map.of("id", abhaAddress),
                        "hiu", Map.of("id", hiuId),
                        "requester", Map.of(
                                "name", "MediSense Health Literacy Agent",
                                "identifier", Map.of("type", "REGNO", "value", "MH1001", "system", "https://www.nmc.org.in")
                        ),
                        "permission", Map.of(
                                "accessMode", "VIEW",
                                "dateRange", Map.of("from", fromDate.toString(), "to", toDate.toString()),
                                "dataEraseAt", dataEraseAt.toString(),
                                "frequency", Map.of("unit", "HOUR", "value", 1, "repeats", 0)
                        ),
                        "hiTypes", List.of("DiagnosticReport", "Observation", "MedicationRequest")
                )
        );

        // Save consent request entity to database
        ConsentRequestEntity entity = ConsentRequestEntity.builder()
                .consentRequestId(requestId)
                .session(sessionEntity)
                .patientAbhaId(abhaAddress)
                .status("REQUESTED")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        consentRequestRepository.save(entity);

        // Execute POST to ABDM gateway
        try {
            String authToken = authService.getValidAccessToken();
            String url = gatewayUrl.endsWith("/") ? gatewayUrl + "v0.5/consent-requests/init" : gatewayUrl + "/v0.5/consent-requests/init";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);
            headers.set("X-CM-ID", "sbx");
            headers.set("REQUEST-ID", requestId);
            headers.set("TIMESTAMP", timestamp);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent consent request to ABDM gateway. RequestId: {}", requestId);
            } else {
                log.warn("ABDM Gateway returned non-2xx status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to communicate with ABDM Sandbox Gateway for consent init: {}. Consent request {} stored locally.", e.getMessage(), requestId);
        }

        return requestId;
    }

    public void revokeConsent(String consentArtefactId) {
        log.info("Revoking ABDM consent grant artefact ID: {}", consentArtefactId);
        Optional<ConsentRequestEntity> entityOpt = consentRequestRepository.findByConsentArtefactId(consentArtefactId);
        if (entityOpt.isPresent()) {
            ConsentRequestEntity entity = entityOpt.get();
            entity.setStatus("REVOKED");
            entity.setUpdatedAt(OffsetDateTime.now());
            consentRequestRepository.save(entity);
        }
    }
}
