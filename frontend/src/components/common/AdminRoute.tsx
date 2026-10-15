import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { ShieldAlert } from 'lucide-react'

interface AdminRouteProps {
  children: ReactNode
}

export function AdminRoute({ children }: AdminRouteProps) {
  const { is_authenticated, user } = useAuthStore()
  const location = useLocation()
  const adminKey = localStorage.getItem('medisense_admin_key')

  // Check if authenticated, has is_admin flag (or valid email pattern/admin status) AND valid admin key stored locally
  const isAdmin = is_authenticated && (user?.is_admin || user?.email?.includes('admin')) && !!adminKey

  if (!isAdmin) {
    return <Navigate to="/dashboard" state={{ from: location }} replace />
  }

  return (
    <div className="flex flex-col min-h-screen">
      <div className="bg-amber-500/10 border-b border-amber-500/20 px-4 py-2 text-xs text-amber-300 flex items-center justify-center gap-2">
        <ShieldAlert className="h-3.5 w-3.5 text-amber-400 shrink-0" />
        <span>You are accessing admin functions. Do not use this on a shared device.</span>
      </div>
      <div className="flex-1">
        {children}
      </div>
    </div>
  )
}

export default AdminRoute
