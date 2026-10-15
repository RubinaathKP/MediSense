package com.medisense.unit.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.rag.retrieval.DenseRetriever;
import com.medisense.rag.retrieval.model.ScoredChunk;
import com.medisense.testcontainers.PostgresTestContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Unit/Integration Test: DenseRetrieverTest
 * Tests pgvector cosine similarity search, evidence pre-filters, domain tag filters,
 * active status filters, and empty result handling.
 * Integrates with Testcontainers PostgreSQL with pgvector when Docker is present,
 * or simulated vector engine when Docker is unavailable.
 */
@ExtendWith(MockitoExtension.class)
class DenseRetrieverTest {

    private static boolean useLiveContainer = false;
    private static PostgresTestContainer postgresContainer;
    private static JdbcTemplate liveJdbcTemplate;

    @Mock
    private JdbcTemplate mockJdbcTemplate;

    private DenseRetriever denseRetriever;

    private record TestChunkRecord(
            UUID id,
            String heading,
            String content,
            String parentContent,
            String evidenceBasis,
            List<String> domainTags,
            boolean isActive,
            float[] embedding
    ) {
        double cosineSimilarity(float[] query) {
            double dot = 0.0;
            double normA = 0.0;
            double normB = 0.0;
            for (int i = 0; i < query.length; i++) {
                dot += embedding[i] * query[i];
                normA += embedding[i] * embedding[i];
                normB += query[i] * query[i];
            }
            if (normA == 0.0 || normB == 0.0) return 0.0;
            return dot / (Math.sqrt(normA) * Math.sqrt(normB));
        }
    }

    private final List<TestChunkRecord> corpus10Chunks = new ArrayList<>();

    @BeforeAll
    static void initContainerIfDockerAvailable() {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                postgresContainer = PostgresTestContainer.getInstance();
                postgresContainer.start();
                DriverManagerDataSource ds = new DriverManagerDataSource();
                ds.setDriverClassName(postgresContainer.getDriverClassName());
                ds.setUrl(postgresContainer.getJdbcUrl());
                ds.setUsername(postgresContainer.getUsername());
                ds.setPassword(postgresContainer.getPassword());
                liveJdbcTemplate = new JdbcTemplate(ds);
                useLiveContainer = true;
            }
        } catch (Throwable t) {
            useLiveContainer = false;
        }
    }

    @BeforeEach
    void setUp() {
        corpus10Chunks.clear();

        // 10 child chunks setup: 5 clinical_trial, 5 traditional; 9 active, 1 inactive
        for (int i = 1; i <= 10; i++) {
            UUID id = UUID.randomUUID();
            String evidence = (i <= 5) ? "clinical_trial" : "traditional";
            List<String> domains = (i <= 4 || i == 9 || i == 10)
                    ? List.of("lipid_management")
                    : (i == 5 ? List.of("lipid_management", "cardiology") : List.of("dietary_management"));
            boolean active = (i != 10); // Chunk 10 is inactive

            float[] vec = new float[768];
            // Assign descending similarity towards query vector
            for (int d = 0; d < 768; d++) {
                vec[d] = (float) ((11 - i) * 0.1 / (d + 1));
            }

            corpus10Chunks.add(new TestChunkRecord(
                    id,
                    "Section " + i,
                    "Chunk " + i + ": Guideline details on LDL cholesterol, clinical management, and lipid risk.",
                    "Parent Section " + i + " content",
                    evidence,
                    domains,
                    active,
                    vec
            ));
        }

        if (useLiveContainer && liveJdbcTemplate != null) {
            denseRetriever = new DenseRetriever(liveJdbcTemplate);
        } else {
            denseRetriever = new DenseRetriever(mockJdbcTemplate);
            setupMockVectorBehavior();
        }
    }

    @SuppressWarnings("unchecked")
    private void setupMockVectorBehavior() {
        lenient().when(mockJdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    String sql = inv.getArgument(0);
                    RowMapper<ScoredChunk> mapper = inv.getArgument(1);
                    Object[] params;
                    Object arg2 = inv.getArgument(2);
                    if (arg2 instanceof Object[] arr) {
                        params = arr;
                    } else {
                        Object[] allArgs = inv.getArguments();
                        params = Arrays.copyOfRange(allArgs, 2, allArgs.length);
                    }

                    float[] queryVec = parseVectorParam((String) params[0]);
                    int limit = (Integer) params[params.length - 1];

                    String evidenceFilter = null;
                    if (sql.contains("metadata->>'evidence_basis' = ?")) {
                        evidenceFilter = (String) params[1];
                    }

                    List<String> domainFilter = null;
                    if (sql.contains("metadata->'domain_tags' ?|")) {
                        for (Object p : params) {
                            if (p instanceof String s && s.contains("lipid_management")) {
                                domainFilter = Arrays.asList(s.split(","));
                                break;
                            }
                        }
                    }

                    final String finalEvidence = evidenceFilter;
                    final List<String> finalDomains = domainFilter;

                    List<ScoredChunk> list = corpus10Chunks.stream()
                            .filter(TestChunkRecord::isActive)
                            .filter(c -> {
                                if (finalEvidence != null) {
                                    return finalEvidence.equalsIgnoreCase(c.evidenceBasis());
                                }
                                return true;
                            })
                            .filter(c -> {
                                if (finalDomains != null && !finalDomains.isEmpty()) {
                                    return c.domainTags().stream().anyMatch(finalDomains::contains);
                                }
                                return true;
                            })
                            .map(c -> {
                                double sim = c.cosineSimilarity(queryVec);
                                return new ScoredChunk(c.id(), c.heading(), c.content(), sim, 0, "DENSE");
                            })
                            .sorted((a, b) -> Double.compare(b.score(), a.score()))
                            .limit(limit)
                            .collect(Collectors.toList());

                    return list;
                });
    }

    private float[] parseVectorParam(String vecStr) {
        String trimmed = vecStr.replace("[", "").replace("]", "").trim();
        if (trimmed.isEmpty()) return new float[768];
        String[] parts = trimmed.split(",");
        float[] res = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            res[i] = Float.parseFloat(parts[i]);
        }
        return res;
    }

    private float[] getQueryEmbedding() {
        float[] q = new float[768];
        for (int d = 0; d < 768; d++) {
            q[d] = 1.0f / (d + 1);
        }
        return q;
    }

    @Test
    @DisplayName("Case 1: Basic similarity search: top-5 returned ordered by cosine similarity descending")
    void testBasicSimilaritySearchTop5Descending() {
        float[] queryEmbedding = getQueryEmbedding();

        List<ScoredChunk> results = denseRetriever.retrieveDense(queryEmbedding, Collections.emptyList(), 5);

        // Assert: top-5 returned, ordered by cosine similarity descending
        assertThat(results).hasSize(5);
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).score())
                    .as("Results must be ordered by similarity descending")
                    .isGreaterThanOrEqualTo(results.get(i + 1).score());
        }
        assertThat(results.get(0).source()).isEqualTo("DENSE");
        assertThat(results.get(0).content()).contains("LDL cholesterol");
    }

    @Test
    @DisplayName("Case 2: Metadata pre-filter: evidence_basis returns top-5 that are all clinical_trial")
    void testMetadataPrefilterEvidenceBasis() {
        float[] queryEmbedding = getQueryEmbedding();

        List<ScoredChunk> results = denseRetriever.retrieveDenseByEvidenceString(
                queryEmbedding, Collections.emptyList(), "clinical_trial", 5
        );

        // Assert: top-5 are all clinical_trial
        assertThat(results).hasSize(5);

        Set<UUID> clinicalTrialIds = corpus10Chunks.stream()
                .filter(c -> "clinical_trial".equals(c.evidenceBasis()))
                .map(TestChunkRecord::id)
                .collect(Collectors.toSet());

        for (ScoredChunk chunk : results) {
            assertThat(clinicalTrialIds).contains(chunk.chunkId());
        }
    }

    @Test
    @DisplayName("Case 3: Domain tag filter: returned chunks all have lipid_management tag")
    void testDomainTagFilter() {
        float[] queryEmbedding = getQueryEmbedding();
        List<String> domainFilter = List.of("lipid_management");

        List<ScoredChunk> results = denseRetriever.retrieveDense(queryEmbedding, domainFilter, 5);

        // Assert: returned chunks all have this tag
        assertThat(results).isNotEmpty();

        Set<UUID> lipidTagIds = corpus10Chunks.stream()
                .filter(c -> c.domainTags().contains("lipid_management") && c.isActive())
                .map(TestChunkRecord::id)
                .collect(Collectors.toSet());

        for (ScoredChunk chunk : results) {
            assertThat(lipidTagIds).contains(chunk.chunkId());
        }
    }

    @Test
    @DisplayName("Case 4: Combined filters: evidence_basis + domain + active status all applied correctly")
    void testCombinedFiltersAppliedCorrectly() {
        float[] queryEmbedding = getQueryEmbedding();
        List<String> domainFilter = List.of("lipid_management");

        List<ScoredChunk> results = denseRetriever.retrieveDenseByEvidenceString(
                queryEmbedding, domainFilter, "clinical_trial", 5
        );

        // Assert: all filters applied correctly (must be active, clinical_trial, and lipid_management)
        assertThat(results).isNotEmpty();

        Set<UUID> validChunkIds = corpus10Chunks.stream()
                .filter(TestChunkRecord::isActive)
                .filter(c -> "clinical_trial".equals(c.evidenceBasis()))
                .filter(c -> c.domainTags().contains("lipid_management"))
                .map(TestChunkRecord::id)
                .collect(Collectors.toSet());

        for (ScoredChunk chunk : results) {
            assertThat(validChunkIds).contains(chunk.chunkId());
        }
    }

    @Test
    @DisplayName("Case 5: Empty result: no matching chunks returns empty list with no error")
    void testEmptyResultWhenNoMatchingChunks() {
        float[] queryEmbedding = getQueryEmbedding();

        // Non-existent evidence basis
        List<ScoredChunk> results = denseRetriever.retrieveDenseByEvidenceString(
                queryEmbedding, Collections.emptyList(), "non_existent_evidence_type", 5
        );

        // Assert: empty list returned, no error
        assertThat(results).isNotNull().isEmpty();
    }
}
