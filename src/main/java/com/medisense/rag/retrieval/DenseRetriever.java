package com.medisense.rag.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Dense vector retriever executing pgvector cosine distance search
 * with metadata pre-filtering on active child chunks.
 */
@Component
public class DenseRetriever {

    private static final Logger log = LoggerFactory.getLogger(DenseRetriever.class);
    private static final int DEFAULT_TOP_K = 20;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DenseRetriever(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<ScoredChunk> retrieveDense(float[] queryEmbedding, List<String> domainTags, EvidenceBasis evidenceFilter, int topK) {
        String filterStr = evidenceFilter != null ? evidenceFilter.name() : null;
        return retrieveDenseByEvidenceString(queryEmbedding, domainTags, filterStr, topK);
    }

    public List<ScoredChunk> retrieveDense(float[] queryEmbedding, List<String> domainTags, int topK) {
        return retrieveDenseByEvidenceString(queryEmbedding, domainTags, (String) null, topK);
    }

    public List<ScoredChunk> retrieveDenseByEvidenceString(float[] queryEmbedding, List<String> domainTags, String evidenceFilter, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            log.warn("Query embedding is null or empty. Skipping dense retrieval.");
            return Collections.emptyList();
        }

        int limit = topK > 0 ? topK : DEFAULT_TOP_K;
        log.info("Executing pgvector cosine similarity dense retrieval (topK: {}, evidenceFilter: {}, domainTags: {}).",
                limit, evidenceFilter, domainTags);

        StringBuilder vectorSb = new StringBuilder("[");
        for (int i = 0; i < queryEmbedding.length; i++) {
            vectorSb.append(queryEmbedding[i]);
            if (i < queryEmbedding.length - 1) {
                vectorSb.append(",");
            }
        }
        vectorSb.append("]");
        String vectorStr = vectorSb.toString();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id, c.content, c.metadata, p.content AS parent_content, ");
        sql.append("1 - (c.embedding <=> ?::vector) AS score ");
        sql.append("FROM child_chunks c ");
        sql.append("LEFT JOIN parent_chunks p ON c.parent_chunk_id = p.id ");
        sql.append("WHERE c.is_active = TRUE ");

        List<Object> params = new ArrayList<>();
        params.add(vectorStr);

        if (evidenceFilter != null && !evidenceFilter.isBlank()) {
            sql.append("AND c.metadata->>'evidence_basis' = ? ");
            params.add(evidenceFilter.trim());
        }

        if (domainTags != null && !domainTags.isEmpty()) {
            List<String> cleanedTags = domainTags.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .toList();
            if (!cleanedTags.isEmpty()) {
                sql.append("AND c.metadata->'domain_tags' ?| string_to_array(?, ',') ");
                params.add(String.join(",", cleanedTags));
            }
        }

        sql.append("ORDER BY c.embedding <=> ?::vector LIMIT ?");
        params.add(vectorStr);
        params.add(limit);

        List<ScoredChunk> results = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            UUID chunkId = rs.getObject("id", UUID.class);
            String content = rs.getString("content");
            String parentContent = rs.getString("parent_content");
            double score = rs.getDouble("score");
            String rawMetadata = rs.getString("metadata");

            String heading = extractHeading(rawMetadata, parentContent);

            return new ScoredChunk(
                    chunkId,
                    heading,
                    content,
                    score,
                    rowNum + 1, // 1-based rank
                    "DENSE"
            );
        }, params.toArray());

        log.info("DenseRetriever returned {} scored chunks.", results.size());
        return results;
    }

    private String extractHeading(String rawMetadata, String parentContent) {
        if (rawMetadata != null && !rawMetadata.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(rawMetadata);
                if (node.hasNonNull("section_heading")) {
                    String h = node.get("section_heading").asText();
                    if (!h.isBlank()) return h;
                }
            } catch (Exception ignored) {
                // fallback to parent content
            }
        }
        if (parentContent != null && !parentContent.isBlank()) {
            int idx = parentContent.indexOf('\n');
            if (idx > 0 && idx < 80) {
                return parentContent.substring(0, idx).trim();
            }
            return parentContent.length() > 60 ? parentContent.substring(0, 57) + "..." : parentContent;
        }
        return "Clinical Guideline Section";
    }
}
