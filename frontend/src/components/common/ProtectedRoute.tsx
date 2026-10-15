/**
 * ProtectedRoute — guards routes requiring authentication.
 *
 * Behaviour:
 *  1. If is_authenticated → render children
 *  2. If not authenticated but refresh_token present → attempt refresh first
 *  3. If refresh fails or no tokens → redirect to /login with returnUrl in state
 *
 * Integrates with useTokenRefresh to handle page-refresh scenario where
 * in-memory access_token is gone but httpOnly cookie may still be valid.
 */
import { Navigate, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import LoadingSpinner from '@/components/common/LoadingSpinner'

interface Props {
  children: React.ReactNode
  requireAdmin?: boolean
}

export function ProtectedRoute({ children, requireAdmin = false }: Props) {
  const location = useLocation()
  const { is_authenticated, refresh_token, refreshToken, user } = useAuthStore()
  const [isChecking, setIsChecking] = useState(!is_authenticated && !!refresh_token)

  useEffect(() => {
    if (!is_authenticated && refresh_token) {
      setIsChecking(true)
      refreshToken()
        .catch(() => {})
        .finally(() => setIsChecking(false))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Show spinner while attempting token refresh
  if (isChecking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface-900">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!is_authenticated) {
    return <Navigate to="/login" state={{ returnUrl: location.pathname }} replace />
  }

  // Admin gate — backend uses ROLE_ADMIN on /internal/** routes
  // The frontend checks user metadata (admin flag not yet in AuthUser — use a separate
  // admin token approach or a dedicated field. For now, admin routes use X-Admin-Key header.)
  if (requireAdmin && !user) {
    return <Navigate to="/dashboard" replace />
  }

  return <>{children}</>
}
