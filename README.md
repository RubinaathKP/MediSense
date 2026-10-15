# MediSense

MediSense is an enterprise-grade, RAG-based AI healthcare intelligence platform engineered for clinical document comprehension, prescription verification, and automated health claim processing.

## Architecture Overview

MediSense is structured as a full-stack, distributed AI medical analysis system:

- **Backend (`/src`)**: Spring Boot 3.3 (Java 21) backend leveraging LangChain4j, HAPI FHIR, Apache Lucene, Apache Tika / PDFBox, pgvector, and Redis.
- **Frontend (`/frontend`)**: Modern React + TypeScript interface powered by Vite and Tailwind CSS.
- **Reranker Service (`/reranker-service`)**: Microservice for clinical cross-encoder passage reranking.
- **Infrastructure (`docker-compose.yml`)**: PostgreSQL (with `pgvector`), Redis, and service containerization.

## Key Capabilities

- **Clinical Document Parsing**: Multi-format ingestion (PDF, scanned prescriptions, lab reports) with OCR and semantic chunking.
- **Hybrid RAG Retrieval**: BM25 lexical search combined with dense vector embeddings (`pgvector`) and cross-encoder reranking.
- **FHIR & ABDM Integration**: Standardized healthcare interoperability conforming to FHIR R4 and ABDM (Ayushman Bharat Digital Mission) specifications.
- **Prescription & Claim Verification**: AI-assisted discrepancy detection, drug interaction analysis, and claim eligibility checks.

## Getting Started

### Prerequisites

- Java 21+ & Maven 3.9+
- Node.js 18+ & npm
- Python 3.10+
- Docker & Docker Compose

### Running Locally

1. **Start Infrastructure Services**:
   ```bash
   docker-compose up -d
   ```

2. **Run Backend**:
   ```bash
   mvn clean spring-boot:run
   ```

3. **Run Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. **Run Reranker Service**:
   ```bash
   cd reranker-service
   pip install -r requirements.txt
   uvicorn main:app --port 8001
   ```

## License

All rights reserved.
