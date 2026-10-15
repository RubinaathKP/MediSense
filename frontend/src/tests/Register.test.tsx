import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Register from '@/pages/auth/Register'
import { authApi } from '@/api/auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    register: vi.fn(),
    login: vi.fn(),
  },
}))

describe('Register Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all register form fields', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByLabelText(/auth\.register\.email_label/i)).toBeDefined()
    expect(screen.getByLabelText(/^auth\.register\.password_label/i)).toBeDefined()
    expect(screen.getByLabelText(/auth\.register\.confirm_password_label/i)).toBeDefined()
    expect(screen.getByRole('button', { name: /auth\.register\.submit/i })).toBeDefined()
  })

  it('validates password mismatch', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.register\.email_label/i)
    const passwordInput = screen.getByLabelText(/^auth\.register\.password_label/i)
    const confirmInput = screen.getByLabelText(/auth\.register\.confirm_password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.register\.submit/i })

    await user.type(emailInput, 'user@example.com')
    await user.type(passwordInput, 'Password123!')
    await user.type(confirmInput, 'DifferentPassword123!')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText(/Passwords do not match/i)).toBeDefined()
    })
  })

  it('shows success message on successful registration', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockResolvedValueOnce({
      data: { user_id: 'new-user-1', message: 'User registered successfully' },
    } as any)
    vi.mocked(authApi.login).mockResolvedValueOnce({
      data: {
        access_token: 'tok-1',
        refresh_token: 'ref-1',
        expires_in: 900,
        user_id: 'new-user-1',
      },
    } as any)

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.register\.email_label/i)
    const passwordInput = screen.getByLabelText(/^auth\.register\.password_label/i)
    const confirmInput = screen.getByLabelText(/auth\.register\.confirm_password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.register\.submit/i })

    await user.type(emailInput, 'newuser@example.com')
    await user.type(passwordInput, 'ValidPassword123!')
    await user.type(confirmInput, 'ValidPassword123!')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        email: 'newuser@example.com',
        password: 'ValidPassword123!',
        language_pref: 'en',
      })
      expect(screen.getByText(/auth\.register\.success/i)).toBeDefined()
    })
  })

  it('displays error when email is already registered', async () => {
    const user = userEvent.setup()
    const errorResponse = {
      response: {
        status: 400,
        data: {
          error_code: 'EMAIL_ALREADY_EXISTS',
          message: 'Email already registered. Please login or use a different email.',
        },
      },
    }
    vi.mocked(authApi.register).mockRejectedValueOnce(errorResponse)

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText(/auth\.register\.email_label/i)
    const passwordInput = screen.getByLabelText(/^auth\.register\.password_label/i)
    const confirmInput = screen.getByLabelText(/auth\.register\.confirm_password_label/i)
    const submitBtn = screen.getByRole('button', { name: /auth\.register\.submit/i })

    await user.type(emailInput, 'existing@example.com')
    await user.type(passwordInput, 'ValidPassword123!')
    await user.type(confirmInput, 'ValidPassword123!')
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText(/auth\.register\.error_email_exists/i)).toBeDefined()
    })
  })
})
