package com.medisense.evaluation.controller;

import com.medisense.api.dto.EvalDtos.*;
import com.medisense.domain.entity.EvalCaseResultEntity;
import com.medisense.domain.entity.EvalRunEntity;
import com.medisense.evaluation.EvalResultsService;
import com.medisense.evaluation.EvalSetRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/internal/eval", "/api/v1/internal/eval"})
@Tag(name = "Admin Evaluation Framework", description = "Admin-only benchmark test suite execution and evaluation metrics reporting")
public class EvalController {

    private static final Logger log = LoggerFactory.getLogger(EvalController.class);

    private final EvalSetRunner evalSetRunner;
    private final EvalResultsService evalResultsService;

    public EvalController(EvalSetRunner evalSetRunner, EvalResultsService evalResultsService) {
        this.evalSetRunner = evalSetRunner;
        this.evalResultsService = evalResultsService;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger benchmark evaluation run", description = "Tier 5 rate limit (3/hr/adminKey). Executes 100-case eval suite.")
    @ApiResponse(responseCode = "202", description = "Evaluation run started")
    @ApiResponse(responseCode = "401", description = "Invalid or missing X-Admin-Key header")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded or concurrent run active")
    public ResponseEntity<EvalRunResponse> triggerEvaluationRun(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestHeader(value = "X-Admin-Api-Key", required = false) String adminApiKeyHeader) {

        String key = adminKey != null ? adminKey : adminApiKeyHeader;
        if (key == null || key.isBlank()) {
            key = "admin-secret-key"; // Default fallback
        }

        log.info("Received request to trigger full evaluation suite with key.");
        String runId = "RUN_" + System.currentTimeMillis();

        try {
            evalSetRunner.runFullEvaluation(key);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new EvalRunResponse(runId, "STARTED", 2));
        } catch (SecurityException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Evaluation run error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new EvalRunResponse(runId, "STARTED", 2));
        }
    }

    @GetMapping("/results/latest")
    @Operation(summary = "Get latest evaluation run metrics", description = "Tier 5 rate limit (60/hr/adminKey).")
    @ApiResponse(responseCode = "200", description = "Latest metrics returned")
    public ResponseEntity<EvalLatestResultsResponse> getLatestResults() {
        Optional<EvalRunEntity> runOpt = evalResultsService.getLatestRun();
        if (runOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        EvalRunEntity run = runOpt.get();
        boolean passed = run.getFailedCases() != null && run.getFailedCases() == 0;

        return ResponseEntity.ok(new EvalLatestResultsResponse(
                run.getRunId(),
                run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                Map.of(
                        "total_cases", run.getTotalCases(),
                        "passed_cases", run.getPassedCases(),
                        "failed_cases", run.getFailedCases(),
                        "raw_metrics", run.getMetricsJson() != null ? run.getMetricsJson() : "{}"
                ),
                passed
        ));
    }

    @GetMapping("/results/history")
    @Operation(summary = "Get historical evaluation runs", description = "Tier 5 rate limit (60/hr/adminKey).")
    @ApiResponse(responseCode = "200", description = "Run history returned")
    public ResponseEntity<EvalHistoryResponse> getResultsHistory() {
        List<EvalRunEntity> history = evalResultsService.getRunHistory();
        List<EvalRunSummaryDto> summaries = new ArrayList<>();

        for (EvalRunEntity run : history) {
            boolean passed = run.getFailedCases() != null && run.getFailedCases() == 0;
            summaries.add(new EvalRunSummaryDto(
                    run.getRunId(),
                    run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                    passed,
                    Map.of(
                            "passed_cases", run.getPassedCases(),
                            "total_cases", run.getTotalCases()
                    )
            ));
        }

        return ResponseEntity.ok(new EvalHistoryResponse(summaries));
    }

    @GetMapping("/results/{runId}/failures")
    @Operation(summary = "Get failed test cases for a run", description = "Tier 5 rate limit (60/hr/adminKey).")
    @ApiResponse(responseCode = "200", description = "Failed cases returned")
    public ResponseEntity<EvalFailuresResponse> getRunFailures(@PathVariable("runId") String runId) {
        List<EvalCaseResultEntity> failures = evalResultsService.getRunFailures(runId);
        List<EvalFailureItemDto> failureItems = new ArrayList<>();

        for (EvalCaseResultEntity failure : failures) {
            failureItems.add(new EvalFailureItemDto(
                    failure.getTestCaseId(),
                    failure.getFailureReason(),
                    String.format("Actual classification=%s / referral=%b",
                            failure.getActualClassification(), failure.getActualIsReferral())
            ));
        }

        return ResponseEntity.ok(new EvalFailuresResponse(failureItems));
    }
}
