package com.medisense.ingestion;

import com.medisense.domain.entity.IngestionJobEntity;
import com.medisense.domain.entity.ParentChunkEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.IngestionJobRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.chunking.model.ParentChunk;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.embedding.model.ChildChunkWithEmbedding;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CorpusIngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CorpusIngestionOrchestrator.class);

    private final DocumentIngestionPipeline ingestionPipeline;
    private final HierarchicalSemanticChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LuceneIndexService luceneIndexService;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final ParentChunkRepository parentChunkRepository;
    @SuppressWarnings("unused")
    private final ChildChunkRepository childChunkRepository;
    private final IngestionJobRepository ingestionJobRepository;

    public CorpusIngestionOrchestrator(DocumentIngestionPipeline ingestionPipeline,
                                       HierarchicalSemanticChunker chunker,
                                       EmbeddingService embeddingService,
                                       VectorStoreService vectorStoreService,
                                       LuceneIndexService luceneIndexService,
                                       SourceDocumentRepository sourceDocumentRepository,
                                       ParentChunkRepository parentChunkRepository,
                                       ChildChunkRepository childChunkRepository,
                                       IngestionJobRepository ingestionJobRepository) {
        this.ingestionPipeline = ingestionPipeline;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.luceneIndexService = luceneIndexService;
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.parentChunkRepository = parentChunkRepository;
        this.childChunkRepository = childChunkRepository;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    @Transactional
    public IngestionJobEntity ingestCorpusDocument(InputStream is, String fileName, int sourceYear, EvidenceBasis evidenceBasis, String versionTag) {
        try {
            byte[] bytes = is.readAllBytes();
            return orchestrateDocumentIngestion(bytes, fileName, fileName, sourceYear, evidenceBasis, "CORPUS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream for " + fileName, e);
        }
    }

    @Transactional
    public IngestionJobEntity orchestrateDocumentIngestion(byte[] documentBytes,
                                                          String fileName,
                                                          String sourceName,
                                                          int sourceYear,
                                                          EvidenceBasis evidenceBasis,
                                                          String sourceType) {
        String checksum = computeSha256(documentBytes);

        log.info("==================================================================");
        log.info("CorpusIngestionOrchestrator starting for '{}' (Checksum: {})", fileName, checksum);
        log.info("==================================================================");

        // 1. Idempotency Check: skip if document with same checksum is COMPLETED
        Optional<IngestionJobEntity> existingJob = ingestionJobRepository.findByChecksum(checksum);
        if (existingJob.isPresent() && "COMPLETED".equalsIgnoreCase(existingJob.get().getStatus())) {
            log.warn("IDEMPOTENCY SKIP: Document '{}' with checksum {} has already been fully ingested.", fileName, checksum);
            return existingJob.get();
        }

        // 2. Initialize or Update Ingestion Job Status (PENDING -> IN_PROGRESS, 0%)
        IngestionJobEntity job = existingJob.orElseGet(() -> IngestionJobEntity.builder()
                .documentName(fileName)
                .checksum(checksum)
                .status("PENDING")
                .progressPercentage(0)
                .build());

        job.setStatus("IN_PROGRESS");
        job.setProgressPercentage(10);
        job = ingestionJobRepository.save(job);

        try {
            // Step 1: Parse & Clean Document (25%)
            log.info("Step 1/4: Parsing document structure through 5-stage ingestion pipeline...");
            InputStream pdfStream = new ByteArrayInputStream(documentBytes);
            IngestionResult ingestionResult = ingestionPipeline.runPipeline(pdfStream, fileName);

            job.setProgressPercentage(25);
            job = ingestionJobRepository.save(job);

            // Step 2: Extract Hierarchical Structure & Chunk (50%)
            log.info("Step 2/4: Chunking document into section-level parents & sentence-aligned children...");
            SourceDocumentEntity docEntity = SourceDocumentEntity.builder()
                    .documentName(sourceName != null ? sourceName : fileName)
                    .documentType(sourceType != null ? sourceType : "Clinical Guideline")
                    .sourceType("GUIDELINE")
                    .contentHash(checksum)
                    .publicationYear(sourceYear)
                    .versionTag("v1.0")
                    .build();

            docEntity = sourceDocumentRepository.save(docEntity);
            UUID documentId = docEntity.getId() != null ? docEntity.getId() : UUID.randomUUID();

            DocumentStructure structure = new DocumentStructure(ingestionResult.getSafeNodes(), ingestionResult.getSafeNodes().size());
            List<ChunkPair> chunkPairs = chunker.chunkDocumentStructure(
                    structure, documentId, sourceName, sourceYear, evidenceBasis, sourceType, Collections.emptyList()
            );

            job.setProgressPercentage(50);
            job = ingestionJobRepository.save(job);

            // Step 3: Generate Batch Embeddings (75%)
            log.info("Step 3/4: Generating 768-dim embeddings via Google Multilingual Embedding API...");
            List<ChildChunkWithEmbedding> chunkEmbeddings = new ArrayList<>();
            List<LuceneIndexService.ChunkIndexEntry> luceneEntries = new ArrayList<>();

            for (ChunkPair pair : chunkPairs) {
                ParentChunk parent = pair.getParentChunk();
                ParentChunkEntity parentEntity = ParentChunkEntity.builder()
                        .document(docEntity)
                        .chunkIndex(parent.getSectionIndex())
                        .content(parent.getParentText())
                        .build();

                parentEntity = parentChunkRepository.save(parentEntity);

                List<String> childTexts = new ArrayList<>();
                for (ChildChunk child : pair.getChildChunks()) {
                    childTexts.add(child.getChunkText());
                }

                List<float[]> vectors = embeddingService.generateEmbeddingsBatch(childTexts);

                for (int i = 0; i < pair.getChildChunks().size(); i++) {
                    ChildChunk child = pair.getChildChunks().get(i);
                    float[] vector = vectors.get(i);

                    chunkEmbeddings.add(new ChildChunkWithEmbedding(child, vector));

                    luceneEntries.add(new LuceneIndexService.ChunkIndexEntry(
                            child.getChunkId() != null ? child.getChunkId().toString() : UUID.randomUUID().toString(),
                            child.getSectionHeading(),
                            child.getChunkText()
                    ));
                }
            }

            job.setProgressPercentage(75);
            job = ingestionJobRepository.save(job);

            // Step 4: Store in Vector DB (pgvector) & Lucene Disk Index (100%)
            log.info("Step 4/4: Persisting batch child chunks into pgvector(768) and flushing Lucene FSDirectory disk index...");
            vectorStoreService.storeChildChunksBatch(chunkEmbeddings);
            luceneIndexService.indexChunksBatch(luceneEntries);

            // Mark completion & mark chunks as active
            job.setStatus("COMPLETED");
            job.setProgressPercentage(100);
            job.setErrorMessage(null);
            job = ingestionJobRepository.save(job);

            log.info("==================================================================");
            log.info("CorpusIngestionOrchestrator completed successfully for '{}'", fileName);
            log.info("Total Child Chunks Ingested: {}, Active Status: TRUE", chunkEmbeddings.size());
            log.info("==================================================================");

            return job;

        } catch (Exception e) {
            log.error("CorpusIngestionOrchestrator FAILED for document '{}': {}", fileName, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            ingestionJobRepository.save(job);
            throw new RuntimeException("Orchestration pipeline execution failed for " + fileName, e);
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(new String(data, StandardCharsets.UTF_8).hashCode());
        }
    }
}
