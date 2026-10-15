package com.medisense.api.exception;

public class ABDMGatewayException extends RuntimeException {
    public ABDMGatewayException(String message) {
        super(message);
    }

    public ABDMGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
