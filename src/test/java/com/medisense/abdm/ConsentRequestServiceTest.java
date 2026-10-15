package com.medisense.abdm;

import com.medisense.domain.entity.ConsentRequestEntity;
import com.medisense.domain.repository.ConsentRequestRepository;
import com.medisense.domain.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsentRequestServiceTest {

    private AbdmAuthService authService;
    private ConsentRequestRepository consentRequestRepository;
    private SessionRepository sessionRepository;
    private RestTemplate restTemplate;
    private ConsentRequestService consentRequestService;

    @BeforeEach
    void setUp() {
        authService = mock(AbdmAuthService.class);
        consentRequestRepository = mock(ConsentRequestRepository.class);
        sessionRepository = mock(SessionRepository.class);
        restTemplate = mock(RestTemplate.class);

        consentRequestService = new ConsentRequestService(
                authService, consentRequestRepository, sessionRepository, restTemplate);

        ReflectionTestUtils.setField(consentRequestService, "gatewayUrl", "https://dev.abdm.gov.in/gateway");
        ReflectionTestUtils.setField(consentRequestService, "hiuId", "medisense-hiu-01");
    }

    @Test
    void testInitiateConsentRequestCreatesPayloadAndSavesEntity() {
        when(authService.getValidAccessToken()).thenReturn("valid_bearer_token");
        when(sessionRepository.findById(any())).thenReturn(Optional.empty());
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("status", "SUCCESS"), HttpStatus.OK));

        UUID sessionId = UUID.randomUUID();
        String requestId = consentRequestService.initiateConsentRequest("919999999999@sbx", sessionId);

        assertNotNull(requestId);

        ArgumentCaptor<ConsentRequestEntity> captor = ArgumentCaptor.forClass(ConsentRequestEntity.class);
        verify(consentRequestRepository).save(captor.capture());

        ConsentRequestEntity saved = captor.getValue();
        assertEquals(requestId, saved.getConsentRequestId());
        assertEquals("919999999999@sbx", saved.getPatientAbhaId());
        assertEquals("REQUESTED", saved.getStatus());
    }

    @Test
    void testRevokeConsentUpdatesEntityStatus() {
        ConsentRequestEntity entity = ConsentRequestEntity.builder()
                .consentArtefactId("artefact-123")
                .status("GRANTED")
                .build();

        when(consentRequestRepository.findByConsentArtefactId("artefact-123")).thenReturn(Optional.of(entity));

        consentRequestService.revokeConsent("artefact-123");

        assertEquals("REVOKED", entity.getStatus());
        verify(consentRequestRepository).save(entity);
    }
}
