package com.medisense.e2e;

import com.medisense.E2ETestBase;
import com.medisense.api.dto.ClaimDtos.ClaimRequest;
import com.medisense.api.dto.ClaimDtos.ClaimResponse;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.domain.repository.UserRepository;
import com.medisense.fixtures.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End (E2E) Test: HealthClaimVerificationE2ETest
 * Validates the dual-framework health claim verification pipeline comparing allopathic
 * clinical trial evidence with traditional medicine without authoritative adjudication.
 */
class HealthClaimVerificationE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("E2E: Should evaluate health claim presenting both allopathic and traditional evidence bases")
    void shouldVerifyHealthClaimWithDualPositions() {
        UserEntity user = userRepository.save(TestDataBuilder.aUser().build());
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        ClaimRequest request = new ClaimRequest(
                session.getId(),
                "does drinking bitter gourd juice completely cure diabetes mellitus",
                "en"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ClaimRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/claims/verify",
                entity,
                ClaimResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ClaimResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.claim_assessment()).isNotNull();
        assertThat(body.claim_assessment().allopathic_position()).isNotNull();
        assertThat(body.claim_assessment().traditional_position()).isNotNull();
        assertThat(body.claim_assessment().allopathic_position().summary()).contains("Clinical trial evidence");
        assertThat(body.claim_assessment().traditional_position().summary()).contains("Ayurveda");
    }

    @Test
    @DisplayName("E2E: Should support multilingual claim verification with Tamil translation stubs")
    void shouldVerifyHealthClaimInTamil() {
        UserEntity user = userRepository.save(TestDataBuilder.aUser().build());
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        ClaimRequest request = new ClaimRequest(
                session.getId(),
                "பாகற்காய் சாறு சர்க்கரை நோயை முழுமையாக குணப்படுத்துமா",
                "ta"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ClaimRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ClaimResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/claims/verify",
                entity,
                ClaimResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ClaimResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.claim_assessment().allopathic_position().summary()).isNotBlank();
    }
}
