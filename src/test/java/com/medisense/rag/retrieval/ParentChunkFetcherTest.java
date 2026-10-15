package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentChunkFetcherTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private ParentChunkFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new ParentChunkFetcher(jdbcTemplate);
    }

    @Test
    @DisplayName("Should group multiple child chunks under their common parent chunk and preserve rank order")
    void testGroupChildChunksByParent() throws SQLException {
        UUID parent1Id = UUID.randomUUID();
        UUID parent2Id = UUID.randomUUID();

        UUID child1 = UUID.randomUUID();
        UUID child2 = UUID.randomUUID();
        UUID child3 = UUID.randomUUID();

        // child1 and child2 share parent1. child3 belongs to parent2.
        List<ScoredChunk> topChildChunks = List.of(
                new ScoredChunk(child1, "H1", "Child 1 content", 0.95, 1, "HYBRID"),
                new ScoredChunk(child3, "H3", "Child 3 content", 0.85, 2, "HYBRID"),
                new ScoredChunk(child2, "H1", "Child 2 content", 0.75, 3, "HYBRID")
        );

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(2);

            ResultSet rs1 = mock(ResultSet.class);
            when(rs1.getObject("child_chunk_id", UUID.class)).thenReturn(child1);
            when(rs1.getObject("parent_id", UUID.class)).thenReturn(parent1Id);
            when(rs1.getString("parent_content")).thenReturn("Parent 1 full clinical text");
            when(rs1.getString("source_name")).thenReturn("KDIGO Guidelines");
            when(rs1.getInt("source_year")).thenReturn(2023);
            when(rs1.getString("evidence_basis")).thenReturn("CLINICAL_TRIAL_BACKED");
            when(rs1.getString("section_heading")).thenReturn("CKD Management");
            handler.processRow(rs1);

            ResultSet rs2 = mock(ResultSet.class);
            when(rs2.getObject("child_chunk_id", UUID.class)).thenReturn(child3);
            when(rs2.getObject("parent_id", UUID.class)).thenReturn(parent2Id);
            when(rs2.getString("parent_content")).thenReturn("Parent 2 full clinical text");
            when(rs2.getString("source_name")).thenReturn("ADA Standards");
            when(rs2.getInt("source_year")).thenReturn(2024);
            when(rs2.getString("evidence_basis")).thenReturn("META_ANALYSIS");
            when(rs2.getString("section_heading")).thenReturn("Glycemic Targets");
            handler.processRow(rs2);

            ResultSet rs3 = mock(ResultSet.class);
            when(rs3.getObject("child_chunk_id", UUID.class)).thenReturn(child2);
            when(rs3.getObject("parent_id", UUID.class)).thenReturn(parent1Id);
            when(rs3.getString("parent_content")).thenReturn("Parent 1 full clinical text");
            when(rs3.getString("source_name")).thenReturn("KDIGO Guidelines");
            when(rs3.getInt("source_year")).thenReturn(2023);
            when(rs3.getString("evidence_basis")).thenReturn("CLINICAL_TRIAL_BACKED");
            when(rs3.getString("section_heading")).thenReturn("CKD Management");
            handler.processRow(rs3);

            return null;
        }).when(jdbcTemplate).query(anyString(), any(SqlParameterSource.class), any(RowCallbackHandler.class));

        List<RetrievedContext> contexts = fetcher.fetchParentContexts(topChildChunks);

        // Deduplicated: 2 unique parent contexts for 3 child chunks
        assertEquals(2, contexts.size());

        // First parent in order should be parent1 (triggered by child 1, rank 1)
        RetrievedContext ctx1 = contexts.get(0);
        assertEquals("Parent 1 full clinical text", ctx1.parentChunkText());
        assertEquals("KDIGO Guidelines", ctx1.sourceName());
        assertEquals(2023, ctx1.sourceYear());
        assertEquals("CLINICAL_TRIAL_BACKED", ctx1.evidenceBasis());
        assertEquals(List.of(child1, child2), ctx1.triggeredChildChunkIds());

        // Second parent in order should be parent2 (triggered by child 3, rank 2)
        RetrievedContext ctx2 = contexts.get(1);
        assertEquals("Parent 2 full clinical text", ctx2.parentChunkText());
        assertEquals("ADA Standards", ctx2.sourceName());
        assertEquals(List.of(child3), ctx2.triggeredChildChunkIds());
    }

    @Test
    @DisplayName("Should return empty list without database query when input child chunks list is empty or null")
    void testEmptyChildChunksInput() {
        List<RetrievedContext> emptyRes = fetcher.fetchParentContexts(Collections.emptyList());
        assertTrue(emptyRes.isEmpty());

        List<RetrievedContext> nullRes = fetcher.fetchParentContexts(null);
        assertTrue(nullRes.isEmpty());

        verifyNoInteractions(jdbcTemplate);
    }
}
