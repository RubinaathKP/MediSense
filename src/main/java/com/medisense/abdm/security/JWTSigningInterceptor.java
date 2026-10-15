package com.medisense.abdm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class JWTSigningInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JWTSigningInterceptor.class);

    @Value("${medisense.abdm.hiu-id:medisense-hiu-01}")
    private String hiuId;

    @Value("${medisense.abdm.private-key:}")
    private String privateKeyPem;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        String requestId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        request.getHeaders().add("REQUEST-ID", requestId);
        request.getHeaders().add("TIMESTAMP", timestamp);
        request.getHeaders().add("X-CM-ID", "sbx");

        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            try {
                String signature = signPayload(body, privateKeyPem);
                request.getHeaders().add("X-HIP-SIGNATURE", signature);
            } catch (Exception e) {
                log.warn("Failed to sign request body with JWT private key: {}", e.getMessage());
            }
        }

        return execution.execute(request, body);
    }

    public String generateSignedJwtToken(String clientId) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        long now = Instant.now().getEpochSecond();
        String payload = String.format("{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"https://dev.abdm.gov.in/gateway/v0.5/sessions\",\"iat\":%d,\"exp\":%d}",
                clientId, clientId, now, now + 1800);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signingInput = header + "." + encodedPayload;
        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            try {
                String signature = signPayload(signingInput.getBytes(StandardCharsets.UTF_8), privateKeyPem);
                return signingInput + "." + signature;
            } catch (Exception e) {
                log.warn("Could not sign JWT token with private key, returning simulated token: {}", e.getMessage());
            }
        }
        return signingInput + ".simulated_rs256_signature";
    }

    private String signPayload(byte[] data, String pemKey) throws Exception {
        String cleanKey = pemKey.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }
}
