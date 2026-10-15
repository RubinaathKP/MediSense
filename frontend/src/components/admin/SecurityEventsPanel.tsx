import { ShieldAlert, AlertTriangle, Clock } from 'lucide-react'
import { format } from 'date-fns'
import type { AuditLogEntry } from '@/api/types'

interface SecurityEventsPanelProps {
  events: AuditLogEntry[]
  isLoading?: boolean
}

export default function SecurityEventsPanel({ events, isLoading = false }: SecurityEventsPanelProps) {
  // Check if any abuse flag / lockout event is active
  const abuseEvents = events.filter(
    (e) =>
      e.eventType?.includes('LOCKOUT') ||
      e.eventType?.includes('ABUSE') ||
      e.severity === 'CRITICAL'
  )

  return (
    <div
      data-testid="security-events-panel"
      className="space-y-4 rounded-3xl border border-surface-700/60 bg-surface-800/50 p-5 shadow-glass backdrop-blur-md"
    >
      <div className="flex items-center justify-between border-b border-surface-700/40 pb-3">
        <div className="flex items-center gap-2">
          <ShieldAlert className="h-4 w-4 text-rose-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
            Security Events (Last 24h)
          </h3>
        </div>
        <span className="flex items-center gap-1 font-mono text-[10px] text-surface-400">
          <Clock className="h-3 w-3" />
          <span>Auto-refreshed (5m)</span>
        </span>
      </div>

      {/* Abuse Flag Alert Card if active */}
      {abuseEvents.length > 0 && (
        <div
          data-testid="abuse-alert-card"
          className="rounded-2xl border border-rose-500/40 bg-rose-950/40 p-4 space-y-2 shadow-sm animate-pulse-slow"
        >
          <div className="flex items-center gap-2 text-rose-400 text-xs font-bold uppercase tracking-wider">
            <AlertTriangle className="h-4 w-4" />
            <span>Abuse Flag Active</span>
          </div>

          <div className="text-xs leading-relaxed text-rose-200 font-mono space-y-1">
            <p>
              Target:{' '}
              <strong className="text-white">
                {abuseEvents[0].userId || abuseEvents[0].ipAddress}
              </strong>
            </p>
            <p className="text-[11px] text-rose-300/80">
              Applied since:{' '}
              {format(new Date(abuseEvents[0].createdAt || Date.now()), 'MMM d, HH:mm:ss')}
            </p>
            <p className="text-[11px] text-rose-300/80">
              Expires: 15 minutes post-strike
            </p>
          </div>
        </div>
      )}

      {/* List of Recent Security Events */}
      <div className="space-y-2 max-h-[380px] overflow-y-auto pr-1">
        {isLoading ? (
          <p className="text-center py-6 text-xs text-surface-400">Loading security logs...</p>
        ) : events.length === 0 ? (
          <div className="rounded-xl border border-surface-700/30 bg-surface-900/30 p-6 text-center text-xs text-surface-400">
            No security violations recorded in the last 24 hours.
          </div>
        ) : (
          events.map((e, idx) => (
            <div
              key={e.logId || idx}
              className={`rounded-xl border p-3 text-xs space-y-1 transition ${
                e.severity === 'CRITICAL'
                  ? 'border-rose-500/40 bg-rose-950/20'
                  : 'border-amber-500/30 bg-amber-950/15'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="font-mono text-[10px] font-bold text-slate-300">
                  {e.eventType}
                </span>
                <span
                  className={`rounded px-1.5 py-0.5 text-[9px] font-mono font-bold ${
                    e.severity === 'CRITICAL'
                      ? 'bg-rose-500/20 text-rose-300'
                      : 'bg-amber-500/20 text-amber-300'
                  }`}
                >
                  {e.severity}
                </span>
              </div>
              <p className="text-[11px] text-surface-400 font-mono">
                IP: {e.ipAddress} {e.userId ? `· User: ${e.userId.slice(0, 8)}` : ''}
              </p>
              <p className="text-[10px] text-surface-500">
                {format(new Date(e.createdAt || Date.now()), 'HH:mm:ss')}
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
