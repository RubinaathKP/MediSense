package com.medisense.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.domain.entity.EvalCaseResultEntity;
import com.medisense.domain.entity.EvalRunEntity;
import com.medisense.domain.repository.EvalCaseResultRepository;
import com.medisense.domain.repository.EvalRunRepository;
import com.medisense.evaluation.model.EvalMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EvalResultsService {

    private static final Logger log = LoggerFactory.getLogger(EvalResultsService.class);

    private final EvalRunRepository evalRunRepository;
    private final EvalCaseResultRepository evalCaseResultRepository;
    private final ObjectMapper objectMapper;

    public EvalResultsService(EvalRunRepository evalRunRepository,
                              EvalCaseResultRepository evalCaseResultRepository,
                              ObjectMapper objectMapper) {
        this.evalRunRepository = evalRunRepository;
        this.evalCaseResultRepository = evalCaseResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvalRun(String runId, String triggeredBy, List<EvaluationMetricsCalculator.CaseExecutionRecord> caseRecords, EvalMetrics metrics) {
        log.info("Persisting evaluation run results for runId: {}", runId);

        int totalCases = caseRecords.size();
        int passedCases = (int) caseRecords.stream().filter(rec -> rec.passed()).count();
        int failedCases = totalCases - passedCases;

        String metricsJson = "{}";
        try {
            metricsJson = objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            log.warn("Failed to serialize eval metrics to JSON: {}", e.getMessage());
        }

        EvalRunEntity runEntity = EvalRunEntity.builder()
                .runId(runId)
                .triggeredBy(triggeredBy)
                .startedAt(OffsetDateTime.now().minusSeconds(totalCases * 1L))
                .completedAt(OffsetDateTime.now())
                .totalCases(totalCases)
                .passedCases(passedCases)
                .failedCases(failedCases)
                .metricsJson(metricsJson)
                .build();

        evalRunRepository.save(runEntity);

        List<EvalCaseResultEntity> caseEntities = caseRecords.stream().map(rec ->
                EvalCaseResultEntity.builder()
                        .runId(runId)
                        .testCaseId(rec.testCase().id())
                        .passed(rec.passed())
                        .actualClassification(rec.actualClassification())
                        .actualIsReferral(rec.actualIsReferral())
                        .actualCitationPresent(rec.actualCitationPresent())
                        .failureReason(rec.failureReason())
                        .build()
        ).toList();

        evalCaseResultRepository.saveAll(caseEntities);
        log.info("Successfully persisted evaluation run {} with {} total cases ({} passed, {} failed).",
                runId, totalCases, passedCases, failedCases);
    }

    public Optional<EvalRunEntity> getLatestRun() {
        return evalRunRepository.findTopByOrderByStartedAtDesc();
    }

    public List<EvalRunEntity> getRunHistory() {
        return evalRunRepository.findAll();
    }

    public List<EvalCaseResultEntity> getRunFailures(String runId) {
        return evalCaseResultRepository.findByRunIdAndPassed(runId, false);
    }
}
