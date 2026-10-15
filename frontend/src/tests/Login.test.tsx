import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Login from '@/pages/auth/Login'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}))

describe('Login Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({
      user: null,
      access_token: null,
      refresh_token: null,
      is_authenticated: false,
      failed_login_attempts: 0,
      is_loading: false,
    })
  })

  it('renders login form inputs and submit button', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    expect(screen.getByLabelText(/auth\.login\.email_label/i)).toBeDefined()
    expect(screen.getByLabelText(/auth\.login\.password_label/i)).toBeDefined()
    expect(screen.getByRole('button', { name: /auth\.login\.submit/i })).toBeDefined()
  })

  it('validates required email and password fields on submit', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const submitBtn = screen.getByRole('button', { name: /auth\.login\.submit/i })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText(/Please enter a valid email address/i)).toBeDefined()
    })
  })

  it('handles successful login and updates authStore', async () => {
    const user = userEvent.setup()
    const mockResponse = {
      data: {
        access_token: 'fake-access-token',
        refresh_token: 'fake-refresh-token',
        expires_in: 900,
        user_id: 'user-uuid-1',
      },
    }
    vi.mocked(authApi.login).mockResolvedValueOnce(mockResponse as any)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.login\.email_label/i)
    const passwordInput = screen.getByLabelText(/auth\.login\.password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.login\.submit/i })

    await user.type(emailInput, 'patient@medisense.org')
    await user.type(passwordInput, 'SecurePassword123!')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'patient@medisense.org',
        password: 'SecurePassword123!',
      })
      const authState = useAuthStore.getState()
      expect(authState.is_authenticated).toBe(true)
      expect(authState.access_token).toBe('fake-access-token')
    })
  })

  it('displays account locked state on 401 account locked response', async () => {
    const user = userEvent.setup()
    const errorResponse = {
      response: {
        status: 401,
        data: {
          error_code: 'ACCOUNT_LOCKED',
          message: 'Account locked due to 5 consecutive failed attempts. Try again in 15 minutes.',
        },
      },
    }
    vi.mocked(authApi.login).mockRejectedValueOnce(errorResponse)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.login\.email_label/i)
    const passwordInput = screen.getByLabelText(/auth\.login\.password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.login\.submit/i })

    await user.type(emailInput, 'locked@medisense.org')
    await user.type(passwordInput, 'WrongPassword123')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText(/auth\.login\.error_account_locked/i)).toBeDefined()
    })
  })

  it('displays rate limit countdown banner on 429 response', async () => {
    const user = userEvent.setup()
    const errorResponse = {
      response: {
        status: 429,
        data: {
          error_code: 'RATE_LIMIT_EXCEEDED',
          retry_after_seconds: 300,
        },
      },
    }
    vi.mocked(authApi.login).mockRejectedValueOnce(errorResponse)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.login\.email_label/i)
    const passwordInput = screen.getByLabelText(/auth\.login\.password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.login\.submit/i })

    await user.type(emailInput, 'ratelimited@medisense.org')
    await user.type(passwordInput, 'Password123!')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeDefined()
    })
  })
})
