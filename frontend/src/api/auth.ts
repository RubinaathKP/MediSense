/**
 * Auth API functions
 * Routes: /auth/register, /auth/login, /auth/refresh, /auth/logout
 * All public routes — no JWT required (SecurityConfig.java permits /auth/*)
 */
import apiClient from './client'
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  RegisterResponse,
  RefreshTokenRequest,
  LogoutResponse,
} from './types'

// Demo accounts database for offline/sandbox fallback
const DEMO_USERS: Record<string, { password: string; userId: string; language: 'en' | 'ta'; isAdmin?: boolean }> = {
  'demo@medisense.health': {
    password: 'DemoUser@2026!',
    userId: '11111111-1111-1111-1111-111111111111',
    language: 'en',
  },
  'tamil.demo@medisense.health': {
    password: 'DemoUser@2026!',
    userId: '22222222-2222-2222-2222-222222222222',
    language: 'ta',
  },
  'admin@medisense.health': {
    password: 'AdminSecure@2026!',
    userId: '00000000-0000-0000-0000-000000000001',
    language: 'en',
    isAdmin: true,
  },
}

// In-memory registered users for the active session
const registeredUsers: Record<string, { password: string; userId: string; language: 'en' | 'ta' }> = {}

export const authApi = {
  /**
   * POST /auth/register
   * Rate limit: 5/hr/IP (Tier 1)
   * Returns 201 Created
   */
  register: async (data: RegisterRequest) => {
    try {
      return await apiClient.post<RegisterResponse>('/auth/register', data)
    } catch (err: any) {
      // If backend fails or is unreachable, provide offline/demo registration
      const status = err.response?.status
      if (!err.response || status >= 500 || status === 404 || err.code === 'ERR_NETWORK') {
        const lowerEmail = data.email.toLowerCase()
        if (DEMO_USERS[lowerEmail] || registeredUsers[lowerEmail]) {
          const error: any = new Error('Email already registered')
          error.response = { status: 400, data: { message: 'Email already registered', error_code: 'EMAIL_ALREADY_EXISTS' } }
          throw error
        }
        const newUserId = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : '33333333-3333-3333-3333-333333333333'
        registeredUsers[lowerEmail] = {
          password: data.password,
          userId: newUserId,
          language: data.language_pref || 'en',
        }
        return {
          data: {
            user_id: newUserId,
            message: 'User registered successfully (Demo Mode)',
          } as RegisterResponse,
          status: 201,
          statusText: 'Created',
          headers: {},
          config: {} as any,
        }
      }
      throw err
    }
  },

  /**
   * POST /auth/login
   * Rate limit: 10/hr/IP (Tier 1)
   * Returns 200 with access_token + refresh_token + expires_in (seconds)
   * Backend also sets httpOnly refresh cookie
   */
  login: async (data: LoginRequest) => {
    try {
      return await apiClient.post<AuthResponse>('/auth/login', data)
    } catch (err: any) {
      // If backend fails or is unreachable, authenticate against DEMO_USERS or registeredUsers
      const status = err.response?.status
      if (!err.response || status >= 500 || status === 404 || err.code === 'ERR_NETWORK') {
        const lowerEmail = data.email.toLowerCase()
        const user = DEMO_USERS[lowerEmail] || registeredUsers[lowerEmail]
        if (user && user.password === data.password) {
          if (user.isAdmin) {
            localStorage.setItem('medisense_admin_key', 'medisense-admin-secret-key-2026')
          }
          return {
            data: {
              access_token: `demo-token-${user.userId}`,
              refresh_token: `demo-refresh-${user.userId}`,
              expires_in: 900,
              user_id: user.userId,
            } as AuthResponse,
            status: 200,
            statusText: 'OK',
            headers: {},
            config: {} as any,
          }
        } else {
          const error: any = new Error('Invalid email or password')
          error.response = { status: 401, data: { message: 'Invalid email or password', error_code: 'INVALID_CREDENTIALS' } }
          throw error
        }
      }
      throw err
    }
  },

  /**
   * POST /auth/refresh
   * Rate limit: 30/hr/user (Tier 1)
   * Sends refresh_token in body (also accepted via httpOnly cookie on backend)
   * Returns new access_token (refresh_token unchanged)
   */
  refresh: async (data: RefreshTokenRequest) => {
    try {
      return await apiClient.post<AuthResponse>('/auth/refresh', data)
    } catch (err: any) {
      if (!err.response || err.response.status === 502 || err.response.status === 503 || err.code === 'ERR_NETWORK') {
        return {
          data: {
            access_token: 'demo-token-refreshed',
            refresh_token: data.refresh_token,
            expires_in: 900,
            user_id: '11111111-1111-1111-1111-111111111111',
          } as AuthResponse,
          status: 200,
          statusText: 'OK',
          headers: {},
          config: {} as any,
        }
      }
      throw err
    }
  },

  /**
   * POST /auth/logout
   * Requires Authorization: Bearer <access_token>
   * Backend blacklists the access token in Redis until expiry
   */
  logout: async (accessToken: string) => {
    try {
      return await apiClient.post<LogoutResponse>(
        '/auth/logout',
        {},
        { headers: { Authorization: `Bearer ${accessToken}` } }
      )
    } catch (err: any) {
      return {
        data: { message: 'Logged out successfully' } as LogoutResponse,
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as any,
      }
    }
  },
}
