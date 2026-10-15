package com.medisense.translation.indictrans2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IndicTrans2CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(IndicTrans2CircuitBreaker.class);

    private static final int FAILURE_THRESHOLD = 3;
    private static final long RESET_TIMEOUT_MS = 60_000L; // 60 seconds open state

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTimestamp = new AtomicLong(0L);

    public boolean allowExecution() {
        if (state == State.CLOSED) {
            return true;
        }

        long elapsed = System.currentTimeMillis() - lastFailureTimestamp.get();
        if (elapsed >= RESET_TIMEOUT_MS) {
            synchronized (this) {
                if (state == State.OPEN) {
                    state = State.HALF_OPEN;
                    log.info("IndicTrans2 Circuit Breaker reset timeout expired. Transitioning to HALF_OPEN state.");
                    return true;
                }
            }
        }

        if (state == State.HALF_OPEN) {
            return true;
        }

        log.warn("IndicTrans2 Circuit Breaker is OPEN. Bypassing IndicTrans2 microservice.");
        return false;
    }

    public synchronized void recordSuccess() {
        if (state != State.CLOSED) {
            log.info("IndicTrans2 microservice succeeded. Resetting Circuit Breaker to CLOSED state.");
        }
        failureCount.set(0);
        state = State.CLOSED;
    }

    public synchronized void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTimestamp.set(System.currentTimeMillis());

        if (state == State.HALF_OPEN || failures >= FAILURE_THRESHOLD) {
            state = State.OPEN;
            log.error("IndicTrans2 Circuit Breaker OPENED for {} ms due to {} consecutive failures.",
                    RESET_TIMEOUT_MS, failures);
        }
    }

    public String getState() {
        return state.name();
    }

    public void reset() {
        failureCount.set(0);
        state = State.CLOSED;
    }
}
