package com.medisense.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class QueryDtos {

    public record QueryRequest(
            @NotNull(message = "session_id is required")
            UUID session_id,

            @NotBlank(message = "query_text is required")
            @Size(max = 500, message = "query_text cannot exceed 500 characters")
            String query_text,

            @NotBlank(message = "query_type is required")
            @Pattern(regexp = "^(lab_explanation|prescription_explanation|claim_verification|follow_up)$", message = "Invalid query_type")
            String query_type,

            @Pattern(regexp = "^(en|ta)$", message = "Language must be 'en' or 'ta'")
            String language
    ) {}

    public record CitationDto(
            String chunk_id,
            String source_name,
            Integer source_year,
            String evidence_basis
    ) {}

    public record QueryResponse(
            UUID query_id,
            String response_text,
            boolean is_referral,
            List<CitationDto> citations,
            String language_out,
            Boolean translation_unavailable,
            Boolean insufficient_context,
            String classifier_flag
    ) {
        public static QueryResponse safe(UUID queryId, String text, List<CitationDto> citations, String lang, boolean transUnavailable, boolean insuffContext) {
            return new QueryResponse(queryId, text, false, citations, lang, transUnavailable, insuffContext, null);
        }

        public static QueryResponse referral(UUID queryId, String text, String flag, String lang) {
            return new QueryResponse(queryId, text, true, List.of(), lang, false, false, flag);
        }
    }
}
