package com.medisense.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Helper class for managing database state during integration testing.
 * Provides reset, truncation, and fixture seeding capabilities.
 */
public class MediSenseTestDatabase {

    private static final Logger log = LoggerFactory.getLogger(MediSenseTestDatabase.class);

    private final DataSource dataSource;

    public MediSenseTestDatabase(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Truncates application tables in dependency order, preserving flyway metadata and vector extensions.
     */
    public void resetDatabase() {
        log.info("Resetting MediSense test database state...");
        String[] tables = {
                "audit_logs",
                "responses",
                "queries",
                "manual_review_queue",
                "consent_requests",
                "ingestion_jobs",
                "eval_case_results",
                "eval_runs",
                "query_cache",
                "child_chunks",
                "parent_chunks",
                "prescriptions",
                "lab_report_values",
                "source_documents",
                "sessions",
                "users"
        };

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.execute("TRUNCATE TABLE " + table + " CASCADE;");
                } catch (Exception ignored) {
                    // Ignore if table does not exist yet in particular test configuration
                }
            }
            log.info("MediSense test database successfully reset.");
        } catch (Exception e) {
            log.error("Failed to reset test database: {}", e.getMessage(), e);
            throw new RuntimeException("Test database reset failed", e);
        }
    }

    /**
     * Seeds the database with test fixtures from an SQL resource path.
     */
    public void seedFixtures(String sqlResourcePath) {
        log.info("Seeding test fixtures from: {}", sqlResourcePath);
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(sqlResourcePath));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Test fixtures seeded successfully from: {}", sqlResourcePath);
        } catch (Exception e) {
            log.error("Failed to seed fixtures from {}: {}", sqlResourcePath, e.getMessage(), e);
            throw new RuntimeException("Fixture seeding failed", e);
        }
    }
}
