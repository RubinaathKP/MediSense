package com.medisense.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class LanguageDetectorService {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectorService.class);

    private static final Set<String> TANGLISH_PHONEMES = Set.of(
            "epdi", "enna", "irukku", "konjam", "sollunga", "pannanga", "varuma", "rooomba",
            "theriyum", "vanakkam", "nalladhu", "seri", "aama", "illai", "sapdu", "maathirai",
            "marundhu", "doctoru", "valikudhu", "udambu", "ratham", "thalaivali"
    );

    public LanguageDetectionResult detectLanguageResult(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return new LanguageDetectionResult("en", 1.0, false, "LATIN");
        }

        int totalChars = 0;
        int tamilChars = 0;
        int latinChars = 0;

        for (char c : queryText.toCharArray()) {
            if (Character.isWhitespace(c)) continue;
            totalChars++;
            if (c >= '\u0B80' && c <= '\u0BFF') {
                tamilChars++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                latinChars++;
            }
        }

        if (totalChars == 0) {
            return new LanguageDetectionResult("en", 1.0, false, "LATIN");
        }

        double tamilRatio = (double) tamilChars / totalChars;

        // 1. Pure or Predominant Tamil Script
        if (tamilRatio > 0.5) {
            log.info("Detected Tamil script query with ratio {}.", tamilRatio);
            return new LanguageDetectionResult("ta", 0.98, latinChars > 0, "TAMIL");
        }

        // 2. Check Tanglish (Latin script code-mixed Tamil)
        String lowerQuery = queryText.toLowerCase();
        int tanglishMatchCount = 0;
        for (String phoneme : TANGLISH_PHONEMES) {
            if (lowerQuery.contains(phoneme)) {
                tanglishMatchCount++;
            }
        }

        if (tanglishMatchCount > 0) {
            double confidence = Math.min(0.85 + (tanglishMatchCount * 0.05), 0.99);
            log.info("Detected Tanglish (romanized Tamil-English mix) query. Matched phonemes: {}", tanglishMatchCount);
            return new LanguageDetectionResult("tanglish", confidence, true, "LATIN");
        }

        if (tamilChars > 0 && latinChars > 0) {
            return new LanguageDetectionResult("ta", 0.90, true, "MIXED");
        }

        // 3. Standard English
        return new LanguageDetectionResult("en", 0.98, false, "LATIN");
    }

    public String detectLanguage(String queryText) {
        return detectLanguageResult(queryText).detectedLanguage();
    }
}
