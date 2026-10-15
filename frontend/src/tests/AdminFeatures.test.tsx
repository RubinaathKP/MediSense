import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MetricCard from '@/components/admin/MetricCard'
import { AdminRoute } from '@/components/common/AdminRoute'
import SecurityEventsPanel from '@/components/admin/SecurityEventsPanel'
import { useAuthStore } from '@/store/authStore'
import type { AuditLogEntry } from '@/api/types'

describe('Admin Dashboard Test Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    useAuthStore.setState({
      user: null,
      is_authenticated: false,
      access_token: null,
      refresh_token: null,
      failed_login_attempts: 0,
      is_loading: false,
    })
  })

  /* 1. MetricCard Green / Red target rendering */
  describe('MetricCard Component', () => {
    it('renders green text when current value meets or exceeds target', () => {
      render(
        <MetricCard
          name="Citation Accuracy"
          currentValue={96.5}
          targetValue={95.0}
        />
      )

      const valueEl = screen.getByTestId('metric-current-value')
      expect(valueEl).toHaveTextContent('96.5')
      expect(valueEl.className).toContain('text-emerald-400')
    })

    it('renders red text when current value is below target threshold', () => {
      render(
        <MetricCard
          name="Citation Accuracy"
          currentValue={89.0}
          targetValue={95.0}
        />
      )

      const valueEl = screen.getByTestId('metric-current-value')
      expect(valueEl).toHaveTextContent('89.0')
      expect(valueEl.className).toContain('text-rose-500')
    })

    it('handles upper-bound metrics correctly (e.g. Hallucination Rate <= 2%)', () => {
      // Below 2% -> Good (Green)
      const { rerender } = render(
        <MetricCard
          name="Hallucination Rate"
          currentValue={1.2}
          targetValue={2.0}
          isUpperBound={true}
        />
      )
      expect(screen.getByTestId('metric-current-value').className).toContain('text-emerald-400')

      // Above 2% -> Bad (Red)
      rerender(
        <MetricCard
          name="Hallucination Rate"
          currentValue={3.5}
          targetValue={2.0}
          isUpperBound={true}
        />
      )
      expect(screen.getByTestId('metric-current-value').className).toContain('text-rose-500')
    })
  })

  /* 2. AdminRoute redirect for non-admin */
  describe('AdminRoute Component', () => {
    function ProtectedAdminView() {
      return <div>Admin Portal Content</div>
    }
    function DashboardFallback() {
      return <div>User Dashboard Fallback</div>
    }

    it('redirects to /dashboard when user is not admin or missing admin key', () => {
      // Authenticated normal user, no admin key
      useAuthStore.setState({
        user: { user_id: 'u-1', email: 'user@medisense.org', language_pref: 'en', is_admin: false },
        is_authenticated: true,
      })

      render(
        <MemoryRouter initialEntries={['/admin/eval']}>
          <Routes>
            <Route path="/dashboard" element={<DashboardFallback />} />
            <Route
              path="/admin/eval"
              element={
                <AdminRoute>
                  <ProtectedAdminView />
                </AdminRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      )

      expect(screen.getByText('User Dashboard Fallback')).toBeDefined()
      expect(screen.queryByText('Admin Portal Content')).toBeNull()
    })

    it('allows access when user is admin and X-Admin-Key is present in localStorage', () => {
      localStorage.setItem('medisense_admin_key', 'adm-secret-key-123')
      useAuthStore.setState({
        user: { user_id: 'adm-1', email: 'admin@medisense.org', language_pref: 'en', is_admin: true },
        is_authenticated: true,
      })

      render(
        <MemoryRouter initialEntries={['/admin/eval']}>
          <Routes>
            <Route path="/dashboard" element={<DashboardFallback />} />
            <Route
              path="/admin/eval"
              element={
                <AdminRoute>
                  <ProtectedAdminView />
                </AdminRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      )

      expect(screen.getByText('Admin Portal Content')).toBeDefined()
      expect(screen.queryByText('User Dashboard Fallback')).toBeNull()
    })
  })

  /* 3. SecurityEventsPanel and Abuse Alert Rendering */
  describe('SecurityEventsPanel Component', () => {
    it('renders abuse alert card when CRITICAL or LOCKOUT event is detected', () => {
      const mockEvents: AuditLogEntry[] = [
        {
          logId: 'log-1',
          eventType: 'ACCOUNT_LOCKOUT',
          sessionId: 'sess-1',
          userId: 'bad-actor-uuid',
          queryId: null,
          consentGrantId: null,
          ipAddress: '192.168.1.100',
          eventDetail: '5 failed login attempts',
          severity: 'CRITICAL',
          createdAt: new Date().toISOString(),
        },
      ]

      render(<SecurityEventsPanel events={mockEvents} />)

      expect(screen.getByTestId('abuse-alert-card')).toBeDefined()
      expect(screen.getByText(/Abuse Flag Active/i)).toBeDefined()
      expect(screen.getByText('bad-actor-uuid')).toBeDefined()
    })
  })
})
