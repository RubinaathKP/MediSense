package com.medisense.ingestion.exception;

/**
 * Exception thrown when a PDF or document cannot be parsed due to corrupt structure or invalid magic bytes.
 */
public class ParseException extends DocumentParsingException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
