package com.medisense.abdm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AbdmCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(AbdmCallbackHandler.class);

    public void handleConsentNotification(String consentArtefactId, String status) {
        log.info("Received ABDM consent callback notification. ArtefactId: {}, Status: {}", consentArtefactId, status);
    }

    public void handleHealthInformationTransfer(String transactionId, String encryptedFhirBundle) {
        log.info("Received ABDM health information transfer callback. TransactionId: {}, Data payload length: {}", transactionId, encryptedFhirBundle.length());
    }
}
