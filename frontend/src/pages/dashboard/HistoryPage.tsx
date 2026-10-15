import { useState, useMemo } from 'react'
import {
  History,
  Search,
  Filter,
  ShieldAlert,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  FileText,
} from 'lucide-react'
import { format } from 'date-fns'
import { useQueryHistory } from '@/hooks/useQueryHistory'
import { useSessionStore } from '@/store/sessionStore'
import QueryResponseCard from '@/components/query/QueryResponseCard'
import LoadingSpinner from '@/components/common/LoadingSpinner'

const PAGE_SIZE = 20

export default function QueryHistory() {
  const { session_id } = useSessionStore()
  const { data: history = [], isLoading, isError } = useQueryHistory(session_id)

  const [searchQuery, setSearchQuery] = useState('')
  const [filterType, setFilterType] = useState<string>('all')
  const [filterReferral, setFilterReferral] = useState<string>('all') // 'all' | 'referral' | 'safe'
  const [expandedQueryId, setExpandedQueryId] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState<number>(1)

  // Filter queries
  const filteredHistory = useMemo(() => {
    return history.filter((item) => {
      // Search match
      const matchesSearch =
        !searchQuery ||
        item.response_text.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (item.query_id && item.query_id.toLowerCase().includes(searchQuery.toLowerCase()))

      // Referral filter
      const matchesReferral =
        filterReferral === 'all' ||
        (filterReferral === 'referral' && item.is_referral) ||
        (filterReferral === 'safe' && !item.is_referral)

      // Query classification/type filter
      const matchesType =
        filterType === 'all' ||
        (filterType === 'emergency' && item.classifier_flag === 'EMERGENCY') ||
        (filterType === 'referral' && item.is_referral) ||
        (filterType === 'standard' && !item.is_referral && !item.classifier_flag)

      return matchesSearch && matchesReferral && matchesType
    })
  }, [history, searchQuery, filterReferral, filterType])

  // Group queries by session date (using simulated current date or session bucket)
  const totalPages = Math.ceil(filteredHistory.length / PAGE_SIZE) || 1
  const paginatedQueries = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE
    return filteredHistory.slice(start, start + PAGE_SIZE)
  }, [filteredHistory, currentPage])

  const toggleExpand = (id: string) => {
    setExpandedQueryId((prev) => (prev === id ? null : id))
  }

  const currentDateLabel = format(new Date(), 'EEEE, MMMM d, yyyy')

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <History className="h-6 w-6 text-primary-400" />
            <span>Session Query History</span>
          </h1>
          <p className="mt-1 text-sm text-surface-400">
            Complete audit trail of health inquiries, doctor referrals, and cited evidence.
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs text-surface-400 bg-surface-800/60 px-3 py-1.5 rounded-xl border border-surface-700/50">
          <Clock className="h-3.5 w-3.5 text-primary-400" />
          <span>Active Session ID: <strong className="font-mono text-slate-200">{session_id || 'Anonymous'}</strong></span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-surface-700/60 bg-surface-800/40 p-4 shadow-sm">
        {/* Search */}
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-500" />
          <input
            type="text"
            data-testid="history-search-input"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search responses or keywords..."
            className="w-full rounded-xl border border-surface-700 bg-surface-900/60 py-2 pl-9 pr-3 text-xs text-white placeholder-surface-500 outline-none focus:border-primary-500"
          />
        </div>

        {/* Filter by Referral Status */}
        <div className="flex items-center gap-1.5">
          <Filter className="h-3.5 w-3.5 text-surface-400" />
          <select
            data-testid="filter-referral-select"
            value={filterReferral}
            onChange={(e) => setFilterReferral(e.target.value)}
            className="rounded-xl border border-surface-700 bg-surface-900/60 px-3 py-2 text-xs text-slate-300 outline-none focus:border-primary-500"
          >
            <option value="all">All Response Types</option>
            <option value="safe">Safe Grounded Responses</option>
            <option value="referral">Doctor Referrals</option>
          </select>
        </div>

        {/* Filter by Flag */}
        <select
          data-testid="filter-type-select"
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
          className="rounded-xl border border-surface-700 bg-surface-900/60 px-3 py-2 text-xs text-slate-300 outline-none focus:border-primary-500"
        >
          <option value="all">All Categories</option>
          <option value="standard">Standard Explanation</option>
          <option value="referral">Clinical Referral</option>
          <option value="emergency">Emergency Alert</option>
        </select>
      </div>

      {/* Main List Body */}
      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <LoadingSpinner size="lg" />
        </div>
      ) : isError ? (
        <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6 text-center text-xs text-red-300">
          Unable to load session query history.
        </div>
      ) : filteredHistory.length === 0 ? (
        <div
          data-testid="history-empty-state"
          className="rounded-2xl border border-surface-700/40 bg-surface-800/30 p-12 text-center"
        >
          <FileText className="h-10 w-10 text-surface-500 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-white">No query logs found</h3>
          <p className="mt-1 text-xs text-surface-400 max-w-sm mx-auto">
            {searchQuery || filterReferral !== 'all' || filterType !== 'all'
              ? 'Try adjusting your filters to see more results.'
              : 'Submit questions in the Query tab to begin building your session history.'}
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Group Header (Date Bucket) */}
          <div className="border-b border-surface-700/50 pb-2">
            <h2 className="text-xs font-bold uppercase tracking-wider text-primary-400">
              {currentDateLabel}
            </h2>
          </div>

          {/* Paginated Query Accordion Items */}
          <div className="space-y-3">
            {paginatedQueries.map((item, idx) => {
              const queryKey = item.query_id || `q-${idx}`
              const isExpanded = expandedQueryId === queryKey

              // Truncate response preview at 150 chars
              const previewText =
                item.response_text.length > 150
                  ? `${item.response_text.slice(0, 150)}...`
                  : item.response_text

              return (
                <div
                  key={queryKey}
                  data-testid={`history-item-${idx}`}
                  className="rounded-2xl border border-surface-700/60 bg-surface-800/50 shadow-sm overflow-hidden transition-all hover:border-surface-600"
                >
                  {/* Summary Bar */}
                  <div
                    onClick={() => toggleExpand(queryKey)}
                    className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 cursor-pointer hover:bg-surface-700/20 select-none"
                  >
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-surface-400">
                          #{filteredHistory.length - ((currentPage - 1) * PAGE_SIZE + idx)}
                        </span>

                        {item.classifier_flag === 'EMERGENCY' ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-500/15 border border-red-500/30 px-2 py-0.5 text-[10px] font-bold text-red-400 uppercase">
                            Emergency
                          </span>
                        ) : item.is_referral ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 border border-amber-500/30 px-2 py-0.5 text-[10px] font-medium text-amber-400">
                            <ShieldAlert className="h-3 w-3" />
                            Doctor Referral
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 text-[10px] font-medium text-emerald-400">
                            <CheckCircle2 className="h-3 w-3" />
                            Safe Grounded
                          </span>
                        )}

                        {item.language_out === 'ta' && (
                          <span className="rounded bg-surface-700 px-1.5 py-0.5 text-[10px] text-amber-300 font-tamil">
                            தமிழ்
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-slate-300 leading-relaxed font-medium">
                        {previewText}
                      </p>
                    </div>

                    <div className="flex items-center gap-3 shrink-0">
                      {item.citations && item.citations.length > 0 && (
                        <span className="text-[11px] text-surface-400">
                          {item.citations.length} source{item.citations.length > 1 ? 's' : ''}
                        </span>
                      )}
                      <button
                        type="button"
                        className="flex h-7 w-7 items-center justify-center rounded-lg bg-surface-700/50 text-surface-300 hover:text-white"
                      >
                        {isExpanded ? (
                          <ChevronUp className="h-4 w-4" />
                        ) : (
                          <ChevronDown className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Expanded Full Details */}
                  {isExpanded && (
                    <div className="border-t border-surface-700/60 p-4 bg-surface-900/40 animate-fade-in">
                      <QueryResponseCard response={item} />
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          {/* Pagination Controls (20 per page) */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-surface-700/40 pt-4">
              <span className="text-xs text-surface-400">
                Showing page <strong className="text-slate-200">{currentPage}</strong> of{' '}
                <strong className="text-slate-200">{totalPages}</strong> ({filteredHistory.length} total entries)
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  data-testid="prev-page-btn"
                  onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                  disabled={currentPage === 1}
                  className="rounded-lg border border-surface-700 bg-surface-800 px-3 py-1.5 text-xs text-slate-300 hover:border-primary-500 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  type="button"
                  data-testid="next-page-btn"
                  onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="rounded-lg border border-surface-700 bg-surface-800 px-3 py-1.5 text-xs text-slate-300 hover:border-primary-500 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
