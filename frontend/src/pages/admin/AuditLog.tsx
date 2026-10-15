import { useState, useMemo } from 'react'
import {
  FileText,
  Filter,
  Search,
  ChevronDown,
  ChevronUp,
} from 'lucide-react'
import { format } from 'date-fns'
import SecurityEventsPanel from '@/components/admin/SecurityEventsPanel'
import { useAuditLog, useSecurityEvents } from '@/hooks/useAuditLog'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import type { AuditLogEntry } from '@/api/types'

export default function AuditLog() {
  const [selectedSeverities, setSelectedSeverities] = useState<string[]>([])
  const [selectedEventTypes, setSelectedEventTypes] = useState<string[]>([])
  const [userIdSearch, setUserIdSearch] = useState('')
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)

  // Fetch paginated audit log
  const { logs, totalPages, totalElements, isLoading } = useAuditLog({
    page: currentPage,
    size: 20,
    severity: selectedSeverities.join(','),
    eventType: selectedEventTypes.join(','),
    userId: userIdSearch || undefined,
  })

  // Fetch right sidebar security events (auto-refreshed)
  const { events: securityEvents, isLoading: isSecurityLoading } = useSecurityEvents()

  // Ensure CRITICAL events are pinned to top regardless of pagination
  const sortedLogs = useMemo(() => {
    const criticalLogs = logs.filter((l) => l.severity === 'CRITICAL')
    const otherLogs = logs.filter((l) => l.severity !== 'CRITICAL')
    return [...criticalLogs, ...otherLogs]
  }, [logs])

  const toggleSeverity = (sev: string) => {
    setSelectedSeverities((prev) =>
      prev.includes(sev) ? prev.filter((s) => s !== sev) : [...prev, sev]
    )
  }

  const toggleEventType = (ev: string) => {
    setSelectedEventTypes((prev) =>
      prev.includes(ev) ? prev.filter((e) => e !== ev) : [...prev, ev]
    )
  }

  const toggleExpand = (logId: string) => {
    setExpandedLogId((prev) => (prev === logId ? null : logId))
  }

  const getSeverityStyle = (sev: AuditLogEntry['severity']) => {
    switch (sev) {
      case 'CRITICAL':
        return 'bg-rose-500/15 text-rose-400 border-rose-500/30'
      case 'ERROR':
        return 'bg-orange-500/15 text-orange-400 border-orange-500/30'
      case 'WARN':
        return 'bg-amber-500/15 text-amber-400 border-amber-500/30'
      case 'INFO':
      default:
        return 'bg-slate-500/15 text-slate-400 border-slate-500/30'
    }
  }

  return (
    <div className="space-y-8 animate-fade-in max-w-7xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-surface-700/60 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-amber-500/20 px-2.5 py-0.5 text-xs font-bold text-amber-300 border border-amber-500/30">
              Compliance & Security
            </span>
            <span className="text-xs text-surface-400 font-mono">
              Audit queries remaining: <strong>10/10</strong> per hour
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white mt-1">
            Immutable Audit Trail & Security Logs
          </h1>
        </div>
      </div>

      {/* Grid: Left Logs (70%) + Right Security Panel (30%) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left Column (8 / 12) */}
        <div className="lg:col-span-8 space-y-5">
          {/* Filters Panel */}
          <div
            data-testid="audit-filters-panel"
            className="rounded-3xl border border-surface-700/60 bg-surface-800/40 p-5 space-y-4 shadow-sm"
          >
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-surface-400 border-b border-surface-700/40 pb-2">
              <Filter className="h-3.5 w-3.5" />
              <span>Filter Audit Events</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Severity multiselect buttons */}
              <div className="space-y-1.5">
                <label className="text-[11px] font-semibold text-slate-300">Severity:</label>
                <div className="flex flex-wrap gap-1.5">
                  {['INFO', 'WARN', 'ERROR', 'CRITICAL'].map((sev) => {
                    const isSelected = selectedSeverities.includes(sev)
                    return (
                      <button
                        key={sev}
                        type="button"
                        data-testid={`filter-severity-${sev.toLowerCase()}`}
                        onClick={() => toggleSeverity(sev)}
                        className={`rounded-lg px-2.5 py-1 text-xs font-bold border transition ${
                          isSelected
                            ? 'bg-primary-600 border-primary-500 text-white'
                            : 'border-surface-700 bg-surface-900/60 text-surface-400 hover:text-slate-200'
                        }`}
                      >
                        {sev}
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* User ID Search input */}
              <div className="space-y-1.5">
                <label className="text-[11px] font-semibold text-slate-300">User ID:</label>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-surface-500" />
                  <input
                    type="text"
                    data-testid="audit-user-search-input"
                    value={userIdSearch}
                    onChange={(e) => setUserIdSearch(e.target.value)}
                    placeholder="Search by UUID..."
                    className="w-full rounded-xl border border-surface-700 bg-surface-900/60 py-1.5 pl-9 pr-3 text-xs text-white placeholder-surface-500 outline-none focus:border-primary-500 font-mono"
                  />
                </div>
              </div>
            </div>

            {/* Event Type Selectors */}
            <div className="space-y-1.5 pt-1">
              <label className="text-[11px] font-semibold text-slate-300">Event Types:</label>
              <div className="flex flex-wrap gap-1.5">
                {[
                  'AUTH_LOGIN_SUCCESS',
                  'ACCOUNT_LOCKOUT',
                  'REPORT_UPLOADED',
                  'CONSENT_GRANTED',
                  'QUERY_EXECUTED',
                  'PRESCRIPTIVE_ATTEMPT_BLOCKED',
                ].map((ev) => {
                  const isSelected = selectedEventTypes.includes(ev)
                  return (
                    <button
                      key={ev}
                      type="button"
                      onClick={() => toggleEventType(ev)}
                      className={`rounded-lg px-2 py-0.5 text-[10px] font-mono border transition ${
                        isSelected
                          ? 'bg-amber-500/20 border-amber-500/50 text-amber-300'
                          : 'border-surface-700/60 bg-surface-900/40 text-surface-400 hover:text-slate-300'
                      }`}
                    >
                      {ev}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>

          {/* Audit Entries Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs text-surface-400">
              <span className="font-bold uppercase tracking-wider">
                Event Log Table ({totalElements} entries)
              </span>
              <span>Page {currentPage + 1} of {totalPages}</span>
            </div>

            {isLoading ? (
              <div className="flex h-48 items-center justify-center">
                <LoadingSpinner size="lg" />
              </div>
            ) : sortedLogs.length === 0 ? (
              <div
                data-testid="audit-empty-state"
                className="rounded-3xl border border-surface-700/60 bg-surface-800/30 p-12 text-center text-xs text-surface-400"
              >
                <FileText className="h-8 w-8 text-surface-500 mx-auto mb-2 opacity-60" />
                <span>No audit events match the specified filter criteria.</span>
              </div>
            ) : (
              <div className="overflow-x-auto rounded-3xl border border-surface-700/60 bg-surface-800/40 shadow-sm">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-surface-900/60 text-[10px] font-bold uppercase tracking-wider text-surface-400 border-b border-surface-700">
                    <tr>
                      <th className="px-4 py-3">Timestamp</th>
                      <th className="px-4 py-3">Event Type</th>
                      <th className="px-4 py-3">Severity</th>
                      <th className="px-4 py-3">Session ID</th>
                      <th className="px-4 py-3">Details Preview</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-700/50">
                    {sortedLogs.map((log) => {
                      const isExpanded = expandedLogId === log.logId
                      const isCritical = log.severity === 'CRITICAL'

                      return (
                        <tr
                          key={log.logId}
                          data-testid={`audit-row-${log.logId}`}
                          onClick={() => toggleExpand(log.logId)}
                          className={`cursor-pointer transition select-none ${
                            isCritical
                              ? 'bg-rose-950/20 hover:bg-rose-950/40 border-l-2 border-rose-500'
                              : 'hover:bg-surface-700/30'
                          }`}
                        >
                          <td className="px-4 py-3 font-mono text-surface-400">
                            {format(new Date(log.createdAt || Date.now()), 'MMM d, HH:mm:ss')}
                          </td>
                          <td className="px-4 py-3 font-mono font-bold text-slate-200">
                            {log.eventType}
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[10px] font-bold ${getSeverityStyle(
                                log.severity
                              )}`}
                            >
                              {log.severity}
                            </span>
                          </td>
                          <td className="px-4 py-3 font-mono text-surface-400 truncate max-w-[120px]">
                            {log.sessionId ? `${log.sessionId.slice(0, 8)}...` : 'N/A'}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-surface-400 truncate max-w-[180px]">
                                {log.eventDetail ? log.eventDetail.slice(0, 40) : 'No payload'}
                              </span>
                              {isExpanded ? (
                                <ChevronUp className="h-3.5 w-3.5 text-primary-400" />
                              ) : (
                                <ChevronDown className="h-3.5 w-3.5 text-surface-500" />
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {/* Expanded JSON Inspector Modal / Row if open */}
            {expandedLogId && (
              <div
                data-testid="audit-json-inspector"
                className="rounded-2xl border border-surface-700 bg-surface-900 p-4 space-y-2 font-mono text-xs text-slate-300 animate-fade-in"
              >
                <div className="flex items-center justify-between text-surface-400 text-[11px] pb-1 border-b border-surface-800">
                  <span>Full Event Payload: {expandedLogId}</span>
                  <button
                    type="button"
                    onClick={() => setExpandedLogId(null)}
                    className="text-primary-400 hover:underline"
                  >
                    Close
                  </button>
                </div>
                <pre className="overflow-x-auto text-[11px] text-teal-300">
                  {JSON.stringify(
                    sortedLogs.find((l) => l.logId === expandedLogId) ?? {},
                    null,
                    2
                  )}
                </pre>
              </div>
            )}

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
                  disabled={currentPage === 0}
                  className="rounded-xl border border-surface-700 px-3 py-1.5 text-xs text-slate-300 hover:border-primary-500 disabled:opacity-40"
                >
                  Previous
                </button>
                <span className="text-xs text-surface-400">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages - 1))}
                  disabled={currentPage >= totalPages - 1}
                  className="rounded-xl border border-surface-700 px-3 py-1.5 text-xs text-slate-300 hover:border-primary-500 disabled:opacity-40"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Right Column (4 / 12) -> Security Events Panel */}
        <div className="lg:col-span-4">
          <SecurityEventsPanel events={securityEvents} isLoading={isSecurityLoading} />
        </div>
      </div>
    </div>
  )
}
