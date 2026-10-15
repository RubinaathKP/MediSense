import { Clock, ShieldCheck, Upload, MessageCircle } from 'lucide-react'
import { useRateLimit } from '@/hooks/useRateLimit'

export default function RateLimitWidget() {
  const queryLimit = useRateLimit('query')
  const uploadLimit = useRateLimit('reports-upload')

  // Maximum hourly quotas per tier (from RateLimitConfig.java)
  const MAX_QUERIES = 30
  const MAX_UPLOADS = 10

  const queriesRemaining = queryLimit.remaining === Infinity ? MAX_QUERIES : Math.max(queryLimit.remaining, 0)
  const uploadsRemaining = uploadLimit.remaining === Infinity ? MAX_UPLOADS : Math.max(uploadLimit.remaining, 0)

  const queryPercent = Math.min((queriesRemaining / MAX_QUERIES) * 100, 100)
  const uploadPercent = Math.min((uploadsRemaining / MAX_UPLOADS) * 100, 100)

  // Countdown timer
  const activeCountdown = queryLimit.countdown || uploadLimit.countdown

  return (
    <div
      data-testid="rate-limit-widget"
      className="rounded-2xl border border-surface-700/60 bg-surface-800/40 p-5 shadow-sm space-y-4"
    >
      <div className="flex items-center justify-between border-b border-surface-700/40 pb-2.5">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-primary-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
            Hourly Usage Quotas
          </h3>
        </div>

        {activeCountdown > 0 && (
          <div className="flex items-center gap-1 text-[11px] font-mono text-amber-400">
            <Clock className="h-3 w-3" />
            <span>Resets in: {activeCountdown}s</span>
          </div>
        )}
      </div>

      <div className="space-y-3">
        {/* Questions Quota */}
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="flex items-center gap-1.5 text-slate-300">
              <MessageCircle className="h-3.5 w-3.5 text-primary-400" />
              <span>Questions remaining this hour:</span>
            </span>
            <span
              data-testid="queries-quota-label"
              className="font-mono font-semibold text-primary-300"
            >
              {queriesRemaining}/{MAX_QUERIES}
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-700">
            <div
              className="h-full bg-gradient-to-r from-primary-500 to-teal-400 transition-all duration-500"
              style={{ width: `${queryPercent}%` }}
            />
          </div>
        </div>

        {/* Uploads Quota */}
        <div className="space-y-1.5">
          <div className="flex justify-between text-xs">
            <span className="flex items-center gap-1.5 text-slate-300">
              <Upload className="h-3.5 w-3.5 text-emerald-400" />
              <span>Uploads remaining this hour:</span>
            </span>
            <span
              data-testid="uploads-quota-label"
              className="font-mono font-semibold text-emerald-300"
            >
              {uploadsRemaining}/{MAX_UPLOADS}
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-700">
            <div
              className="h-full bg-gradient-to-r from-emerald-500 to-green-400 transition-all duration-500"
              style={{ width: `${uploadPercent}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
