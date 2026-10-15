/**
 * MediSense Axios API Client
 *
 * Integration contract verified against:
 *  - SecurityConfig.java  — CORS allowed origins: localhost:5173 / localhost:3000
 *  - JwtAuthenticationFilter.java — expects 'Authorization: Bearer <token>'
 *  - RateLimitFilter.java — emits X-RateLimit-* + Retry-After headers on every request
 *  - GlobalExceptionHandler.java — all error shapes
 *  - JwtTokenProvider.java — access token expires in 900s (15 min), refresh 7 days
 */
import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from './types'
import { useRateLimitStore } from '@/store/rateLimitStore'

// ─── Lazy store imports to avoid circular deps ───────────────────────────────
let getAuthStore: () => { access_token: string | null; logout: () => void; refreshToken: () => Promise<void> }
let getRateLimitStore: () => {
  limits: Map<string, any>
  updateFromHeaders: (endpoint: string, headers: any) => void
  getRateLimit: (slug: string) => any
}

export function injectStores(
  authStoreFn: typeof getAuthStore,
  rateLimitStoreFn: typeof getRateLimitStore
) {
  getAuthStore = authStoreFn
  getRateLimitStore = rateLimitStoreFn
}

// ─── Endpoint → slug mapping ─────────────────────────────────────────────────
export function urlToSlug(url: string, method: string = 'GET'): string {
  const m = method.toUpperCase()
  if (m === 'POST' && url.includes('/auth/register')) return 'auth-register'
  if (m === 'POST' && url.includes('/auth/login'))    return 'auth-login'
  if (m === 'POST' && url.includes('/auth/refresh'))  return 'auth-refresh'
  if (m === 'POST' && url.includes('/reports/upload')) return 'reports-upload'
  if (m === 'POST' && url.includes('/reports/abdm-pull')) return 'reports-abdm-pull'
  if (m === 'POST' && url.includes('/reports/confirm-value')) return 'reports-confirm-value'
  if (m === 'POST' && url.includes('/query'))         return 'query'
  if (m === 'POST' && url.includes('/claims/verify')) return 'claims-verify'
  if (m === 'GET'  && url.includes('/query/'))        return 'query-get'
  if (m === 'GET'  && url.includes('/session/'))      return 'session-history'
  if (m === 'GET'  && url.includes('/reports/'))      return 'reports-get'
  if (m === 'POST' && url.includes('/internal/eval/run')) return 'internal-eval-run'
  if (url.includes('/internal/eval/results'))         return 'internal-eval-results'
  if (url.includes('/internal/audit'))                return 'internal-audit'
  return 'unknown'
}

// ─── Singleton Axios instance ─────────────────────────────────────────────────
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  withCredentials: true,
})

export class RateLimitError extends Error {
  public endpoint: string
  public retry_after: number
  public isRateLimitError = true

  constructor(endpoint: string, retry_after: number, message?: string) {
    super(message || `Rate limit exceeded for ${endpoint}. Retry after ${retry_after}s.`)
    this.name = 'RateLimitError'
    this.endpoint = endpoint
    this.retry_after = retry_after
  }
}

// ─── Request interceptor: attach JWT and enforce client-side rate limit gate ──
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const slug = urlToSlug(config.url ?? '', config.method ?? 'GET')
    
    // Client-side rate limit gate: check rateLimitStore for endpoint
    const store = getRateLimitStore ? getRateLimitStore() : useRateLimitStore.getState()
    const info = store.getRateLimit ? store.getRateLimit(slug) : store.limits.get(slug)
    if (info && info.is_limited) {
      const nowSeconds = Math.floor(Date.now() / 1000)
      const secondsLeft = info.reset_at > 0 ? info.reset_at - nowSeconds : info.retry_after
      if (secondsLeft > 0) {
        return Promise.reject(new RateLimitError(slug, secondsLeft))
      }
    }

    if (getAuthStore) {
      const { access_token } = getAuthStore()
      if (access_token) {
        config.headers.Authorization = `Bearer ${access_token}`
      }
    }
    const adminKey = localStorage.getItem('medisense_admin_key')
    if (adminKey) {
      config.headers['X-Admin-Key'] = adminKey
    }
    return config
  },
  error => Promise.reject(error)
)

// ─── Token refresh flag — prevent concurrent refresh attempts ─────────────────
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function subscribeRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function onRefreshDone(token: string) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

// ─── Response interceptor: handle 401 / 429 / 503 ───────────────────────────
apiClient.interceptors.response.use(
  response => {
    // On every successful response, capture remaining rate-limit headers
    const slug = urlToSlug(response.config?.url ?? '', response.config?.method ?? 'GET')
    const store = getRateLimitStore ? getRateLimitStore() : useRateLimitStore.getState()
    store.updateFromHeaders(slug, response.headers)
    return response
  },

  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retried?: boolean }
    const status = error.response?.status
    const slug = urlToSlug(originalRequest?.url ?? '', originalRequest?.method ?? 'GET')

    // ── 401: attempt silent token refresh once ────────────────────────────────
    if (status === 401 && !originalRequest._retried) {
      // Token blacklisted — logout immediately, no refresh attempt
      const errorCode = error.response?.data?.error_code
      if (errorCode === 'TOKEN_BLACKLISTED') {
        getAuthStore?.().logout()
        return Promise.reject(error)
      }

      if (!isRefreshing) {
        isRefreshing = true
        originalRequest._retried = true
        try {
          await getAuthStore?.().refreshToken()
          const { access_token } = getAuthStore()
          onRefreshDone(access_token ?? '')
          isRefreshing = false
          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${access_token}`
          return apiClient(originalRequest)
        } catch {
          // Refresh failed — clear auth and redirect
          isRefreshing = false
          refreshSubscribers = []
          getAuthStore?.().logout()
          return Promise.reject(error)
        }
      }

      // Queue request to retry after ongoing refresh completes
      return new Promise(resolve => {
        subscribeRefresh((token: string) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          resolve(apiClient(originalRequest))
        })
      })
    }

    // ── 429: extract rate-limit info, store, throw RateLimitError ─────────────
    if (status === 429) {
      const store = getRateLimitStore ? getRateLimitStore() : useRateLimitStore.getState()
      store.updateFromHeaders(slug, error.response?.headers ?? {})

      const retryAfter = parseInt(
        (error.response?.headers?.['retry-after'] || error.response?.data?.retry_after_seconds || '60') as string,
        10
      )
      return Promise.reject(new RateLimitError(slug, retryAfter, error.response?.data?.message))
    }

    // ── 503 / others ─────────────────────────────────────────────────────────
    return Promise.reject(error)
  }
)

export default apiClient
