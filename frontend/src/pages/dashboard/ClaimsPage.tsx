import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { ShieldCheck, History, ChevronDown, ChevronUp, AlertCircle } from 'lucide-react'
import { AxiosError } from 'axios'

import { claimsApi } from '@/api/claims'
import { useSessionStore } from '@/store/sessionStore'
import { useAuthStore } from '@/store/authStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import type { ClaimResponse, ApiError } from '@/api/types'
import ClaimInput from '@/components/claims/ClaimInput'
import ClaimAssessmentCard from '@/components/claims/ClaimAssessment'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LoadingSpinner from '@/components/common/LoadingSpinner'

interface ClaimHistoryItem {
  id: string
  claimText: string
  response: ClaimResponse
  timestamp: number
}

export default function ClaimPage() {
  const { session_id } = useSessionStore()
  const { user } = useAuthStore()
  const claimsRateLimit = useRateLimit('claims-verify')

  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const [currentClaimText, setCurrentClaimText] = useState('')
  const [currentResponse, setCurrentResponse] = useState<ClaimResponse | null>(null)
  const [claimHistory, setClaimHistory] = useState<ClaimHistoryItem[]>([])
  const [isHistoryOpen, setIsHistoryOpen] = useState(false)

  const mutation = useMutation({
    mutationFn: async (claim: string) => {
      setCurrentClaimText(claim)
      const res = await claimsApi.verifyClaim({
        session_id: session_id ?? 'anonymous',
        claim_text: claim,
        language: user?.language_pref ?? 'en',
      })
      return res.data
    },
    onSuccess: (data) => {
      setCurrentResponse(data)
      setClaimHistory((prev) => [
        {
          id: data.query_id || `claim-${Date.now()}`,
          claimText: currentClaimText,
          response: data,
          timestamp: Date.now(),
        },
        ...prev,
      ])
    },
    onError: (err: AxiosError<ApiError>) => {
      if (err.response?.status === 429) {
        setRateLimitSeconds(err.response.data?.retry_after_seconds ?? 3600)
      }
    },
  })

  const handleClaimSubmit = (claim: string) => {
    mutation.mutate(claim)
  }

  const isTier3Limited = claimsRateLimit.is_limited || rateLimitSeconds > 0
  const activeCountdown = rateLimitSeconds || claimsRateLimit.countdown

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <ShieldCheck className="h-6 w-6 text-primary-400" />
          <span>Verify a Health Claim</span>
        </h1>
        <p className="mt-1 text-sm text-surface-400">
          Check health tips, traditional remedies, and viral wellness claims against clinical trial guidelines and documented traditional pharmacopoeias.
        </p>
      </div>

      {/* Rate Limit Banner */}
      {isTier3Limited && (
        <RateLimitBanner
          retryAfterSeconds={activeCountdown}
          onExpired={() => setRateLimitSeconds(0)}
        />
      )}

      {/* Full-width Claim Input */}
      <ClaimInput
        onSubmit={handleClaimSubmit}
        isLoading={mutation.isPending}
        isRateLimited={isTier3Limited}
        rateLimitCountdown={activeCountdown}
        remainingQuota={claimsRateLimit.remaining}
      />

      {/* Loading state */}
      {mutation.isPending && (
        <div className="flex items-center gap-3 rounded-2xl border border-surface-700/60 bg-surface-800/40 p-6">
          <LoadingSpinner size="md" />
          <div>
            <p className="text-sm font-semibold text-white">
              Retrieving evidence from clinical and traditional medicine databases...
            </p>
            <p className="text-xs text-surface-400 mt-0.5">
              Comparing peer-reviewed trials against AYUSH/WHO pharmacopoeia guidelines
            </p>
          </div>
        </div>
      )}

      {/* Mutation Error */}
      {mutation.isError && !isTier3Limited && (
        <div className="flex items-center gap-3 rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-xs text-red-300">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>
            {mutation.error?.response?.data?.message ||
              'Failed to verify claim. Please try again.'}
          </span>
        </div>
      )}

      {/* Claim Assessment Result (Slides in on response) */}
      <AnimatePresence mode="wait">
        {currentResponse && !mutation.isPending && (
          <motion.div
            key={currentResponse.query_id}
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -16 }}
          >
            <ClaimAssessmentCard
              assessment={currentResponse.claim_assessment}
              claimText={currentClaimText}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Previous Claims Collapsible History */}
      {claimHistory.length > 0 && (
        <div className="rounded-2xl border border-surface-700/60 bg-surface-800/40 shadow-sm overflow-hidden mt-8">
          <div
            onClick={() => setIsHistoryOpen((v) => !v)}
            data-testid="claim-history-toggle"
            className="flex items-center justify-between p-4 cursor-pointer hover:bg-surface-700/30 transition select-none"
          >
            <div className="flex items-center gap-2">
              <History className="h-4 w-4 text-primary-400" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
                Verified Claims in Current Session ({claimHistory.length})
              </h3>
            </div>
            {isHistoryOpen ? (
              <ChevronUp className="h-4 w-4 text-surface-400" />
            ) : (
              <ChevronDown className="h-4 w-4 text-surface-400" />
            )}
          </div>

          {isHistoryOpen && (
            <div className="border-t border-surface-700/50 p-4 space-y-4 bg-surface-900/40">
              {claimHistory.map((item, idx) => (
                <div
                  key={item.id || idx}
                  className="rounded-xl border border-surface-700/50 bg-surface-800/60 p-4 space-y-3"
                >
                  <p className="text-xs font-bold text-primary-300">
                    "{item.claimText}"
                  </p>
                  <ClaimAssessmentCard
                    assessment={item.response.claim_assessment}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
