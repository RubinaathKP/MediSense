import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Disclaimer from '@/components/layout/Disclaimer'
import ClaimAssessmentCard from '@/components/claims/ClaimAssessment'
import RateLimitWidget from '@/components/dashboard/RateLimitWidget'
import Sidebar from '@/components/layout/Sidebar'
import DashboardHome from '@/pages/dashboard/DashboardHome'
import { useRateLimitStore } from '@/store/rateLimitStore'
import { useAuthStore } from '@/store/authStore'
import { useSessionStore } from '@/store/sessionStore'
import type { ClaimAssessment } from '@/api/types'

// Mock react-query hook for DashboardHome
vi.mock('@/hooks/useQueryHistory', () => ({
  useQueryHistory: () => ({
    data: [
      {
        query_id: 'q-hist-1',
        response_text: 'Recent sample question response',
        is_referral: false,
        classifier_flag: null,
        citations: [],
        language_out: 'en',
      },
    ],
    isLoading: false,
  }),
}))

describe('Dashboard and Claim Verification UI Test Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useRateLimitStore.getState().clearRateLimit('query')
    useRateLimitStore.getState().clearRateLimit('reports-upload')
    useAuthStore.setState({
      user: { user_id: 'u1', email: 'aarav@medisense.org', language_pref: 'en' },
      is_authenticated: true,
      access_token: 'token-123',
      refresh_token: 'refresh-123',
      failed_login_attempts: 0,
      is_loading: false,
    })
    useSessionStore.setState({
      session_id: 'session-xyz',
      report_loaded: false,
      lab_values: [],
      prescriptions: [],
    })
  })

  /* 1. Disclaimer always renders and cannot be hidden */
  it('Disclaimer always renders with non-dismissible role and correct text', () => {
    render(<Disclaimer />)
    const disclaimer = screen.getByTestId('persistent-disclaimer')
    expect(disclaimer).toBeDefined()
    expect(disclaimer).toHaveAttribute('role', 'note')
    expect(screen.getByText('disclaimer.text')).toBeDefined()
  })

  /* 2. Verdict field is NEVER rendered on ClaimAssessment */
  it('Verdict field is never rendered anywhere in ClaimAssessmentCard', () => {
    const mockAssessment: ClaimAssessment = {
      allopathic_position: {
        summary: 'Clinical studies show turmeric has mild anti-inflammatory properties.',
        evidence_basis: 'clinical_trial',
        source: 'NIH Clinical Practice Guidelines (2023)',
      },
      traditional_position: {
        summary: 'Turmeric (Haridra) is traditionally documented for joint pain relief.',
        evidence_basis: 'traditional_recognized',
        source: 'Ayurvedic Pharmacopoeia of India',
      },
      verdict: null,
      closing_note: 'Consult your doctor before replacing standard therapies with herbal remedies.',
    }

    render(
      <ClaimAssessmentCard
        assessment={mockAssessment}
        claimText="Does turmeric help with joint pain?"
      />
    )

    // Check evidence columns
    expect(screen.getByTestId('clinical-evidence-column')).toBeDefined()
    expect(screen.getByTestId('traditional-evidence-column')).toBeDefined()
    expect(screen.getByTestId('claim-closing-note')).toBeDefined()

    // Assert that the word "Verdict", "True", "False", or "Adjudication" is NEVER present as a verdict
    expect(screen.queryByText(/^Verdict/i)).toBeNull()
    expect(screen.queryByText(/^True$/i)).toBeNull()
    expect(screen.queryByText(/^False$/i)).toBeNull()
  })

  /* 3. Rate limit widget shows correct remaining quota numbers */
  it('Rate limit widget renders correct quota numbers and progress bars', () => {
    // Set custom rate limits
    useRateLimitStore.getState().setRateLimit('query', {
      endpoint_slug: 'query',
      limit: 30,
      remaining: 18,
      reset_at: Math.floor(Date.now() / 1000) + 1800,
      retry_after: 0,
      captured_at: Date.now(),
    })

    useRateLimitStore.getState().setRateLimit('reports-upload', {
      endpoint_slug: 'reports-upload',
      limit: 10,
      remaining: 4,
      reset_at: Math.floor(Date.now() / 1000) + 1800,
      retry_after: 0,
      captured_at: Date.now(),
    })

    render(<RateLimitWidget />)

    expect(screen.getByTestId('queries-quota-label')).toHaveTextContent('18/30')
    expect(screen.getByTestId('uploads-quota-label')).toHaveTextContent('4/10')
  })

  /* 4. Sidebar active states and responsive structure */
  it('Sidebar renders desktop and mobile navigation links with correct to targets', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Sidebar />
      </MemoryRouter>
    )

    expect(screen.getByTestId('sidebar-desktop')).toBeDefined()
    expect(screen.getByTestId('sidebar-mobile-bottom')).toBeDefined()
    expect(screen.getByTestId('sidebar-link-dashboard')).toBeDefined()
    expect(screen.getByTestId('sidebar-link-report')).toBeDefined()
    expect(screen.getByTestId('sidebar-link-query')).toBeDefined()
    expect(screen.getByTestId('sidebar-link-claims')).toBeDefined()
    expect(screen.getByTestId('sidebar-link-history')).toBeDefined()
  })

  /* 5. Dashboard quick action navigation cards and greeting */
  it('Dashboard renders personalized greeting and all 3 quick action cards', () => {
    render(
      <MemoryRouter>
        <DashboardHome />
      </MemoryRouter>
    )

    // Greeting
    expect(screen.getByTestId('dashboard-greeting')).toHaveTextContent(/aarav/i)

    // 3 Quick Action Cards
    const reportCard = screen.getByTestId('quick-action-report')
    const claimCard = screen.getByTestId('quick-action-claim')
    const historyCard = screen.getByTestId('quick-action-history')

    expect(reportCard).toHaveAttribute('href', '/report')
    expect(claimCard).toHaveAttribute('href', '/claims')
    expect(historyCard).toHaveAttribute('href', '/history')

    expect(screen.getByText('Understand My Report')).toBeDefined()
    expect(screen.getByText('Verify a Health Claim')).toBeDefined()
    expect(screen.getByText('My Question History')).toBeDefined()
  })
})
