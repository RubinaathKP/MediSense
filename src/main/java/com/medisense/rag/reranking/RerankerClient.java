package com.medisense.rag.reranking;

import com.medisense.rag.reranking.model.RerankRequest;
import com.medisense.rag.reranking.model.RerankResponse;

public interface RerankerClient {
    RerankResponse rerank(RerankRequest request);
}
