package com.medisense;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all MediSense unit tests.
 * Enforces mock isolation via MockitoExtension and provides unified lifecycle hooks.
 */
@ExtendWith(MockitoExtension.class)
public abstract class UnitTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private AutoCloseable mocksCloser;

    @BeforeEach
    public void setupUnitMocks() {
        mocksCloser = MockitoAnnotations.openMocks(this);
        onSetup();
    }

    @AfterEach
    public void teardownUnitMocks() throws Exception {
        onTeardown();
        if (mocksCloser != null) {
            mocksCloser.close();
        }
    }

    /**
     * Subclasses can override this hook for custom per-test initializations.
     */
    protected void onSetup() {
        // Optional override
    }

    /**
     * Subclasses can override this hook for post-test cleanups.
     */
    protected void onTeardown() {
        // Optional override
    }
}
