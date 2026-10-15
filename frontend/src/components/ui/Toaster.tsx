/**
 * Toaster — Renders toast notifications with shadcn/ui styling and framer-motion animations.
 *
 * Variants:
 *  - error: Red / Rose styling, auto-dismisses in 5s
 *  - warning: Amber styling, auto-dismisses in 8s
 *  - success: Green / Safe styling, auto-dismisses in 3s
 *  - rateLimit: Amber alert styling, persistent until manually dismissed
 */
import { motion, AnimatePresence } from 'framer-motion'
import { CheckCircle2, AlertTriangle, XCircle, AlertCircle, X } from 'lucide-react'
import { useToast, type ToastItem } from './useToast'
import { cn } from '@/utils/cn'

export function Toaster() {
  const { toasts, dismiss } = useToast()

  return (
    <div
      aria-live="assertive"
      className="fixed bottom-0 right-0 z-50 flex max-h-screen w-full flex-col-reverse p-4 sm:bottom-0 sm:right-0 sm:top-auto sm:flex-col md:max-w-[420px] gap-2 pointer-events-none"
    >
      <AnimatePresence>
        {toasts.map((t: ToastItem) => {
          let bg = 'bg-surface-800 border-surface-700 text-slate-100'
          let Icon = AlertCircle
          let iconColor = 'text-surface-400'

          if (t.variant === 'success') {
            bg = 'bg-emerald-500/15 border-emerald-500/30 text-emerald-200'
            Icon = CheckCircle2
            iconColor = 'text-emerald-400'
          } else if (t.variant === 'warning') {
            bg = 'bg-amber-500/15 border-amber-500/30 text-amber-200'
            Icon = AlertTriangle
            iconColor = 'text-amber-400'
          } else if (t.variant === 'error') {
            bg = 'bg-rose-500/15 border-rose-500/30 text-rose-200'
            Icon = XCircle
            iconColor = 'text-rose-400'
          } else if (t.variant === 'rateLimit') {
            bg = 'bg-amber-500/20 border-amber-500/40 text-amber-200'
            Icon = AlertCircle
            iconColor = 'text-amber-400'
          }

          return (
            <motion.div
              key={t.id}
              initial={{ opacity: 0, y: 16, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.15 } }}
              className={cn(
                'pointer-events-auto relative flex w-full items-start gap-3 overflow-hidden rounded-xl border p-4 shadow-xl backdrop-blur-md transition-all',
                bg
              )}
            >
              <Icon className={cn('h-5 w-5 shrink-0 mt-0.5', iconColor)} />

              <div className="flex-1 text-sm">
                {t.title && <div className="font-semibold">{t.title}</div>}
                {t.description && <div className="text-xs opacity-90 mt-0.5 leading-relaxed">{t.description}</div>}
              </div>

              <button
                type="button"
                onClick={() => dismiss(t.id)}
                className="shrink-0 rounded-lg p-1 opacity-70 hover:opacity-100 hover:bg-surface-700/50 transition-all cursor-pointer"
                aria-label="Dismiss toast"
              >
                <X className="h-4 w-4" />
              </button>
            </motion.div>
          )
        })}
      </AnimatePresence>
    </div>
  )
}

export default Toaster
