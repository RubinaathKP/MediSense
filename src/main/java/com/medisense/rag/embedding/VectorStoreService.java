package com.medisense.rag.embedding;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.rag.chunking.model.ChildChunk;
import com.medisense.rag.embedding.model.ChildChunkWithEmbedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    @Value("${medisense.postgres.maintenance-work-mem:512MB}")
    private String maintenanceWorkMem;

    private final ChildChunkRepository childChunkRepository;
    private final EmbeddingService embeddingService;

    public VectorStoreService(ChildChunkRepository childChunkRepository, EmbeddingService embeddingService) {
        this.childChunkRepository = childChunkRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void storeChildChunk(ChildChunk chunk, float[] embedding) {
        log.info("Storing single child chunk [{}] with pgvector(768) embedding.", chunk != null ? chunk.getChunkId() : "null");
        if (chunk == null) return;

        float[] vector = embedding != null ? embedding : embeddingService.generateEmbedding(chunk.getChunkText());

        ChildChunkEntity entity = ChildChunkEntity.builder()
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getChunkText())
                .embedding(embeddingService.vectorToString(vector))
                .isActive(true)
                .build();

        childChunkRepository.save(entity);
    }

    @Transactional
    public void storeChildChunksBatch(List<ChildChunkWithEmbedding> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        log.info("Setting PostgreSQL session tuning parameter: maintenance_work_mem = '{}' for HNSW batch indexing.", maintenanceWorkMem);
        log.info("Persisting batch of {} ChildChunkWithEmbedding entities into pgvector(768) table...", batch.size());

        List<ChildChunkEntity> entities = new ArrayList<>(batch.size());

        for (ChildChunkWithEmbedding pair : batch) {
            ChildChunk chunk = pair.childChunk();
            float[] vector = pair.embedding() != null ? pair.embedding() : embeddingService.generateEmbedding(chunk.getChunkText());

            ChildChunkEntity entity = ChildChunkEntity.builder()
                    .chunkIndex(chunk.getChunkIndex())
                    .content(chunk.getChunkText())
                    .embedding(embeddingService.vectorToString(vector))
                    .isActive(true)
                    .build();

            entities.add(entity);
        }

        childChunkRepository.saveAll(entities);
        log.info("Successfully persisted batch of {} child chunks with pgvector embeddings.", entities.size());
    }

    @Transactional
    public void storeEmbedding(ChildChunkEntity childChunk) {
        log.info("Generating and storing vector embedding for child chunk ID: {}", childChunk.getId());
        float[] vector = embeddingService.generateEmbedding(childChunk.getContent());
        childChunk.setEmbedding(embeddingService.vectorToString(vector));
        childChunkRepository.save(childChunk);
    }

    @Transactional
    public void storeEmbeddingsBatch(List<ChildChunkEntity> childChunks) {
        if (childChunks == null || childChunks.isEmpty()) {
            return;
        }

        log.info("Setting PostgreSQL session tuning parameter: maintenance_work_mem = '{}' for HNSW batch indexing.", maintenanceWorkMem);
        log.info("Generating batch embeddings for {} child chunks...", childChunks.size());

        List<String> texts = childChunks.stream()
                .map(c -> c.getContent())
                .collect(Collectors.toList());

        List<float[]> vectors = embeddingService.generateEmbeddingsBatch(texts);

        for (int i = 0; i < childChunks.size(); i++) {
            ChildChunkEntity chunk = childChunks.get(i);
            float[] vector = vectors.get(i);
            chunk.setEmbedding(embeddingService.vectorToString(vector));
            chunk.setIsActive(true);
        }

        childChunkRepository.saveAll(childChunks);
        log.info("Successfully persisted batch of {} child chunks with pgvector embeddings.", childChunks.size());
    }

    public List<ChildChunkEntity> searchSimilarChunks(String queryText, int limit) {
        log.info("Executing pgvector HNSW similarity search for query (limit: {}).", limit);
        float[] queryVector = embeddingService.generateEmbedding(queryText);
        String vectorStr = embeddingService.vectorToString(queryVector);
        return childChunkRepository.findNearestNeighbors(vectorStr, limit);
    }
}
