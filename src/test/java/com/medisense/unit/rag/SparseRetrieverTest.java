package com.medisense.unit.rag;

import com.medisense.rag.retrieval.BM25Retriever;
import com.medisense.rag.retrieval.LuceneIndexBuilder;
import com.medisense.rag.retrieval.SparseRetriever;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: SparseRetrieverTest
 * Verifies Lucene BM25 sparse keyword search, ranking by term frequency,
 * no match handling, and case insensitivity across 10 indexed chunks.
 */
class SparseRetrieverTest {

    private ByteBuffersDirectory directory;
    private StandardAnalyzer analyzer;
    private LuceneIndexBuilder indexBuilder;
    private BM25Retriever bm25Retriever;
    private SparseRetriever sparseRetriever;

    private UUID chunk1Both;
    private UUID chunk2Both;
    private UUID chunk3LdlHighTf;
    private UUID chunk4LdlSingle;
    private UUID chunk5CholesterolOnly;

    @BeforeEach
    void setUp() throws IOException {
        directory = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        indexBuilder = new LuceneIndexBuilder(directory, analyzer, "./test_sparse_index");
        indexBuilder.init();

        bm25Retriever = new BM25Retriever(indexBuilder);
        sparseRetriever = new SparseRetriever(indexBuilder, bm25Retriever);

        // Setup: 10 indexed chunks
        chunk1Both = UUID.randomUUID();
        chunk2Both = UUID.randomUUID();
        chunk3LdlHighTf = UUID.randomUUID();
        chunk4LdlSingle = UUID.randomUUID();
        chunk5CholesterolOnly = UUID.randomUUID();

        // 1 & 2 have BOTH keywords "LDL" and "cholesterol"
        sparseRetriever.indexChunk(chunk1Both, "Elevated LDL cholesterol is a direct causative factor in atherosclerotic cardiovascular events.");
        sparseRetriever.indexChunk(chunk2Both, "Clinical guideline recommends monitoring serum LDL cholesterol every six months for high risk patients.");

        // 3 has "LDL" repeated multiple times (high term frequency for LDL)
        sparseRetriever.indexChunk(chunk3LdlHighTf, "Patients with elevated LDL require LDL reduction therapy to reach optimal LDL targets.");

        // 4 has single "LDL"
        sparseRetriever.indexChunk(chunk4LdlSingle, "Baseline assessment includes fasting blood sugar and LDL measurement.");

        // 5 has only "cholesterol"
        sparseRetriever.indexChunk(chunk5CholesterolOnly, "Total cholesterol and triglycerides contribute to metabolic syndrome.");

        // Chunks 6-10: unrelated medical topics
        sparseRetriever.indexChunk(UUID.randomUUID(), "HbA1c levels above 6.5 percent indicate diabetes mellitus.");
        sparseRetriever.indexChunk(UUID.randomUUID(), "Serum creatinine and estimated glomerular filtration rate assess renal clearance.");
        sparseRetriever.indexChunk(UUID.randomUUID(), "Blood pressure measurement protocol recommends sitting position for five minutes.");
        sparseRetriever.indexChunk(UUID.randomUUID(), "Liver function tests include alanine aminotransferase and aspartate aminotransferase.");
        sparseRetriever.indexChunk(UUID.randomUUID(), "Complete blood count assesses hemoglobin, hematocrit, and platelet counts.");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (indexBuilder != null) {
            indexBuilder.close();
        }
        if (directory != null) {
            directory.close();
        }
    }

    @Test
    @DisplayName("Case 1: Exact keyword match: chunks containing both keywords ranked first")
    void testExactKeywordMatchRankedFirst() {
        String query = "LDL cholesterol";

        List<ScoredChunk> results = sparseRetriever.retrieveSparse(query, 10);

        // Assert: results found and chunks containing both keywords ranked first
        assertThat(results).isNotEmpty();
        assertThat(results.size()).isGreaterThanOrEqualTo(2);

        List<UUID> top2Ids = List.of(results.get(0).chunkId(), results.get(1).chunkId());
        assertThat(top2Ids).contains(chunk1Both, chunk2Both);

        assertThat(results.get(0).content()).contains("LDL").contains("cholesterol");
        assertThat(results.get(1).content()).contains("LDL").contains("cholesterol");
    }

    @Test
    @DisplayName("Case 2: Partial match: query 'LDL' returns all chunks with LDL ranked by term frequency")
    void testPartialMatchRankedByTermFrequency() {
        String query = "LDL";

        List<ScoredChunk> results = sparseRetriever.retrieveSparse(query, 10);

        // Assert: all 4 chunks with LDL returned
        assertThat(results).hasSize(4);

        for (ScoredChunk chunk : results) {
            assertThat(chunk.content()).contains("LDL");
        }

        // Chunk 3 mentions LDL 3 times -> highest term frequency, so it should rank #1
        assertThat(results.get(0).chunkId()).isEqualTo(chunk3LdlHighTf);
        assertThat(results.get(0).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("Case 3: No match: unknown term returns empty result")
    void testNoMatchReturnsEmptyResult() {
        String query = "zzzzunknown";

        List<ScoredChunk> results = sparseRetriever.retrieveSparse(query, 10);

        // Assert: empty result
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Case 4: Case insensitivity: 'LDL' vs 'ldl' returns same results")
    void testCaseInsensitivityReturnsSameResults() {
        List<ScoredChunk> uppercaseResults = sparseRetriever.retrieveSparse("LDL", 10);
        List<ScoredChunk> lowercaseResults = sparseRetriever.retrieveSparse("ldl", 10);

        // Assert: same results (case-insensitive)
        assertThat(uppercaseResults).isNotEmpty();
        assertThat(lowercaseResults).isNotEmpty();
        assertThat(uppercaseResults).hasSameSizeAs(lowercaseResults);

        for (int i = 0; i < uppercaseResults.size(); i++) {
            assertThat(uppercaseResults.get(i).chunkId())
                    .isEqualTo(lowercaseResults.get(i).chunkId());
            assertThat(uppercaseResults.get(i).score())
                    .isEqualTo(lowercaseResults.get(i).score());
        }
    }
}
