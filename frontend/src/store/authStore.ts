/**
 * authStore — Zustand store for authentication state
 *
 * Token strategy (from JwtTokenProvider.java):
 *  - access_token: expires_in=900s (15 min) — stored IN MEMORY ONLY (XSS protection)
 *  - refresh_token: 7-day expiry — also in memory; backend sets httpOnly cookie
 *    so /auth/refresh works on page reload even if in-memory state is cleared
 *
 * On page refresh:
 *  - access_token is gone from memory
 *  - useTokenRefresh hook calls refreshToken() on mount
 *  - If refresh fails (cookie expired or not present) → redirect to /login
 */
import { create } from 'zustand'
import { authApi } from '@/api/auth'
import type { AuthResponse } from '@/api/types'

export interface AuthUser {
  user_id: string
  email: string
  language_pref: 'en' | 'ta'
  is_admin?: boolean
}

interface AuthState {
  user: AuthUser | null
  access_token: string | null
  refresh_token: string | null
  is_authenticated: boolean
  failed_login_attempts: number
  is_loading: boolean

  // Actions
  loginSuccess: (response: AuthResponse, email: string, langPref: 'en' | 'ta') => void
  logout: () => Promise<void>
  refreshToken: () => Promise<void>
  setLanguagePref: (lang: 'en' | 'ta') => void
  updateLanguage: (lang: 'en' | 'ta') => void
  incrementFailedAttempts: () => void
  resetFailedAttempts: () => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  access_token: null,
  refresh_token: null,
  is_authenticated: false,
  failed_login_attempts: 0,
  is_loading: false,

  /**
   * Called after successful /auth/login or /auth/refresh
   * Stores tokens IN MEMORY ONLY — never localStorage/sessionStorage
   */
  loginSuccess: (response, email, langPref) => {
    set({
      access_token: response.access_token,
      refresh_token: response.refresh_token,
      is_authenticated: true,
      failed_login_attempts: 0,
      user: {
        user_id: response.user_id,
        email,
        language_pref: langPref,
      },
    })
  },

  /**
   * Logout: blacklists access token via POST /auth/logout, then clears all state.
   * Redirect handled by the caller (ProtectedRoute / Axios interceptor).
   */
  logout: async () => {
    const { access_token } = get()
    try {
      if (access_token) {
        await authApi.logout(access_token)
      }
    } catch {
      // Ignore logout errors — clear state regardless
    } finally {
      set({
        user: null,
        access_token: null,
        refresh_token: null,
        is_authenticated: false,
        failed_login_attempts: 0,
      })
      // Navigate to /login — imported lazily to avoid circular dep
      window.location.href = '/login'
    }
  },

  /**
   * Refresh access token using stored refresh_token.
   * Called by:
   *  1. Axios 401 interceptor (silent retry)
   *  2. useTokenRefresh hook (proactive, 2 min before expiry)
   *  3. App.tsx mount (page refresh scenario)
   */
  refreshToken: async () => {
    const { refresh_token } = get()
    if (!refresh_token) throw new Error('No refresh token available')

    const response = await authApi.refresh({ refresh_token })
    const data: AuthResponse = response.data

    set(state => ({
      access_token: data.access_token,
      refresh_token: data.refresh_token, // backend returns same refresh token
      user: state.user
        ? { ...state.user }
        : null,
      is_authenticated: true,
    }))
  },

  setLanguagePref: (lang) =>
    set(state => ({
      user: state.user ? { ...state.user, language_pref: lang } : null,
    })),

  updateLanguage: (lang) =>
    set(state => ({
      user: state.user ? { ...state.user, language_pref: lang } : null,
    })),

  incrementFailedAttempts: () =>
    set(state => ({ failed_login_attempts: state.failed_login_attempts + 1 })),

  resetFailedAttempts: () =>
    set({ failed_login_attempts: 0 }),
}))
