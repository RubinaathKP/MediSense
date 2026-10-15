package com.medisense.rag.reranking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RerankerCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(RerankerCircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN
    }

    private static final int FAILURE_THRESHOLD = 3;
    private static final long RESET_TIMEOUT_MS = 60_000L; // 60 seconds

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedTimestamp = new AtomicLong(0);

    private Clock clock = Clock.systemUTC();

    public RerankerCircuitBreaker() {}

    public RerankerCircuitBreaker(Clock clock) {
        this.clock = clock;
    }

    public boolean allowExecution() {
        if (state.get() == State.OPEN) {
            long now = clock.millis();
            if (now - circuitOpenedTimestamp.get() >= RESET_TIMEOUT_MS) {
                log.info("Circuit breaker reset timeout expired. Allowing execution attempt.");
                return true;
            }
            log.warn("Circuit breaker is OPEN. Fast-failing execution attempt.");
            return false;
        }
        return true;
    }

    public synchronized void recordSuccess() {
        if (state.get() == State.OPEN || consecutiveFailures.get() > 0) {
            log.info("Reranker service call succeeded. Resetting circuit breaker to CLOSED state.");
        }
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    public synchronized void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        log.warn("Recorded failure #{} for reranker service.", failures);

        if (failures >= FAILURE_THRESHOLD) {
            state.set(State.OPEN);
            circuitOpenedTimestamp.set(clock.millis());
            log.error("Circuit breaker OPENED for {} ms due to {} consecutive failures.", RESET_TIMEOUT_MS, failures);
        }
    }

    public State getState() {
        if (state.get() == State.OPEN) {
            long now = clock.millis();
            if (now - circuitOpenedTimestamp.get() >= RESET_TIMEOUT_MS) {
                return State.CLOSED;
            }
        }
        return state.get();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public synchronized void reset() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
        circuitOpenedTimestamp.set(0);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
