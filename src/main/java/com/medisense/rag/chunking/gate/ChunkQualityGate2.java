package com.medisense.rag.chunking.gate;

import com.medisense.rag.chunking.model.ChildChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ChunkQualityGate2 {

    private static final Logger log = LoggerFactory.getLogger(ChunkQualityGate2.class);

    private static final int MIN_CHUNK_TOKENS = 50;
    private static final int MAX_CHUNK_TOKENS = 512;

    // Prescriptive patterns for chunk-level re-verification
    private static final Pattern DOSAGE_DIRECTIVE_PATTERN = Pattern.compile(
            "(?i).*\\b\\d+\\s*(mg|ml|mcg|iu|units|g)\\b.*\\b(take|administer|prescribe|give|dosage|intake|daily|b.i.d.|t.i.d.)\\b.*"
    );
    private static final Pattern FORMULARY_PATTERN = Pattern.compile(
            "(?i).*\\b(drug|medicine|brand|generic)\\b.*\\b(dose|dosage|frequency|route|administer)\\b.*"
    );
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
            "(?i).*\\b(first-line therapy|second-line treatment|dosing schedule|titrate|start at|initial dose|maintenance dose|recommended dosage)\\b.*"
    );

    public boolean validateChildChunk(ChildChunk chunk) {
        if (chunk == null || chunk.getChunkText() == null) {
            log.warn("ChunkQualityGate2 REJECTED: Chunk or chunk text is null.");
            return false;
        }

        // Rule 1: Token Count Lower Bound (must be > 50 tokens, i.e. 50 tokens is rejected) - skipped if table chunk
        if (!chunk.isTable() && chunk.getTokenCount() <= MIN_CHUNK_TOKENS) {
            log.warn("ChunkQualityGate2 REJECTED chunk [{}] at path [{}]: Token count ({}) <= minimum required ({}). Content snippet: '{}'",
                    chunk.getChunkId(), chunk.getSectionHeading(), chunk.getTokenCount(), MIN_CHUNK_TOKENS, truncate(chunk.getChunkText(), 80));
            return false;
        }

        // Rule 2: Token Count Upper Bound (max 512 tokens)
        if (chunk.getTokenCount() > MAX_CHUNK_TOKENS) {
            log.warn("ChunkQualityGate2 REJECTED chunk [{}] at path [{}]: Token count ({}) > maximum allowed ({}). Content snippet: '{}'",
                    chunk.getChunkId(), chunk.getSectionHeading(), chunk.getTokenCount(), MAX_CHUNK_TOKENS, truncate(chunk.getChunkText(), 80));
            return false;
        }

        // Rule 3: Re-run Prescriptive Content Detection on every child chunk text
        String text = chunk.getChunkText();
        if (DOSAGE_DIRECTIVE_PATTERN.matcher(text).matches()) {
            log.warn("ChunkQualityGate2 REJECTED chunk [{}] (Prescriptive Content Detected - Dosage + Directive Language). Content: '{}'",
                    chunk.getChunkId(), truncate(text, 100));
            return false;
        }
        if (FORMULARY_PATTERN.matcher(text).matches()) {
            log.warn("ChunkQualityGate2 REJECTED chunk [{}] (Prescriptive Content Detected - Drug Formulary Pattern). Content: '{}'",
                    chunk.getChunkId(), truncate(text, 100));
            return false;
        }
        if (PROTOCOL_PATTERN.matcher(text).matches()) {
            log.warn("ChunkQualityGate2 REJECTED chunk [{}] (Prescriptive Content Detected - Prescribing Protocol Language). Content: '{}'",
                    chunk.getChunkId(), truncate(text, 100));
            return false;
        }

        log.trace("ChunkQualityGate2 PASSED for chunk [{}]", chunk.getChunkId());
        return true;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
