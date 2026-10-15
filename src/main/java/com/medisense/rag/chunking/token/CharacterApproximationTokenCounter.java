package com.medisense.rag.chunking.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CharacterApproximationTokenCounter implements TokenCounterStrategy {

    private static final Logger log = LoggerFactory.getLogger(CharacterApproximationTokenCounter.class);
    private static final double CHARS_PER_TOKEN = 4.0;

    @Override
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int tokenCount = (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
        log.trace("Token count calculated: {} tokens for {} chars.", tokenCount, text.length());
        return Math.max(1, tokenCount);
    }
}
