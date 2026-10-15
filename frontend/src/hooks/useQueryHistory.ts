import { useQuery } from '@tanstack/react-query'
import { queriesApi } from '@/api/queries'
import type { QueryResponse } from '@/api/types'

export function useQueryHistory(sessionId: string | null) {
  return useQuery<QueryResponse[]>({
    queryKey: ['session-history', sessionId],
    queryFn: async () => {
      if (!sessionId) return []
      const res = await queriesApi.getSessionHistory(sessionId)
      return res.data
    },
    enabled: !!sessionId,
    staleTime: 5 * 60 * 1000, // 5 minutes caching
    gcTime: 10 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}
