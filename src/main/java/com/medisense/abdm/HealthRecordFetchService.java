package com.medisense.abdm;

import com.medisense.domain.repository.ConsentRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class HealthRecordFetchService {

    private static final Logger log = LoggerFactory.getLogger(HealthRecordFetchService.class);

    @Value("${medisense.abdm.gateway-url:https://dev.abdm.gov.in/gateway}")
    private String gatewayUrl;

    @Value("${medisense.abdm.hiu-id:medisense-hiu-01}")
    private String hiuId;

    private final AbdmAuthService authService;
    private final ConsentRequestRepository consentRequestRepository;
    private final RestTemplate restTemplate;

    public HealthRecordFetchService(AbdmAuthService authService,
                                   ConsentRequestRepository consentRequestRepository,
                                   RestTemplate restTemplate) {
        this.authService = authService;
        this.consentRequestRepository = consentRequestRepository;
        this.restTemplate = restTemplate;
    }

    public String fetchHealthRecords(String consentArtefactId) {
        log.info("Initiating ABDM health record fetch request for consent artefact ID: {}", consentArtefactId);

        String transactionId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        // Update database record with transaction ID
        consentRequestRepository.findByConsentArtefactId(consentArtefactId).ifPresent(entity -> {
            entity.setTransactionId(transactionId);
            consentRequestRepository.save(entity);
        });

        Map<String, Object> payload = Map.of(
                "requestId", requestId,
                "timestamp", timestamp,
                "hiRequest", Map.of(
                        "consent", Map.of("id", consentArtefactId),
                        "dateRange", Map.of("from", Instant.now().minusSeconds(86400 * 30).toString(), "to", Instant.now().toString()),
                        "dataPushUrl", "https://api.medisense.app/abdm/callback/health-info",
                        "keyMaterial", Map.of(
                                "cryptoAlg", "ECDH",
                                "curve", "Curve25519",
                                "dhPublicKey", Map.of("expiry", Instant.now().plusSeconds(86400).toString(), "parameters", "Curve25519", "keyValue", "dh-public-key-material"),
                                "nonce", "nonce-material-12345"
                        )
                )
        );

        try {
            String authToken = authService.getValidAccessToken();
            String url = gatewayUrl.endsWith("/") ? gatewayUrl + "v0.5/health-information/cm/request" : gatewayUrl + "/v0.5/health-information/cm/request";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);
            headers.set("X-CM-ID", "sbx");
            headers.set("REQUEST-ID", requestId);
            headers.set("TIMESTAMP", timestamp);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent health record request to ABDM. TransactionId: {}", transactionId);
            }
        } catch (Exception e) {
            log.warn("Failed to send health record request to ABDM gateway: {}. TransactionId {} recorded locally.", e.getMessage(), transactionId);
        }

        return transactionId;
    }
}
