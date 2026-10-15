package com.medisense.rag.retrieval;

import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DenseRetrieverTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DenseRetriever denseRetriever;

    @BeforeEach
    void setUp() {
        denseRetriever = new DenseRetriever(jdbcTemplate);
    }

    @Test
    @DisplayName("Should build parameterized SQL with active status, evidence filter, domain tags, and vector distance")
    void testRetrieveDenseWithAllFilters() {
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};
        List<String> domainTags = List.of("CARDIOLOGY", "HYPERTENSION");
        EvidenceBasis evidenceFilter = EvidenceBasis.CLINICAL_TRIAL_BACKED;

        UUID chunkId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<ScoredChunk> mapper = invocation.getArgument(1);
                    // verify row mapper works if invoked
                    return List.of(new ScoredChunk(chunkId, "Guideline Section", "Clinical content", 0.92, 1, "DENSE"));
                });

        List<ScoredChunk> results = denseRetriever.retrieveDense(queryVector, domainTags, evidenceFilter, 20);

        assertEquals(1, results.size());
        assertEquals(chunkId, results.get(0).chunkId());
        assertEquals("DENSE", results.get(0).source());
        assertEquals(0.92, results.get(0).score());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), paramsCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("WHERE c.is_active = TRUE"), "Must filter active chunks only");
        assertTrue(sql.contains("AND c.metadata->>'evidence_basis' = ?"), "Must filter by evidence_basis");
        assertTrue(sql.contains("AND c.metadata->'domain_tags' ?| string_to_array(?, ',')"), "Must filter by domain_tags array");
        assertTrue(sql.contains("ORDER BY c.embedding <=> ?::vector LIMIT ?"), "Must order by cosine distance");

        Object[] params = paramsCaptor.getValue();
        assertEquals(5, params.length);
        assertEquals("[0.1,0.2,0.3]", params[0]); // for score calculation
        assertEquals("CLINICAL_TRIAL_BACKED", params[1]); // evidence filter
        assertEquals("CARDIOLOGY,HYPERTENSION", params[2]); // domain tags
        assertEquals("[0.1,0.2,0.3]", params[3]); // for order by
        assertEquals(20, params[4]); // limit
    }

    @Test
    @DisplayName("Should omit evidence and domain filters when not provided")
    void testRetrieveDenseWithoutFilters() {
        float[] queryVector = new float[]{0.5f, 0.6f};

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        denseRetriever.retrieveDense(queryVector, null, (EvidenceBasis) null, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), paramsCaptor.capture());

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("WHERE c.is_active = TRUE"));
        assertFalse(sql.contains("evidence_basis"));
        assertFalse(sql.contains("domain_tags"));

        Object[] params = paramsCaptor.getValue();
        assertEquals(3, params.length);
        assertEquals("[0.5,0.6]", params[0]);
        assertEquals("[0.5,0.6]", params[1]);
        assertEquals(10, params[2]);
    }

    @Test
    @DisplayName("Should return empty list for null or empty embedding vector")
    void testNullOrEmptyVector() {
        List<ScoredChunk> nullRes = denseRetriever.retrieveDense(null, List.of(), (EvidenceBasis) null, 20);
        assertTrue(nullRes.isEmpty());

        List<ScoredChunk> emptyRes = denseRetriever.retrieveDense(new float[0], List.of(), (EvidenceBasis) null, 20);
        assertTrue(emptyRes.isEmpty());

        verifyNoInteractions(jdbcTemplate);
    }
}
