package com.medisense;

import com.medisense.testcontainers.MediSenseTestDatabase;
import com.medisense.testcontainers.PostgresTestContainer;
import com.medisense.testcontainers.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

/**
 * Base class for all MediSense Integration Tests.
 * Bootstraps the full Spring Boot test context with live PostgreSQL (with pgvector)
 * and Redis Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Container
    public static PostgresTestContainer postgres = PostgresTestContainer.getInstance();

    @Container
    public static RedisTestContainer redis = RedisTestContainer.getInstance();

    @Autowired
    protected DataSource dataSource;

    protected MediSenseTestDatabase testDatabase;

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!redis.isRunning()) {
            redis.start();
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    @BeforeEach
    public void initIntegrationEnvironment() {
        if (testDatabase == null && dataSource != null) {
            testDatabase = new MediSenseTestDatabase(dataSource);
        }
        onIntegrationSetup();
    }

    /**
     * Subclasses can override for setup before each test.
     */
    protected void onIntegrationSetup() {
        // Default no-op
    }

    /**
     * Helper to reset test database between integration test scenarios.
     */
    protected void resetDatabase() {
        if (testDatabase != null) {
            testDatabase.resetDatabase();
        }
    }

    /**
     * Helper to seed standard fixtures.
     */
    protected void seedFixtures(String path) {
        if (testDatabase != null) {
            testDatabase.seedFixtures(path);
        }
    }
}
