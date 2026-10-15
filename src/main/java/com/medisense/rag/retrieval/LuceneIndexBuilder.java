package com.medisense.rag.retrieval;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * LuceneIndexBuilder indexes active child chunks into an Apache Lucene FSDirectory.
 * Implements an incremental update strategy using Lucene's IndexWriter in append mode,
 * as well as complete index rebuild capability.
 */
@Component
public class LuceneIndexBuilder implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexBuilder.class);

    @Value("${medisense.lucene.index-dir:./data/lucene_index}")
    private String indexDirPath;

    private Directory directory;
    private StandardAnalyzer analyzer;
    private IndexWriter indexWriter;
    private final boolean externalDirectoryProvided;

    public record ChunkEntry(UUID chunkId, String chunkText, String sectionHeading) {
        public ChunkEntry(UUID chunkId, String chunkText) {
            this(chunkId, chunkText, "Clinical Section");
        }
        public ChunkEntry(String chunkId, String chunkText) {
            this(UUID.fromString(chunkId), chunkText, "Clinical Section");
        }
    }

    public LuceneIndexBuilder() {
        this.directory = null;
        this.analyzer = new StandardAnalyzer();
        this.externalDirectoryProvided = false;
    }

    @Autowired
    public LuceneIndexBuilder(@Autowired(required = false) Directory directory,
                              @Autowired(required = false) StandardAnalyzer analyzer) {
        this.directory = directory;
        this.analyzer = analyzer != null ? analyzer : new StandardAnalyzer();
        this.externalDirectoryProvided = (directory != null);
    }

    public LuceneIndexBuilder(Directory directory, StandardAnalyzer analyzer, String indexDirPath) {
        this.directory = directory;
        this.analyzer = analyzer != null ? analyzer : new StandardAnalyzer();
        this.indexDirPath = indexDirPath;
        this.externalDirectoryProvided = (directory != null);
    }

    @PostConstruct
    public synchronized void init() throws IOException {
        if (directory == null) {
            File dirFile = new File(indexDirPath);
            if (!dirFile.exists()) {
                boolean created = dirFile.mkdirs();
                log.info("Created Lucene index directory on disk at '{}': {}", indexDirPath, created);
            }
            directory = FSDirectory.open(Paths.get(indexDirPath));
        }

        if (analyzer == null) {
            analyzer = new StandardAnalyzer();
        }

        ensureWriterOpen(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        log.info("Initialized LuceneIndexBuilder with directory: {}", directory);
    }

    private synchronized void ensureWriterOpen(IndexWriterConfig.OpenMode openMode) throws IOException {
        if (indexWriter != null && indexWriter.isOpen()) {
            if (openMode == IndexWriterConfig.OpenMode.CREATE) {
                indexWriter.close();
            } else {
                return;
            }
        }
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(openMode);
        indexWriter = new IndexWriter(directory, config);
    }

    /**
     * Incremental update strategy: updates document if chunk_id already exists, or appends if new.
     */
    public synchronized void indexChunk(UUID chunkId, String chunkText, String sectionHeading) throws IOException {
        if (chunkId == null || chunkText == null || chunkText.isBlank()) {
            return;
        }
        ensureWriterOpen(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        Document doc = createDocument(chunkId, chunkText, sectionHeading);
        indexWriter.updateDocument(new Term("chunk_id", chunkId.toString()), doc);
        indexWriter.commit();
        log.debug("Incrementally indexed/updated chunk_id: {}", chunkId);
    }

    public synchronized void indexChunk(UUID chunkId, String chunkText) throws IOException {
        indexChunk(chunkId, chunkText, "Clinical Section");
    }

    /**
     * Incrementally indexes a batch of child chunks using append mode.
     */
    public synchronized void indexChunks(List<ChunkEntry> chunks) throws IOException {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        ensureWriterOpen(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        for (ChunkEntry entry : chunks) {
            if (entry != null && entry.chunkId() != null && entry.chunkText() != null) {
                Document doc = createDocument(entry.chunkId(), entry.chunkText(), entry.sectionHeading());
                indexWriter.updateDocument(new Term("chunk_id", entry.chunkId().toString()), doc);
            }
        }
        indexWriter.flush();
        indexWriter.commit();
        log.info("Incrementally committed {} child chunks to Lucene index.", chunks.size());
    }

    /**
     * Rebuilds the Lucene index from scratch using CREATE open mode.
     */
    public synchronized void rebuildIndex(List<ChunkEntry> activeChildChunks) throws IOException {
        log.info("Rebuilding Lucene index with {} active child chunks...", activeChildChunks != null ? activeChildChunks.size() : 0);
        ensureWriterOpen(IndexWriterConfig.OpenMode.CREATE);

        if (activeChildChunks != null) {
            for (ChunkEntry entry : activeChildChunks) {
                if (entry != null && entry.chunkId() != null && entry.chunkText() != null) {
                    Document doc = createDocument(entry.chunkId(), entry.chunkText(), entry.sectionHeading());
                    indexWriter.addDocument(doc);
                }
            }
        }
        indexWriter.flush();
        indexWriter.commit();
        log.info("Successfully rebuilt Lucene index with {} documents.", activeChildChunks != null ? activeChildChunks.size() : 0);
    }

    public synchronized void deleteChunk(UUID chunkId) throws IOException {
        if (chunkId != null && indexWriter != null && indexWriter.isOpen()) {
            indexWriter.deleteDocuments(new Term("chunk_id", chunkId.toString()));
            indexWriter.commit();
            log.info("Deleted chunk {} from Lucene index.", chunkId);
        }
    }

    private Document createDocument(UUID chunkId, String chunkText, String sectionHeading) {
        Document doc = new Document();
        // chunk_id indexed as StringField (exact match, stored)
        doc.add(new StringField("chunk_id", chunkId.toString(), Field.Store.YES));
        // chunk_text indexed as TextField (tokenized BM25 searchable, stored)
        doc.add(new TextField("chunk_text", chunkText, Field.Store.YES));
        // content alias for compatibility
        doc.add(new TextField("content", chunkText, Field.Store.YES));
        // section heading stored
        if (sectionHeading != null && !sectionHeading.isBlank()) {
            doc.add(new TextField("section_heading", sectionHeading, Field.Store.YES));
        }
        return doc;
    }

    public synchronized IndexWriter getIndexWriter() {
        return indexWriter;
    }

    public synchronized Directory getDirectory() {
        return directory;
    }

    public StandardAnalyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    @PreDestroy
    public synchronized void close() throws IOException {
        if (indexWriter != null && indexWriter.isOpen()) {
            indexWriter.close();
        }
        if (!externalDirectoryProvided && directory != null) {
            directory.close();
        }
        log.info("LuceneIndexBuilder closed.");
    }
}
