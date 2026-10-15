/**
 * Claims API functions
 * Routes match ClaimController.java:
 *  POST /claims/verify
 */
import apiClient from './client'
import type { ClaimRequest, ClaimResponse } from './types'

export const claimsApi = {
  /**
   * POST /claims/verify  (JSON)
   * Rate limit: 20/hr/user (Tier 3)
   * claim_text max 300 characters
   *
   * Response always 200 — is_referral=false for normal assessments.
   * claim_assessment.verdict is ALWAYS null (SRS FR-40 — system never adjudicates).
   * closing_note always present — display it prominently after dual-position cards.
   */
  verifyClaim: (data: ClaimRequest) =>
    apiClient.post<ClaimResponse>('/claims/verify', data),
}
