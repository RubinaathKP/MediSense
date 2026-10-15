package com.medisense.api;

import com.medisense.abdm.AbdmCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController("apiAbdmCallbackController")
@RequestMapping("/api/v1/abdm/callback")
public class AbdmCallbackController {

    private static final Logger log = LoggerFactory.getLogger(AbdmCallbackController.class);

    private final AbdmCallbackHandler callbackHandler;

    public AbdmCallbackController(AbdmCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    @PostMapping("/consent/on-notify")
    public ResponseEntity<Map<String, String>> onConsentNotify(@RequestBody Map<String, Object> payload) {
        log.info("Received ABDM consent notification callback payload.");
        callbackHandler.handleConsentNotification("artefact-123", "GRANTED");
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @PostMapping("/data/on-transfer")
    public ResponseEntity<Map<String, String>> onDataTransfer(@RequestBody Map<String, Object> payload) {
        log.info("Received ABDM health data transfer callback payload.");
        callbackHandler.handleHealthInformationTransfer("tx-456", "ENCRYPTED_FHIR_BUNDLE_CONTENT");
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
