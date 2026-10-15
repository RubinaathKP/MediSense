import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ValueConfirmation from '@/components/report/ValueConfirmation'
import { reportsApi } from '@/api/reports'
import type { ReportUploadResponse } from '@/api/types'

vi.mock('@/api/reports', () => ({
  reportsApi: {
    confirmValue: vi.fn(),
  },
}))

describe('ValueConfirmation Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockUploadResponse: ReportUploadResponse = {
    report_id: 'rep-val-1',
    requires_confirmation: true,
    extracted_values: [
      {
        testName: 'Hemoglobin',
        extractedValue: '13.5',
        normalizedValue: 13.5,
        unit: 'g/dL',
        referenceRange: '12.0 - 15.5',
        flagStatus: 'NORMAL',
        extractionConfidence: 0.98,
        requiresUserConfirmation: false,
      },
      {
        testName: 'LDL Cholesterol',
        extractedValue: '145',
        normalizedValue: 145,
        unit: 'mg/dL',
        referenceRange: '< 100',
        flagStatus: 'BORDERLINE',
        extractionConfidence: 0.65,
        requiresUserConfirmation: true,
      },
    ],
  }

  it('renders low confidence value needing confirmation', () => {
    render(
      <ValueConfirmation
        uploadResponse={mockUploadResponse}
        onConfirmed={vi.fn()}
      />
    )

    expect(screen.getByText(/LDL Cholesterol/i)).toBeDefined()
    expect(screen.getByText(/145/i)).toBeDefined()
    expect(screen.getByRole('button', { name: /report\.confirm_yes/i })).toBeDefined()
    expect(screen.getByRole('button', { name: /report\.confirm_edit/i })).toBeDefined()
  })

  it('handles confirmation of value correctly', async () => {
    const user = userEvent.setup()
    const onConfirmed = vi.fn()
    vi.mocked(reportsApi.confirmValue).mockResolvedValueOnce({
      data: {
        session_id: 'session-val-1',
        test_name: 'LDL Cholesterol',
        confirmed_value: '145',
        confirmed_unit: 'mg/dL',
        status: 'UPDATED',
        requires_further_confirmation: false,
      },
    } as any)

    render(
      <ValueConfirmation
        uploadResponse={mockUploadResponse}
        onConfirmed={onConfirmed}
      />
    )

    const confirmBtn = screen.getByRole('button', { name: /report\.confirm_yes/i })
    await user.click(confirmBtn)

    await waitFor(() => {
      expect(reportsApi.confirmValue).toHaveBeenCalledWith({
        value_id: 'rep-val-1',
        confirmed_value: '145',
        confirmed_unit: 'mg/dL',
      })
    })

    const doneBtn = await screen.findByRole('button', { name: /report\.confirm_done/i })
    await user.click(doneBtn)

    expect(onConfirmed).toHaveBeenCalled()
  })
})
