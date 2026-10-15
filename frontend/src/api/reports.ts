/**
 * Reports API functions
 * Routes match ReportController.java exactly:
 *  POST /reports/upload            (multipart/form-data)
 *  POST /reports/abdm-pull         (JSON)
 *  GET  /reports/{session_id}      (poll for ABDM record delivery)
 *  POST /reports/confirm-value     (JSON)
 */
import apiClient from './client'
import type {
  ReportUploadResponse,
  AbdmPullRequest,
  AbdmPullResponse,
  SessionReportDetailsResponse,
  ConfirmValueRequest,
  ConfirmValueResponse,
} from './types'

export const reportsApi = {
  /**
   * POST /reports/upload  (multipart/form-data)
   * Rate limit: 10/hr/user (Tier 2)
   *
   * Backend validates:
   *  - file size ≤ 10MB (throws FILE_TOO_LARGE)
   *  - content-type = application/pdf (throws INVALID_FILE_TYPE)
   *  - OCR confidence — if < 0.85 throws DOCUMENT_QUALITY_FAILED (400)
   *  - requires_confirmation=true when confidence < 0.90 (still 200 OK)
   *
   * session_id: optional UUID param (omit if starting a new session)
   * report_type: 'lab_report' | 'prescription' (defaults to 'lab_report')
   */
  uploadReport: (
    file: File,
    reportType: 'lab_report' | 'prescription',
    sessionId?: string
  ) => {
    const form = new FormData()
    form.append('file', file)
    form.append('report_type', reportType)
    if (sessionId) form.append('session_id', sessionId)

    return apiClient.post<ReportUploadResponse>('/reports/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /**
   * POST /reports/abdm-pull  (JSON)
   * Rate limit: 5/hr/user (Tier 2)
   * Returns status='PENDING_PATIENT_APPROVAL' — poll /reports/{session_id} afterwards
   */
  abdmPull: (data: AbdmPullRequest) =>
    apiClient.post<AbdmPullResponse>('/reports/abdm-pull', data),

  /**
   * GET /reports/{session_id}
   * Rate limit: 60/hr/user (Tier 4)
   * Poll this endpoint after ABDM pull — returns lab_values/prescriptions once
   * ABDM data arrives. Returns empty lists if data not yet delivered.
   * Throws SESSION_NOT_FOUND (404) if session does not exist.
   */
  getReportDetails: (sessionId: string) =>
    apiClient.get<SessionReportDetailsResponse>(`/reports/${sessionId}`),

  /**
   * POST /reports/confirm-value  (JSON)
   * Rate limit: 30/hr/user (Tier 2)
   * Confirm or correct a low-confidence extracted value.
   */
  confirmValue: (data: ConfirmValueRequest) =>
    apiClient.post<ConfirmValueResponse>('/reports/confirm-value', data),
}
