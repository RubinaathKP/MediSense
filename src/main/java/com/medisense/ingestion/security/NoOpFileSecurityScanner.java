package com.medisense.ingestion.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * NoOpFileSecurityScanner — Default file scanner implementation.
 * Performs basic sanity checks and passes clean files; can be overridden by ClamAV in production.
 */
@Component
@ConditionalOnMissingBean(type = "com.medisense.ingestion.security.ClamAvFileSecurityScanner")
public class NoOpFileSecurityScanner implements FileSecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(NoOpFileSecurityScanner.class);

    @Override
    public void scan(byte[] content) throws MaliciousFileException {
        if (content == null || content.length == 0) {
            throw new MaliciousFileException("Empty file buffer supplied for security scanning");
        }

        // Check for common executable payload signatures embedded in files
        if (content.length > 2 && content[0] == 0x4D && content[1] == 0x5A) { // 'MZ' DOS/PE header
            log.warn("Security Alert: Executable PE header detected in uploaded file!");
            throw new MaliciousFileException("Dangerous executable file signature detected");
        }

        log.debug("File security scanner passed {} bytes cleanly.", content.length);
    }
}
