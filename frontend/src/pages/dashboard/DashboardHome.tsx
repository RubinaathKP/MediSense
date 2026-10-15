import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  FileText,
  ShieldCheck,
  Clock,
  ArrowRight,
  Sparkles,
} from 'lucide-react'
import { motion } from 'framer-motion'
import { useAuthStore } from '@/store/authStore'
import { useSessionStore } from '@/store/sessionStore'
import { useQueryHistory } from '@/hooks/useQueryHistory'
import RateLimitWidget from '@/components/dashboard/RateLimitWidget'
import QueryResponseCard from '@/components/query/QueryResponseCard'

export default function DashboardHome() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { session_id, report_loaded, lab_values, prescriptions } = useSessionStore()
  const { data: history = [] } = useQueryHistory(session_id)

  // Time-based personalized greeting
  const greeting = useMemo(() => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good morning'
    if (hour < 17) return 'Good afternoon'
    return 'Good evening'
  }, [])

  const displayName = user?.email ? user.email.split('@')[0] : 'Patient'

  // Quick Action Cards
  const quickActions = [
    {
      id: 'quick-action-report',
      title: 'Understand My Report',
      description: 'Upload a lab report or prescription for a plain-language explanation',
      icon: FileText,
      to: '/report',
      color: 'from-teal-500/20 to-emerald-500/10 border-teal-500/30 text-teal-400',
    },
    {
      id: 'quick-action-claim',
      title: 'Verify a Health Claim',
      description: 'Check whether a health tip or remedy is supported by verified guidelines',
      icon: ShieldCheck,
      to: '/claims',
      color: 'from-blue-500/20 to-indigo-500/10 border-blue-500/30 text-blue-400',
    },
    {
      id: 'quick-action-history',
      title: 'My Question History',
      description: 'Review your previous questions and verified source citations',
      icon: Clock,
      to: '/history',
      color: 'from-amber-500/20 to-orange-500/10 border-amber-500/30 text-amber-400',
    },
  ]

  // Recent activity (last 3 queries)
  const recentQueries = history.slice(0, 3)

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Personalized Hero Greeting */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-3xl border border-surface-700/60 bg-gradient-card p-6 md:p-8 shadow-glass">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary-400" />
            <span className="text-xs font-bold uppercase tracking-wider text-primary-300">
              Personal Health Hub
            </span>
          </div>
          <h1
            data-testid="dashboard-greeting"
            className="text-2xl md:text-3xl font-extrabold text-white tracking-tight"
          >
            {greeting}, <span className="text-primary-300 capitalize">{displayName}</span>
          </h1>
          <p className="text-xs md:text-sm text-surface-400 max-w-xl leading-relaxed">
            MediSense translates complex medical documents, prescription instructions, and health tips into clear, cited knowledge.
          </p>
        </div>

        {report_loaded && (
          <div className="rounded-2xl border border-primary-500/30 bg-primary-600/10 p-4 shrink-0 text-left sm:text-right">
            <p className="text-xs font-semibold text-primary-300">Active Document</p>
            <p className="text-sm font-bold text-white mt-0.5">
              {lab_values.length} Lab Values · {prescriptions.length} Prescriptions
            </p>
            <button
              type="button"
              onClick={() => navigate('/report')}
              className="mt-2 text-xs text-primary-400 hover:text-primary-300 font-semibold inline-flex items-center gap-1"
            >
              Review Report <ArrowRight className="h-3 w-3" />
            </button>
          </div>
        )}
      </div>

      {/* Grid: Quick Actions (Left 2 cols on lg) + Rate Limit Widget (Right col) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Quick Action Cards (2 Columns) */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-xs font-bold uppercase tracking-wider text-surface-400">
            Quick Actions
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {quickActions.map((action, i) => {
              const Icon = action.icon
              return (
                <motion.div
                  key={action.id}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.08 }}
                >
                  <Link
                    id={action.id}
                    data-testid={action.id}
                    to={action.to}
                    className={`group flex h-full flex-col justify-between rounded-2xl border bg-gradient-to-b ${action.color} p-5 shadow-sm transition-all duration-200 hover:scale-[1.02] hover:shadow-glow`}
                  >
                    <div className="space-y-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-surface-800/80 shadow-inner">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-white group-hover:text-primary-300 transition">
                          {action.title}
                        </h3>
                        <p className="mt-1 text-[11px] leading-relaxed text-surface-400">
                          {action.description}
                        </p>
                      </div>
                    </div>

                    <div className="mt-4 flex items-center gap-1 text-xs font-semibold text-white/80 group-hover:text-white transition">
                      <span>Get started</span>
                      <ArrowRight className="h-3.5 w-3.5 transition group-hover:translate-x-1" />
                    </div>
                  </Link>
                </motion.div>
              )
            })}
          </div>
        </div>

        {/* Rate Limit Status Widget */}
        <div className="space-y-4">
          <h2 className="text-xs font-bold uppercase tracking-wider text-surface-400">
            Usage & Security
          </h2>
          <RateLimitWidget />
        </div>
      </div>

      {/* Recent Activity Section */}
      <div className="space-y-4 pt-2">
        <div className="flex items-center justify-between">
          <h2 className="text-xs font-bold uppercase tracking-wider text-surface-400">
            Recent Inquiries ({recentQueries.length})
          </h2>
          {history.length > 3 && (
            <Link
              to="/history"
              className="text-xs font-semibold text-primary-400 hover:text-primary-300 inline-flex items-center gap-1"
            >
              <span>View full history ({history.length})</span>
              <ArrowRight className="h-3 w-3" />
            </Link>
          )}
        </div>

        {recentQueries.length === 0 ? (
          <div className="rounded-2xl border border-surface-700/50 bg-surface-800/30 p-8 text-center text-xs text-surface-500">
            No query activity yet. Ask a question or verify a claim above to get started!
          </div>
        ) : (
          <div className="space-y-3">
            {recentQueries.map((resp, idx) => (
              <QueryResponseCard
                key={resp.query_id || idx}
                response={resp}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
