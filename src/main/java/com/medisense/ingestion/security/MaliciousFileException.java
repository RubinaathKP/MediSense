package com.medisense.ingestion.security;

public class MaliciousFileException extends RuntimeException {
    public MaliciousFileException(String message) {
        super(message);
    }

    public MaliciousFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
