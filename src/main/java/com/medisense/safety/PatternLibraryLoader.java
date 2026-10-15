package com.medisense.safety;

import com.medisense.safety.model.QuerySafetyCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class PatternLibraryLoader {

    private static final Logger log = LoggerFactory.getLogger(PatternLibraryLoader.class);

    @Value("${medisense.safety.patterns-file:patterns.yaml}")
    private String patternsFileName = "patterns.yaml";

    public record CategoryPatterns(
            Set<String> exactPhrases,
            List<Pattern> regexPatterns,
            Set<String> directiveVerbs,
            Set<String> directiveDrugs
    ) {}

    private volatile Map<QuerySafetyCategory, CategoryPatterns> categoryPatternsMap = Collections.emptyMap();

    @PostConstruct
    public void init() {
        reloadPatterns();
    }

    @SuppressWarnings("unchecked")
    public synchronized void reloadPatterns() {
        String fileName = (patternsFileName != null && !patternsFileName.isBlank()) ? patternsFileName : "patterns.yaml";
        log.info("Loading query safety patterns from {}", fileName);

        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        if (is == null) {
            is = getClass().getResourceAsStream("/" + fileName);
        }
        if (is == null) {
            File localFile = new File("src/main/resources/" + fileName);
            if (localFile.exists()) {
                try {
                    is = new FileInputStream(localFile);
                    log.info("Found {} at local file path: {}", fileName, localFile.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("Failed to open local file stream for {}: {}", fileName, e.getMessage());
                }
            }
        }

        if (is == null) {
            log.error("Could not find {} anywhere on classpath or file system. Pattern library is empty!", fileName);
            return;
        }

        try (InputStream stream = is) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(stream);
            if (root == null || !root.containsKey("safety_patterns")) {
                log.error("Invalid YAML format in {}. Missing 'safety_patterns' key.", fileName);
                return;
            }

            Map<String, Object> patterns = (Map<String, Object>) root.get("safety_patterns");
            Map<QuerySafetyCategory, CategoryPatterns> newMap = new EnumMap<>(QuerySafetyCategory.class);

            parseCategory(patterns, "prescriptive", QuerySafetyCategory.PRESCRIPTIVE, newMap);
            parseCategory(patterns, "diagnostic", QuerySafetyCategory.DIAGNOSTIC, newMap);
            parseCategory(patterns, "drug_interaction", QuerySafetyCategory.DRUG_INTERACTION, newMap);
            parseCategory(patterns, "treatment_substitution", QuerySafetyCategory.TREATMENT_SUBSTITUTION, newMap);
            parseCategory(patterns, "emergency", QuerySafetyCategory.EMERGENCY, newMap);

            this.categoryPatternsMap = Collections.unmodifiableMap(newMap);
            log.info("Successfully loaded safety pattern library with {} categories and total patterns: {}",
                    newMap.size(), countTotalPatterns(newMap));
        } catch (Exception e) {
            log.error("Failed to load safety pattern library: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseCategory(Map<String, Object> rootMap, String key, QuerySafetyCategory category, Map<QuerySafetyCategory, CategoryPatterns> targetMap) {
        if (!rootMap.containsKey(key)) return;

        Map<String, Object> section = (Map<String, Object>) rootMap.get(key);
        Set<String> exactPhrases = new HashSet<>();
        List<Pattern> regexPatterns = new ArrayList<>();
        Set<String> verbs = new HashSet<>();
        Set<String> drugs = new HashSet<>();

        if (section.containsKey("exact_phrases")) {
            List<String> list = (List<String>) section.get("exact_phrases");
            if (list != null) {
                for (String phrase : list) {
                    exactPhrases.add(phrase.toLowerCase().trim());
                }
            }
        }

        if (section.containsKey("regex_patterns")) {
            List<String> list = (List<String>) section.get("regex_patterns");
            if (list != null) {
                for (String reg : list) {
                    regexPatterns.add(Pattern.compile(reg, Pattern.CASE_INSENSITIVE));
                }
            }
        }

        if (section.containsKey("drug_directives")) {
            Map<String, Object> directives = (Map<String, Object>) section.get("drug_directives");
            if (directives != null) {
                if (directives.containsKey("verbs") && directives.get("verbs") != null) {
                    verbs.addAll((List<String>) directives.get("verbs"));
                }
                if (directives.containsKey("drug_names") && directives.get("drug_names") != null) {
                    drugs.addAll((List<String>) directives.get("drug_names"));
                }
            }
        }

        targetMap.put(category, new CategoryPatterns(exactPhrases, regexPatterns, verbs, drugs));
    }

    private int countTotalPatterns(Map<QuerySafetyCategory, CategoryPatterns> map) {
        int count = 0;
        for (CategoryPatterns cp : map.values()) {
            count += cp.exactPhrases().size() + cp.regexPatterns().size() + cp.directiveDrugs().size();
        }
        return count;
    }

    public CategoryPatterns getPatternsForCategory(QuerySafetyCategory category) {
        return categoryPatternsMap.getOrDefault(category, new CategoryPatterns(Collections.emptySet(), Collections.emptyList(), Collections.emptySet(), Collections.emptySet()));
    }

    public Map<QuerySafetyCategory, CategoryPatterns> getAllCategoryPatterns() {
        return categoryPatternsMap;
    }
}
