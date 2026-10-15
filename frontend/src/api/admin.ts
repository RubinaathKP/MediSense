import apiClient from './client'
import type {
  EvalRunResponse,
  EvalLatestResultsResponse,
  EvalHistoryResponse,
  EvalFailuresResponse,
  AuditLogPage,
} from './types'

export const evalApi = {
  triggerRun: () => apiClient.post<EvalRunResponse>('/internal/eval/run'),
  getLatest: () => apiClient.get<EvalLatestResultsResponse>('/internal/eval/results/latest'),
  getHistory: () => apiClient.get<EvalHistoryResponse>('/internal/eval/results/history'),
  getFailures: (runId: string) =>
    apiClient.get<EvalFailuresResponse>(`/internal/eval/results/${runId}/failures`),
}

export const auditApi = {
  getLogs: (params: {
    page?: number
    size?: number
    eventType?: string
    severity?: string
    userId?: string
    dateFrom?: string
    dateTo?: string
  }) => apiClient.get<AuditLogPage>('/internal/audit', { params }),
}
