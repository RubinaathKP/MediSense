package com.medisense.ingestion;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class FhirParserService {

    private static final Logger log = LoggerFactory.getLogger(FhirParserService.class);
    private final IParser fhirJsonParser;

    public FhirParserService(IParser fhirJsonParser) {
        this.fhirJsonParser = fhirJsonParser;
    }

    public Bundle parseBundle(String fhirJsonContent) {
        log.info("Parsing FHIR R4 Bundle JSON payload.");
        try {
            return fhirJsonParser.parseResource(Bundle.class, fhirJsonContent);
        } catch (Exception e) {
            log.error("Failed to parse FHIR R4 Bundle: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing FHIR bundle", e);
        }
    }

    public List<Observation> extractObservations(Bundle bundle) {
        log.info("Extracting Observation resources from FHIR Bundle.");
        List<Observation> observations = new ArrayList<>();
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof Observation obs) {
                observations.add(obs);
            }
        });
        log.info("Extracted {} Observation resources.", observations.size());
        return observations;
    }

    public List<MedicationRequest> extractMedicationRequests(Bundle bundle) {
        log.info("Extracting MedicationRequest resources from FHIR Bundle.");
        List<MedicationRequest> medicationRequests = new ArrayList<>();
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof MedicationRequest req) {
                medicationRequests.add(req);
            }
        });
        log.info("Extracted {} MedicationRequest resources.", medicationRequests.size());
        return medicationRequests;
    }

    public List<DiagnosticReport> extractDiagnosticReports(Bundle bundle) {
        log.info("Extracting DiagnosticReport resources from FHIR Bundle.");
        List<DiagnosticReport> reports = new ArrayList<>();
        bundle.getEntry().forEach(entry -> {
            if (entry.getResource() instanceof DiagnosticReport report) {
                reports.add(report);
            }
        });
        log.info("Extracted {} DiagnosticReport resources.", reports.size());
        return reports;
    }
}
