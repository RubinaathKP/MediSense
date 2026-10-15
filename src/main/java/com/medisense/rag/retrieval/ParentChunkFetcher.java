package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ParentChunkFetcher retrieves full coarse-grained parent chunk text
 * and source document metadata for the selected top child chunks.
 * Handles grouping when multiple child chunks share the same parent chunk.
 */
@Component
public class ParentChunkFetcher {

    private static final Logger log = LoggerFactory.getLogger(ParentChunkFetcher.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public ParentChunkFetcher(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ParentChunkFetcher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<RetrievedContext> fetchParentContexts(List<ScoredChunk> topChildChunks) {
        if (topChildChunks == null || topChildChunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> chunkIds = topChildChunks.stream()
                .map(ScoredChunk::chunkId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (chunkIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Fetching parent contexts for {} child chunks.", chunkIds.size());

        String sql = "SELECT p.id AS parent_id, p.content AS parent_content, " +
                "s.document_name AS source_name, s.publication_year AS source_year, " +
                "COALESCE(c.metadata->>'evidence_basis', 'NOT_SPECIFIED') AS evidence_basis, " +
                "c.id AS child_chunk_id, " +
                "COALESCE(c.metadata->>'section_heading', p.metadata->>'section_heading', 'Clinical Guideline Section') AS section_heading " +
                "FROM child_chunks c " +
                "JOIN parent_chunks p ON c.parent_chunk_id = p.id " +
                "JOIN source_documents s ON p.document_id = s.id " +
                "WHERE c.id IN (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", chunkIds);

        record ChildRow(UUID childId, UUID parentId, String parentContent,
                        String sourceName, int sourceYear, String evidenceBasis, String sectionHeading) {}

        Map<UUID, ChildRow> childRowMap = new HashMap<>();

        jdbcTemplate.query(sql, parameters, (rs) -> {
            UUID childId = rs.getObject("child_chunk_id", UUID.class);
            UUID parentId = rs.getObject("parent_id", UUID.class);
            String parentContent = rs.getString("parent_content");
            String sourceName = rs.getString("source_name");
            int sourceYear = rs.getInt("source_year");
            String evidenceBasis = rs.getString("evidence_basis");
            String sectionHeading = rs.getString("section_heading");

            childRowMap.put(childId, new ChildRow(
                    childId, parentId, parentContent, sourceName, sourceYear, evidenceBasis, sectionHeading
            ));
        });

        // Group by parent chunk while strictly preserving the ranking order of topChildChunks
        Map<UUID, RetrievedContextBuilder> orderedParentMap = new LinkedHashMap<>();

        for (ScoredChunk chunk : topChildChunks) {
            ChildRow row = childRowMap.get(chunk.chunkId());
            if (row != null) {
                RetrievedContextBuilder builder = orderedParentMap.computeIfAbsent(row.parentId(), pid ->
                        new RetrievedContextBuilder(row.parentContent(), row.sourceName(), row.sourceYear(),
                                row.evidenceBasis(), row.sectionHeading())
                );
                if (!builder.triggeredChildChunkIds.contains(row.childId())) {
                    builder.addChildId(row.childId());
                }
            }
        }

        List<RetrievedContext> contexts = orderedParentMap.values().stream()
                .map(RetrievedContextBuilder::build)
                .collect(Collectors.toList());

        log.info("Successfully fetched {} unique parent contexts for {} child chunks.",
                contexts.size(), topChildChunks.size());
        return contexts;
    }

    private static class RetrievedContextBuilder {
        private final String parentChunkText;
        private final String sourceName;
        private final int sourceYear;
        private final String evidenceBasis;
        private final String sectionHeading;
        private final List<UUID> triggeredChildChunkIds = new ArrayList<>();

        RetrievedContextBuilder(String parentChunkText, String sourceName, int sourceYear,
                                String evidenceBasis, String sectionHeading) {
            this.parentChunkText = parentChunkText;
            this.sourceName = sourceName;
            this.sourceYear = sourceYear;
            this.evidenceBasis = evidenceBasis != null ? evidenceBasis : "NOT_SPECIFIED";
            this.sectionHeading = sectionHeading;
        }

        void addChildId(UUID id) {
            triggeredChildChunkIds.add(id);
        }

        RetrievedContext build() {
            return new RetrievedContext(
                    parentChunkText,
                    sourceName,
                    sourceYear,
                    evidenceBasis,
                    sectionHeading,
                    triggeredChildChunkIds
            );
        }
    }
}
