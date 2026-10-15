package com.medisense.ingestion.security;

/**
 * FileSecurityScanner — Interface for virus / malware scanning of uploaded documents.
 * Pluggable for ClamAV, AWS GuardDuty, or enterprise malware scanners.
 */
public interface FileSecurityScanner {

    /**
     * Scan the raw content of an uploaded file.
     *
     * @param content Raw byte array of the file
     * @throws MaliciousFileException if malware, virus, or dangerous payload is detected
     */
    void scan(byte[] content) throws MaliciousFileException;
}
