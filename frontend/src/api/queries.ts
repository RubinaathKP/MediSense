/**
 * Query API functions
 * Routes match QueryController.java:
 *  POST /query
 *  GET  /query/{id}
 *  GET  /session/{id}/history
 */
import apiClient from './client'
import type { QueryRequest, QueryResponse, SessionHistory } from './types'

export const queriesApi = {
  /**
   * POST /query  (JSON)
   * Rate limit: 30/hr/user (Tier 3)
   * query_text max 500 chars, query_type must be one of:
   *   'lab_explanation' | 'prescription_explanation' | 'claim_verification' | 'follow_up'
   *
   * ALWAYS returns 200 — is_referral=true means safety classifier blocked it.
   * The frontend must check is_referral before rendering CitationCard vs ReferralCard.
   */
  submitQuery: (data: QueryRequest) =>
    apiClient.post<QueryResponse>('/query', data),

  /**
   * GET /query/{id}
   * Rate limit: 100/hr/user (Tier 4)
   */
  getQueryById: (queryId: string) =>
    apiClient.get<QueryResponse>(`/query/${queryId}`),

  /**
   * GET /session/{id}/history
   * Rate limit: 60/hr/user (Tier 4)
   * Throws SESSION_NOT_FOUND (404) if session does not exist.
   */
  getSessionHistory: (sessionId: string) =>
    apiClient.get<SessionHistory>(`/session/${sessionId}/history`),
}
