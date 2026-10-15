package com.medisense.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class LangChainConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);

    @Value("${medisense.llm.api-key}")
    private String apiKey;

    @Value("${medisense.llm.model-name:gemini-1.5-pro}")
    private String modelName;

    @Value("${medisense.llm.temperature:0.2}")
    private Double temperature;

    @Value("${medisense.llm.timeout-seconds:30}")
    private Long timeoutSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Configuring LangChain4j ChatLanguageModel with model: {}", modelName);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
