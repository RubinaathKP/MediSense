package com.medisense.abdm.parser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.medisense.domain.entity.LabReportValueEntity;
import com.medisense.domain.entity.PrescriptionEntity;
import com.medisense.domain.enums.FlagStatus;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class FhirBundleParser {

    private static final Logger log = LoggerFactory.getLogger(FhirBundleParser.class);

    private final FhirContext fhirContext;

    public FhirBundleParser(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public record ParsedFhirHealthData(
            List<LabReportValueEntity> labReportValues,
            List<PrescriptionEntity> prescriptions
    ) {}

    public ParsedFhirHealthData parseBundle(String fhirBundleRaw) {
        if (fhirBundleRaw == null || fhirBundleRaw.isBlank() || !fhirBundleRaw.contains("resourceType")) {
            log.warn("Empty or non-FHIR bundle received for parsing.");
            return new ParsedFhirHealthData(List.of(), List.of());
        }

        List<LabReportValueEntity> labReportValues = new ArrayList<>();
        List<PrescriptionEntity> prescriptions = new ArrayList<>();

        try {
            IParser parser = fhirBundleRaw.trim().startsWith("<") ? fhirContext.newXmlParser() : fhirContext.newJsonParser();
            parser.setParserErrorHandler(new ca.uhn.fhir.parser.LenientErrorHandler());

            Bundle bundle = parser.parseResource(Bundle.class, fhirBundleRaw);
            log.info("Parsed FHIR R4 Bundle with {} entry items.", bundle.getEntry().size());

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Observation obs) {
                    LabReportValueEntity entity = parseObservation(obs);
                    if (entity != null) labReportValues.add(entity);
                } else if (resource instanceof MedicationRequest medReq) {
                    PrescriptionEntity entity = parseMedicationRequest(medReq);
                    if (entity != null) prescriptions.add(entity);
                } else if (resource instanceof DiagnosticReport diagReport) {
                    List<LabReportValueEntity> extracted = parseDiagnosticReport(diagReport);
                    labReportValues.addAll(extracted);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse FHIR R4 bundle cleanly: {}. Returning empty parsed data.", e.getMessage());
            return new ParsedFhirHealthData(List.of(), List.of());
        }

        return new ParsedFhirHealthData(labReportValues, prescriptions);
    }

    public LabReportValueEntity parseObservation(Observation obs) {
        try {
            String testName = "Unknown Test";
            if (obs.hasCode()) {
                testName = obs.getCode().hasText() ? obs.getCode().getText() :
                        (obs.getCode().hasCoding() ? obs.getCode().getCodingFirstRep().getDisplay() : "Unknown Test");
            }

            String extractedValue = "N/A";
            BigDecimal normalizedValue = null;
            String unit = null;

            if (obs.hasValueQuantity()) {
                Quantity q = obs.getValueQuantity();
                extractedValue = q.getValue() != null ? q.getValue().toPlainString() : "N/A";
                normalizedValue = q.getValue();
                unit = q.getUnit() != null ? q.getUnit() : q.getCode();
            } else if (obs.hasValueCodeableConcept()) {
                extractedValue = obs.getValueCodeableConcept().hasText() ? obs.getValueCodeableConcept().getText() :
                        (obs.getValueCodeableConcept().hasCoding() ? obs.getValueCodeableConcept().getCodingFirstRep().getDisplay() : "N/A");
            } else if (obs.hasValueStringType()) {
                extractedValue = obs.getValueStringType().getValue();
            }

            String referenceRange = null;
            if (obs.hasReferenceRange() && !obs.getReferenceRange().isEmpty()) {
                Observation.ObservationReferenceRangeComponent ref = obs.getReferenceRangeFirstRep();
                String low = ref.hasLow() ? ref.getLow().getValue().toPlainString() : "";
                String high = ref.hasHigh() ? ref.getHigh().getValue().toPlainString() : "";
                if (!low.isBlank() || !high.isBlank()) {
                    referenceRange = low + " - " + high + (unit != null ? " " + unit : "");
                } else if (ref.hasText()) {
                    referenceRange = ref.getText();
                }
            }

            FlagStatus flagStatus = FlagStatus.NORMAL;
            if (obs.hasInterpretation() && !obs.getInterpretation().isEmpty()) {
                CodeableConcept interp = obs.getInterpretationFirstRep();
                if (interp.hasCoding()) {
                    String code = interp.getCodingFirstRep().getCode();
                    if (code != null) {
                        switch (code.toUpperCase()) {
                            case "H", "HIGH", "A", "ABNORMAL" -> flagStatus = FlagStatus.ABNORMAL;
                            case "L", "LOW" -> flagStatus = FlagStatus.BORDERLINE;
                            case "N", "NORMAL" -> flagStatus = FlagStatus.NORMAL;
                        }
                    }
                }
            }

            return LabReportValueEntity.builder()
                    .testName(testName)
                    .extractedValue(extractedValue)
                    .normalizedValue(normalizedValue)
                    .unit(unit)
                    .referenceRange(referenceRange)
                    .flagStatus(flagStatus)
                    .extractionConfidence(BigDecimal.valueOf(0.99))
                    .requiresUserConfirmation(false)
                    .build();

        } catch (Exception e) {
            log.warn("Error extracting canonical LabReportValue from FHIR Observation: {}", e.getMessage());
            return null;
        }
    }

    public PrescriptionEntity parseMedicationRequest(MedicationRequest medReq) {
        try {
            String drugName = "Unknown Medication";
            if (medReq.hasMedicationCodeableConcept()) {
                CodeableConcept concept = medReq.getMedicationCodeableConcept();
                drugName = concept.hasText() ? concept.getText() :
                        (concept.hasCoding() ? concept.getCodingFirstRep().getDisplay() : "Unknown Medication");
            } else if (medReq.hasMedicationReference()) {
                drugName = medReq.getMedicationReference().getDisplay();
            }

            String dosage = "As directed";
            String frequency = "Once daily";
            String timing = "With meals";
            String duration = "7 days";

            if (medReq.hasDosageInstruction() && !medReq.getDosageInstruction().isEmpty()) {
                Dosage d = medReq.getDosageInstructionFirstRep();
                if (d.hasText()) dosage = d.getText();
                if (d.hasDoseAndRate() && !d.getDoseAndRate().isEmpty() && d.getDoseAndRateFirstRep().hasDoseQuantity()) {
                    Quantity doseQty = d.getDoseAndRateFirstRep().getDoseQuantity();
                    dosage = doseQty.getValue().toPlainString() + " " + (doseQty.getUnit() != null ? doseQty.getUnit() : "mg");
                }
                if (d.hasTiming()) {
                    if (d.getTiming().hasCode() && d.getTiming().getCode().hasText()) {
                        frequency = d.getTiming().getCode().getText();
                    } else if (d.getTiming().hasRepeat()) {
                        frequency = d.getTiming().getRepeat().getFrequency() + " time(s) per " + d.getTiming().getRepeat().getPeriodUnit();
                    }
                }
            }

            if (medReq.hasDispenseRequest() && medReq.getDispenseRequest().hasExpectedSupplyDuration()) {
                Duration dur = medReq.getDispenseRequest().getExpectedSupplyDuration();
                duration = dur.getValue().toPlainString() + " " + (dur.getUnit() != null ? dur.getUnit() : "days");
            }

            String brandName = drugName;
            String genericName = drugName;
            String drugClass = "General Medication";

            return PrescriptionEntity.builder()
                    .brandName(brandName)
                    .genericName(genericName)
                    .drugClass(drugClass)
                    .dosage(dosage)
                    .frequency(frequency)
                    .timing(timing)
                    .duration(duration)
                    .extractionConfidence(BigDecimal.valueOf(0.99))
                    .requiresUserConfirmation(false)
                    .build();

        } catch (Exception e) {
            log.warn("Error extracting canonical Prescription from FHIR MedicationRequest: {}", e.getMessage());
            return null;
        }
    }

    public List<LabReportValueEntity> parseDiagnosticReport(DiagnosticReport diagReport) {
        List<LabReportValueEntity> results = new ArrayList<>();
        try {
            if (diagReport.hasResult()) {
                for (Reference ref : diagReport.getResult()) {
                    if (ref.getResource() instanceof Observation obs) {
                        LabReportValueEntity entity = parseObservation(obs);
                        if (entity != null) results.add(entity);
                    }
                }
            }
            if (results.isEmpty() && diagReport.hasConclusion()) {
                results.add(LabReportValueEntity.builder()
                        .testName("Diagnostic Impression")
                        .extractedValue(diagReport.getConclusion())
                        .flagStatus(FlagStatus.NORMAL)
                        .extractionConfidence(BigDecimal.valueOf(0.95))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error parsing FHIR DiagnosticReport: {}", e.getMessage());
        }
        return results;
    }
}
