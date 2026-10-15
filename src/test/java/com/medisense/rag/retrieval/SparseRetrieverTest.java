package com.medisense.rag.retrieval;

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

import static org.junit.jupiter.api.Assertions.*;

class SparseRetrieverTest {

    private ByteBuffersDirectory directory;
    private StandardAnalyzer analyzer;
    private LuceneIndexBuilder indexBuilder;
    private BM25Retriever bm25Retriever;
    private SparseRetriever sparseRetriever;

    @BeforeEach
    void setUp() throws IOException {
        directory = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        indexBuilder = new LuceneIndexBuilder(directory, analyzer, "./test_index");
        indexBuilder.init();

        bm25Retriever = new BM25Retriever(indexBuilder);
        sparseRetriever = new SparseRetriever(indexBuilder, bm25Retriever);
    }

    @AfterEach
    void tearDown() throws IOException {
        indexBuilder.close();
    }

    @Test
    @DisplayName("Should index chunks and retrieve them via BM25 with normalized scores in [0, 1]")
    void testIndexAndRetrieveWithBM25Normalization() throws IOException {
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        UUID chunk3 = UUID.randomUUID();

        // Index 3 clinical chunks
        sparseRetriever.indexChunk(chunk1, "First-line therapy for type 2 diabetes mellitus is metformin along with lifestyle modifications.");
        sparseRetriever.indexChunk(chunk2, "Metformin reduces hepatic glucose production and increases insulin sensitivity in peripheral tissues.");
        sparseRetriever.indexChunk(chunk3, "In acute myocardial infarction, immediate aspirin and emergency catheterization are indicated.");

        // Query: "metformin type 2 diabetes"
        List<ScoredChunk> results = sparseRetriever.retrieveSparse("metformin type 2 diabetes", 20);

        assertFalse(results.isEmpty(), "BM25 retrieval should return matching chunks");
        assertEquals(2, results.size(), "Only matching metformin chunks should return");

        // Top hit should be chunk 1 (has both metformin and type 2 diabetes)
        assertEquals(chunk1, results.get(0).chunkId());
        assertEquals(1, results.get(0).rank());
        assertEquals("SPARSE", results.get(0).source());

        // Normalized score verification
        assertEquals(1.0, results.get(0).score(), 1e-6, "Top Lucene score must normalize to 1.0");

        // Second hit score should be in (0, 1]
        assertTrue(results.get(1).score() > 0.0 && results.get(1).score() <= 1.0,
                "Subsequent BM25 score must be normalized to (0, 1]");
        assertEquals(2, results.get(1).rank());
    }

    @Test
    @DisplayName("Should incrementally update index in append mode without wiping existing documents")
    void testIncrementalUpdateAppendMode() throws IOException {
        UUID initialChunk = UUID.randomUUID();
        sparseRetriever.indexChunk(initialChunk, "Clinical guideline on statin therapy for LDL reduction.");

        List<ScoredChunk> initialResults = sparseRetriever.retrieveSparse("statin", 10);
        assertEquals(1, initialResults.size());

        // Append new chunk
        UUID appendedChunk = UUID.randomUUID();
        sparseRetriever.indexChunk(appendedChunk, "High-intensity statin therapy includes atorvastatin 80mg daily.");

        List<ScoredChunk> updatedResults = sparseRetriever.retrieveSparse("statin", 10);
        assertEquals(2, updatedResults.size(), "Append mode must preserve existing chunks while adding new ones");
    }

    @Test
    @DisplayName("Should handle clinical queries with Lucene special characters safely")
    void testQueryWithSpecialCharacters() throws IOException {
        UUID chunkId = UUID.randomUUID();
        sparseRetriever.indexChunk(chunkId, "Patient presents with HbA1c > 8.0% and BP: 140/90 mmHg (hypertension).");

        // Query with special characters: +, -, &&, ||, !, (, ), {, }, [, ], ^, ", ~, *, ?, :, \, /
        assertDoesNotThrow(() -> {
            List<ScoredChunk> results = sparseRetriever.retrieveSparse("HbA1c > 8.0% + (BP: 140/90 mmHg)?", 10);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("Should rebuild index completely when rebuildIndex is invoked")
    void testRebuildIndex() throws IOException {
        UUID oldChunk = UUID.randomUUID();
        sparseRetriever.indexChunk(oldChunk, "Obsolete 2010 cardiology guideline on beta blockers.");

        UUID newChunk = UUID.randomUUID();
        sparseRetriever.rebuildIndex(List.of(
                new LuceneIndexBuilder.ChunkEntry(newChunk, "Updated 2024 AHA guideline on SGLT2 inhibitors.")
        ));

        // Old chunk should be gone, new chunk present
        List<ScoredChunk> oldQuery = sparseRetriever.retrieveSparse("beta blockers", 10);
        assertTrue(oldQuery.isEmpty(), "Rebuilt index should not contain previous documents");

        List<ScoredChunk> newQuery = sparseRetriever.retrieveSparse("SGLT2 inhibitors", 10);
        assertEquals(1, newQuery.size());
        assertEquals(newChunk, newQuery.get(0).chunkId());
    }

    @Test
    @DisplayName("Should return empty list gracefully when query is blank or index is empty")
    void testEmptyQueryAndEmptyIndex() {
        List<ScoredChunk> emptyQueryResults = sparseRetriever.retrieveSparse("   ", 10);
        assertTrue(emptyQueryResults.isEmpty());

        List<ScoredChunk> nullQueryResults = sparseRetriever.retrieveSparse(null, 10);
        assertTrue(nullQueryResults.isEmpty());
    }
}
