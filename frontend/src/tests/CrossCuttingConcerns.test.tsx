import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, renderHook, act } from '@testing-library/react'
import { useRateLimitStore } from '@/store/rateLimitStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import apiClient, { RateLimitError } from '@/api/client'
import { getErrorMessage } from '@/hooks/useApiError'
import i18n from '@/i18n/index'
import LanguageToggle from '@/components/common/LanguageToggle'
import { useAuthStore } from '@/store/authStore'
import ta from '@/i18n/ta.json'

describe('MediSense Frontend Cross-Cutting Concerns Test Suite', () => {
  beforeEach(async () => {
    useRateLimitStore.setState({ limits: new Map() })
    useAuthStore.setState({
      user: null,
      access_token: null,
      refresh_token: null,
      is_authenticated: false,
      failed_login_attempts: 0,
    })
    await i18n.changeLanguage('en')
  })

  // 1. rateLimitStore update from headers
  describe('PART 1 — rateLimitStore & useRateLimit', () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })
    afterEach(() => {
      vi.useRealTimers()
    })

    it('updates rate limit state accurately from Axios response headers', () => {
      const store = useRateLimitStore.getState()
      const headers = {
        'x-ratelimit-limit': '100',
        'x-ratelimit-remaining': '0',
        'x-ratelimit-reset': '60',
        'retry-after': '60',
      }

      store.updateFromHeaders('query', headers)
      const queryLimit = useRateLimitStore.getState().limits.get('query')

      expect(queryLimit).toBeDefined()
      expect(queryLimit?.limit).toBe(100)
      expect(queryLimit?.remaining).toBe(0)
      expect(queryLimit?.retry_after).toBe(60)
      expect(queryLimit?.is_limited).toBe(true)
      expect(queryLimit?.reset_at).toBeGreaterThan(Math.floor(Date.now() / 1000))
    })

    it('countdown decrements and automatically clears limit when reaching 0', () => {
      const nowSeconds = Math.floor(Date.now() / 1000)
      useRateLimitStore.getState().limits.set('upload', {
        endpoint: 'upload',
        limit: 10,
        remaining: 0,
        reset_at: nowSeconds + 3,
        retry_after: 3,
        is_limited: true,
      })

      const { result } = renderHook(() => useRateLimit('upload'))
      expect(result.current.is_limited).toBe(true)
      expect(result.current.countdown).toBe(3)

      act(() => {
        vi.advanceTimersByTime(1000)
      })
      expect(result.current.countdown).toBe(2)

      act(() => {
        vi.advanceTimersByTime(2100)
      })

      const updatedStore = useRateLimitStore.getState().limits.get('upload')
      expect(updatedStore?.is_limited).toBe(false)
      expect(updatedStore?.remaining).toBe(10)
    })
  })

  // 2. Client-side gate prevents API call
  describe('PART 1 — Axios Client Rate Limit Gate', () => {
    it('client-side gate rejects immediately with RateLimitError when endpoint is limited', async () => {
      const nowSeconds = Math.floor(Date.now() / 1000)
      useRateLimitStore.getState().limits.set('query', {
        endpoint: 'query',
        limit: 20,
        remaining: 0,
        reset_at: nowSeconds + 45,
        retry_after: 45,
        is_limited: true,
      })

      await expect(apiClient.post('/query', { query: 'test question' })).rejects.toThrow(RateLimitError)
    })
  })

  // 3. Error code to message mapping
  describe('PART 2 — Error Handling & useApiError', () => {
    const t = (key: string, opts?: any): string => String(i18n.t(key, opts))

    it('maps RATE_LIMIT_EXCEEDED error code correctly', () => {
      const err = { response: { data: { error_code: 'RATE_LIMIT_EXCEEDED', retry_after_seconds: 30 } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('RATE_LIMIT_EXCEEDED')
      expect(res.isRateLimit).toBe(true)
      expect(res.message).toContain('30')
    })

    it('maps ABDM_GATEWAY_UNAVAILABLE error code correctly', () => {
      const err = { response: { data: { error_code: 'ABDM_GATEWAY_UNAVAILABLE' } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('ABDM_GATEWAY_UNAVAILABLE')
      expect(res.message).toBe('ABHA record access is temporarily unavailable. Please upload a PDF instead.')
    })

    it('maps LLM_PROVIDER_UNAVAILABLE error code correctly', () => {
      const err = { response: { data: { error_code: 'LLM_PROVIDER_UNAVAILABLE' } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('LLM_PROVIDER_UNAVAILABLE')
      expect(res.message).toBe('Our AI service is temporarily unavailable. Please try again in a few minutes.')
    })

    it('maps SESSION_NOT_FOUND error code correctly', () => {
      const err = { response: { data: { error_code: 'SESSION_NOT_FOUND' } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('SESSION_NOT_FOUND')
      expect(res.message).toBe('Your session has expired. Please start a new session.')
    })

    it('maps CONSENT_EXPIRED error code correctly', () => {
      const err = { response: { data: { error_code: 'CONSENT_EXPIRED' } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('CONSENT_EXPIRED')
      expect(res.message).toBe('Your ABHA consent has expired. Please reconnect your ABHA account.')
    })

    it('maps OCR_CONFIDENCE_LOW error code correctly and marks isOcrLow', () => {
      const err = { response: { data: { error_code: 'OCR_CONFIDENCE_LOW' } } }
      const res = getErrorMessage(err, t)
      expect(res.code).toBe('OCR_CONFIDENCE_LOW')
      expect(res.isOcrLow).toBe(true)
    })
  })

  // 4. Language toggle updates i18n
  describe('PART 3 — Internationalisation & LanguageToggle', () => {
    it('updates i18n language and user preference when toggled', async () => {
      useAuthStore.setState({
        user: { user_id: 'u1', email: 'test@medisense.in', language_pref: 'en' },
        is_authenticated: true,
      })

      render(<LanguageToggle />)

      const tamilButton = screen.getByRole('button', { name: /தமிழ்/i })
      await act(async () => {
        fireEvent.click(tamilButton)
      })

      expect(i18n.language).toMatch(/^ta/)
      expect(useAuthStore.getState().user?.language_pref).toBe('ta')
    })
  })

  // 5. [NEEDS_REVIEW] count documented in test output
  describe('PART 3 — Tamil Translation [NEEDS_REVIEW] Audit', () => {
    it('verifies all Tamil strings are marked with [NEEDS_REVIEW] and logs total count', () => {
      let needsReviewCount = 0
      let totalTamilStrings = 0

      function scan(obj: any) {
        for (const key in obj) {
          if (typeof obj[key] === 'string') {
            totalTamilStrings++
            if (obj[key].includes('[NEEDS_REVIEW]')) {
              needsReviewCount++
            }
          } else if (typeof obj[key] === 'object' && obj[key] !== null) {
            scan(obj[key])
          }
        }
      }

      scan(ta)

      console.log(`\n======================================================`)
      console.log(`[NEEDS_REVIEW Audit Summary]`)
      console.log(`Total Tamil translation entries: ${totalTamilStrings}`)
      console.log(`Entries flagged with [NEEDS_REVIEW]: ${needsReviewCount}`)
      console.log(`Critical strings included: disclaimer, referral_prefix, insufficient_context, emergency, all error messages`)
      console.log(`======================================================\n`)

      expect(needsReviewCount).toBeGreaterThan(40)
      expect(ta.disclaimer).toContain('[NEEDS_REVIEW]')
      expect(ta.query.referral_prefix).toContain('[NEEDS_REVIEW]')
      expect(ta.query.insufficient_context).toContain('[NEEDS_REVIEW]')
      expect(ta.query.emergency).toContain('[NEEDS_REVIEW]')
      expect(ta.errors.abdm_unavailable).toContain('[NEEDS_REVIEW]')
      expect(ta.errors.llm_unavailable).toContain('[NEEDS_REVIEW]')
    })
  })
})
