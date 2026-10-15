package com.medisense.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ClaimDtos {

    public record ClaimRequest(
            @NotNull(message = "session_id is required")
            UUID session_id,

            @NotBlank(message = "claim_text is required")
            @Size(max = 300, message = "claim_text cannot exceed 300 characters")
            String claim_text,

            @Pattern(regexp = "^(en|ta)$", message = "Language must be 'en' or 'ta'")
            String language
    ) {}

    public record PositionDto(
            String summary,
            String evidence_basis,
            String source
    ) {}

    public record ClaimAssessmentDto(
            PositionDto allopathic_position,
            PositionDto traditional_position,
            String verdict,
            String closing_note
    ) {}

    public record ClaimResponse(
            UUID query_id,
            ClaimAssessmentDto claim_assessment,
            boolean is_referral,
            String language_out
    ) {}
}
