/**
 * rateLimitStore — Zustand store for rate-limit state
 *
 * State is populated by the Axios response interceptor in client.ts
 * whenever a 429 is received or X-RateLimit-* headers arrive.
 * Keyed by endpoint_slug (matches urlToSlug() in client.ts).
 */
import { create } from 'zustand'

export interface RateLimitInfo {
  endpoint: string
  endpoint_slug?: string // Backward compatibility alias
  limit: number
  remaining: number
  reset_at: number // epoch seconds
  retry_after: number // seconds, 0 if not limited
  is_limited: boolean
  captured_at?: number
}

export interface RateLimitState {
  limits: Map<string, RateLimitInfo>
  updateFromHeaders: (endpoint: string, headers: Record<string, any> | Headers | any) => void
  clearLimit: (endpoint: string) => void
  setRateLimit: (endpoint: string, info: Partial<RateLimitInfo> & { limit?: number; remaining?: number; reset_at?: number; retry_after?: number }) => void
  clearRateLimit: (endpoint: string) => void
  getRateLimit: (endpoint: string) => RateLimitInfo | undefined
}

export const useRateLimitStore = create<RateLimitState>((set, get) => ({
  limits: new Map<string, RateLimitInfo>(),

  updateFromHeaders: (endpoint, headers) => {
    if (!headers) return

    const getHeader = (key: string): string | undefined => {
      if (typeof headers.get === 'function') {
        return headers.get(key) ?? headers.get(key.toLowerCase()) ?? headers.get(key.toUpperCase())
      }
      return headers[key] ?? headers[key.toLowerCase()] ?? headers[key.toUpperCase()]
    }

    const limitStr = getHeader('x-ratelimit-limit')
    const remainingStr = getHeader('x-ratelimit-remaining')
    const resetStr = getHeader('x-ratelimit-reset')
    const afterStr = getHeader('retry-after')

    const current = get().limits.get(endpoint)
    const limit = limitStr !== undefined ? parseInt(limitStr, 10) : (current?.limit ?? 0)
    const remaining = remainingStr !== undefined ? parseInt(remainingStr, 10) : (current?.remaining ?? 0)
    
    // Parse reset_at: If resetStr is in relative seconds (e.g. 60) vs epoch timestamp
    let resetAt = resetStr !== undefined ? parseInt(resetStr, 10) : (current?.reset_at ?? 0)
    if (resetAt > 0 && resetAt < 1000000000) {
      // Relative seconds converted to epoch timestamp
      resetAt = Math.floor(Date.now() / 1000) + resetAt
    } else if (resetAt === 0 && afterStr !== undefined) {
      resetAt = Math.floor(Date.now() / 1000) + parseInt(afterStr, 10)
    }

    const retryAfter = afterStr !== undefined ? parseInt(afterStr, 10) : (remaining === 0 ? Math.max(0, resetAt - Math.floor(Date.now() / 1000)) : 0)
    const isLimited = remaining <= 0 || retryAfter > 0

    const info: RateLimitInfo = {
      endpoint,
      endpoint_slug: endpoint,
      limit: isNaN(limit) ? 0 : limit,
      remaining: isNaN(remaining) ? 0 : Math.max(0, remaining),
      reset_at: isNaN(resetAt) ? 0 : resetAt,
      retry_after: isNaN(retryAfter) ? 0 : Math.max(0, retryAfter),
      is_limited: isLimited,
      captured_at: Date.now(),
    }

    set(state => {
      const next = new Map(state.limits)
      next.set(endpoint, info)
      return { limits: next }
    })
  },

  clearLimit: (endpoint) => {
    set(state => {
      const current = state.limits.get(endpoint)
      if (!current) return state

      const next = new Map(state.limits)
      next.set(endpoint, {
        ...current,
        remaining: current.limit > 0 ? current.limit : 10,
        retry_after: 0,
        is_limited: false,
      })
      return { limits: next }
    })
  },

  setRateLimit: (endpoint, partialInfo) => {
    set(state => {
      const current = state.limits.get(endpoint)
      const limit = partialInfo.limit ?? current?.limit ?? 0
      const remaining = partialInfo.remaining ?? current?.remaining ?? 0
      const reset_at = partialInfo.reset_at ?? current?.reset_at ?? 0
      const retry_after = partialInfo.retry_after ?? current?.retry_after ?? (remaining <= 0 ? 60 : 0)
      const is_limited = partialInfo.is_limited ?? (remaining <= 0 || retry_after > 0)

      const next = new Map(state.limits)
      next.set(endpoint, {
        endpoint,
        endpoint_slug: endpoint,
        limit,
        remaining,
        reset_at,
        retry_after,
        is_limited,
        captured_at: Date.now(),
      })
      return { limits: next }
    })
  },

  clearRateLimit: (endpoint) => {
    set(state => {
      const next = new Map(state.limits)
      next.delete(endpoint)
      return { limits: next }
    })
  },

  getRateLimit: (endpoint) => get().limits.get(endpoint),
}))
