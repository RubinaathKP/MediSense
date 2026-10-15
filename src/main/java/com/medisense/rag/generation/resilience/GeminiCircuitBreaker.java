package com.medisense.rag.generation.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GeminiCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(GeminiCircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN
    }

    private static final int FAILURE_THRESHOLD = 3;
    private static final long FAILURE_WINDOW_MS = 60_000L; // 60 seconds
    private static final long OPEN_DURATION_MS = 300_000L; // 5 minutes (300,000 ms)

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTimestamp = new AtomicLong(0);
    private final AtomicLong circuitOpenedTimestamp = new AtomicLong(0);

    private Clock clock = Clock.systemUTC();

    public GeminiCircuitBreaker() {}

    public GeminiCircuitBreaker(Clock clock) {
        this.clock = clock;
    }

    public boolean allowExecution() {
        if (state.get() == State.OPEN) {
            long now = clock.millis();
            if (now - circuitOpenedTimestamp.get() >= OPEN_DURATION_MS) {
                log.info("Gemini Circuit Breaker 5-minute cooldown expired. Allowing trial execution.");
                return true;
            }
            log.warn("Gemini Circuit Breaker is OPEN. Bypassing Gemini to Ollama fallback.");
            return false;
        }
        return true;
    }

    public synchronized void recordSuccess() {
        if (state.get() == State.OPEN || failureCount.get() > 0) {
            log.info("Gemini Provider execution succeeded. Resetting circuit breaker to CLOSED.");
        }
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    public synchronized void recordFailure() {
        long now = clock.millis();
        if (now - lastFailureTimestamp.get() > FAILURE_WINDOW_MS) {
            failureCount.set(1);
        } else {
            failureCount.incrementAndGet();
        }
        lastFailureTimestamp.set(now);

        log.warn("Recorded failure #{} for Gemini Provider within 60s window.", failureCount.get());

        if (failureCount.get() >= FAILURE_THRESHOLD) {
            state.set(State.OPEN);
            circuitOpenedTimestamp.set(now);
            log.error("Gemini Circuit Breaker OPENED for 5 minutes (300,000 ms) due to {} consecutive failures.", failureCount.get());
        }
    }

    public State getState() {
        if (state.get() == State.OPEN) {
            long now = clock.millis();
            if (now - circuitOpenedTimestamp.get() >= OPEN_DURATION_MS) {
                return State.CLOSED;
            }
        }
        return state.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public synchronized void reset() {
        failureCount.set(0);
        lastFailureTimestamp.set(0);
        circuitOpenedTimestamp.set(0);
        state.set(State.CLOSED);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
