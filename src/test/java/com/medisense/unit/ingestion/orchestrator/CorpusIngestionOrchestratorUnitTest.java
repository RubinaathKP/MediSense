package com.medisense.unit.ingestion.orchestrator;

import com.medisense.UnitTestBase;
import com.medisense.domain.entity.IngestionJobEntity;
import com.medisense.domain.entity.SourceDocumentEntity;
import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.domain.repository.ChildChunkRepository;
import com.medisense.domain.repository.IngestionJobRepository;
import com.medisense.domain.repository.ParentChunkRepository;
import com.medisense.domain.repository.SourceDocumentRepository;
import com.medisense.ingestion.CorpusIngestionOrchestrator;
import com.medisense.ingestion.DocumentIngestionPipeline;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.rag.chunking.HierarchicalSemanticChunker;
import com.medisense.rag.chunking.model.ChunkPair;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.embedding.VectorStoreService;
import com.medisense.rag.retrieval.LuceneIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test: CorpusIngestionOrchestrator
 * Verifies end-to-end orchestration of document parsing, chunking, embedding generation,
 * vector store indexing, and database persistence.
 */
class CorpusIngestionOrchestratorUnitTest extends UnitTestBase {

    @Mock
    private DocumentIngestionPipeline ingestionPipeline;

    @Mock
    private HierarchicalSemanticChunker chunker;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private LuceneIndexService luceneIndexService;

    @Mock
    private SourceDocumentRepository sourceDocumentRepository;

    @Mock
    private ParentChunkRepository parentChunkRepository;

    @Mock
    private ChildChunkRepository childChunkRepository;

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    private CorpusIngestionOrchestrator orchestrator;

    @Override
    protected void onSetup() {
        orchestrator = new CorpusIngestionOrchestrator(
                ingestionPipeline,
                chunker,
                embeddingService,
                vectorStoreService,
                luceneIndexService,
                sourceDocumentRepository,
                parentChunkRepository,
                childChunkRepository,
                ingestionJobRepository
        );
    }

    @Test
    @DisplayName("Should coordinate successful ingestion pipeline and mark job completed")
    void shouldSuccessfullyIngestDocument() {
        byte[] content = "Clinical guidance for hypertension screening.".getBytes();
        InputStream is = new ByteArrayInputStream(content);
        String docName = "Hypertension_ICMR.pdf";

        when(sourceDocumentRepository.findByContentHash(anyString())).thenReturn(Optional.empty());
        when(ingestionJobRepository.save(any(IngestionJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sourceDocumentRepository.save(any(SourceDocumentEntity.class))).thenAnswer(inv -> {
            SourceDocumentEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        IngestionResult ingestionResult = IngestionResult.builder()
                .documentName(docName)
                .safeNodes(Collections.emptyList())
                .excludedNodesLog(Collections.emptyList())
                .totalNodesScanned(5)
                .excludedNodeCount(0)
                .build();
        when(ingestionPipeline.processDocument(any(InputStream.class), eq(docName))).thenReturn(ingestionResult);
        when(chunker.chunkDocumentStructure(any(), any(), eq(docName), anyInt(), any(), eq("CORPUS"), any())).thenReturn(Collections.emptyList());

        IngestionJobEntity job = orchestrator.ingestCorpusDocument(
                is,
                docName,
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "v1.0"
        );

        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
        verify(sourceDocumentRepository).save(any(SourceDocumentEntity.class));
        verify(ingestionPipeline).processDocument(any(), eq(docName));
    }

    @Test
    @DisplayName("Should detect duplicate document hash and return existing completed job")
    void shouldDetectDuplicateDocumentHash() {
        byte[] content = "Duplicate document content".getBytes();
        InputStream is = new ByteArrayInputStream(content);
        String docName = "Duplicate_Doc.pdf";

        SourceDocumentEntity existingDoc = SourceDocumentEntity.builder()
                .id(UUID.randomUUID())
                .documentName(docName)
                .build();
        when(sourceDocumentRepository.findByContentHash(anyString())).thenReturn(Optional.of(existingDoc));

        IngestionJobEntity job = orchestrator.ingestCorpusDocument(
                is,
                docName,
                2024,
                EvidenceBasis.CLINICAL_TRIAL_BACKED,
                "v1.0"
        );

        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo("COMPLETED_DUPLICATE_SKIPPED");
        verifyNoInteractions(ingestionPipeline);
        verifyNoInteractions(chunker);
    }
}
