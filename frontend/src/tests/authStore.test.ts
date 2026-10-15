import { describe, it, expect, beforeEach } from 'vitest'
import { act } from '@testing-library/react'
import { useAuthStore } from '@/store/authStore'
import type { AuthResponse } from '@/api/types'

const mockAuthResponse: AuthResponse = {
  access_token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test',
  refresh_token: 'refresh-token-test',
  expires_in: 900,
  user_id: 'user-123',
}

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      access_token: null,
      refresh_token: null,
      is_authenticated: false,
      failed_login_attempts: 0,
      is_loading: false,
    })
  })

  it('starts with no authentication state', () => {
    const state = useAuthStore.getState()
    expect(state.is_authenticated).toBe(false)
    expect(state.access_token).toBeNull()
    expect(state.user).toBeNull()
  })

  it('loginSuccess stores tokens in memory', () => {
    act(() => {
      useAuthStore.getState().loginSuccess(mockAuthResponse, 'test@example.com', 'en')
    })
    const state = useAuthStore.getState()
    expect(state.is_authenticated).toBe(true)
    expect(state.access_token).toBe(mockAuthResponse.access_token)
    expect(state.refresh_token).toBe(mockAuthResponse.refresh_token)
    expect(state.user?.email).toBe('test@example.com')
    expect(state.user?.language_pref).toBe('en')
    expect(state.failed_login_attempts).toBe(0)
  })

  it('loginSuccess sets Tamil language preference', () => {
    act(() => {
      useAuthStore.getState().loginSuccess(mockAuthResponse, 'user@example.com', 'ta')
    })
    expect(useAuthStore.getState().user?.language_pref).toBe('ta')
  })

  it('incrementFailedAttempts tracks failures', () => {
    act(() => { useAuthStore.getState().incrementFailedAttempts() })
    act(() => { useAuthStore.getState().incrementFailedAttempts() })
    expect(useAuthStore.getState().failed_login_attempts).toBe(2)
  })

  it('resetFailedAttempts clears counter', () => {
    act(() => { useAuthStore.getState().incrementFailedAttempts() })
    act(() => { useAuthStore.getState().resetFailedAttempts() })
    expect(useAuthStore.getState().failed_login_attempts).toBe(0)
  })

  it('setLanguagePref updates user language', () => {
    act(() => {
      useAuthStore.getState().loginSuccess(mockAuthResponse, 'test@example.com', 'en')
    })
    act(() => { useAuthStore.getState().setLanguagePref('ta') })
    expect(useAuthStore.getState().user?.language_pref).toBe('ta')
  })

  it('setLanguagePref is no-op when no user', () => {
    act(() => { useAuthStore.getState().setLanguagePref('ta') })
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('stores no tokens in localStorage (XSS protection)', () => {
    act(() => {
      useAuthStore.getState().loginSuccess(mockAuthResponse, 'test@example.com', 'en')
    })
    expect(localStorage.getItem('access_token')).toBeNull()
    expect(localStorage.getItem('refresh_token')).toBeNull()
    expect(sessionStorage.getItem('access_token')).toBeNull()
  })
})
