/**
 * ConsentStatus — ABDM consent flow status indicator
 *
 * Backend flow:
 *  1. POST /reports/abdm-pull → status='PENDING_PATIENT_APPROVAL' + consent_request_id
 *  2. Patient approves in their ABHA app
 *  3. Backend receives callback at /api/v1/abdm/callback/consent/on-notify
 *  4. Frontend polls GET /reports/{session_id} every 5 seconds (max 30 polls = 2.5 min)
 *
 * On timeout: offer PDF upload fallback (NFR-12, FR-15)
 */
import { useEffect, useRef } from 'react'
import { motion } from 'framer-motion'
import { Loader2, CheckCircle, XCircle, RefreshCw } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import { reportsApi } from '@/api/reports'
import { useSessionStore } from '@/store/sessionStore'

const POLL_INTERVAL_MS = 5_000
const MAX_POLLS = 30   // 30 × 5s = 2.5 minutes

interface Props {
  onRecordReceived: () => void
  onTimeout: () => void
}

export default function ConsentStatus({ onRecordReceived, onTimeout }: Props) {
  const { t } = useTranslation()
  const { session_id, consent_status, setConsentStatus, setReportLoaded } = useSessionStore()
  const pollCountRef = useRef(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (!session_id || consent_status !== 'pending') return

    timerRef.current = setInterval(async () => {
      pollCountRef.current++

      if (pollCountRef.current > MAX_POLLS) {
        clearInterval(timerRef.current!)
        setConsentStatus('timeout')
        onTimeout()
        return
      }

      try {
        const response = await reportsApi.getReportDetails(session_id)
        const { lab_values, prescriptions } = response.data

        // If data arrived — record is now available
        if (lab_values.length > 0 || prescriptions.length > 0) {
          clearInterval(timerRef.current!)
          setReportLoaded(
            session_id,
            prescriptions.length > 0 ? 'prescription' : 'lab_report',
            lab_values,
            prescriptions
          )
          onRecordReceived()
        }
      } catch {
        // SESSION_NOT_FOUND or network error — continue polling
      }
    }, POLL_INTERVAL_MS)

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [session_id, consent_status]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="space-y-4 text-center">
      {consent_status === 'pending' && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex flex-col items-center gap-4 py-8"
        >
          <Loader2 className="h-12 w-12 text-primary-500 animate-spin" />
          <div>
            <p className="font-semibold text-white">{t('abdm.consent_sent_title')}</p>
            <p className="mt-1 text-sm text-surface-600">{t('abdm.consent_sent_body')}</p>
          </div>
          <p className="text-xs text-surface-600 animate-pulse">{t('abdm.consent_waiting')}</p>
          <p className="text-xs text-surface-600">
            {Math.min(pollCountRef.current * 5, MAX_POLLS * 5)} / {MAX_POLLS * 5}s elapsed
          </p>
        </motion.div>
      )}

      {consent_status === 'granted' && (
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="flex flex-col items-center gap-4 py-8"
        >
          <CheckCircle className="h-12 w-12 text-health-safe" />
          <p className="font-semibold text-white">{t('abdm.consent_sent_title')}</p>
        </motion.div>
      )}

      {(consent_status === 'timeout' || consent_status === 'denied') && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center gap-4 py-8"
        >
          <XCircle className="h-12 w-12 text-health-abnormal" />
          <div>
            <p className="font-semibold text-white">{t('abdm.consent_timeout_title')}</p>
            <p className="mt-1 text-sm text-surface-600">{t('abdm.consent_timeout_body')}</p>
          </div>
          <div className="flex gap-3">
            <button
              id="abdm-retry-button"
              type="button"
              onClick={() => { pollCountRef.current = 0; setConsentStatus('pending') }}
              className="flex items-center gap-2 rounded-xl border border-primary-600 px-4 py-2 text-sm font-medium text-primary-400 hover:bg-primary-600/10 transition"
            >
              <RefreshCw className="h-4 w-4" />
              {t('abdm.consent_retry')}
            </button>
            <button
              id="abdm-use-pdf-button"
              type="button"
              onClick={onTimeout}
              className="rounded-xl border border-surface-600 px-4 py-2 text-sm font-medium text-surface-600 hover:text-slate-300 transition"
            >
              {t('abdm.consent_use_pdf')}
            </button>
          </div>
        </motion.div>
      )}
    </div>
  )
}
