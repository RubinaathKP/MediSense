/**
 * LanguageToggle — Switches application language between English and Tamil.
 *
 * Selected language has a solid background; unselected is outlined.
 * Dispatches debounced backend sync to PATCH /users/preferences.
 */
import { useLanguage } from '@/hooks/useLanguage'
import { cn } from '@/utils/cn'

interface LanguageToggleProps {
  className?: string
  variant?: 'compact' | 'full'
}

export default function LanguageToggle({ className, variant = 'compact' }: LanguageToggleProps) {
  const { current_language, change } = useLanguage()

  const handleLanguageChange = (lang: 'en' | 'ta') => {
    change(lang)
  }

  if (variant === 'full') {
    return (
      <div className={cn('flex items-center gap-2 rounded-xl bg-surface-800 p-1 border border-surface-700/50', className)}>
        <button
          type="button"
          onClick={() => handleLanguageChange('en')}
          className={cn(
            'flex-1 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all cursor-pointer',
            current_language === 'en'
              ? 'bg-primary-600 text-white shadow-sm border border-primary-500'
              : 'bg-transparent border border-surface-600/60 text-surface-400 hover:text-slate-200 hover:border-surface-500'
          )}
        >
          English
        </button>
        <button
          type="button"
          onClick={() => handleLanguageChange('ta')}
          className={cn(
            'flex-1 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all font-tamil cursor-pointer',
            current_language === 'ta'
              ? 'bg-primary-600 text-white shadow-sm border border-primary-500'
              : 'bg-transparent border border-surface-600/60 text-surface-400 hover:text-slate-200 hover:border-surface-500'
          )}
        >
          தமிழ்
        </button>
      </div>
    )
  }

  return (
    <div
      className={cn('inline-flex items-center gap-1.5 rounded-lg bg-surface-900/80 p-1 border border-surface-700/50', className)}
      role="group"
      aria-label="Language selection"
    >
      <button
        type="button"
        onClick={() => handleLanguageChange('en')}
        className={cn(
          'rounded-md px-2.5 py-1 text-xs font-semibold transition-all cursor-pointer',
          current_language === 'en'
            ? 'bg-primary-600 text-white shadow-sm border border-primary-500'
            : 'bg-transparent border border-surface-700 text-surface-400 hover:text-slate-200 hover:border-surface-600'
        )}
        aria-pressed={current_language === 'en'}
      >
        EN
      </button>
      <button
        type="button"
        onClick={() => handleLanguageChange('ta')}
        className={cn(
          'rounded-md px-2.5 py-1 text-xs font-semibold transition-all font-tamil cursor-pointer',
          current_language === 'ta'
            ? 'bg-primary-600 text-white shadow-sm border border-primary-500'
            : 'bg-transparent border border-surface-700 text-surface-400 hover:text-slate-200 hover:border-surface-600'
        )}
        aria-pressed={current_language === 'ta'}
      >
        தமிழ்
      </button>
    </div>
  )
}
