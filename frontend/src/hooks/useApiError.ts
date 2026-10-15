/**
 * useApiError — Centralized error handler hook for API requests and TanStack Query.
 *
 * Maps ApiError error_codes to user-friendly translated messages:
 *  - RATE_LIMIT_EXCEEDED     → Rate limit banner message with retry countdown
 *  - ABDM_GATEWAY_UNAVAILABLE → ABHA record access temporarily unavailable prompt
 *  - LLM_PROVIDER_UNAVAILABLE → AI service temporarily unavailable message
 *  - SESSION_NOT_FOUND        → Session expired prompt
 *  - CONSENT_EXPIRED          → ABHA consent expired prompt
 *  - OCR_CONFIDENCE_LOW       → Value confirmation workflow trigger
 */
import { useTranslation } from 'react-i18next'
import { useRateLimitStore } from '@/store/rateLimitStore'
import { toast } from '@/components/ui/useToast'
import type { ApiError } from '@/api/types'
import { RateLimitError } from '@/api/client'

export interface ApiErrorMessageInfo {
  message: string
  code: string
  isOcrLow: boolean
  isRateLimit: boolean
  retryAfter?: number
}

export function getErrorMessage(
  error: unknown,
  t: (key: string, options?: any) => string
): ApiErrorMessageInfo {
  if (!error) {
    return {
      message: t('errors.generic', { defaultValue: 'Something went wrong. Please try again.' }),
      code: 'UNKNOWN',
      isOcrLow: false,
      isRateLimit: false,
    }
  }

  // 1. Check if instance of RateLimitError
  if (error instanceof RateLimitError || (error as any)?.isRateLimitError) {
    const rlError = error as RateLimitError
    const retryAfter = rlError.retry_after ?? 60
    return {
      message: t('rate_limit.banner', {
        defaultValue: `You've reached your ${rlError.endpoint || 'request'} limit. Please wait ${retryAfter}s before trying again.`,
        endpoint: rlError.endpoint || 'request',
        countdown: `${retryAfter}s`,
      }),
      code: 'RATE_LIMIT_EXCEEDED',
      isOcrLow: false,
      isRateLimit: true,
      retryAfter,
    }
  }

  // 2. Check Axios error response data
  const axiosError = error as { response?: { data?: ApiError; status?: number } }
  const apiData = axiosError.response?.data
  const errorCode = apiData?.error_code || ''

  switch (errorCode) {
    case 'RATE_LIMIT_EXCEEDED': {
      const seconds = apiData?.retry_after_seconds ?? 60
      return {
        message: t('query.rate_limited', {
          defaultValue: `You've reached your question limit. Try again in ${seconds} seconds.`,
          countdown: seconds,
        }),
        code: errorCode,
        isOcrLow: false,
        isRateLimit: true,
        retryAfter: seconds,
      }
    }
    case 'ABDM_GATEWAY_UNAVAILABLE':
      return {
        message: t('errors.abdm_unavailable', {
          defaultValue: 'ABHA record access is temporarily unavailable. Please upload a PDF instead.',
        }),
        code: errorCode,
        isOcrLow: false,
        isRateLimit: false,
      }
    case 'LLM_PROVIDER_UNAVAILABLE':
      return {
        message: t('errors.llm_unavailable', {
          defaultValue: 'Our AI service is temporarily unavailable. Please try again in a few minutes.',
        }),
        code: errorCode,
        isOcrLow: false,
        isRateLimit: false,
      }
    case 'SESSION_NOT_FOUND':
      return {
        message: t('errors.session_expired', {
          defaultValue: 'Your session has expired. Please start a new session.',
        }),
        code: errorCode,
        isOcrLow: false,
        isRateLimit: false,
      }
    case 'CONSENT_EXPIRED':
      return {
        message: t('errors.consent_expired', {
          defaultValue: 'Your ABHA consent has expired. Please reconnect your ABHA account.',
        }),
        code: errorCode,
        isOcrLow: false,
        isRateLimit: false,
      }
    case 'OCR_CONFIDENCE_LOW':
      return {
        message: t('report.confirm_value', {
          defaultValue: 'We read this value with low confidence. Please verify.',
        }),
        code: errorCode,
        isOcrLow: true,
        isRateLimit: false,
      }
    default:
      if (apiData?.message) {
        return {
          message: apiData.message,
          code: errorCode || 'API_ERROR',
          isOcrLow: false,
          isRateLimit: false,
        }
      }
      return {
        message: t('errors.generic', { defaultValue: 'Something went wrong. Please try again.' }),
        code: errorCode || 'GENERIC_ERROR',
        isOcrLow: false,
        isRateLimit: false,
      }
  }
}

export function useApiError() {
  const { t } = useTranslation()
  const getRateLimit = useRateLimitStore(s => s.getRateLimit)

  const handleError = (error: unknown, customFallback?: string): string => {
    const { message, code, isOcrLow, isRateLimit } = getErrorMessage(error, t)

    if (code === 'RATE_LIMIT_EXCEEDED' || isRateLimit) {
      toast.rateLimit({
        title: 'Rate Limit Reached',
        description: message,
      })
    } else if (code === 'OCR_CONFIDENCE_LOW' || isOcrLow) {
      toast.warning({
        title: 'Confirmation Needed',
        description: message,
      })
    } else {
      toast.error({
        title: 'Error',
        description: customFallback || message,
      })
    }

    return message
  }

  return {
    handleError,
    getErrorMessage: (err: unknown) => getErrorMessage(err, t),
    getRateLimit,
  }
}

export default useApiError
