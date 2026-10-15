/**
 * useRateLimit — reads rate-limit state for a given endpoint slug
 * and provides a live countdown.
 *
 * Input: endpoint slug (e.g. "query", "upload", "auth-login")
 * Returns:
 *   remaining: number
 *   limit: number
 *   is_limited: boolean
 *   countdown: number (live countdown in seconds)
 *   reset_at: Date
 */
import { useEffect, useState } from 'react'
import { useRateLimitStore } from '@/store/rateLimitStore'

export interface RateLimitStatus {
  remaining: number
  limit: number
  is_limited: boolean
  countdown: number // seconds until reset (live, decrements every second)
  reset_at: Date
}

export function useRateLimit(endpointSlug: string): RateLimitStatus {
  const limits = useRateLimitStore(s => s.limits)
  const clearLimit = useRateLimitStore(s => s.clearLimit)
  const info = limits.get(endpointSlug)

  const computeRemaining = () => {
    if (!info) return 0
    const nowSeconds = Math.floor(Date.now() / 1000)
    let secondsLeft = 0
    if (info.reset_at > 0) {
      secondsLeft = Math.max(0, info.reset_at - nowSeconds)
    } else if (info.retry_after > 0) {
      secondsLeft = info.retry_after
    }
    return secondsLeft
  }

  const [countdown, setCountdown] = useState<number>(() => computeRemaining())

  useEffect(() => {
    if (!info) {
      setCountdown(0)
      return
    }

    const initial = computeRemaining()
    setCountdown(initial)

    if (initial <= 0) {
      if (info.is_limited) {
        clearLimit(endpointSlug)
      }
      return
    }

    const interval = setInterval(() => {
      const remaining = computeRemaining()
      setCountdown(remaining)
      if (remaining <= 0) {
        clearInterval(interval)
        clearLimit(endpointSlug)
      }
    }, 1000)

    return () => clearInterval(interval)
  }, [info?.reset_at, info?.retry_after, info?.is_limited, endpointSlug, clearLimit])

  if (!info) {
    return {
      remaining: Infinity,
      limit: Infinity,
      is_limited: false,
      countdown: 0,
      reset_at: new Date(0),
    }
  }

  const isLimited = (info.is_limited || info.remaining <= 0) && countdown > 0

  return {
    remaining: info.remaining,
    limit: info.limit,
    is_limited: isLimited,
    countdown,
    reset_at: new Date(info.reset_at * 1000),
  }
}

export default useRateLimit
