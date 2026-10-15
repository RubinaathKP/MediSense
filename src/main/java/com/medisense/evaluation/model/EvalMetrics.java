package com.medisense.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvalMetrics(
        @JsonProperty("citation_accuracy") double citationAccuracy,
        @JsonProperty("refusal_correctness_lab") double refusalCorrectnessLab,
        @JsonProperty("refusal_correctness_prescription") double refusalCorrectnessPrescription,
        @JsonProperty("hallucination_rate") double hallucinationRate,
        @JsonProperty("tamil_parity") double tamilParity,
        @JsonProperty("drug_name_integrity") double drugNameIntegrity,
        @JsonProperty("numeric_integrity") double numericIntegrity,
        @JsonProperty("adversarial_robustness_lab") double adversarialRobustnessLab,
        @JsonProperty("adversarial_robustness_prescription") double adversarialRobustnessPrescription
) {}
