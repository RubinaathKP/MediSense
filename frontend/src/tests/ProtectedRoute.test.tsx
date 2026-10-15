import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from '@/components/common/ProtectedRoute'
import { useAuthStore } from '@/store/authStore'

function LoginPage() { return <div>Login Page</div> }
function SecurePage() { return <div>Secure Content</div> }

function TestApp({ initialPath = '/secure' }: { initialPath?: string }) {
  return (
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/secure"
          element={
            <ProtectedRoute>
              <SecurePage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  )
}

describe('ProtectedRoute', () => {
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

  it('redirects to /login when not authenticated', () => {
    render(<TestApp />)
    expect(screen.getByText('Login Page')).toBeDefined()
    expect(screen.queryByText('Secure Content')).toBeNull()
  })

  it('renders children when authenticated', () => {
    useAuthStore.setState({
      is_authenticated: true,
      access_token: 'token-123',
      refresh_token: 'refresh-123',
      user: { user_id: 'user-1', email: 'a@b.com', language_pref: 'en' },
      failed_login_attempts: 0,
      is_loading: false,
    })
    render(<TestApp />)
    expect(screen.getByText('Secure Content')).toBeDefined()
  })
})
