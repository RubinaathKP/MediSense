package com.medisense.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainer instance providing PostgreSQL with pgvector extension enabled.
 * Used across integration and E2E test suites for database verification.
 */
public class PostgresTestContainer extends PostgreSQLContainer<PostgresTestContainer> {

    private static final Logger log = LoggerFactory.getLogger(PostgresTestContainer.class);
    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    private static PostgresTestContainer container;

    private PostgresTestContainer() {
        super(PGVECTOR_IMAGE);
        withDatabaseName("medisense_test");
        withUsername("medisense_test_user");
        withPassword("medisense_test_pass");
        withReuse(true);
    }

    public static synchronized PostgresTestContainer getInstance() {
        if (container == null) {
            container = new PostgresTestContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        log.info("PostgresTestContainer started at: {} (User: {})", getJdbcUrl(), getUsername());
        System.setProperty("DB_URL", getJdbcUrl());
        System.setProperty("DB_USER", getUsername());
        System.setProperty("DB_PASS", getPassword());
    }

    @Override
    public void stop() {
        // Container reuse is enabled across test JVM execution; JVM shutdown hook manages final termination
        log.info("PostgresTestContainer stop requested.");
    }
}
