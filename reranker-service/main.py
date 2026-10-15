import os
import logging
from typing import List
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from FlagEmbedding import FlagReranker

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("reranker-service")

app = FastAPI(title="MediSense BAAI/bge-reranker-v2-m3 Microservice")

MODEL_NAME = os.getenv("RERANKER_MODEL", "BAAI/bge-reranker-v2-m3")
logger.info(f"Loading reranker model: {MODEL_NAME}")
try:
    reranker = FlagReranker(MODEL_NAME, use_fp16=True)
    logger.info("Reranker model loaded successfully.")
except Exception as err:
    logger.warning(f"FP16 load failed or GPU unavailable, falling back to standard precision: {err}")
    reranker = FlagReranker(MODEL_NAME, use_fp16=False)

class Passage(BaseModel):
    chunk_id: str
    text: str

class RerankRequest(BaseModel):
    query: str
    passages: List[Passage] = Field(..., max_length=20)
    top_k: int = Field(default=5, ge=1, le=20)

class RerankResult(BaseModel):
    chunk_id: str
    score: float

class RerankResponse(BaseModel):
    results: List[RerankResult]

@app.get("/health")
def health_check():
    return {"status": "ok", "model": MODEL_NAME}

@app.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest):
    if not request.passages:
        return RerankResponse(results=[])

    try:
        pairs = [[request.query, p.text] for p in request.passages]
        scores = reranker.compute_score(pairs, normalize=True)
        
        if isinstance(scores, (float, int)):
            scores = [float(scores)]

        scored_results = []
        for p, s in zip(request.passages, scores):
            scored_results.append(RerankResult(chunk_id=p.chunk_id, score=float(s)))

        scored_results.sort(key=lambda x: x.score, reverse=True)
        top_k_results = scored_results[: request.top_k]

        return RerankResponse(results=top_k_results)
    except Exception as e:
        logger.error(f"Error during reranking calculation: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
