import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import ConsentStatus from '@/components/abdm/ConsentStatus'
import { reportsApi } from '@/api/reports'
import { useSessionStore } from '@/store/sessionStore'

vi.mock('@/api/reports', () => ({
  reportsApi: {
    getReportDetails: vi.fn(),
  },
}))

describe('ConsentStatus Component', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    useSessionStore.setState({
      session_id: 'session-abdm-1',
      consent_status: 'pending',
      consent_request_id: 'req-123',
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders pending status and countdown', () => {
    render(
      <ConsentStatus
        onRecordReceived={vi.fn()}
        onTimeout={vi.fn()}
      />
    )

    expect(screen.getByText(/abdm\.consent_sent_title/i)).toBeDefined()
    expect(screen.getByText(/abdm\.consent_sent_body/i)).toBeDefined()
  })

  it('triggers onTimeout after 30 poll intervals without record data', async () => {
    const onTimeout = vi.fn()
    vi.mocked(reportsApi.getReportDetails).mockResolvedValue({
      data: {
        session_id: 'session-abdm-1',
        report_type: 'lab_report',
        lab_values: [],
        prescriptions: [],
      },
    } as any)

    render(
      <ConsentStatus
        onRecordReceived={vi.fn()}
        onTimeout={onTimeout}
      />
    )

    // Advance through 31 intervals of 5000ms
    for (let i = 0; i <= 31; i++) {
      await act(async () => {
        vi.advanceTimersByTime(5000)
      })
    }

    expect(onTimeout).toHaveBeenCalled()
    expect(useSessionStore.getState().consent_status).toBe('timeout')
  })
})
