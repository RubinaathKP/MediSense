package com.medisense.rag.chunking.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class DomainTagger {

    private static final Logger log = LoggerFactory.getLogger(DomainTagger.class);

    private final Map<String, List<Pattern>> domainKeywordMap = new LinkedHashMap<>();

    public DomainTagger() {
        // Initialize configurable taxonomy keyword map
        addDomainPatterns("lipid_management", List.of("lipid", "cholesterol", "ldl", "hdl", "triglyceride", "statin", "dyslipidemia", "plaque"));
        addDomainPatterns("diabetes_management", List.of("diabetes", "glucose", "hba1c", "glycated hemoglobin", "insulin", "glycemic", "blood sugar", "diabetic"));
        addDomainPatterns("hypertension", List.of("hypertension", "blood pressure", "systolic", "diastolic", "htn", "bp"));
        addDomainPatterns("cardiovascular_risk", List.of("cardiovascular", "heart disease", "vascular", "stroke", "atherosclerosis", "coronary", "risk factor"));
        addDomainPatterns("dietary_intervention", List.of("diet", "saturated fat", "sodium", "nutrition", "lifestyle", "exercise", "weight loss", "mediterranean"));
        addDomainPatterns("pharmacology_education", List.of("drug class", "mechanism of action", "indication", "pharmacological", "receptor", "inhibitor", "pharma"));
        addDomainPatterns("traditional_medicine", List.of("ayurveda", "siddha", "unani", "herbal", "traditional system", "natural remedy"));
    }

    private void addDomainPatterns(String domain, List<String> keywords) {
        List<Pattern> patterns = new ArrayList<>();
        for (String kw : keywords) {
            patterns.add(Pattern.compile("(?i)\\b" + Pattern.quote(kw) + "\\b"));
        }
        domainKeywordMap.put(domain, patterns);
    }

    public List<String> tagChunk(String chunkText) {
        if (chunkText == null || chunkText.isBlank()) {
            return Collections.emptyList();
        }

        List<String> assignedTags = new ArrayList<>();

        for (Map.Entry<String, List<Pattern>> entry : domainKeywordMap.entrySet()) {
            String domain = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(chunkText).find()) {
                    assignedTags.add(domain);
                    break; // Move to next domain once matched
                }
            }
        }

        log.trace("Tagged chunk text with {} domain tags: {}", assignedTags.size(), assignedTags);
        return assignedTags;
    }
}
