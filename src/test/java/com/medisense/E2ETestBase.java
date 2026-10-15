package com.medisense;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.medisense.fixtures.MockResponses;
import com.medisense.testcontainers.PostgresTestContainer;
import com.medisense.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all MediSense End-to-End (E2E) Tests.
 * Bootstraps the complete application stack with random server port, live PostgreSQL + Redis
 * Testcontainers, and an embedded WireMock server mocking Gemini, Sarvam, and ABDM sandbox APIs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class E2ETestBase {

    protected static final Logger log = LoggerFactory.getLogger(E2ETestBase.class);

    @Container
    public static PostgresTestContainer postgres = PostgresTestContainer.getInstance();

    @Container
    public static RedisTestContainer redis = RedisTestContainer.getInstance();

    protected static WireMockServer wireMockServer;

    @LocalServerPort
    protected int localPort;

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeAll
    public static void startWireMock() {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            wireMockServer.start();
            log.info("E2ETestBase: WireMock server started on port {}", wireMockServer.port());
        }
    }

    @AfterAll
    public static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("E2ETestBase: WireMock server stopped.");
        }
    }

    @DynamicPropertySource
    static void configureE2EProperties(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!redis.isRunning()) {
            redis.start();
        }
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            wireMockServer.start();
        }

        int wmPort = wireMockServer.port();

        // Database & Cache
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);

        // WireMock Mock Ports
        registry.add("wiremock.port", () -> wmPort);
        registry.add("medisense.abdm.gateway-url", () -> "http://localhost:" + wmPort + "/v0.5");
        registry.add("medisense.translation.sarvam.base-url", () -> "http://localhost:" + wmPort + "/sarvam");
        registry.add("medisense.reranker.url", () -> "http://localhost:" + wmPort + "/rerank");
        registry.add("medisense.llm.fallback-ollama-url", () -> "http://localhost:" + wmPort + "/ollama");
    }

    @BeforeEach
    public void setupDefaultWireMockStubs() {
        wireMockServer.resetAll();
        MockResponses.stubGeminiSuccess(wireMockServer);
        MockResponses.stubSarvamTranslation(wireMockServer);
        MockResponses.stubAbdmAuth(wireMockServer);
        onE2ESetup();
    }

    protected void onE2ESetup() {
        // Optional override
    }

    protected String getBaseUrl() {
        return "http://localhost:" + localPort;
    }
}
