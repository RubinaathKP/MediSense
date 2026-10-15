package com.medisense.ingestion;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.model.ParentChunk;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestionService.class);

    private final DocumentIngestionPipeline ingestionPipeline;
    private final HierarchicalSemanticChunker chunker;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final ParentChunkRepository parentChunkRepository;
    @SuppressWarnings("unused")
    private final ChildChunkRepository childChunkRepository;
    private final VectorStoreService vectorStoreService;
    private final LuceneIndexService luceneIndexService;

    public KnowledgeBaseIngestionService(DocumentIngestionPipeline ingestionPipeline,
                                        HierarchicalSemanticChunker chunker,
                                        SourceDocumentRepository sourceDocumentRepository,
                                        ParentChunkRepository parentChunkRepository,
                                        ChildChunkRepository childChunkRepository,
                                        VectorStoreService vectorStoreService,
                                        LuceneIndexService luceneIndexService) {
        this.ingestionPipeline = ingestionPipeline;
        this.chunker = chunker;
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.parentChunkRepository = parentChunkRepository;
        this.childChunkRepository = childChunkRepository;
        this.vectorStoreService = vectorStoreService;
        this.luceneIndexService = luceneIndexService;
    }

    @Transactional
    public void ingestGuidelineDocument(InputStream pdfStream,
                                       String fileName,
                                       String sourceName,
                                       int sourceYear,
                                       EvidenceBasis evidenceBasis,
                                       String sourceType) {
        log.info("==================================================================");
        log.info("Starting Knowledge Base Ingestion for '{}'", fileName);
        log.info("==================================================================");

        // 1. Run 5-stage ingestion pipeline
        IngestionResult ingestionResult = ingestionPipeline.runPipeline(pdfStream, fileName);

        // 2. Create and persist SourceDocumentEntity
        SourceDocumentEntity docEntity = SourceDocumentEntity.builder()
                .documentName(sourceName != null ? sourceName : fileName)
                .documentType(sourceType != null ? sourceType : "Clinical Guideline")
                .sourceType("GUIDELINE")
                .contentHash("hash_" + System.currentTimeMillis())
                .publicationYear(sourceYear)
                .versionTag("v1.0")
                .build();

        docEntity = sourceDocumentRepository.save(docEntity);
        UUID documentId = docEntity.getId() != null ? docEntity.getId() : UUID.randomUUID();

        // 3. Hierarchical Semantic Chunking
        DocumentStructure structure = new DocumentStructure(ingestionResult.getSafeNodes(), ingestionResult.getSafeNodes().size());
        List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                structure, documentId, sourceName, sourceYear, evidenceBasis, sourceType, Collections.emptyList()
        );

        log.info("Generated {} ChunkPairs for document '{}'", chunkPairs.size(), fileName);

        List<ChildChunkEntity> allChildEntities = new ArrayList<>();
        List<LuceneIndexService.ChunkIndexEntry> luceneEntries = new ArrayList<>();

        for (ChunkPair pair : chunkPairs) {
            ParentChunk parent = pair.getParentChunk();
            ParentChunkEntity parentEntity = ParentChunkEntity.builder()
                    .document(docEntity)
                    .chunkIndex(parent.getSectionIndex())
                    .content(parent.getParentText())
                    .build();

            parentEntity = parentChunkRepository.save(parentEntity);

            for (ChildChunk child : pair.getChildChunks()) {
                ChildChunkEntity childEntity = ChildChunkEntity.builder()
                        .parentChunk(parentEntity)
                        .chunkIndex(child.getChunkIndex())
                        .content(child.getChunkText())
                        .build();

                allChildEntities.add(childEntity);

                luceneEntries.add(new LuceneIndexService.ChunkIndexEntry(
                        child.getChunkId() != null ? child.getChunkId().toString() : UUID.randomUUID().toString(),
                        child.getSectionHeading(),
                        child.getChunkText()
                ));
            }
        }

        // 4. Batch Store Embeddings with exponential backoff & Postgres HNSW maintenance_work_mem tuning
        vectorStoreService.storeEmbeddingsBatch(allChildEntities);

        // 5. Batch Index into Lucene FSDirectory with explicit commit & flush
        luceneIndexService.indexChunksBatch(luceneEntries);

        log.info("==================================================================");
        log.info("Successfully ingested document '{}' into MediSense Knowledge Base.", fileName);
        log.info("Ingestion Summary: {} Parent Chunks, {} Child Chunks indexed in Vector Store & Lucene.",
                chunkPairs.size(), allChildEntities.size());
        log.info("==================================================================");
    }
}
