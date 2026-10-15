package com.medisense.api.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final long limit;
    private final String window;

    public RateLimitExceededException(String message, long retryAfterSeconds, long limit, String window) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
        this.window = window;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getLimit() {
        return limit;
    }

    public String getWindow() {
        return window;
    }
}
