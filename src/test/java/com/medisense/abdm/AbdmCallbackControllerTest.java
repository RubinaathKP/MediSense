package com.medisense.abdm;

import com.medisense.abdm.parser.FhirBundleParser;
import com.medisense.domain.entity.ConsentRequestEntity;
import com.medisense.domain.repository.ConsentRequestRepository;
import com.medisense.domain.repository.LabReportValueRepository;
import com.medisense.domain.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AbdmCallbackControllerTest {

    private ConsentRequestRepository consentRequestRepository;
    private HealthRecordFetchService healthRecordFetchService;
    private AbdmDataDecryptor dataDecryptor;
    private FhirBundleParser fhirBundleParser;
    private LabReportValueRepository labReportValueRepository;
    private PrescriptionRepository prescriptionRepository;
    private AbdmCallbackController controller;

    @BeforeEach
    void setUp() {
        consentRequestRepository = Mockito.mock(ConsentRequestRepository.class);
        healthRecordFetchService = Mockito.mock(HealthRecordFetchService.class);
        dataDecryptor = Mockito.mock(AbdmDataDecryptor.class);
        fhirBundleParser = Mockito.mock(FhirBundleParser.class);
        labReportValueRepository = Mockito.mock(LabReportValueRepository.class);
        prescriptionRepository = Mockito.mock(PrescriptionRepository.class);

        controller = new AbdmCallbackController(
                consentRequestRepository,
                healthRecordFetchService,
                dataDecryptor,
                fhirBundleParser,
                labReportValueRepository,
                prescriptionRepository
        );
    }

    @Test
    void testHandleConsentNotificationReturns202AcceptedImmediately() {
        Map<String, Object> body = Map.of(
                "notification", Map.of(
                        "consentRequestId", "req-uuid-101",
                        "status", "GRANTED",
                        "consentArtefacts", List.of(Map.of("id", "artefact-999"))
                )
        );

        ResponseEntity<Map<String, String>> response = controller.handleConsentNotification("sbx", body);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("ACCEPTED", response.getBody().get("status"));
    }

    @Test
    void testProcessConsentNotificationAsyncGrantedTriggersFetch() {
        ConsentRequestEntity entity = ConsentRequestEntity.builder()
                .consentRequestId("req-uuid-101")
                .status("REQUESTED")
                .build();

        Mockito.when(consentRequestRepository.findByConsentRequestId("req-uuid-101")).thenReturn(Optional.of(entity));

        Map<String, Object> body = Map.of(
                "notification", Map.of(
                        "consentRequestId", "req-uuid-101",
                        "status", "GRANTED",
                        "consentArtefacts", List.of(Map.of("id", "artefact-999"))
                )
        );

        controller.processConsentNotificationAsync(body);

        assertEquals("GRANTED", entity.getStatus());
        assertEquals("artefact-999", entity.getConsentArtefactId());
        Mockito.verify(healthRecordFetchService).fetchHealthRecords("artefact-999");
    }

    @Test
    void testHandleHealthInfoTransferReturns202AcceptedImmediately() {
        Map<String, Object> body = Map.of(
                "transactionId", "txn-uuid-888",
                "entries", List.of(Map.of("content", "{...}"))
        );

        ResponseEntity<Map<String, String>> response = controller.handleHealthInfoTransfer("sbx", body);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("ACCEPTED", response.getBody().get("status"));
    }
}
