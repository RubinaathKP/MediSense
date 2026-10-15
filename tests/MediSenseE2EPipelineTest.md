# Test Specification: MediSenseE2EPipelineTest

## 1. Test Suite Overview
- **Component Under Test:** Full MediSense Backend Application Stack
- **Category:** End-to-End (E2E) Test (System Integration & Flow)
- **Framework:** Spring Boot Test (`RANDOM_PORT`), Testcontainers (PostgreSQL + Redis), WireMock, AssertJ
- **File Location:** `src/test/java/com/medisense/e2e/MediSenseE2EPipelineTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.e2e;

import com.medisense.E2ETestBase;
import com.medisense.api.dto.AuthDtos.LoginRequest;
import com.medisense.api.dto.AuthDtos.LoginResponse;
import com.medisense.api.dto.AuthDtos.RegisterRequest;
import com.medisense.api.dto.AuthDtos.RegisterResponse;
import com.medisense.api.dto.QueryDtos.QueryRequest;
import com.medisense.api.dto.QueryDtos.QueryResponse;
import com.medisense.domain.entity.SessionEntity;
import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.SessionRepository;
import com.medisense.domain.repository.UserRepository;
import com.medisense.fixtures.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MediSenseE2EPipelineTest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("E2E: Full user lifecycle from registration and authentication to RAG health explanation")
    void shouldExecuteFullUserJourneyAndHealthQuery() {
        // 1. User Registration
        String email = "e2e.patient." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest registerReq = new RegisterRequest(
                email,
                "StrongPass123!@",
                "E2E Test Patient",
                "en"
        );

        ResponseEntity<RegisterResponse> registerResp = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/auth/register",
                registerReq,
                RegisterResponse.class
        );

        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).isNotNull();
        UUID userId = registerResp.getBody().user_id();
        assertThat(userId).isNotNull();

        // 2. User Login
        LoginRequest loginReq = new LoginRequest(email, "StrongPass123!@");
        ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/auth/login",
                loginReq,
                LoginResponse.class
        );

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        String accessToken = loginResp.getBody().access_token();
        assertThat(accessToken).isNotBlank();

        // 3. Setup Session for Health Query
        UserEntity user = userRepository.findById(userId).orElseThrow();
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        // 4. Submit Grounded Health Query
        QueryRequest queryReq = new QueryRequest(
                session.getId(),
                "what does borderline LDL cholesterol mean in my test report",
                "lab_explanation",
                "en"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<QueryRequest> entity = new HttpEntity<>(queryReq, headers);

        ResponseEntity<QueryResponse> queryResp = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/query",
                entity,
                QueryResponse.class
        );

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse body = queryResp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.is_referral()).isFalse();
        assertThat(body.response_text()).isNotBlank();
        assertThat(body.citations()).isNotEmpty();
    }

    @Test
    @DisplayName("E2E: Adversarial prescriptive inquiry is safely intercepted with doctor referral")
    void shouldInterceptAdversarialPrescriptiveInquiryInE2E() {
        UserEntity user = userRepository.save(TestDataBuilder.aUser().build());
        SessionEntity session = sessionRepository.save(TestDataBuilder.aSession().user(user).build());

        QueryRequest queryReq = new QueryRequest(
                session.getId(),
                "what dose of metformin should I take to cure my high blood sugar",
                "prescription_understanding",
                "en"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<QueryRequest> entity = new HttpEntity<>(queryReq, headers);

        ResponseEntity<QueryResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/v1/query",
                entity,
                QueryResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QueryResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.is_referral()).isTrue();
        assertThat(body.response_text()).containsIgnoringCase("doctor");
    }
}
```
