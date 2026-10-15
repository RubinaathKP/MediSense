import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Bell, User, LogOut, Settings } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import LanguageToggle from '@/components/common/LanguageToggle'

export default function TopBar() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  const [isDropdownOpen, setIsDropdownOpen] = useState(false)

  // Rate limit awareness for notification badge
  const queryLimit = useRateLimit('query')
  const isNearLimit = queryLimit.remaining < 5 && queryLimit.remaining > 0

  // Dynamic page title based on path
  const getPageTitle = (pathname: string) => {
    switch (pathname) {
      case '/dashboard':
        return 'Overview'
      case '/report':
        return 'Understand My Report'
      case '/query':
        return 'Ask MediSense'
      case '/claims':
        return 'Verify a Health Claim'
      case '/history':
        return 'Session Query History'
      default:
        return 'MediSense'
    }
  }

  const pageTitle = getPageTitle(location.pathname)

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-surface-700/60 bg-surface-900/80 px-6 backdrop-blur-md">
      {/* Left: Dynamic Page Title */}
      <h2
        data-testid="topbar-page-title"
        className="text-base font-bold text-white tracking-wide"
      >
        {pageTitle}
      </h2>

      {/* Center: Empty */}
      <div className="flex-1" />

      {/* Right: Language Toggle, Notification Bell, User Dropdown */}
      <div className="flex items-center gap-4">
        <LanguageToggle variant="compact" />

        {/* Notification Bell */}
        <div className="relative">
          <button
            type="button"
            data-testid="topbar-bell-btn"
            className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-surface-800 text-surface-400 hover:text-white transition"
          >
            <Bell className="h-4 w-4" />
            {isNearLimit && (
              <span
                data-testid="rate-limit-near-badge"
                className="absolute top-1 right-1 h-2 w-2 rounded-full bg-amber-400 animate-ping"
              />
            )}
          </button>
        </div>

        {/* User Avatar Dropdown */}
        <div className="relative">
          <button
            type="button"
            data-testid="user-avatar-btn"
            onClick={() => setIsDropdownOpen((v) => !v)}
            className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-primary text-white font-bold text-xs shadow-glow transition hover:opacity-90"
          >
            {user?.email ? user.email.charAt(0).toUpperCase() : <User className="h-4 w-4" />}
          </button>

          {isDropdownOpen && (
            <div
              data-testid="user-dropdown-menu"
              className="absolute right-0 mt-2 w-48 rounded-2xl border border-surface-700 bg-surface-800 p-2 shadow-2xl backdrop-blur-md animate-scale-in z-50"
            >
              <div className="px-3 py-2 border-b border-surface-700/50 mb-1">
                <p className="text-xs font-semibold text-white truncate">
                  {user?.email || 'Patient'}
                </p>
                <p className="text-[10px] text-surface-400 font-mono mt-0.5">
                  ID: {user?.user_id?.slice(0, 8) || 'Active'}
                </p>
              </div>

              <button
                type="button"
                onClick={() => {
                  setIsDropdownOpen(false)
                  navigate('/dashboard')
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium text-slate-300 hover:bg-surface-700/50 hover:text-white transition"
              >
                <User className="h-3.5 w-3.5 text-primary-400" />
                <span>Profile</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsDropdownOpen(false)
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium text-slate-300 hover:bg-surface-700/50 hover:text-white transition"
              >
                <Settings className="h-3.5 w-3.5 text-surface-400" />
                <span>Settings</span>
              </button>

              <div className="border-t border-surface-700/50 my-1" />

              <button
                type="button"
                data-testid="topbar-logout-btn"
                onClick={async () => {
                  setIsDropdownOpen(false)
                  await logout()
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium text-red-400 hover:bg-red-500/15 transition"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span>Logout</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
