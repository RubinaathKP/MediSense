# MediSense Comprehensive Testing Infrastructure & Evaluation Index

Welcome to the **MediSense Verification & Credibility Suite**. This directory contains the exhaustive test specifications and evaluation reports demonstrating project credibility, clinical safety guardrails, and software engineering rigor.

---

## 1. Testing Stack & Architecture

| Layer | Technologies | Primary Purpose |
| :--- | :--- | :--- |
| **Unit Testing** | JUnit 5, Mockito, AssertJ | Fast, isolated verification of parsers, chunkers, embedding math, and guardrails |
| **Integration Testing** | Spring Boot Test, Testcontainers (PostgreSQL + pgvector, Redis) | Real schema migrations, JPA relational mapping, and vector search operations |
| **End-to-End (E2E)** | Spring Boot (`RANDOM_PORT`), WireMock (Gemini, Sarvam, ABDM) | Full lifecycle testing from patient auth to RAG grounded answer generation |
| **Fixtures** | PDFBox Synthetic Lab & Prescription PDFs, SQL Seeds, YAML benchmarks | Standardized, reproducible clinical test fixtures |

---

## 2. Test Directory & Evaluation Catalog

Each test suite has two corresponding files in this folder:
1. `<TestName>.md`: The complete test code, structure, assertions, and mock configurations.
2. `eval_<TestName>.md`: The formal evaluation report containing **Purpose**, **Approach of Test**, **Test Cases Matrix**, and **Execution Results**.

| Module | Test Specification | Evaluation Report | Scope | Status | Score |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Ingestion: Parser** | [FormatParserTest.md](FormatParserTest.md) | [eval_FormatParserTest.md](eval_FormatParserTest.md) | PDFBox extraction, corrupted/empty PDF rejection | :white_check_mark: PASSED | 100% |
| **Ingestion: Cleaner** | [TextCleanerTest.md](TextCleanerTest.md) | [eval_TextCleanerTest.md](eval_TextCleanerTest.md) | Hyphenation fix, header/footer removal | :white_check_mark: PASSED | 100% |
| **Ingestion: Detector** | [PrescriptiveContentDetectorTest.md](PrescriptiveContentDetectorTest.md) | [eval_PrescriptiveContentDetectorTest.md](eval_PrescriptiveContentDetectorTest.md) | Dosage directive & formulary exclusion | :white_check_mark: PASSED | 100% |
| **Ingestion: Orchestrator** | [CorpusIngestionOrchestratorTest.md](CorpusIngestionOrchestratorTest.md) | [eval_CorpusIngestionOrchestratorTest.md](eval_CorpusIngestionOrchestratorTest.md) | End-to-end corpus ingestion & deduplication | :white_check_mark: PASSED | 100% |
| **RAG: Chunking** | [HierarchicalChunkerTest.md](HierarchicalChunkerTest.md) | [eval_HierarchicalChunkerTest.md](eval_HierarchicalChunkerTest.md) | Parent-child chunks & section isolation | :white_check_mark: PASSED | 100% |
| **RAG: Embedding** | [EmbeddingServiceTest.md](EmbeddingServiceTest.md) | [eval_EmbeddingServiceTest.md](eval_EmbeddingServiceTest.md) | SHA-256 caching & circuit breaker fallback | :white_check_mark: PASSED | 100% |
| **RAG: Retrieval** | [HybridRetrieverTest.md](HybridRetrieverTest.md) | [eval_HybridRetrieverTest.md](eval_HybridRetrieverTest.md) | Dense HNSW + Sparse BM25 + RRF | :white_check_mark: PASSED | 100% |
| **RAG: Generation** | [RagGenerationTest.md](RagGenerationTest.md) | [eval_RagGenerationTest.md](eval_RagGenerationTest.md) | Grounded prompt assembly & clinical tone | :white_check_mark: PASSED | 100% |
| **Safety: Classifier** | [InputSafetyClassifierTest.md](InputSafetyClassifierTest.md) | [eval_InputSafetyClassifierTest.md](eval_InputSafetyClassifierTest.md) | Emergency triage & prescriptive interception | :white_check_mark: PASSED | 100% |
| **Safety: Filter** | [OutputSafetyFilterTest.md](OutputSafetyFilterTest.md) | [eval_OutputSafetyFilterTest.md](eval_OutputSafetyFilterTest.md) | Directive command redaction & referral | :white_check_mark: PASSED | 100% |
| **Safety: Verifier** | [CitationVerifierTest.md](CitationVerifierTest.md) | [eval_CitationVerifierTest.md](eval_CitationVerifierTest.md) | Anti-hallucination citation tag verification | :white_check_mark: PASSED | 100% |
| **API: Query** | [QueryControllerTest.md](QueryControllerTest.md) | [eval_QueryControllerTest.md](eval_QueryControllerTest.md) | Health query submission, length limits | :white_check_mark: PASSED | 100% |
| **API: Report** | [ReportControllerTest.md](ReportControllerTest.md) | [eval_ReportControllerTest.md](eval_ReportControllerTest.md) | 10MB limit & PDF magic byte validation | :white_check_mark: PASSED | 100% |
| **API: Auth** | [AuthControllerTest.md](AuthControllerTest.md) | [eval_AuthControllerTest.md](eval_AuthControllerTest.md) | Registration, BCrypt, JWT auth tokens | :white_check_mark: PASSED | 100% |
| **DB: Postgres+pgvector** | [PostgresPgvectorIntegrationTest.md](PostgresPgvectorIntegrationTest.md) | [eval_PostgresPgvectorIntegrationTest.md](eval_PostgresPgvectorIntegrationTest.md) | Live pgvector container entity persistence | :white_check_mark: PASSED | 100% |
| **DB: Redis Cache** | [RedisCacheIntegrationTest.md](RedisCacheIntegrationTest.md) | [eval_RedisCacheIntegrationTest.md](eval_RedisCacheIntegrationTest.md) | TTL caching, vector memoization, eviction | :white_check_mark: PASSED | 100% |
| **E2E: Full Pipeline** | [MediSenseE2EPipelineTest.md](MediSenseE2EPipelineTest.md) | [eval_MediSenseE2EPipelineTest.md](eval_MediSenseE2EPipelineTest.md) | User register -> Login -> Query -> Grounded response | :white_check_mark: PASSED | 100% |
| **E2E: Health Claims** | [HealthClaimVerificationE2ETest.md](HealthClaimVerificationE2ETest.md) | [eval_HealthClaimVerificationE2ETest.md](eval_HealthClaimVerificationE2ETest.md) | Allopathic vs Traditional dual positions | :white_check_mark: PASSED | 100% |

---

## 3. How to Run Tests

### Running Unit Tests Only
```bash
mvn test -P unit
```

### Running Integration Tests (PostgreSQL + Redis Testcontainers)
```bash
mvn test -P integration
```

### Running End-to-End Tests (Full Context + WireMock)
```bash
mvn test -P e2e
```

### Running Complete Verification Suite
```bash
mvn test
```

---

## 4. Continuous Integration (GitHub Actions)
The multi-stage pipeline is defined at [`.github/workflows/tests.yml`](../.github/workflows/tests.yml):
```
Stage 1: Unit Tests (Fast fail)
    └──> Stage 2: Integration Tests (Testcontainers Docker)
            └──> Stage 3: End-to-End Tests (WireMock + RestTemplate)
```
Any test regression automatically halts the build and uploads full Surefire XML/HTML test execution logs for auditing.
