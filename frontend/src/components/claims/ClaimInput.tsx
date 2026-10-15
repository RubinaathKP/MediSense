import { useState, useEffect } from 'react'
import { Send } from 'lucide-react'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { cn } from '@/utils/cn'

interface ClaimInputProps {
  onSubmit: (claim: string) => void
  isLoading?: boolean
  isRateLimited?: boolean
  rateLimitCountdown?: number
  remainingQuota?: number
}

const MAX_CLAIM_CHARS = 300

const PLACEHOLDER_CYCLE = [
  'Does drinking neem water cure diabetes?',
  'Is turmeric effective for joint pain?',
  'Can fasting reverse high cholesterol?',
  'Is this WhatsApp health tip accurate?',
]

export default function ClaimInput({
  onSubmit,
  isLoading = false,
  isRateLimited = false,
  rateLimitCountdown = 0,
  remainingQuota = 20,
}: ClaimInputProps) {
  const [claimText, setClaimText] = useState('')
  const [placeholderIndex, setPlaceholderIndex] = useState(0)

  // Cycling placeholder text animation every 3 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setPlaceholderIndex((prev) => (prev + 1) % PLACEHOLDER_CYCLE.length)
    }, 3000)
    return () => clearInterval(timer)
  }, [])

  const remainingChars = MAX_CLAIM_CHARS - claimText.length
  const isSubmitDisabled =
    isLoading ||
    isRateLimited ||
    claimText.trim().length < 5 ||
    remainingChars < 0

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (isSubmitDisabled) return
    onSubmit(claimText.trim())
    setClaimText('')
  }

  return (
    <div className="w-full space-y-3">
      <div className="relative rounded-2xl border border-surface-700/80 bg-surface-800/80 p-4 shadow-glass backdrop-blur-md transition-all focus-within:border-primary-500 focus-within:ring-1 focus-within:ring-primary-500/30">
        <label
          htmlFor="claim-input-textarea"
          className="mb-2 block text-xs font-semibold uppercase tracking-wider text-surface-400"
        >
          Verify Health Tip or Remedy Claim
        </label>

        <textarea
          id="claim-input-textarea"
          data-testid="claim-input-textarea"
          rows={3}
          value={claimText}
          onChange={(e) => setClaimText(e.target.value.slice(0, MAX_CLAIM_CHARS))}
          placeholder={PLACEHOLDER_CYCLE[placeholderIndex]}
          disabled={isLoading || isRateLimited}
          className="w-full resize-none bg-transparent text-sm leading-relaxed text-slate-100 placeholder-surface-500 outline-none disabled:cursor-not-allowed disabled:opacity-50"
        />

        <div className="mt-2 flex items-center justify-between border-t border-surface-700/50 pt-2">
          <div className="flex flex-col">
            <span
              data-testid="claim-char-counter"
              className={cn(
                'text-xs font-mono font-medium',
                remainingChars < 30 ? 'text-red-400 font-bold' : 'text-surface-400'
              )}
            >
              {remainingChars} remaining
            </span>
            {remainingQuota < Infinity && !isRateLimited && (
              <span className="text-[11px] text-surface-500">
                You can verify {remainingQuota} more claims this hour.
              </span>
            )}
          </div>

          <button
            type="button"
            id="claim-submit-btn"
            data-testid="claim-submit-btn"
            onClick={() => handleSubmit()}
            disabled={isSubmitDisabled}
            className={cn(
              'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-semibold text-white shadow-glow transition-all duration-200',
              isRateLimited
                ? 'bg-surface-700 text-surface-400 cursor-not-allowed'
                : isSubmitDisabled
                ? 'bg-surface-700/60 text-surface-400 opacity-60 cursor-not-allowed'
                : 'bg-gradient-primary hover:opacity-95 active:scale-95'
            )}
          >
            {isLoading ? (
              <>
                <LoadingSpinner size="sm" />
                <span>Verifying...</span>
              </>
            ) : isRateLimited ? (
              <span>Try again in {rateLimitCountdown}s</span>
            ) : (
              <>
                <span>Verify Claim</span>
                <Send className="h-3.5 w-3.5" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
