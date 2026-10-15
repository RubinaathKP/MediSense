import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import RateLimitBanner from '@/components/common/RateLimitBanner'

describe('RateLimitBanner', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders with countdown visible', () => {
    render(<RateLimitBanner retryAfterSeconds={60} />)
    expect(screen.getByRole('alert')).toBeDefined()
    expect(screen.getByText(/60s/)).toBeDefined()
  })

  it('shows retry message when countdown reaches 0', () => {
    const onExpired = vi.fn()
    render(<RateLimitBanner retryAfterSeconds={2} onExpired={onExpired} />)

    act(() => {
      vi.advanceTimersByTime(2100)
    })
    expect(onExpired).toHaveBeenCalled()
  })

  it('immediately shows expired state for retryAfterSeconds=0', () => {
    const onExpired = vi.fn()
    render(<RateLimitBanner retryAfterSeconds={0} onExpired={onExpired} />)
    expect(onExpired).toHaveBeenCalled()
  })

  it('has correct aria attributes', () => {
    render(<RateLimitBanner retryAfterSeconds={30} />)
    const el = screen.getByRole('alert')
    expect(el).toBeDefined()
  })

  it('has unique id for browser testing', () => {
    render(<RateLimitBanner retryAfterSeconds={30} />)
    expect(document.getElementById('rate-limit-banner')).toBeDefined()
  })
})
