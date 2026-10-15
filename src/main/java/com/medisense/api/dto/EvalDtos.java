package com.medisense.api.dto;

import java.util.List;
import java.util.Map;

public class EvalDtos {

    public record EvalRunResponse(
            String run_id,
            String status,
            int estimated_duration_minutes
    ) {}

    public record EvalLatestResultsResponse(
            String run_id,
            String run_at,
            Map<String, Object> metrics,
            boolean passed
    ) {}

    public record EvalRunSummaryDto(
            String run_id,
            String run_at,
            boolean passed,
            Map<String, Object> summary_metrics
    ) {}

    public record EvalHistoryResponse(
            List<EvalRunSummaryDto> runs
    ) {}

    public record EvalFailureItemDto(
            String test_case_id,
            String failure_reason,
            String actual_vs_expected
    ) {}

    public record EvalFailuresResponse(
            List<EvalFailureItemDto> failures
    ) {}
}
