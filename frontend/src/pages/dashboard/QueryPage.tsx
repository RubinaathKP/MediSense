import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  FileText,
  ShieldCheck,
  HelpCircle,
  MessageCircle,
  AlertCircle,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AxiosError } from 'axios'

import { queriesApi } from '@/api/queries'
import { useSessionStore } from '@/store/sessionStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import { useQueryHistory } from '@/hooks/useQueryHistory'
import type { QueryResponse, ApiError } from '@/api/types'
import QueryInput from '@/components/query/QueryInput'
import QueryResponseCard from '@/components/query/QueryResponseCard'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LanguageToggle from '@/components/common/LanguageToggle'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { cn } from '@/utils/cn'

type TabType = 'report' | 'claim' | 'general'

export default function QueryPage() {
  const { t } = useTranslation()
  const location = useLocation()
  const queryClient = useQueryClient()

  const prefill = (location.state as { prefillQuery?: string } | null)?.prefillQuery ?? ''
  const { session_id, report_loaded, report_type } = useSessionStore()
  const queryRateLimit = useRateLimit('query')

  const [selectedTab, setSelectedTab] = useState<TabType>('report')
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const [activeQueryText, setActiveQueryText] = useState<string>('')
  const [displayCount, setDisplayCount] = useState<number>(5)

  // Fetch cached session query history
  const {
    data: history = [],
    isLoading: isHistoryLoading,
    isError: isHistoryError,
  } = useQueryHistory(session_id)

  const mutation = useMutation({
    mutationFn: async ({
      text,
      language,
    }: {
      text: string
      language: 'en' | 'ta'
    }) => {
      setActiveQueryText(text)
      const mappedQueryType =
        selectedTab === 'claim'
          ? 'claim_verification'
          : selectedTab === 'report'
          ? report_type === 'prescription'
            ? 'prescription_explanation'
            : 'lab_explanation'
          : 'follow_up'

      const response = await queriesApi.submitQuery({
        session_id: session_id ?? 'anonymous',
        query_text: text,
        query_type: mappedQueryType,
        language,
      })
      return response.data
    },
    onSuccess: (newResponse) => {
      // Optimistically update query history cache
      if (session_id) {
        queryClient.setQueryData<QueryResponse[]>(
          ['session-history', session_id],
          (old = []) => [newResponse, ...old]
        )
      }
    },
    onError: (err: AxiosError<ApiError>) => {
      if (err.response?.status === 429) {
        setRateLimitSeconds(err.response.data?.retry_after_seconds ?? 3600)
      }
    },
  })

  const handleQuerySubmit = (text: string, language: 'en' | 'ta') => {
    mutation.mutate({ text, language })
  }

  const isTier3Limited = queryRateLimit.is_limited || rateLimitSeconds > 0
  const activeCountdown = rateLimitSeconds || queryRateLimit.countdown

  const visibleQueries = history.slice(0, displayCount)
  const hasMore = history.length > displayCount

  return (
    <div className="space-y-6">
      {/* Header with Title and Language Toggle */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <MessageCircle className="h-6 w-6 text-primary-400" />
            <span>{t('query.page_title', 'Ask MediSense')}</span>
          </h1>
          <p className="mt-1 text-sm text-surface-400">
            Citation-grounded medical explanations and health claim verifications.
          </p>
        </div>

        <LanguageToggle variant="compact" />
      </div>

      {/* Main Split Layout: Left (35%) / Right (65%) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left Panel (35% -> lg:col-span-5) */}
        <div className="lg:col-span-5 space-y-4">
          {/* Query Type Selector Tabs */}
          <div className="flex rounded-xl border border-surface-700 bg-surface-900/60 p-1">
            <button
              type="button"
              data-testid="tab-report"
              onClick={() => setSelectedTab('report')}
              className={cn(
                'flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-xs font-semibold transition-all',
                selectedTab === 'report'
                  ? 'bg-primary-600 text-white shadow-glow'
                  : 'text-surface-400 hover:text-slate-200'
              )}
            >
              <FileText className="h-3.5 w-3.5" />
              <span>My Report</span>
            </button>

            <button
              type="button"
              data-testid="tab-claim"
              onClick={() => setSelectedTab('claim')}
              className={cn(
                'flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-xs font-semibold transition-all',
                selectedTab === 'claim'
                  ? 'bg-primary-600 text-white shadow-glow'
                  : 'text-surface-400 hover:text-slate-200'
              )}
            >
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>Health Claim</span>
            </button>

            <button
              type="button"
              data-testid="tab-general"
              onClick={() => setSelectedTab('general')}
              className={cn(
                'flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-xs font-semibold transition-all',
                selectedTab === 'general'
                  ? 'bg-primary-600 text-white shadow-glow'
                  : 'text-surface-400 hover:text-slate-200'
              )}
            >
              <HelpCircle className="h-3.5 w-3.5" />
              <span>General</span>
            </button>
          </div>

          {/* Rate Limit Banner if Tier 3 reached */}
          {isTier3Limited && (
            <div data-testid="tier3-rate-limit-card" className="space-y-2">
              <RateLimitBanner
                retryAfterSeconds={activeCountdown}
                onExpired={() => setRateLimitSeconds(0)}
              />
              <p className="text-xs text-surface-400 px-1">
                You've reached your question limit for this hour. You can ask{' '}
                <span className="font-semibold text-primary-300">
                  {Math.max(queryRateLimit.remaining, 0)}
                </span>{' '}
                more questions. Limit resets in {activeCountdown}s.
              </p>
            </div>
          )}

          {/* Core Query Input Component */}
          <QueryInput
            onSubmit={handleQuerySubmit}
            isLoading={mutation.isPending}
            isRateLimited={isTier3Limited}
            rateLimitCountdown={activeCountdown}
            initialQuery={prefill}
            reportType={report_loaded ? (report_type as any) : 'none'}
            selectedTab={selectedTab}
          />
        </div>

        {/* Right Panel (65% -> lg:col-span-7) */}
        <div className="lg:col-span-7 space-y-4">
          {/* Ongoing Query Mutation Loading Indicator */}
          {mutation.isPending && (
            <div
              data-testid="query-loading-spinner"
              className="flex items-center gap-3 rounded-2xl border border-surface-700/60 bg-surface-800/40 p-5 animate-pulse"
            >
              <LoadingSpinner size="md" />
              <div>
                <p className="text-sm font-semibold text-white">
                  Grounding response in verified clinical sources...
                </p>
                <p className="text-xs text-surface-400 mt-0.5">
                  Filtering safe boundaries & retrieving medical guideline chunks
                </p>
              </div>
            </div>
          )}

          {/* Mutation error notification */}
          {mutation.isError && !isTier3Limited && (
            <div className="flex items-center gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-xs text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>
                {mutation.error?.response?.data?.message ||
                  'Failed to process query. Please try again.'}
              </span>
            </div>
          )}

          {/* Query Response List */}
          {isHistoryLoading ? (
            <div className="flex h-64 items-center justify-center">
              <LoadingSpinner size="lg" />
            </div>
          ) : isHistoryError ? (
            <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6 text-center text-xs text-red-300">
              Failed to load past session history.
            </div>
          ) : history.length === 0 && !mutation.isPending ? (
            /* Empty state */
            <div
              data-testid="query-empty-state"
              className="flex flex-col items-center justify-center rounded-2xl border border-surface-700/40 bg-surface-800/30 p-12 text-center"
            >
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-surface-700/40 text-primary-400 mb-4 shadow-glass">
                <MessageCircle className="h-7 w-7" />
              </div>
              <h3 className="text-base font-semibold text-white">
                Ask your first question about your health report
              </h3>
              <p className="mt-1 text-xs text-surface-400 max-w-md">
                Type any medical value, medication inquiry, or traditional remedy
                claim on the left to receive an objective, citation-grounded breakdown.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {visibleQueries.map((resp, idx) => (
                <QueryResponseCard
                  key={resp.query_id || idx}
                  response={resp}
                  queryText={idx === 0 && activeQueryText ? activeQueryText : undefined}
                />
              ))}

              {/* Pagination (Load More Button) */}
              {hasMore && (
                <div className="flex justify-center pt-2">
                  <button
                    type="button"
                    data-testid="load-more-btn"
                    onClick={() => setDisplayCount((prev) => prev + 5)}
                    className="rounded-xl border border-surface-700/80 bg-surface-800/60 px-5 py-2 text-xs font-semibold text-slate-300 shadow-sm hover:border-primary-500 hover:text-white transition"
                  >
                    Load more questions ({history.length - displayCount} remaining)
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
