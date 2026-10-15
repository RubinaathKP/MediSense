package com.medisense.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LogMasker — Utility to redact sensitive clinical values, health identifiers,
 * and drug names from application logs before emission.
 */
public final class LogMasker {

    private LogMasker() {}

    // Regex for numeric lab values with medical units (e.g. "145 mg/dL", "6.5 %", "12.4 g/dL")
    private static final Pattern LAB_VALUE_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?\\s*(?:mg/dL|g/dL|mmol/L|IU/L|%|pg/mL|ng/mL|U/L|mm/hr|cells/mcL|mEq/L)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Regex for Indian ABHA IDs, 12-digit Aadhaar-like numbers, and 10-digit mobile numbers
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "\\b(?:\\d{2}-\\d{4}-\\d{4}-\\d{4}|\\d{12}|[6-9]\\d{9})\\b"
    );

    // Common medication brand/generic names to mask from raw logs
    private static final List<String> COMMON_DRUGS = List.of(
            "metformin", "atorvastatin", "paracetamol", "amoxicillin", "amlodipine",
            "azithromycin", "omeprazole", "ciprofloxacin", "ibuprofen", "cetirizine",
            "losartan", "pantoprazole", "telmisartan", "glimepiride", "aspirin"
    );

    /**
     * Mask sensitive health data, lab values, and personal identifiers in log messages.
     *
     * @param input Raw log text
     * @return Sanitized log text safe for logging
     */
    public static String mask(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String masked = LAB_VALUE_PATTERN.matcher(input).replaceAll("[REDACTED_LAB_VALUE]");
        masked = IDENTIFIER_PATTERN.matcher(masked).replaceAll("[REDACTED_IDENTIFIER]");

        for (String drug : COMMON_DRUGS) {
            Pattern drugPattern = Pattern.compile("(?i)\\b" + Pattern.quote(drug) + "\\b");
            masked = drugPattern.matcher(masked).replaceAll("[REDACTED_DRUG]");
        }

        return masked;
    }

    /**
     * Computes deterministic SHA-256 hash of query text for safe audit logging.
     *
     * @param query Raw user query string
     * @return Hex-encoded SHA-256 digest
     */
    public static String hashQuery(String query) {
        if (query == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(query.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(query.hashCode());
        }
    }
}
