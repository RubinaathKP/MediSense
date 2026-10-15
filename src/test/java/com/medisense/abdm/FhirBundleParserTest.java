package com.medisense.abdm;

import ca.uhn.fhir.context.FhirContext;
import com.medisense.abdm.parser.FhirBundleParser;
import com.medisense.domain.entity.LabReportValueEntity;
import com.medisense.domain.entity.PrescriptionEntity;
import com.medisense.domain.enums.FlagStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FhirBundleParserTest {

    private FhirBundleParser parser;

    @BeforeEach
    void setUp() {
        FhirContext fhirContext = FhirContext.forR4();
        parser = new FhirBundleParser(fhirContext);
    }

    @Test
    void testParseFhirBundleWithObservationAndMedicationRequest() {
        String jsonBundle = """
                {
                  "resourceType": "Bundle",
                  "type": "collection",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Observation",
                        "status": "final",
                        "code": {
                          "text": "Hemoglobin A1c"
                        },
                        "valueQuantity": {
                          "value": 7.8,
                          "unit": "%"
                        },
                        "referenceRange": [
                          {
                            "low": { "value": 4.0 },
                            "high": { "value": 5.6 }
                          }
                        ],
                        "interpretation": [
                          {
                            "coding": [
                              { "code": "H" }
                            ]
                          }
                        ]
                      }
                    },
                    {
                      "resource": {
                        "resourceType": "MedicationRequest",
                        "status": "active",
                        "intent": "order",
                        "medicationCodeableConcept": {
                          "text": "Metformin 500mg"
                        },
                        "dosageInstruction": [
                          {
                            "text": "Take 1 tablet twice daily with meals",
                            "timing": {
                              "code": { "text": "Twice daily" }
                            }
                          }
                        ],
                        "dispenseRequest": {
                          "expectedSupplyDuration": {
                            "value": 30,
                            "unit": "days"
                          }
                        }
                      }
                    }
                  ]
                }
                """;

        FhirBundleParser.ParsedFhirHealthData parsed = parser.parseBundle(jsonBundle);

        assertNotNull(parsed);
        assertEquals(1, parsed.labReportValues().size());
        assertEquals(1, parsed.prescriptions().size());

        LabReportValueEntity lab = parsed.labReportValues().get(0);
        assertEquals("Hemoglobin A1c", lab.getTestName());
        assertEquals("7.8", lab.getExtractedValue());
        assertEquals("%", lab.getUnit());
        assertEquals(FlagStatus.ABNORMAL, lab.getFlagStatus());

        PrescriptionEntity rx = parsed.prescriptions().get(0);
        assertEquals("Metformin 500mg", rx.getBrandName());
        assertEquals("Take 1 tablet twice daily with meals", rx.getDosage());
        assertEquals("30 days", rx.getDuration());
    }

    @Test
    void testParseEmptyBundleReturnsEmptyLists() {
        FhirBundleParser.ParsedFhirHealthData parsed = parser.parseBundle("{}");
        assertNotNull(parsed);
        assertTrue(parsed.labReportValues().isEmpty());
        assertTrue(parsed.prescriptions().isEmpty());
    }
}
