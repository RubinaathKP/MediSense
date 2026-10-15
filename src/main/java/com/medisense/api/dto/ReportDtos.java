package com.medisense.api.dto;

import com.medisense.domain.dto.LabReportDto;
import com.medisense.domain.dto.PrescriptionDto;

import java.util.List;
import java.util.UUID;

public class ReportDtos {

    public record ReportUploadResponse(
            UUID report_id,
            List<?> extracted_values,
            boolean requires_confirmation
    ) {}

    public record AbdmPullRequest(
            UUID session_id,
            String abha_address,
            List<String> record_types,
            String date_from,
            String date_to
    ) {}

    public record AbdmPullResponse(
            String consent_request_id,
            String status,
            String message
    ) {}

    public record ConfirmValueRequest(
            UUID value_id,
            String confirmed_value,
            String confirmed_unit
    ) {}

    public record ConfirmValueResponse(
            UUID value_id,
            boolean confirmed
    ) {}

    public record SessionReportDetailsResponse(
            List<LabReportDto> lab_values,
            List<PrescriptionDto> prescriptions
    ) {}
}
