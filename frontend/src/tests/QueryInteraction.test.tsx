import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import QueryInput from '@/components/query/QueryInput'
import CitationCard from '@/components/query/CitationCard'
import ReferralCard from '@/components/query/ReferralCard'
import QueryResponseCard from '@/components/query/QueryResponseCard'
import type { Citation, QueryResponse } from '@/api/types'

describe('Query Interaction Layer Component Tests', () => {
  /* 1. Character limit and validation tests */
  describe('QueryInput Component', () => {
    it('enforces character count down and warning on short query', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()

      render(<QueryInput onSubmit={onSubmit} />)

      const textarea = screen.getByTestId('query-textarea')
      const submitBtn = screen.getByTestId('query-submit-btn')

      // Initially empty -> Submit is disabled
      expect(submitBtn).toBeDisabled()
      expect(screen.getByTestId('char-counter')).toHaveTextContent('500 remaining')

      // Type 5 characters -> Short query warning appears, submit still disabled (< 10 chars)
      await user.type(textarea, 'Hello')
      expect(screen.getByTestId('min-char-warning')).toHaveTextContent(
        'Please enter a complete question'
      )
      expect(submitBtn).toBeDisabled()

      // Type more than 10 characters -> Submit enabled, warning gone
      await user.type(textarea, ' world, what is LDL?')
      expect(screen.queryByTestId('min-char-warning')).toBeNull()
      expect(submitBtn).not.toBeDisabled()

      // Auto detects language
      expect(screen.getByTestId('language-indicator')).toHaveTextContent('EN')

      // Click submit
      await user.click(submitBtn)
      expect(onSubmit).toHaveBeenCalledWith('Hello world, what is LDL?', 'en')
    })

    it('auto detects Tamil language script input', async () => {
      const user = userEvent.setup()
      render(<QueryInput onSubmit={vi.fn()} />)

      const textarea = screen.getByTestId('query-textarea')
      await user.type(textarea, 'கொலஸ்ட்ரால் என்றால் என்ன?')

      expect(screen.getByTestId('language-indicator')).toHaveTextContent('தமிழ்')
    })

    it('handles rate limited disabled state and countdown display', () => {
      render(
        <QueryInput
          onSubmit={vi.fn()}
          isRateLimited={true}
          rateLimitCountdown={45}
        />
      )

      const submitBtn = screen.getByTestId('query-submit-btn')
      expect(submitBtn).toBeDisabled()
      expect(submitBtn).toHaveTextContent('Try again in 45s')
    })

    it('populates query from suggestion chip click', async () => {
      const user = userEvent.setup()
      render(<QueryInput onSubmit={vi.fn()} selectedTab="claim" />)

      const chip = screen.getByTestId('suggestion-chip-0')
      expect(chip).toHaveTextContent('Is drinking neem water good for diabetes?')

      await user.click(chip)
      const textarea = screen.getByTestId('query-textarea')
      expect(textarea).toHaveValue('Is drinking neem water good for diabetes?')
    })
  })

  /* 2. CitationCard Evidence basis color coding */
  describe('CitationCard Component', () => {
    it('renders Clinical Evidence badge with blue scheme for clinical_trial', () => {
      const citation: Citation = {
        chunk_id: 'icmr-chunk-1',
        source_name: 'ICMR Guidelines',
        source_year: 2024,
        evidence_basis: 'clinical_trial',
      }

      render(<CitationCard citation={citation} index={0} />)

      expect(screen.getByText('ICMR Guidelines')).toBeDefined()
      expect(screen.getByText('2024')).toBeDefined()
      const badge = screen.getByTestId('evidence-basis-badge')
      expect(badge).toHaveTextContent('Clinical Evidence')
      expect(badge.className).toContain('text-blue-300')
    })

    it('renders Traditional Medicine badge with amber scheme for traditional_recognized', () => {
      const citation: Citation = {
        chunk_id: 'ayush-chunk-2',
        source_name: 'AYUSH Formulary',
        source_year: 2022,
        evidence_basis: 'traditional_recognized',
      }

      render(<CitationCard citation={citation} index={1} />)

      expect(screen.getByText('AYUSH Formulary')).toBeDefined()
      const badge = screen.getByTestId('evidence-basis-badge')
      expect(badge).toHaveTextContent('Traditional Medicine')
      expect(badge.className).toContain('text-amber-300')
    })

    it('renders Unverified badge for not_found', () => {
      const citation: Citation = {
        chunk_id: 'general-chunk-3',
        source_name: 'General Web Reference',
        source_year: null,
        evidence_basis: 'not_found',
      }

      render(<CitationCard citation={citation} index={2} />)

      const badge = screen.getByTestId('evidence-basis-badge')
      expect(badge).toHaveTextContent('Unverified')
      expect(badge.className).toContain('text-slate-400')
    })
  })

  /* 3. ReferralCard Component */
  describe('ReferralCard Component', () => {
    it('renders clinical judgment notice and client-side safe alternatives for PRESCRIPTIVE flag', () => {
      render(
        <ReferralCard
          responseText="Please consult your doctor regarding dosage changes."
          classifierFlag="PRESCRIPTIVE"
        />
      )

      expect(screen.getByTestId('referral-card')).toBeDefined()
      expect(screen.getByTestId('clinical-judgment-tag')).toHaveTextContent(
        'This question requires clinical judgment'
      )
      expect(
        screen.getByText('Please consult your doctor regarding dosage changes.')
      ).toBeDefined()
      expect(screen.getByTestId('safe-alt-0')).toHaveTextContent(
        'What is this medication class generally used for?'
      )
    })
  })

  /* 4. QueryResponseCard (Emergency, Safe, Insufficient Context, Translation) */
  describe('QueryResponseCard Component', () => {
    it('renders urgent Emergency response with Call 112 button', () => {
      const emergencyResp: QueryResponse = {
        query_id: 'q-emg-1',
        response_text: 'Chest pain is an emergency. Please seek immediate assistance.',
        is_referral: true,
        classifier_flag: 'EMERGENCY',
        citations: [],
        language_out: 'en',
        translation_unavailable: null,
        insufficient_context: null,
      }

      render(<QueryResponseCard response={emergencyResp} />)

      expect(screen.getByTestId('emergency-response-card')).toBeDefined()
      const callBtn = screen.getByTestId('call-112-btn')
      expect(callBtn).toHaveAttribute('href', 'tel:112')
      expect(
        screen.getByText(/Please call 112 immediately or go to your nearest emergency room/i)
      ).toBeDefined()
    })

    it('renders safe markdown response with citations list', () => {
      const safeResp: QueryResponse = {
        query_id: 'q-safe-1',
        response_text: '**Atorvastatin** belongs to HMG-CoA reductase inhibitors.',
        is_referral: false,
        classifier_flag: null,
        citations: [
          {
            chunk_id: 'who-1',
            source_name: 'WHO Model Formulary',
            source_year: 2023,
            evidence_basis: 'clinical_trial',
          },
        ],
        language_out: 'en',
        translation_unavailable: null,
        insufficient_context: null,
      }

      render(<QueryResponseCard response={safeResp} />)

      expect(screen.getByTestId('safe-response-card')).toBeDefined()
      expect(screen.getByTestId('markdown-response')).toBeDefined()
      expect(screen.getByTestId('citations-section')).toBeDefined()
      expect(screen.getByText('WHO Model Formulary')).toBeDefined()
    })

    it('renders insufficient context notice when no guideline matches', () => {
      const noContextResp: QueryResponse = {
        query_id: 'q-no-ctx',
        response_text: '',
        is_referral: false,
        classifier_flag: null,
        citations: [],
        language_out: 'en',
        translation_unavailable: null,
        insufficient_context: true,
      }

      render(<QueryResponseCard response={noContextResp} />)

      expect(screen.getByTestId('insufficient-context-box')).toBeDefined()
      expect(screen.queryByTestId('citations-section')).toBeNull()
    })

    it('renders translation unavailable banner when translation_unavailable is true', () => {
      const transUnavailableResp: QueryResponse = {
        query_id: 'q-trans-unavail',
        response_text: 'English fallback text.',
        is_referral: false,
        classifier_flag: null,
        citations: [],
        language_out: 'en',
        translation_unavailable: true,
        insufficient_context: null,
      }

      render(<QueryResponseCard response={transUnavailableResp} />)

      expect(screen.getByTestId('translation-unavailable-banner')).toBeDefined()
      expect(screen.getByText(/Tamil translation temporarily unavailable/i)).toBeDefined()
    })
  })
})
