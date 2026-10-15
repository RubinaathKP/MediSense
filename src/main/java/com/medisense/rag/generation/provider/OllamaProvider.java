package com.medisense.rag.generation.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class OllamaProvider implements LLMProviderService {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    public static final String MODEL = "mistral:7b-instruct-q4_K_M";
    public static final double TEMPERATURE = 0.1;
    public static final int MAX_TOKENS = 1000;
    public static final int TIMEOUT_SECONDS = 30;

    @FunctionalInterface
    public interface HttpSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    private final String ollamaUrl;
    private final ObjectMapper objectMapper;
    private final HttpSender httpSender;

    public OllamaProvider(@Value("${medisense.ollama.url:http://localhost:11434/api/generate}") String ollamaUrl,
                          ObjectMapper objectMapper) {
        this(ollamaUrl, objectMapper, defaultHttpSender());
    }

    public OllamaProvider(String ollamaUrl,
                          ObjectMapper objectMapper,
                          HttpSender httpSender) {
        this.ollamaUrl = ollamaUrl;
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;
    }

    private static HttpSender defaultHttpSender() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        return req -> client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public GenerationResult generate(AssembledPrompt prompt) {
        log.info("Calling Ollama Local Fallback Provider (Model: {}, Temp: {}, Timeout: {}s)...",
                MODEL, TEMPERATURE, TIMEOUT_SECONDS);

        try {
            Map<String, Object> bodyMap = Map.of(
                    "model", MODEL,
                    "prompt", prompt.fullPromptText(),
                    "stream", false,
                    "options", Map.of(
                            "temperature", TEMPERATURE,
                            "num_predict", MAX_TOKENS
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpSender.send(request);

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("response")) {
                    String rawText = root.get("response").asText();
                    log.info("Ollama execution succeeded.");
                    return new GenerationResult(rawText, getProviderName(), TEMPERATURE, MAX_TOKENS, true, null);
                }
            }
            return new GenerationResult(null, getProviderName(), TEMPERATURE, MAX_TOKENS, false, "HTTP " + response.statusCode() + ": " + response.body());
        } catch (Exception e) {
            log.error("Ollama execution failed: {}", e.getMessage());
            return new GenerationResult(null, getProviderName(), TEMPERATURE, MAX_TOKENS, false, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Ollama-Mistral-7B";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
