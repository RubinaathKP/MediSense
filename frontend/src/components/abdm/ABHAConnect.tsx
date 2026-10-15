/**
 * ABHAConnect — multi-step ABDM record pull form
 *
 * Backend: POST /reports/abdm-pull { session_id, abha_address, record_types, date_from, date_to }
 * Rate limit: 5/hr/user (Tier 2)
 *
 * ABHA address format: username@abdm (validated on frontend before submit)
 */
import { useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { motion } from 'framer-motion'
import { User, Calendar, FileText, Pill, AlertCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AxiosError } from 'axios'
import { format, subMonths } from 'date-fns'

import { reportsApi } from '@/api/reports'
import { useSessionStore } from '@/store/sessionStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import type { ApiError } from '@/api/types'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ConsentStatus from './ConsentStatus'

const ABHA_REGEX = /^[a-zA-Z0-9._-]+@abdm$/i

const schema = z.object({
  abha_address: z.string().regex(ABHA_REGEX, 'Format: username@abdm'),
  date_from: z.string().min(1, 'Required'),
  date_to: z.string().min(1, 'Required'),
  record_types: z.array(z.string()).min(1, 'Select at least one record type'),
})
type FormData = z.infer<typeof schema>

interface Props {
  onRecordReceived: () => void
  onFallbackToPdf: () => void
}

export default function ABHAConnect({ onRecordReceived, onFallbackToPdf }: Props) {
  const { t } = useTranslation()
  const { session_id, setConsentRequest } = useSessionStore()
  const abdmRateLimit = useRateLimit('reports-abdm-pull')

  const [submitting, setSubmitting] = useState(false)
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const [showRateLimit, setShowRateLimit] = useState(false)
  const [error, setError] = useState('')
  const [consentInitiated, setConsentInitiated] = useState(false)

  const defaultFrom = format(subMonths(new Date(), 6), 'yyyy-MM-dd')
  const defaultTo   = format(new Date(), 'yyyy-MM-dd')

  const { register, handleSubmit, control, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      abha_address: '',
      date_from: defaultFrom,
      date_to: defaultTo,
      record_types: ['DiagnosticReport', 'Observation'],
    },
  })

  const onSubmit = async (data: FormData) => {
    if (abdmRateLimit.is_limited) {
      setRateLimitSeconds(abdmRateLimit.countdown)
      setShowRateLimit(true)
      return
    }
    if (!session_id) { setError(t('errors.session_not_found')); return }

    setSubmitting(true)
    setError('')
    try {
      const response = await reportsApi.abdmPull({
        session_id,
        abha_address: data.abha_address,
        record_types: data.record_types,
        date_from: data.date_from,
        date_to: data.date_to,
      })
      setConsentRequest(response.data.consent_request_id)
      setConsentInitiated(true)
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>
      const status = axiosErr.response?.status
      const body = axiosErr.response?.data

      if (status === 429) {
        setRateLimitSeconds(body?.retry_after_seconds ?? 3600)
        setShowRateLimit(true)
        return
      }
      if (body?.error_code === 'ABDM_GATEWAY_UNAVAILABLE') {
        setError(t('errors.abdm_unavailable'))
        return
      }
      setError(t('errors.generic'))
    } finally {
      setSubmitting(false)
    }
  }

  if (consentInitiated) {
    return (
      <ConsentStatus
        onRecordReceived={onRecordReceived}
        onTimeout={onFallbackToPdf}
      />
    )
  }

  const RECORD_TYPE_OPTIONS = [
    { value: 'DiagnosticReport', label: t('abdm.step3_lab'), icon: FileText },
    { value: 'MedicationRequest', label: t('abdm.step3_prescription'), icon: Pill },
  ]

  return (
    <motion.form
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-5"
      noValidate
    >
      {showRateLimit && (
        <RateLimitBanner retryAfterSeconds={rateLimitSeconds} onExpired={() => setShowRateLimit(false)} />
      )}

      {error && (
        <div className="flex items-start gap-2 rounded-xl border border-health-abnormal/30 bg-health-abnormal/10 px-4 py-3 text-sm text-health-abnormal">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* ABHA address */}
      <div>
        <label htmlFor="abha-address" className="mb-1.5 block text-sm font-medium text-slate-300">
          {t('abdm.step1_label')}
        </label>
        <div className="relative">
          <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
          <input
            id="abha-address"
            type="text"
            placeholder={t('abdm.step1_placeholder')}
            {...register('abha_address')}
            className="w-full rounded-xl border border-surface-700 bg-surface-900/50 py-2.5 pl-10 pr-4 text-sm text-white placeholder-surface-600 outline-none focus:border-primary-500"
          />
        </div>
        <p className="mt-1 text-xs text-surface-600">{t('abdm.step1_hint')}</p>
        {errors.abha_address && (
          <p className="mt-1 text-xs text-health-abnormal">{errors.abha_address.message}</p>
        )}
      </div>

      {/* Date range */}
      <div className="grid grid-cols-2 gap-3">
        {(['date_from', 'date_to'] as const).map((field, i) => (
          <div key={field}>
            <label htmlFor={field} className="mb-1.5 block text-sm font-medium text-slate-300">
              {i === 0 ? t('abdm.step2_from') : t('abdm.step2_to')}
            </label>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
              <input
                id={field}
                type="date"
                {...register(field)}
                className="w-full rounded-xl border border-surface-700 bg-surface-900/50 py-2.5 pl-10 pr-4 text-sm text-white outline-none focus:border-primary-500"
              />
            </div>
            {errors[field] && (
              <p className="mt-1 text-xs text-health-abnormal">{errors[field]?.message}</p>
            )}
          </div>
        ))}
      </div>

      {/* Record types */}
      <div>
        <p className="mb-2 text-sm font-medium text-slate-300">{t('abdm.step3_title')}</p>
        <Controller
          name="record_types"
          control={control}
          render={({ field }) => (
            <div className="flex gap-3">
              {RECORD_TYPE_OPTIONS.map(({ value, label, icon: Icon }) => {
                const checked = field.value.includes(value)
                return (
                  <button
                    key={value}
                    id={`abdm-record-type-${value}`}
                    type="button"
                    onClick={() => {
                      const next = checked
                        ? field.value.filter(v => v !== value)
                        : [...field.value, value]
                      field.onChange(next)
                    }}
                    className={`flex flex-1 items-center gap-2 rounded-xl border py-2.5 px-4 text-sm font-medium transition ${
                      checked
                        ? 'border-primary-500 bg-primary-600/15 text-primary-300'
                        : 'border-surface-700 text-surface-600 hover:border-primary-500 hover:text-slate-300'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    {label}
                  </button>
                )
              })}
            </div>
          )}
        />
        {errors.record_types && (
          <p className="mt-1 text-xs text-health-abnormal">{errors.record_types.message}</p>
        )}
      </div>

      <button
        id="abdm-submit-button"
        type="submit"
        disabled={submitting || showRateLimit}
        className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-primary py-2.5 text-sm font-semibold text-white shadow-glow hover:opacity-90 disabled:opacity-50 transition"
      >
        {submitting ? <LoadingSpinner size="sm" /> : null}
        {t('abdm.submit')}
      </button>

      {!abdmRateLimit.is_limited && abdmRateLimit.remaining < Infinity && (
        <p className="text-center text-xs text-surface-600">
          {t('rate_limit.abdm_remaining', { remaining: abdmRateLimit.remaining })}
        </p>
      )}
    </motion.form>
  )
}
