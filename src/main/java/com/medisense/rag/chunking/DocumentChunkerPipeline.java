package com.medisense.rag.chunking;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DocumentChunkerPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkerPipeline.class);

    private final ParentChildChunkerService chunkerService;
    private final ParentChunkRepository parentChunkRepository;
    private final ChildChunkRepository childChunkRepository;

    public DocumentChunkerPipeline(ParentChildChunkerService chunkerService,
                                  ParentChunkRepository parentChunkRepository,
                                  ChildChunkRepository childChunkRepository) {
        this.chunkerService = chunkerService;
        this.parentChunkRepository = parentChunkRepository;
        this.childChunkRepository = childChunkRepository;
    }

    @Transactional
    public void processDocument(SourceDocumentEntity document, String fullText) {
        log.info("Processing document ID: {} through parent-child chunking pipeline.", document.getId());

        List<String> parentTexts = chunkerService.createParentChunks(fullText);
        int parentIdx = 0;

        for (String parentText : parentTexts) {
            ParentChunkEntity parentChunk = ParentChunkEntity.builder()
                    .document(document)
                    .chunkIndex(parentIdx++)
                    .content(parentText)
                    .build();
            parentChunk = parentChunkRepository.save(parentChunk);

            List<String> childTexts = chunkerService.createChildChunks(parentText);
            int childIdx = 0;

            for (String childText : childTexts) {
                ChildChunkEntity childChunk = ChildChunkEntity.builder()
                        .parentChunk(parentChunk)
                        .chunkIndex(childIdx++)
                        .content(childText)
                        .build();
                childChunkRepository.save(childChunk);
            }
        }

        log.info("Finished chunking document ID: {}. Created {} parent chunks.", document.getId(), parentTexts.size());
    }
}
