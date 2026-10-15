/**
 * RateLimitBanner — shows a live countdown when a rate limit is active.
 * Auto-dismisses when countdown reaches 0 and re-enables the parent form.
 *
 * Supports both full banner (page-level) and inline (input-level) variants.
 * Both variants read from rateLimitStore or fallback retryAfterSeconds prop.
 */
import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Clock, AlertTriangle, AlertCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useRateLimit } from '@/hooks/useRateLimit'
import { cn } from '@/utils/cn'

export interface RateLimitBannerProps {
  endpoint?: string
  retryAfterSeconds?: number
  onExpired?: () => void
  message?: string
  variant?: 'banner' | 'inline'
  className?: string
}

export default function RateLimitBanner({
  endpoint,
  retryAfterSeconds,
  onExpired,
  message,
  variant = 'banner',
  className,
}: RateLimitBannerProps) {
  const { t } = useTranslation()
  const hookLimit = useRateLimit(endpoint || '')

  // If endpoint is supplied, use hook values; otherwise fallback to local countdown with retryAfterSeconds
  const [localCountdown, setLocalCountdown] = useState<number>(() => retryAfterSeconds ?? 0)

  useEffect(() => {
    if (endpoint) return

    if (retryAfterSeconds === undefined) return

    setLocalCountdown(retryAfterSeconds)

    if (retryAfterSeconds <= 0) {
      onExpired?.()
      return
    }

    const interval = setInterval(() => {
      setLocalCountdown(prev => {
        const next = prev - 1
        if (next <= 0) {
          clearInterval(interval)
          onExpired?.()
          return 0
        }
        return next
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [endpoint, retryAfterSeconds, onExpired])

  // Also if hook countdown reaches 0 when previously limited, call onExpired
  useEffect(() => {
    if (endpoint && !hookLimit.is_limited && hookLimit.countdown === 0) {
      onExpired?.()
    }
  }, [endpoint, hookLimit.is_limited, hookLimit.countdown, onExpired])

  const countdown = endpoint ? hookLimit.countdown : localCountdown
  const isLimited = endpoint ? hookLimit.is_limited : countdown > 0

  if (!isLimited && countdown <= 0) {
    return null
  }

  if (variant === 'inline') {
    return (
      <div className={cn('flex items-center gap-1.5 text-xs text-amber-400 font-mono mt-1', className)}>
        <Clock className="h-3 w-3 animate-pulse shrink-0" />
        <span>
          {message || t('rate_limit.inline', {
            defaultValue: `Available again in ${countdown}s`,
            countdown,
          })}
        </span>
      </div>
    )
  }

  const endpointLabel = endpoint || 'request'
  const defaultMessage = message || t('rate_limit.banner', {
    defaultValue: `You've reached your ${endpointLabel} limit. Please wait ${countdown}s before trying again.`,
    endpoint: endpointLabel,
    countdown: `${countdown}s`,
  })

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -8 }}
        transition={{ duration: 0.2 }}
        className={cn(
          'flex items-center gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-200 shadow-sm',
          className
        )}
        role="alert"
        aria-live="polite"
        id="rate-limit-banner"
      >
        <AlertTriangle className="h-4 w-4 shrink-0 text-amber-400 animate-pulse" />

        <div className="flex-1">
          <span className="font-medium">{defaultMessage}</span>
        </div>

        <div className="flex items-center gap-1.5 font-mono text-xs font-semibold text-amber-300 bg-amber-500/20 px-2 py-0.5 rounded-md border border-amber-500/30">
          <AlertCircle className="h-3 w-3" />
          <span className="tabular-nums">{countdown}s</span>
        </div>
      </motion.div>
    </AnimatePresence>
  )
}
