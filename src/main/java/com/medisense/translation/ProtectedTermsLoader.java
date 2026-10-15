package com.medisense.translation;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@Component
public class ProtectedTermsLoader {

    private static final Logger log = LoggerFactory.getLogger(ProtectedTermsLoader.class);

    @Value("${medisense.translation.protected-terms-file:protected_terms.yaml}")
    private String termsFileName = "protected_terms.yaml";

    private volatile Set<String> protectedTerms = Collections.emptySet();

    @PostConstruct
    public void init() {
        reloadTerms();
    }

    @SuppressWarnings("unchecked")
    public synchronized void reloadTerms() {
        String fileName = (termsFileName != null && !termsFileName.isBlank()) ? termsFileName : "protected_terms.yaml";
        log.info("Loading protected medical terms from {}", fileName);

        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        if (is == null) {
            is = getClass().getResourceAsStream("/" + fileName);
        }
        if (is == null) {
            File localFile = new File("src/main/resources/" + fileName);
            if (localFile.exists()) {
                try {
                    is = new FileInputStream(localFile);
                } catch (Exception e) {
                    log.warn("Failed to open local file stream for {}: {}", fileName, e.getMessage());
                }
            }
        }

        if (is == null) {
            log.warn("Could not find {} anywhere on classpath or file system. Using default term set.", fileName);
            this.protectedTerms = Set.of("LDL Cholesterol", "HbA1c", "Statins", "ACE Inhibitors", "Beta Blockers");
            return;
        }

        try (InputStream stream = is) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(stream);
            Set<String> resultSet = new HashSet<>();

            if (root != null) {
                if (root.containsKey("lab_tests") && root.get("lab_tests") instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null) resultSet.add(item.toString().trim());
                    }
                }
                if (root.containsKey("drug_classes") && root.get("drug_classes") instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null) resultSet.add(item.toString().trim());
                    }
                }
            }

            this.protectedTerms = Collections.unmodifiableSet(resultSet);
            log.info("Successfully loaded {} protected medical terms/drug classes.", resultSet.size());
        } catch (Exception e) {
            log.error("Failed to load protected medical terms: {}", e.getMessage(), e);
            this.protectedTerms = Set.of("LDL Cholesterol", "HbA1c", "Statins");
        }
    }

    public Set<String> getProtectedTerms() {
        return protectedTerms;
    }
}
