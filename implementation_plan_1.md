# Implementation Plan: MediSense Spring Boot 3.x Architecture Scaffolding

This document outlines the architecture, components, configuration, database schema, and project structure for scaffolding **MediSense** — a production-grade RAG-based AI Health Literacy application built on Spring Boot 3.x and Java 21.

## Proposed System Architecture & Package Structure

The application will be structured under `com.medisense` adhering to clean architecture principles:

```
e:/agent
├── pom.xml
├── docker-compose.yml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── medisense
    │   │           ├── abdm/           # ABDM HIU consent & gateway integration
    │   │           ├── api/            # REST controllers, DTOs, global exception handling
    │   │           ├── audit/          # Field-level encrypted write-once audit logging
    │   │           ├── config/         # Spring, Security, Redis, Lucene, VectorStore, FHIR, OCR configs
    │   │           ├── domain/         # JPA entities, DTOs, enums, repositories
    │   │           ├── evaluation/     # RAG evaluation set runner & metric tracking
    │   │           ├── ingestion/      # PDFBox parsing, Tess4j OCR, HAPI FHIR parsing, normalization
    │   │           ├── rag/
    │   │           │   ├── chunking/   # Parent-child document chunking pipeline
    │   │           │   ├── embedding/  # LangChain4j embedding services
    │   │           │   ├── generation/ # Prompt assembly & retrieval-grounded LLM synthesis
    │   │           │   ├── reranking/  # Cross-encoder reranking engine
    │   │           │   └── retrieval/  # Hybrid (Dense pgvector HNSW + Sparse Lucene BM25) retrieval
    │   │           ├── safety/         # Query intent classifier, output filter, citation verifier
    │   │           └── translation/    # Sarvam AI translation & language preservation
    │   └── resources
    │       ├── application.properties
    │       └── db
    │           └── migration
    │               └── V1__init_medisense_schema.sql
    └── test
        └── java
            └── com
                └── medisense
                    └── MediSenseApplicationTests.java
```

## Database Schema Design (Flyway V1 Migration)

The Flyway migration script `V1__init_medisense_schema.sql` creates 12 tables with pgvector support:

1. `users` — Authentication & user metadata
2. `sessions` — Active health session management (DPDP compliance)
3. `source_documents` — Ingested guideline & user documents metadata
4. `parent_chunks` — Coarse parent document chunks for context retrieval
5. `child_chunks` — Fine-grained chunks with `vector(768)` embedding column & HNSW index (`m=16`, `ef_construction=200`, `vector_cosine_ops`)
6. `lab_report_values` — Extracted lab test values, reference ranges, and flags
7. `prescriptions` — Deterministic prescription entries (brand, generic, class, dosage, frequency)
8. `queries` — User queries & classified intent
9. `responses` — Grounded responses, citations, safety status
10. `audit_log` — Field-encrypted write-once audit log (DPDP & ABDM audit requirements)
11. `drug_name_lookup` — Brand-to-generic mapping lookup table
12. `query_cache` — Hash-indexed Redis/DB query result caching

## Key Technology Stack & Versions (`pom.xml`)

- **Java**: 21 (LTS)
- **Spring Boot**: 3.3.2
- **LangChain4j**: 0.33.0 (pgvector, core)
- **HAPI FHIR**: 7.2.0 (R4 structures)
- **Apache Lucene**: 9.11.1 (BM25 search)
- **Apache PDFBox**: 3.0.2 & **Apache Tika**: 2.9.2
- **Tess4j**: 5.12.0 (Tesseract OCR wrapper)
- **Flyway**: Core + PostgreSQL module 10.15.0
- **MapStruct**: 1.5.5.Final
- **Lombok**: 1.18.34
- **pgvector-java**: 0.1.6

## User Review Required

> [!IMPORTANT]
> - **Strict Safety & Non-Prescriptive Boundary**: Per FR-34, FR-35, FR-42, MediSense will hard-route prescriptive, diagnostic, dosage evaluation, or drug-drug interaction queries directly to fixed doctor-referral responses without calling the LLM.
> - **OCR & Tesseract Native Dependency**: Tess4j requires local `tesseract` binaries and `tessdata` language models installed on the system (or containerized image) for active OCR operations.

## Verification Plan

### Automated Verification
- Run `mvn clean compile` to ensure zero compilation errors and MapStruct code generation succeeds.
- Run `mvn test` for basic application context bootstrapping.

### Infrastructure Verification
- Bring up PostgreSQL (with pgvector) and Redis using `docker-compose up -d`.
- Verify database migrations apply cleanly via Flyway on startup.
