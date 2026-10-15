/**
 * useTokenRefresh — proactively refreshes access token before it expires.
 *
 * Based on JwtTokenProvider.java:
 *   - access_token expires in 900 seconds (15 min) by default
 *   - We schedule a refresh 2 minutes (120s) before expiry
 *
 * Also handles page-refresh scenario: on mount, if no access_token in memory
 * but refresh_token is available (or httpOnly cookie set by backend), try refresh.
 */
import { useEffect, useRef } from 'react'
import { useAuthStore } from '@/store/authStore'
import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  exp: number   // Unix epoch seconds
  sub: string   // user_id
  email?: string
  type?: 'access' | 'refresh'
}

const REFRESH_BUFFER_MS = 2 * 60 * 1000  // 2 minutes before expiry

export function useTokenRefresh() {
  const { access_token, refresh_token, is_authenticated, refreshToken, logout } = useAuthStore()
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Helper: decode access token and return ms until refresh should fire
  function getMsUntilRefresh(token: string): number | null {
    try {
      const decoded = jwtDecode<JwtPayload>(token)
      const expiresAtMs = decoded.exp * 1000
      const refreshAtMs = expiresAtMs - REFRESH_BUFFER_MS
      const nowMs = Date.now()
      return Math.max(refreshAtMs - nowMs, 0)
    } catch {
      return null
    }
  }

  // Schedule proactive refresh
  function scheduleRefresh(token: string) {
    if (timerRef.current) clearTimeout(timerRef.current)

    const msUntilRefresh = getMsUntilRefresh(token)
    if (msUntilRefresh === null) return

    timerRef.current = setTimeout(async () => {
      try {
        await refreshToken()
        // After refresh, re-read the new token and reschedule
        const newToken = useAuthStore.getState().access_token
        if (newToken) scheduleRefresh(newToken)
      } catch {
        // Refresh failed — trigger logout
        logout()
      }
    }, msUntilRefresh)
  }

  // On mount: if no access_token but authenticated (page refresh scenario), try refresh
  useEffect(() => {
    if (!is_authenticated && refresh_token) {
      refreshToken().catch(() => logout())
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Whenever access_token changes, schedule next refresh
  useEffect(() => {
    if (access_token) {
      scheduleRefresh(access_token)
    }
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [access_token]) // eslint-disable-line react-hooks/exhaustive-deps
}
