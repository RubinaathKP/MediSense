package com.medisense.config;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig {

    private static final Logger log = LoggerFactory.getLogger(LuceneConfig.class);

    @Value("${medisense.lucene.index-dir:./data/lucene_index}")
    private String indexDir;

    @Bean
    public StandardAnalyzer luceneAnalyzer() {
        return new StandardAnalyzer();
    }

    @Bean
    public Directory luceneDirectory() throws IOException {
        log.info("Initializing Lucene FSDirectory at path: {}", indexDir);
        File dir = new File(indexDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("Created Lucene index directory: {}", created);
        }
        return FSDirectory.open(Paths.get(indexDir));
    }
}
