package com.medisense.rag.chunking.token;

public interface TokenCounterStrategy {
    int countTokens(String text);
}
