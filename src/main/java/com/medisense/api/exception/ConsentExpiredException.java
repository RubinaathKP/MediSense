package com.medisense.api.exception;

public class ConsentExpiredException extends RuntimeException {
    public ConsentExpiredException(String message) {
        super(message);
    }
}
