package com.medisense.abdm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AbdmService {

    private static final Logger log = LoggerFactory.getLogger(AbdmService.class);

    @Value("${medisense.abdm.gateway-url}")
    private String gatewayUrl;

    @Value("${medisense.abdm.client-id}")
    private String clientId;

    public String generateGatewayAuthToken() {
        log.info("Requesting JWT authentication token from ABDM gateway at: {}", gatewayUrl);
        // Simulated JWT fetch from ABDM Sandbox Gateway
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.simulated_abdm_gateway_token";
    }
}
