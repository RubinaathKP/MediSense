package com.medisense.rag.chunking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParentChildChunkerService {

    private static final Logger log = LoggerFactory.getLogger(ParentChildChunkerService.class);

    private static final int DEFAULT_PARENT_SIZE = 1000;
    private static final int DEFAULT_CHILD_SIZE = 200;
    private static final int DEFAULT_CHILD_OVERLAP = 50;

    public List<String> createParentChunks(String fullText) {
        log.info("Generating parent chunks (size: {} characters) for document text length: {}.", DEFAULT_PARENT_SIZE, fullText.length());
        return chunkText(fullText, DEFAULT_PARENT_SIZE, 100);
    }

    public List<String> createChildChunks(String parentChunkText) {
        log.info("Generating child chunks (size: {} characters, overlap: {}) from parent chunk length: {}.", DEFAULT_CHILD_SIZE, DEFAULT_CHILD_OVERLAP, parentChunkText.length());
        return chunkText(parentChunkText, DEFAULT_CHILD_SIZE, DEFAULT_CHILD_OVERLAP);
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start += (chunkSize - overlap);
        }
        return chunks;
    }
}
