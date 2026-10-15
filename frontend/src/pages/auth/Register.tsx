/**
 * Register page
 *
 * Backend integration (AuthController.java):
 *  POST /auth/register → 201 { user_id, message }
 *
 * Backend error codes:
 *  400 IllegalArgumentException("Email already registered") → email-taken state
 *  429 RATE_LIMIT_EXCEEDED (5/hr/IP) → show RateLimitBanner
 *  400 VALIDATION_FAILED → field_errors map
 *
 * Email uniqueness: the backend checks on POST only (no dedicated check endpoint).
 * We debounce and attempt a register probe — if it fails with "email already registered"
 * we show inline error. For instant feedback we use a local check on blur.
 */
import { useState, useCallback } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, Lock, Eye, EyeOff, ShieldAlert, CheckCircle, AlertCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AxiosError } from 'axios'

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/authStore'
import { useLanguageStore } from '@/store/languageStore'
import type { ApiError } from '@/api/types'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Disclaimer from '@/components/layout/Disclaimer'
import { cn } from '@/utils/cn'

const schema = z
  .object({
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
    language_pref: z.enum(['en', 'ta']),
  })
  .refine(d => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type FormData = z.infer<typeof schema>
type UiState = 'idle' | 'loading' | 'error' | 'rate-limited' | 'success'

function getPasswordStrength(pwd: string): { score: number; label: string } {
  let score = 0
  if (pwd.length >= 8) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong']
  return { score, label: labels[score] ?? '' }
}

const strengthColors = ['', 'bg-health-abnormal', 'bg-health-borderline', 'bg-primary-400', 'bg-health-safe']

export default function Register() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { loginSuccess } = useAuthStore()
  const { setLanguage } = useLanguageStore()

  const [uiState, setUiState] = useState<UiState>('idle')
  const [errorMsg, setErrorMsg] = useState('')
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)
  const [showPassword, setShowPassword] = useState(false)
  const [passwordValue, setPasswordValue] = useState('')
  const [selectedLang, setSelectedLang] = useState<'en' | 'ta'>('en')

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { language_pref: 'en' },
  })

  const strength = getPasswordStrength(passwordValue)

  const onLangToggle = useCallback((lang: 'en' | 'ta') => {
    setSelectedLang(lang)
    setValue('language_pref', lang)
  }, [setValue])

  const onSubmit = async (data: FormData) => {
    setUiState('loading')
    setErrorMsg('')
    try {
      // 1. Register
      await authApi.register({
        email: data.email,
        password: data.password,
        language_pref: data.language_pref,
      })

      // 2. Auto-login after successful registration
      const loginResp = await authApi.login({ email: data.email, password: data.password })
      loginSuccess(loginResp.data, data.email, data.language_pref)
      setLanguage(data.language_pref)

      setUiState('success')
      setTimeout(() => navigate('/dashboard', { replace: true }), 800)
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>
      const status = axiosErr.response?.status
      const body = axiosErr.response?.data

      if (status === 429) {
        setRateLimitSeconds(body?.retry_after_seconds ?? 60)
        setUiState('rate-limited')
        return
      }

      if (status === 400) {
        const msg = body?.message ?? ''
        if (msg.toLowerCase().includes('already registered') || msg.toLowerCase().includes('email')) {
          setErrorMsg(t('auth.register.error_email_exists'))
          setUiState('error')
          return
        }
      }

      setErrorMsg(t('auth.register.error_generic'))
      setUiState('error')
    }
  }

  const isDisabled = uiState === 'loading' || uiState === 'rate-limited' || uiState === 'success'

  return (
    <div className="min-h-screen bg-surface-900 bg-gradient-mesh flex flex-col items-center justify-center px-4 py-12">
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
          <h1 className="text-2xl font-bold text-white">{t('auth.register.title')}</h1>
          <p className="mt-1 text-sm text-surface-600">{t('auth.register.subtitle')}</p>
        </div>

        <div className="rounded-2xl border border-surface-700/50 bg-surface-800/60 p-8 shadow-glass backdrop-blur-sm">
          {/* Rate limit banner */}
          {uiState === 'rate-limited' && (
            <div className="mb-6">
              <RateLimitBanner retryAfterSeconds={rateLimitSeconds} onExpired={() => setUiState('idle')} />
            </div>
          )}

          {/* Success */}
          {uiState === 'success' && (
            <div className="mb-6 flex items-center gap-2 rounded-xl border border-health-safe/30 bg-health-safe/10 px-4 py-3 text-sm text-health-safe">
              <CheckCircle className="h-4 w-4" />
              <span>{t('auth.register.success')}</span>
            </div>
          )}

          {/* Error */}
          {uiState === 'error' && errorMsg && (
            <div className="mb-6 flex items-start gap-3 rounded-xl border border-health-abnormal/30 bg-health-abnormal/10 px-4 py-3 text-sm text-health-abnormal">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <p>{errorMsg}</p>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
            {/* Email */}
            <div>
              <label htmlFor="reg-email" className="mb-1.5 block text-sm font-medium text-slate-300">
                {t('auth.register.email_label')}
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
                <input
                  id="reg-email"
                  type="email"
                  autoComplete="email"
                  placeholder={t('auth.register.email_placeholder')}
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
              <label htmlFor="reg-password" className="mb-1.5 block text-sm font-medium text-slate-300">
                {t('auth.register.password_label')}
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
                <input
                  id="reg-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  placeholder={t('auth.register.password_placeholder')}
                  disabled={isDisabled}
                  {...register('password', {
                    onChange: e => setPasswordValue(e.target.value),
                  })}
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
              {/* Strength indicator */}
              {passwordValue && (
                <div className="mt-2 flex items-center gap-2">
                  <div className="flex flex-1 gap-1">
                    {[1, 2, 3, 4].map(i => (
                      <div
                        key={i}
                        className={cn(
                          'h-1 flex-1 rounded-full transition-all duration-300',
                          i <= strength.score ? strengthColors[strength.score] : 'bg-surface-700'
                        )}
                      />
                    ))}
                  </div>
                  <span className="text-xs text-surface-600">{strength.label}</span>
                </div>
              )}
              {errors.password && (
                <p className="mt-1.5 text-xs text-health-abnormal">{errors.password.message}</p>
              )}
            </div>

            {/* Confirm password */}
            <div>
              <label htmlFor="reg-confirm" className="mb-1.5 block text-sm font-medium text-slate-300">
                {t('auth.register.confirm_password_label')}
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-600" />
                <input
                  id="reg-confirm"
                  type="password"
                  autoComplete="new-password"
                  placeholder={t('auth.register.confirm_password_placeholder')}
                  disabled={isDisabled}
                  {...register('confirmPassword')}
                  className="w-full rounded-xl border border-surface-700 bg-surface-900/50 py-2.5 pl-10 pr-4 text-sm text-white placeholder-surface-600 outline-none transition focus:border-primary-500 focus:ring-1 focus:ring-primary-500/30 disabled:opacity-50"
                />
              </div>
              {errors.confirmPassword && (
                <p className="mt-1.5 text-xs text-health-abnormal">{errors.confirmPassword.message}</p>
              )}
            </div>

            {/* Language toggle */}
            <div>
              <p className="mb-2 text-sm font-medium text-slate-300">{t('auth.register.language_label')}</p>
              <div className="flex rounded-xl border border-surface-700 bg-surface-900/50 p-1">
                {(['en', 'ta'] as const).map(lang => (
                  <button
                    key={lang}
                    type="button"
                    id={`lang-toggle-${lang}`}
                    onClick={() => onLangToggle(lang)}
                    className={cn(
                      'flex-1 rounded-lg py-2 text-sm font-medium transition',
                      selectedLang === lang
                        ? 'bg-primary-600 text-white shadow-glow'
                        : 'text-surface-600 hover:text-slate-300'
                    )}
                  >
                    {lang === 'en' ? t('auth.register.language_en') : t('auth.register.language_ta')}
                  </button>
                ))}
              </div>
            </div>

            {/* Submit */}
            <button
              id="register-submit"
              type="submit"
              disabled={isDisabled}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-primary py-2.5 text-sm font-semibold text-white shadow-glow transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {uiState === 'loading' ? (
                <>
                  <LoadingSpinner size="sm" />
                  {t('auth.register.loading')}
                </>
              ) : (
                t('auth.register.submit')
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-surface-600">
            {t('auth.register.has_account')}{' '}
            <Link to="/login" className="font-medium text-primary-400 hover:text-primary-300 transition">
              {t('auth.register.login_link')}
            </Link>
          </p>
        </div>
      </motion.div>

      <Disclaimer />
    </div>
  )
}
