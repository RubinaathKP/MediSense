import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  FileText,
  MessageCircle,
  ShieldCheck,
  History,
  Settings,
  LogOut,
  ShieldAlert,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import LanguageToggle from '@/components/common/LanguageToggle'
import { cn } from '@/utils/cn'

const primaryNav = [
  { name: 'Dashboard', icon: LayoutDashboard, to: '/dashboard' },
  { name: 'My Report', icon: FileText, to: '/report' },
  { name: 'Ask a Question', icon: MessageCircle, to: '/query' },
  { name: 'Verify a Claim', icon: ShieldCheck, to: '/claims' },
  { name: 'History', icon: History, to: '/history' },
]

export default function Sidebar() {
  const { logout, user } = useAuthStore()

  const handleLogout = async () => {
    await logout()
  }

  return (
    <>
      {/* Desktop Sidebar (hidden on mobile, md:flex) */}
      <aside
        data-testid="sidebar-desktop"
        className="hidden md:flex h-screen w-64 flex-col justify-between border-r border-surface-700/60 bg-surface-900/95 p-4 shadow-xl backdrop-blur-md"
      >
        <div className="space-y-6">
          {/* Logo */}
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-primary shadow-glow">
              <ShieldAlert className="h-6 w-6 text-white" />
            </div>
            <div>
              <span className="text-lg font-bold text-white tracking-tight">MediSense</span>
              <p className="text-[10px] font-semibold text-primary-400 uppercase tracking-widest">
                Health AI Layer
              </p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="space-y-1.5" data-testid="sidebar-nav">
            {primaryNav.map(({ name, icon: Icon, to }) => (
              <NavLink
                key={to}
                to={to}
                data-testid={`sidebar-link-${to.replace('/', '')}`}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all duration-150',
                    isActive
                      ? 'bg-gradient-primary text-white shadow-glow'
                      : 'text-surface-400 hover:bg-surface-800/60 hover:text-slate-200'
                  )
                }
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span>{name}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Bottom Section */}
        <div className="space-y-4 border-t border-surface-700/60 pt-4">
          <div className="space-y-1">
            <button
              type="button"
              className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2 text-xs font-medium text-surface-400 hover:bg-surface-800/60 hover:text-slate-200 transition"
            >
              <Settings className="h-4 w-4" />
              <span>Settings</span>
            </button>

            <button
              type="button"
              data-testid="sidebar-logout-btn"
              onClick={handleLogout}
              className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2 text-xs font-medium text-red-400/90 hover:bg-red-500/10 hover:text-red-300 transition"
            >
              <LogOut className="h-4 w-4" />
              <span>Logout</span>
            </button>
          </div>

          {/* Bottom language toggle */}
          <div className="px-1">
            <LanguageToggle variant="full" />
          </div>

          {/* User info snippet */}
          {user?.email && (
            <div className="rounded-xl border border-surface-700/40 bg-surface-800/30 p-2.5">
              <p className="text-[11px] font-medium text-slate-300 truncate">
                {user.email}
              </p>
            </div>
          )}
        </div>
      </aside>

      {/* Mobile Bottom Navigation Bar (md:hidden) */}
      <nav
        data-testid="sidebar-mobile-bottom"
        className="fixed bottom-0 left-0 right-0 z-40 flex md:hidden h-16 items-center justify-around border-t border-surface-700/80 bg-surface-900/95 backdrop-blur-lg px-2"
      >
        {primaryNav.map(({ name, icon: Icon, to }) => (
          <NavLink
            key={to}
            to={to}
            data-testid={`mobile-nav-${to.replace('/', '')}`}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center justify-center gap-1 rounded-xl p-2 text-[10px] font-medium transition-all',
                isActive ? 'text-primary-400' : 'text-surface-400 hover:text-slate-200'
              )
            }
          >
            <Icon className="h-5 w-5" />
            <span>{name.split(' ')[0]}</span>
          </NavLink>
        ))}
      </nav>
    </>
  )
}
