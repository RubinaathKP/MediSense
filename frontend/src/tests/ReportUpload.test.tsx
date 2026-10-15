import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ReportUpload from '@/components/report/ReportUpload'
import { reportsApi } from '@/api/reports'
import { useSessionStore } from '@/store/sessionStore'
import { useRateLimitStore } from '@/store/rateLimitStore'

vi.mock('@/api/reports', () => ({
  reportsApi: {
    uploadReport: vi.fn(),
  },
}))

describe('ReportUpload Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useSessionStore.setState({
      session_id: 'test-session-1',
      report_loaded: false,
      lab_values: [],
      prescriptions: [],
    })
    useRateLimitStore.getState().clearRateLimit('reports-upload')
  })

  it('renders upload area and report type toggle', () => {
    render(<ReportUpload onUploadSuccess={vi.fn()} />)

    expect(screen.getByText('report.report_type_lab')).toBeDefined()
    expect(screen.getByText('report.report_type_prescription')).toBeDefined()
    expect(screen.getByText('report.upload_idle')).toBeDefined()
  })

  it('handles report upload when valid PDF is selected', async () => {
    const user = userEvent.setup()
    const onUploadSuccess = vi.fn()
    const mockUploadRes = {
      data: {
        session_id: 'test-session-1',
        report_type: 'lab_report',
        total_extracted: 2,
        requires_confirmation_count: 0,
        extracted_values: [
          {
            testName: 'Hemoglobin',
            canonicalName: 'Hemoglobin',
            numericValue: 14.2,
            unit: 'g/dL',
            flag: 'NORMAL',
            requiresUserConfirmation: false,
          },
        ],
      },
    }
    vi.mocked(reportsApi.uploadReport).mockResolvedValueOnce(mockUploadRes as any)

    render(<ReportUpload onUploadSuccess={onUploadSuccess} />)

    const file = new File(['%PDF-1.4 test'], 'blood_test.pdf', { type: 'application/pdf' })
    const input = screen.getByTestId('file-input') as HTMLInputElement
    await user.upload(input, file)

    const uploadBtn = screen.getByRole('button', { name: /report\.upload_button/i })
    await user.click(uploadBtn)

    await waitFor(() => {
      expect(reportsApi.uploadReport).toHaveBeenCalled()
      expect(onUploadSuccess).toHaveBeenCalledWith(mockUploadRes.data, 'lab_report')
    })
  })

  it('displays rate limit error state if rate limited', async () => {
    const user = userEvent.setup()
    const errorRes = {
      response: {
        status: 429,
        data: {
          error_code: 'RATE_LIMIT_EXCEEDED',
          retry_after_seconds: 3600,
        },
      },
    }
    vi.mocked(reportsApi.uploadReport).mockRejectedValueOnce(errorRes)

    render(<ReportUpload onUploadSuccess={vi.fn()} />)

    const file = new File(['%PDF-1.4 test'], 'blood_test.pdf', { type: 'application/pdf' })
    const input = screen.getByTestId('file-input') as HTMLInputElement
    await user.upload(input, file)

    const uploadBtn = screen.getByRole('button', { name: /report\.upload_button/i })
    await user.click(uploadBtn)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeDefined()
    })
  })
})
