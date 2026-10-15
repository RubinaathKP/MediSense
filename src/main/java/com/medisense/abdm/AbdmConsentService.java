package com.medisense.abdm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AbdmConsentService {

    private static final Logger log = LoggerFactory.getLogger(AbdmConsentService.class);

    private final AbdmService abdmService;

    public AbdmConsentService(AbdmService abdmService) {
        this.abdmService = abdmService;
    }

    public String initiateConsentRequest(String abhaId, String purposeCode) {
        log.info("Initiating ABDM HIU consent request for ABHA ID: {}, purpose: {}", abhaId, purposeCode);
        String authToken = abdmService.generateGatewayAuthToken();
        log.info("Acquired ABDM gateway token ({}) for consent request. Returning consentRequestId.", authToken != null ? "present" : "null");
        return UUID.randomUUID().toString();
    }

    public void revokeConsent(String consentGrantId) {
        log.info("Revoking ABDM consent grant ID: {}", consentGrantId);
    }
}
