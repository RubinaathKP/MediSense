/**
 * MediSense API Types
 *
 * All interfaces here are exact TypeScript mirrors of the backend Java DTOs.
 * Field names match the JSON serialised snake_case form produced by Spring Boot.
 *
 * Backend source references:
 *  - AuthDtos.java       → LoginRequest, RegisterRequest, AuthResponse, RefreshTokenRequest, LogoutResponse
 *  - ReportDtos.java     → ReportUploadResponse, AbdmPullRequest, AbdmPullResponse, ConfirmValueRequest/Response, SessionReportDetailsResponse
 *  - LabReportDto.java   → LabValue (domain DTO)
 *  - PrescriptionDto.java → Prescription (domain DTO)
 *  - QueryDtos.java      → QueryRequest, QueryResponse, Citation
 *  - ClaimDtos.java      → ClaimRequest, ClaimResponse, ClaimAssessment, Position
 *  - EvalDtos.java       → EvalRunResponse, EvalLatestResultsResponse, EvalHistoryResponse, EvalFailuresResponse
 *  - GlobalExceptionHandler → ApiError (all error shapes)
 *  - RateLimitFilter     → RateLimitInfo (from response headers + body)
 */

// ─────────────────────────────────────────────────────────────────────────────
// Auth DTOs  (AuthController.java + AuthDtos.java)
// ─────────────────────────────────────────────────────────────────────────────

/** POST /auth/register  or  /api/v1/auth/register */
export interface RegisterRequest {
  email: string          // @Email, @NotBlank
  password: string       // @Size(min=8), @NotBlank
  language_pref: 'en' | 'ta' | null
}

/** 201 Created response from /auth/register */
export interface RegisterResponse {
  user_id: string        // UUID
  message: string
}

/** POST /auth/login  or  /api/v1/auth/login */
export interface LoginRequest {
  email: string
  password: string
}

/**
 * 200 OK from /auth/login and /auth/refresh
 * JWT access token expires in `expires_in` seconds (default 900 = 15 min).
 * Refresh token is 7 days. Backend also sets httpOnly cookie but
 * the body contains both tokens for in-memory storage.
 */
export interface AuthResponse {
  access_token: string
  refresh_token: string
  expires_in: number     // seconds — use this to schedule proactive refresh
  user_id: string        // UUID
}

/** POST /auth/refresh */
export interface RefreshTokenRequest {
  refresh_token: string
}

/** POST /auth/logout */
export interface LogoutResponse {
  message: string
}

// ─────────────────────────────────────────────────────────────────────────────
// Lab Value  (LabReportDto.java — FlagStatus enum: NORMAL | BORDERLINE | ABNORMAL)
// ─────────────────────────────────────────────────────────────────────────────

export type FlagStatus = 'NORMAL' | 'BORDERLINE' | 'ABNORMAL'

/**
 * Domain DTO for a single extracted lab test value.
 * Returned inside SessionReportDetailsResponse.lab_values.
 */
export interface LabValue {
  testName: string
  extractedValue: string
  normalizedValue: number | null
  unit: string
  referenceRange: string
  flagStatus: FlagStatus
  extractionConfidence: number | null
  requiresUserConfirmation: boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// Prescription  (PrescriptionDto.java)
// ─────────────────────────────────────────────────────────────────────────────

/** Domain DTO for a single extracted prescription line item */
export interface Prescription {
  brandName: string
  genericName: string
  drugClass: string
  dosage: string
  frequency: string
  timing: string
  duration: string
  prescribingSpecialty: string | null
  extractionConfidence: number | null
  requiresUserConfirmation: boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// Report Ingestion DTOs  (ReportController.java + ReportDtos.java)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * POST /reports/upload  (multipart/form-data)
 * Fields: file (PDF), session_id? (UUID), report_type ('lab_report' | 'prescription')
 *
 * Note: upload is multipart — NOT JSON. Use FormData in Axios.
 * report_type default: 'lab_report'
 */
export interface ReportUploadResponse {
  report_id: string      // UUID
  extracted_values: LabValue[] | Prescription[]  // may be empty list initially
  requires_confirmation: boolean                  // true when OCR confidence < 0.90
}

/** POST /reports/abdm-pull  (JSON body) */
export interface AbdmPullRequest {
  session_id: string     // UUID
  abha_address: string   // format: username@abdm
  record_types: string[] // e.g. ['DiagnosticReport', 'Observation', 'MedicationRequest']
  date_from: string      // ISO date YYYY-MM-DD
  date_to: string        // ISO date YYYY-MM-DD
}

/**
 * 200 OK from /reports/abdm-pull
 * status will be 'PENDING_PATIENT_APPROVAL' — frontend must poll /reports/{session_id}
 */
export interface AbdmPullResponse {
  consent_request_id: string
  status: string
  message: string
}

/** POST /reports/confirm-value  (JSON body) */
export interface ConfirmValueRequest {
  value_id: string       // UUID of the ambiguous value
  confirmed_value: string
  confirmed_unit: string
}

/** 200 OK from /reports/confirm-value */
export interface ConfirmValueResponse {
  value_id: string       // UUID
  confirmed: boolean
}

/**
 * GET /reports/{session_id}
 * Returns the session's extracted lab values and prescriptions after
 * either PDF parsing or ABDM record delivery.
 */
export interface SessionReportDetailsResponse {
  lab_values: LabValue[]
  prescriptions: Prescription[]
}

// ─────────────────────────────────────────────────────────────────────────────
// Query DTOs  (QueryController.java + QueryDtos.java)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * POST /query  (JSON body)
 * query_type: 'lab_explanation' | 'prescription_explanation' | 'claim_verification' | 'follow_up'
 * language: 'en' | 'ta'  (defaults to 'en' server-side)
 * query_text: max 500 characters
 */
export interface QueryRequest {
  session_id: string
  query_text: string
  query_type: 'lab_explanation' | 'prescription_explanation' | 'claim_verification' | 'follow_up'
  language: 'en' | 'ta'
}

/**
 * CitationDto — grounding source for a generated response.
 * evidence_basis values: 'clinical_trial' | 'traditional_recognized' | 'not_found'
 */
export interface Citation {
  chunk_id: string
  source_name: string
  source_year: number | null
  evidence_basis: 'clinical_trial' | 'traditional_recognized' | 'not_found'
}

/**
 * 200 OK from POST /query and GET /query/{id}
 *
 * IMPORTANT integration notes:
 *  - is_referral=true means the response is a safe doctor-referral message,
 *    not an error. The frontend must display ReferralCard instead of CitationCard.
 *  - classifier_flag present only when is_referral=true
 *    (values: 'PRESCRIPTIVE' | 'DIAGNOSTIC' | 'DRUG_INTERACTION' | 'EMERGENCY')
 *  - translation_unavailable=true means Tamil output failed; display
 *    translation fallback notice but still show english response_text
 *  - insufficient_context=true means no relevant guideline chunk was found
 */
export interface QueryResponse {
  query_id: string
  response_text: string
  is_referral: boolean
  citations: Citation[]
  language_out: string
  translation_unavailable: boolean | null
  insufficient_context: boolean | null
  classifier_flag: 'PRESCRIPTIVE' | 'DIAGNOSTIC' | 'DRUG_INTERACTION' | 'EMERGENCY' | null
}

/** GET /session/{id}/history — returns list of past QueryResponse objects */
export type SessionHistory = QueryResponse[]

// ─────────────────────────────────────────────────────────────────────────────
// Claim DTOs  (ClaimController.java + ClaimDtos.java)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * POST /claims/verify  (JSON body)
 * claim_text: max 300 characters
 */
export interface ClaimRequest {
  session_id: string
  claim_text: string
  language: 'en' | 'ta'
}

/**
 * PositionDto — evidence position for one knowledge tradition.
 * evidence_basis: 'clinical_trial' | 'traditional_recognized' | 'not_found'
 */
export interface Position {
  summary: string
  evidence_basis: 'clinical_trial' | 'traditional_recognized' | 'not_found'
  source: string
}

/**
 * ClaimAssessmentDto — dual-tradition evidence presentation.
 * verdict is ALWAYS null per SRS rule FR-40.
 * The frontend must NOT show a verdict field.
 */
export interface ClaimAssessment {
  allopathic_position: Position
  traditional_position: Position
  verdict: null           // always null — never adjudicated by the system
  closing_note: string
}

/** 200 OK from /claims/verify */
export interface ClaimResponse {
  query_id: string
  claim_assessment: ClaimAssessment
  is_referral: boolean
  language_out: string
}

// ─────────────────────────────────────────────────────────────────────────────
// Evaluation DTOs  (EvalController.java + EvalDtos.java)
// Admin-only — requires ROLE_ADMIN + X-Admin-Key header
// ─────────────────────────────────────────────────────────────────────────────

/** 202 Accepted from POST /internal/eval/run */
export interface EvalRunResponse {
  run_id: string
  status: string                   // 'STARTED'
  estimated_duration_minutes: number
}

/** 200 OK from GET /internal/eval/results/latest */
export interface EvalLatestResultsResponse {
  run_id: string
  run_at: string | null
  metrics: {
    total_cases: number
    passed_cases: number
    failed_cases: number
    raw_metrics: string    // JSON string of EvalMetrics
    [key: string]: unknown
  }
  passed: boolean
}

export interface EvalRunSummary {
  run_id: string
  run_at: string | null
  passed: boolean
  summary_metrics: {
    passed_cases: number
    total_cases: number
    [key: string]: unknown
  }
}

/** 200 OK from GET /internal/eval/results/history */
export interface EvalHistoryResponse {
  runs: EvalRunSummary[]
}

export interface EvalFailureItem {
  test_case_id: string
  failure_reason: string
  actual_vs_expected: string
}

/** 200 OK from GET /internal/eval/results/{runId}/failures */
export interface EvalFailuresResponse {
  failures: EvalFailureItem[]
}

// ─────────────────────────────────────────────────────────────────────────────
// Audit Log  (AuditController.java — paginated Spring Data Page)
// ─────────────────────────────────────────────────────────────────────────────

export interface AuditLogEntry {
  logId: string
  eventType: string
  sessionId: string | null
  userId: string | null
  queryId: string | null
  consentGrantId: string | null
  ipAddress: string
  eventDetail: string | null     // JSON string
  severity: 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL'
  createdAt: string              // OffsetDateTime ISO
}

/** Spring Data Page<AuditLogEntity> shape */
export interface AuditLogPage {
  content: AuditLogEntry[]
  totalElements: number
  totalPages: number
  size: number
  number: number                 // current page (0-indexed)
  first: boolean
  last: boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// Error Shapes  (GlobalExceptionHandler.java)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Standard error envelope returned for all 4xx / 5xx responses.
 * error_code values emitted by GlobalExceptionHandler:
 *   RATE_LIMIT_EXCEEDED | DOCUMENT_QUALITY_FAILED | PRESCRIPTIVE_CONTENT_DETECTED
 *   TRANSLATION_INTEGRITY_FAILED | HALLUCINATED_CITATION_DETECTED
 *   ABDM_GATEWAY_UNAVAILABLE | LLM_PROVIDER_UNAVAILABLE | CONSENT_EXPIRED
 *   SESSION_NOT_FOUND | FILE_TOO_LARGE | INVALID_FILE_TYPE
 *   VALIDATION_FAILED | INTERNAL_SERVER_ERROR | TOKEN_BLACKLISTED
 */
export interface ApiError {
  error_code: string
  message: string
  referral_response: string | null
  timestamp: string             // OffsetDateTime ISO
  // Populated only for RATE_LIMIT_EXCEEDED (429):
  retry_after_seconds?: number
  limit?: number
  window?: string               // e.g. '1 hour'
  // Populated only for VALIDATION_FAILED (400):
  field_errors?: Record<string, string>
  // Populated only for TRANSLATION_INTEGRITY_FAILED (200):
  english_text?: string
  translation_unavailable?: boolean
  // Populated for HALLUCINATED_CITATION_DETECTED (200):
  is_referral?: boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// Rate Limit State  (parsed from 429 body + response headers)
// Headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, Retry-After
// ─────────────────────────────────────────────────────────────────────────────

export interface RateLimitInfo {
  endpoint_slug: string      // e.g. 'reports-upload', 'query', 'auth-login'
  limit: number              // X-RateLimit-Limit
  remaining: number          // X-RateLimit-Remaining
  reset_at: number           // X-RateLimit-Reset (Unix epoch seconds)
  retry_after: number        // Retry-After (seconds)
  captured_at: number        // Date.now() when interceptor captured this
}
