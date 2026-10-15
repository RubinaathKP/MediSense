package com.medisense.safety.exception;

public class HallucinatedCitationException extends RuntimeException {
    public HallucinatedCitationException(String message) {
        super(message);
    }

    public HallucinatedCitationException(String message, Throwable cause) {
        super(message, cause);
    }
}
