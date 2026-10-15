package com.medisense.fixtures;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Pre-configured mock responses and WireMock stubs for external APIs:
 * - Google Gemini (LLM generation)
 * - Sarvam AI (Tamil / Indic translation)
 * - ABDM Sandbox (Authentication, Consent, and FHIR records)
 * - Cross-Encoder Reranker
 */
public class MockResponses {

    // ===================================================================
    // 1. Google Gemini Responses
    // ===================================================================

    public static String geminiGroundedSuccessResponse(String explanationText, String citationId) {
        return """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "%s [Source: %s]"
                  }
                ],
                "role": "model"
              },
              "finishReason": "STOP",
              "index": 0
            }
          ],
          "usageMetadata": {
            "promptTokenCount": 210,
            "candidatesTokenCount": 85,
            "totalTokenCount": 295
          }
        }
        """.formatted(explanationText, citationId);
    }

    public static String geminiPrescriptionAnalysisResponse() {
        return """
        {
          "explanation": "Metformin is an oral antidiabetic medicine prescribed for managing type 2 diabetes by reducing glucose production in the liver.",
          "medications": [
            {
              "brand_name": "Glycomet",
              "generic_name": "Metformin",
              "dosage": "500mg",
              "frequency": "Once daily",
              "purpose": "Glycemic control"
            }
          ],
          "lifestyle_notes": "Take with meals to minimize gastrointestinal discomfort.",
          "warning": "Consult your doctor before altering medication dose."
        }
        """;
    }

    // ===================================================================
    // 2. Sarvam AI Translation Responses
    // ===================================================================

    public static String sarvamTamilTranslationSuccessResponse(String translatedTamilText) {
        return """
        {
          "translated_text": "%s",
          "source_language_code": "en-IN",
          "target_language_code": "ta-IN",
          "request_id": "sarvam-tx-99281"
        }
        """.formatted(translatedTamilText);
    }

    // ===================================================================
    // 3. ABDM Gateway Responses
    // ===================================================================

    public static String abdmAuthTokenResponse() {
        return """
        {
          "accessToken": "mock-abdm-gateway-jwt-access-token",
          "expiresIn": 1800,
          "refreshExpiresIn": 0,
          "tokenType": "bearer"
        }
        """;
    }

    public static String abdmConsentInitResponse(String consentRequestId) {
        return """
        {
          "requestId": "req-98765",
          "timestamp": "2026-10-14T09:00:00.000Z",
          "consentRequest": {
            "id": "%s"
          },
          "error": null,
          "resp": {
            "requestId": "req-98765"
          }
        }
        """.formatted(consentRequestId);
    }

    public static String abdmFhirBundleResponse() {
        return """
        {
          "resourceType": "Bundle",
          "id": "bundle-lab-report-01",
          "type": "document",
          "entry": [
            {
              "resource": {
                "resourceType": "Observation",
                "id": "obs-ldl-01",
                "status": "final",
                "code": {
                  "coding": [
                    {
                      "system": "http://loinc.org",
                      "code": "13457-7",
                      "display": "Cholesterol in LDL [Mass/volume] in Serum or Plasma"
                    }
                  ],
                  "text": "LDL Cholesterol"
                },
                "valueQuantity": {
                  "value": 145,
                  "unit": "mg/dL",
                  "system": "http://unitsofmeasure.org",
                  "code": "mg/dL"
                },
                "referenceRange": [
                  {
                    "high": {
                      "value": 100,
                      "unit": "mg/dL"
                    }
                  }
                ]
              }
            }
          ]
        }
        """;
    }

    // ===================================================================
    // 4. Cross-Encoder Reranker Response
    // ===================================================================

    public static String rerankerSuccessResponse() {
        return """
        {
          "results": [
            {
              "index": 0,
              "relevance_score": 0.942
            },
            {
              "index": 1,
              "relevance_score": 0.812
            }
          ]
        }
        """;
    }

    // ===================================================================
    // 5. WireMock Stubbing Helpers
    // ===================================================================

    public static void stubGeminiSuccess(WireMockServer wireMock) {
        wireMock.stubFor(post(urlMatching("/v1beta/models/.*:generateContent.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(geminiGroundedSuccessResponse("Borderline LDL represents elevated cardiovascular risk.", "ICMR_Lipid_2024"))));
    }

    public static void stubSarvamTranslation(WireMockServer wireMock) {
        wireMock.stubFor(post(urlPathEqualTo("/sarvam/translate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(sarvamTamilTranslationSuccessResponse("எல்டிஎல் அளவு 145 mg/dL என்பது உங்கள் கொலஸ்ட்ரால் அளவைக் குறிக்கிறது."))));
    }

    public static void stubAbdmAuth(WireMockServer wireMock) {
        wireMock.stubFor(post(urlPathEqualTo("/v0.5/sessions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(abdmAuthTokenResponse())));
    }

    public static void stubAbdmConsent(WireMockServer wireMock, String consentRequestId) {
        wireMock.stubFor(post(urlPathEqualTo("/v0.5/consent-requests/init"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(abdmConsentInitResponse(consentRequestId))));
    }

    public static void stubReranker(WireMockServer wireMock) {
        wireMock.stubFor(post(urlPathEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(rerankerSuccessResponse())));
    }
}
