import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { evalApi } from '@/api/admin'
import type { EvalLatestResultsResponse, EvalHistoryResponse } from '@/api/types'

export function useEvalData(pollActive = false) {
  const queryClient = useQueryClient()

  const latestQuery = useQuery<EvalLatestResultsResponse>({
    queryKey: ['eval-latest'],
    queryFn: () => evalApi.getLatest().then((r) => r.data),
    refetchInterval: (query) => {
      // If run is running or caller asked for active polling, poll every 30 seconds
      if (pollActive || (query.state.data && !query.state.data.run_at)) {
        return 30_000
      }
      return 60_000
    },
    staleTime: 10_000,
  })

  const historyQuery = useQuery<EvalHistoryResponse>({
    queryKey: ['eval-history'],
    queryFn: () => evalApi.getHistory().then((r) => r.data),
    staleTime: 30_000,
  })

  const triggerRunMutation = useMutation({
    mutationFn: () => evalApi.triggerRun().then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['eval-latest'] })
      queryClient.invalidateQueries({ queryKey: ['eval-history'] })
    },
  })

  return {
    latest: latestQuery.data,
    isLatestLoading: latestQuery.isLoading,
    isLatestError: latestQuery.isError,
    history: historyQuery.data?.runs ?? [],
    isHistoryLoading: historyQuery.isLoading,
    triggerRun: triggerRunMutation.mutateAsync,
    isTriggering: triggerRunMutation.isPending,
  }
}

export function useEvalFailures(runId: string | null) {
  return useQuery({
    queryKey: ['eval-failures', runId],
    queryFn: () => (runId ? evalApi.getFailures(runId).then((r) => r.data.failures) : []),
    enabled: !!runId,
  })
}
