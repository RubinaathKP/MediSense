package com.medisense.rag.reranking.exception;

public class RerankerUnavailableException extends RuntimeException {
    public RerankerUnavailableException(String message) {
        super(message);
    }

    public RerankerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
