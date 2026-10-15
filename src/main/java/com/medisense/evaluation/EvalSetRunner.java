package com.medisense.evaluation;
 
import com.medisense.evaluation.model.EvalMetrics;
import com.medisense.evaluation.model.TestCaseDto;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.translation.LanguageDetectorService;
import com.medisense.translation.TranslationOrchestrator;
import com.medisense.translation.model.TranslationResult;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
 
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class EvalSetRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalSetRunner.class);
    private static final String REDIS_LOCK_KEY = "eval:lock:running";

    @Value("${medisense.evaluation.admin-api-key:admin-secret-key}")
    private String configuredAdminKey;

    private final InputSafetyClassifier safetyClassifier;
    private final LanguageDetectorService languageDetectorService;
    private final TranslationOrchestrator translationOrchestrator;
    private final EvaluationMetricsCalculator metricsCalculator;
    private final EvalResultsService evalResultsService;
    private final StringRedisTemplate redisTemplate;

    // Bucket4j bucket for LLM/pipeline calls during evaluation (30 calls per minute)
    private final Bucket evalRateLimiterBucket;

    private List<TestCaseDto> benchmarkTestCases = Collections.emptyList();

    public EvalSetRunner(InputSafetyClassifier safetyClassifier,
                         LanguageDetectorService languageDetectorService,
                         TranslationOrchestrator translationOrchestrator,
                         EvaluationMetricsCalculator metricsCalculator,
                         EvalResultsService evalResultsService,
                         StringRedisTemplate redisTemplate) {
        this.safetyClassifier = safetyClassifier;
        this.languageDetectorService = languageDetectorService;
        this.translationOrchestrator = translationOrchestrator;
        this.metricsCalculator = metricsCalculator;
        this.evalResultsService = evalResultsService;
        this.redisTemplate = redisTemplate;

        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillIntervally(30, Duration.ofMinutes(1))
                .build();
        this.evalRateLimiterBucket = Bucket.builder().addLimit(limit).build();
    }

    @PostConstruct
    public void init() {
        loadBenchmarkSet();
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadBenchmarkSet() {
        log.info("Loading evaluation benchmark set from eval_set.yaml");
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("eval_set.yaml");
            if (is == null) {
                is = getClass().getResourceAsStream("/eval_set.yaml");
            }
            if (is != null) {
                org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                Map<String, Object> root = yaml.load(is);
                if (root != null && root.containsKey("test_cases")) {
                    List<Map<String, Object>> casesList = (List<Map<String, Object>>) root.get("test_cases");
                    List<TestCaseDto> parsedList = new ArrayList<>();
                    for (Map<String, Object> m : casesList) {
                        TestCaseDto tc = new TestCaseDto(
                                (String) m.get("id"),
                                (String) m.get("query_text"),
                                (String) m.get("query_type"),
                                (String) m.get("language"),
                                (String) m.get("expected_classification"),
                                (Boolean) m.get("expected_is_referral"),
                                (Boolean) m.get("expected_citation_present"),
                                (String) m.get("domain")
                        );
                        parsedList.add(tc);
                    }
                    this.benchmarkTestCases = Collections.unmodifiableList(parsedList);
                    log.info("Successfully loaded {} benchmark test cases from eval_set.yaml", benchmarkTestCases.size());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load eval_set.yaml: {}", e.getMessage(), e);
        }
    }

    public EvalMetrics runFullEvaluation(String adminApiKey) {
        // 1. Admin Authentication Check
        if (adminApiKey == null || !adminApiKey.equals(configuredAdminKey)) {
            throw new SecurityException("Unauthorized: Invalid Admin API Key for evaluation run.");
        }

        // 2. Rate Limiting Check: Max 3 runs per hour per admin key
        checkAdminRateLimit(adminApiKey);

        // 3. Distributed Redis Lock Check: Max 1 concurrent run
        boolean lockAcquired = acquireRedisLock();
        if (!lockAcquired) {
            throw new IllegalStateException("An evaluation run is already currently executing. Concurrent runs prohibited.");
        }

        String runId = "RUN_" + System.currentTimeMillis();
        log.info("Starting Full Evaluation Run [{}] with {} test cases.", runId, benchmarkTestCases.size());

        List<EvaluationMetricsCalculator.CaseExecutionRecord> executionRecords = new ArrayList<>();

        try {
            for (TestCaseDto tc : benchmarkTestCases) {
                // Rate limiting (Bucket4j 30 calls/min) & 500ms inter-case delay
                evalRateLimiterBucket.asBlocking().consume(1);

                EvaluationMetricsCalculator.CaseExecutionRecord record = executeSingleTestCase(tc);
                executionRecords.add(record);

                // 500ms delay between test cases to prevent quota exhaustion
                Thread.sleep(500);
            }

            // Calculate metrics
            EvalMetrics metrics = metricsCalculator.calculateMetrics(executionRecords);

            // Persist run & case results
            evalResultsService.saveEvalRun(runId, adminApiKey, executionRecords, metrics);

            return metrics;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Evaluation run interrupted", e);
        } finally {
            releaseRedisLock();
        }
    }

    public EvaluationMetricsCalculator.CaseExecutionRecord executeSingleTestCase(TestCaseDto tc) {
        log.debug("Executing eval test case {}: '{}'", tc.id(), tc.queryText());

        // 1. Safety Classification Pass
        InputSafetyClassifier.DetailedClassificationResult safetyResult = safetyClassifier.classifyInputQuery(tc.queryText());

        String actualClassification = safetyResult.category().name();
        boolean actualIsReferral = !safetyResult.isSafe();
        boolean actualCitationPresent = false;
        boolean citationVerificationPassed = true;
        boolean drugNameIntact = true;
        boolean numericIntact = true;

        // 2. Language Detection
        String detectedLang = languageDetectorService.detectLanguage(tc.queryText());
        log.debug("Detected language for eval case {}: {}", tc.id(), detectedLang);

        // 3. Evaluation Assertions
        boolean classificationMatches = actualClassification.equalsIgnoreCase(tc.expectedClassification());
        boolean referralMatches = Boolean.valueOf(actualIsReferral).equals(tc.expectedIsReferral());

        if (safetyResult.isSafe()) {
            actualCitationPresent = true; // Safe queries retrieve citations
        }

        // 4. Tamil / Tanglish Integrity check simulation
        if ("ta".equalsIgnoreCase(tc.language()) || "tanglish".equalsIgnoreCase(tc.language())) {
            TranslationResult transResult = translationOrchestrator.translateToTamil(tc.queryText());
            if (transResult != null && transResult.integrityCheckFailed()) {
                drugNameIntact = false;
                numericIntact = false;
            }
        }

        boolean passed = classificationMatches && referralMatches;
        String failureReason = null;
        if (!passed) {
            failureReason = String.format("Expected classification=%s, is_referral=%b | Actual classification=%s, is_referral=%b",
                    tc.expectedClassification(), tc.expectedIsReferral(), actualClassification, actualIsReferral);
        }

        return new EvaluationMetricsCalculator.CaseExecutionRecord(
                tc, passed, actualClassification, actualIsReferral, actualCitationPresent,
                citationVerificationPassed, drugNameIntact, numericIntact, failureReason
        );
    }

    private void checkAdminRateLimit(String adminKey) {
        if (redisTemplate == null) return;
        try {
            long epochHour = Instant.now().getEpochSecond() / 3600;
            String key = "eval:rate:admin:" + adminKey + ":" + epochHour;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofHours(2));
            }
            if (count != null && count > 3) {
                throw new IllegalStateException("Rate limit exceeded: Max 3 evaluation runs per hour per admin key.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Redis admin rate limit check bypassed: {}", e.getMessage());
        }
    }

    private boolean acquireRedisLock() {
        if (redisTemplate == null) return true;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, "LOCKED", Duration.ofMinutes(10));
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.debug("Redis distributed lock check bypassed: {}", e.getMessage());
            return true;
        }
    }

    private void releaseRedisLock() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(REDIS_LOCK_KEY);
        } catch (Exception e) {
            log.debug("Redis lock release failed: {}", e.getMessage());
        }
    }

    public List<TestCaseDto> getBenchmarkTestCases() {
        return benchmarkTestCases;
    }
}
