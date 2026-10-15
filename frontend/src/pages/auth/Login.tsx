/**
 * Login page
 *
 * Backend integration (AuthController.java):
 *  POST /auth/login → { access_token, refresh_token, expires_in, user_id }
 *
 * Error handling from GlobalExceptionHandler:
 *  401 SecurityException("Account locked...") → show account-locked state
 *  401 SecurityException("Invalid email...")  → show invalid credentials
 *  429 RATE_LIMIT_EXCEEDED                   → show RateLimitBanner with countdown
 *
 * Rate limit: 10/hr/IP (Tier 1)
 */
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, Lock, Eye, EyeOff, AlertCircle, ShieldAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AxiosError } from 'axios'

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useLanguageStore } from '@/store/languageStore'
import type { ApiError } from '@/api/types'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Disclaimer from '@/components/layout/Disclaimer'

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})
type FormData = z.infer<typeof schema>

type UiState = 'idle' | 'loading' | 'error' | 'account-locked' | 'rate-limited'

export default function Login() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const { loginSuccess, incrementFailedAttempts } = useAuthStore()
  const { setLanguage } = useLanguageStore()

  const [uiState, setUiState] = useState<UiState>('idle')
  const [errorMsg, setErrorMsg] = useState('')
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const [showPassword, setShowPassword] = useState(false)

  const returnUrl = (location.state as { returnUrl?: string } | null)?.returnUrl ?? '/dashboard'

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const onSubmit = async (data: FormData) => {
    setUiState('loading')
    setErrorMsg('')
    try {
      const response = await authApi.login({ email: data.email, password: data.password })
      const auth = response.data

      // loginSuccess stores tokens in memory only
      loginSuccess(auth, data.email, 'en')
      // Sync i18n with user's preferred language (will be updated after profile fetch)
      setLanguage('en')

      navigate(returnUrl, { replace: true })
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>
      const status = axiosErr.response?.status
      const body = axiosErr.response?.data

      if (status === 429) {
        setRateLimitSeconds(body?.retry_after_seconds ?? 60)
        setUiState('rate-limited')
        return
      }

      if (status === 401) {
        const msg = body?.message ?? ''
        if (msg.toLowerCase().includes('locked')) {
          setUiState('account-locked')
          return
        }
        incrementFailedAttempts()
        setErrorMsg(t('auth.login.error_invalid_credentials'))
        setUiState('error')
        return
      }

      setErrorMsg(t('auth.login.error_generic'))
      setUiState('error')
    }
  }

  const isDisabled = uiState === 'loading' || uiState === 'account-locked' || uiState === 'rate-limited'

  return (
    <div className="min-h-screen bg-surface-900 bg-gradient-mesh flex flex-col items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-md"
      >
        {/* Logo */}
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-primary shadow-glow">
            <ShieldAlert className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">{t('auth.login.title')}</h1>
          <p className="mt-1 text-sm text-surface-600">{t('auth.login.subtitle')}</p>
        </div>

        {/* Card */}
        <div className="rounded-2xl border border-surface-700/50 bg-surface-800/60 p-8 shadow-glass backdrop-blur-sm">
          {/* Rate limit banner */}
          {uiState === 'rate-limited' && (
            <div className="mb-6">
              <RateLimitBanner
                retryAfterSeconds={rateLimitSeconds}
                onExpired={() => setUiState('idle')}
              />
            </div>
          )}

          {/* Account locked */}
          {uiState === 'account-locked' && (
            <div className="mb-6 flex items-start gap-3 rounded-xl border border-health-abnormal/30 bg-health-abnormal/10 px-4 py-3 text-sm text-health-abnormal">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <p>{t('auth.login.error_account_locked')}</p>
            </div>
          )}

          {/* Generic error */}
          {uiState === 'error' && errorMsg && (
            <div className="mb-6 flex items-start gap-3 rounded-xl border border-health-abnormal/30 bg-health-abnormal/10 px-4 py-3 text-sm text-health-abnormal">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <p>{errorMsg}</p>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
            {/* Email */}
            <div>
              <label htmlFor="login-email" className="mb-1.5 block text-sm font-medium text-slate-300">
                {t('auth.login.email_label')}
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
                <input
                  id="login-email"
                  type="email"
                  autoComplete="email"
                  placeholder={t('auth.login.email_placeholder')}
                  disabled={isDisabled}
                  {...register('email')}
                  className="w-full rounded-xl border border-surface-700 bg-surface-900/50 py-2.5 pl-10 pr-4 text-sm text-white placeholder-surface-600 outline-none transition focus:border-primary-500 focus:ring-1 focus:ring-primary-500/30 disabled:opacity-50"
                />
              </div>
              {errors.email && (
                <p className="mt-1.5 text-xs text-health-abnormal">{errors.email.message}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label htmlFor="login-password" className="mb-1.5 block text-sm font-medium text-slate-300">
                {t('auth.login.password_label')}
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder={t('auth.login.password_placeholder')}
                  disabled={isDisabled}
                  {...register('password')}
                  className="w-full rounded-xl border border-surface-700 bg-surface-900/50 py-2.5 pl-10 pr-10 text-sm text-white placeholder-surface-600 outline-none transition focus:border-primary-500 focus:ring-1 focus:ring-primary-500/30 disabled:opacity-50"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-600 hover:text-slate-300 transition"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1.5 text-xs text-health-abnormal">{errors.password.message}</p>
              )}
            </div>

            {/* Submit */}
            <button
              id="login-submit"
              type="submit"
              disabled={isDisabled}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-primary py-2.5 text-sm font-semibold text-white shadow-glow transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {uiState === 'loading' ? (
                <>
                  <LoadingSpinner size="sm" />
                  {t('auth.login.loading')}
                </>
              ) : (
                t('auth.login.submit')
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-surface-600">
            {t('auth.login.no_account')}{' '}
            <Link to="/register" className="font-medium text-primary-400 hover:text-primary-300 transition">
              {t('auth.login.register_link')}
            </Link>
          </p>
        </div>
      </motion.div>

      <Disclaimer />
    </div>
  )
}
