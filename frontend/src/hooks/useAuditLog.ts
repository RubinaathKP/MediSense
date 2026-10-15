import { useQuery } from '@tanstack/react-query'
import { auditApi } from '@/api/admin'
import type { AuditLogPage } from '@/api/types'

export interface AuditFilterParams {
  page?: number
  size?: number
  eventType?: string
  severity?: string
  userId?: string
  dateFrom?: string
  dateTo?: string
}

export function useAuditLog(params: AuditFilterParams) {
  const query = useQuery<AuditLogPage>({
    queryKey: ['audit-logs', params],
    queryFn: () => auditApi.getLogs(params).then((r) => r.data),
    staleTime: 10_000,
  })

  return {
    logs: query.data?.content ?? [],
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 1,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useSecurityEvents() {
  const query = useQuery<AuditLogPage>({
    queryKey: ['security-events-24h'],
    queryFn: () =>
      auditApi
        .getLogs({
          page: 0,
          size: 15,
          severity: 'WARN,CRITICAL',
        })
        .then((r) => r.data),
    refetchInterval: 5 * 60 * 1000, // auto-refresh every 5 minutes
    staleTime: 30_000,
  })

  return {
    events: query.data?.content ?? [],
    isLoading: query.isLoading,
  }
}
