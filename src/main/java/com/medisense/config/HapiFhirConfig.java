package com.medisense.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HapiFhirConfig {

    private static final Logger log = LoggerFactory.getLogger(HapiFhirConfig.class);

    @Bean
    public FhirContext fhirContext() {
        log.info("Initializing HAPI FHIR R4 Context.");
        return FhirContext.forR4();
    }

    @Bean
    public IParser fhirJsonParser(FhirContext fhirContext) {
        return fhirContext.newJsonParser().setPrettyPrint(true);
    }
}
