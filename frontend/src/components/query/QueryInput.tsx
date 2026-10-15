import { useState, useEffect, useMemo, useRef } from 'react'
import { Sparkles, Languages, Send } from 'lucide-react'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { cn } from '@/utils/cn'

interface QueryInputProps {
  onSubmit: (query: string, language: 'en' | 'ta') => void
  isLoading?: boolean
  isRateLimited?: boolean
  rateLimitCountdown?: number
  initialQuery?: string
  reportType?: 'lab_report' | 'prescription' | 'none'
  selectedTab?: 'report' | 'claim' | 'general'
}

const MAX_CHAR_LIMIT = 500
const MIN_CHAR_REQUIRED = 10

// Tamil Unicode regex range: \u0B80-\u0BFF
const TAMIL_REGEX = /[\u0B80-\u0BFF]/

export default function QueryInput({
  onSubmit,
  isLoading = false,
  isRateLimited = false,
  rateLimitCountdown = 0,
  initialQuery = '',
  reportType = 'none',
  selectedTab = 'report',
}: QueryInputProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const [query, setQuery] = useState(initialQuery)
  const [overrideLang, setOverrideLang] = useState<'en' | 'ta' | null>(null)

  // Pre-fill query support and auto-focus
  useEffect(() => {
    if (initialQuery) {
      setQuery(initialQuery)
      if (textareaRef.current) {
        textareaRef.current.focus()
      }
    }
  }, [initialQuery])

  // Auto detect language from query text if not manually overridden
  const detectedLanguage: 'en' | 'ta' = useMemo(() => {
    if (overrideLang) return overrideLang
    if (TAMIL_REGEX.test(query)) return 'ta'
    return 'en'
  }, [query, overrideLang])

  const remainingChars = MAX_CHAR_LIMIT - query.length
  const isTooShort = query.trim().length > 0 && query.trim().length < MIN_CHAR_REQUIRED
  const isSubmitDisabled =
    isLoading ||
    isRateLimited ||
    query.trim().length < MIN_CHAR_REQUIRED ||
    remainingChars < 0

  // Suggestion chips based on selectedTab and reportType
  const suggestions = useMemo(() => {
    if (selectedTab === 'claim') {
      return [
        'Is drinking neem water good for diabetes?',
        'Does turmeric help reduce inflammation?',
        'Can garlic cure high blood pressure?',
      ]
    }
    if (selectedTab === 'report') {
      if (reportType === 'prescription') {
        return [
          'What is this medication for?',
          'What drug class is this?',
          'How does this medicine work?',
        ]
      }
      return [
        'What does my LDL result mean?',
        'What lifestyle changes help with borderline cholesterol?',
        'What should I discuss with my doctor about this report?',
      ]
    }
    // General
    return [
      'What are normal resting heart rate ranges?',
      'How does routine blood sugar screening work?',
      'What questions should I bring to my annual checkup?',
    ]
  }, [selectedTab, reportType])

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (isSubmitDisabled) return
    onSubmit(query.trim(), detectedLanguage)
    setQuery('')
    setOverrideLang(null)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  return (
    <div className="w-full space-y-3">
      {/* Input container */}
      <div className="relative rounded-2xl border border-surface-700/80 bg-surface-800/80 p-3 shadow-glass backdrop-blur-md transition-all focus-within:border-primary-500 focus-within:ring-1 focus-within:ring-primary-500/30">
        {/* Top Header: Language Tag + Manual Override */}
        <div className="mb-2 flex items-center justify-between border-b border-surface-700/50 pb-2">
          <div className="flex items-center gap-1.5">
            <span
              data-testid="language-indicator"
              className={cn(
                'inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold uppercase tracking-wide transition-all',
                detectedLanguage === 'ta'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                  : 'bg-primary-500/20 text-primary-300 border border-primary-500/30'
              )}
            >
              {detectedLanguage === 'ta' ? 'தமிழ்' : 'EN'}
            </span>
            {overrideLang && (
              <span className="text-[10px] text-surface-400 italic">
                (Manual)
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            <Languages className="h-3.5 w-3.5 text-surface-400 mr-1" />
            <button
              type="button"
              data-testid="lang-override-en"
              onClick={() => setOverrideLang('en')}
              className={cn(
                'rounded px-2 py-0.5 text-xs font-medium transition',
                detectedLanguage === 'en'
                  ? 'bg-primary-600/30 text-primary-300 font-bold'
                  : 'text-surface-400 hover:text-slate-200'
              )}
            >
              EN
            </button>
            <span className="text-surface-600 text-xs">|</span>
            <button
              type="button"
              data-testid="lang-override-ta"
              onClick={() => setOverrideLang('ta')}
              className={cn(
                'rounded px-2 py-0.5 text-xs font-medium transition',
                detectedLanguage === 'ta'
                  ? 'bg-amber-500/30 text-amber-300 font-bold'
                  : 'text-surface-400 hover:text-slate-200'
              )}
            >
              தமிழ்
            </button>
          </div>
        </div>

        {/* Text Area */}
        <textarea
          ref={textareaRef}
          id="query-textarea"
          data-testid="query-textarea"
          rows={4}
          value={query}
          onChange={(e) => setQuery(e.target.value.slice(0, MAX_CHAR_LIMIT))}
          onKeyDown={handleKeyDown}
          placeholder={
            detectedLanguage === 'ta'
              ? 'உங்கள் அறிக்கை அல்லது மருந்துகளைப் பற்றி கேள்வி கேட்கவும்...'
              : 'Ask a clear question about your lab results, prescription, or health topic...'
          }
          disabled={isLoading || isRateLimited}
          className="w-full resize-none bg-transparent text-sm leading-relaxed text-slate-100 placeholder-surface-500 outline-none disabled:cursor-not-allowed disabled:opacity-50"
        />

        {/* Bottom Bar: Character counter & Submit button */}
        <div className="mt-2 flex items-center justify-between border-t border-surface-700/50 pt-2">
          <div className="flex flex-col">
            <span
              data-testid="char-counter"
              className={cn(
                'text-xs font-mono font-medium transition-colors',
                remainingChars < 50 ? 'text-red-400 font-bold' : 'text-surface-400'
              )}
            >
              {remainingChars} remaining
            </span>
            {isTooShort && (
              <span
                data-testid="min-char-warning"
                className="text-[11px] text-amber-400 mt-0.5"
              >
                Please enter a complete question
              </span>
            )}
          </div>

          <button
            type="button"
            id="query-submit-btn"
            data-testid="query-submit-btn"
            onClick={() => handleSubmit()}
            disabled={isSubmitDisabled}
            className={cn(
              'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-semibold text-white shadow-glow transition-all duration-200',
              isRateLimited
                ? 'bg-surface-700 text-surface-400 cursor-not-allowed'
                : isSubmitDisabled
                ? 'bg-surface-700/60 text-surface-400 opacity-60 cursor-not-allowed'
                : 'bg-gradient-primary hover:opacity-95 active:scale-95'
            )}
          >
            {isLoading ? (
              <>
                <LoadingSpinner size="sm" />
                <span>Getting answer...</span>
              </>
            ) : isRateLimited ? (
              <span>Try again in {rateLimitCountdown}s</span>
            ) : (
              <>
                <span>Ask</span>
                <Send className="h-3.5 w-3.5" />
              </>
            )}
          </button>
        </div>
      </div>

      {/* Suggestion Chips (Shown when input is empty) */}
      {query.trim().length === 0 && (
        <div data-testid="suggestion-chips" className="space-y-1.5 animate-fade-in">
          <div className="flex items-center gap-1.5 text-xs text-surface-400">
            <Sparkles className="h-3.5 w-3.5 text-primary-400" />
            <span>Suggested questions:</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {suggestions.map((suggestion, idx) => (
              <button
                key={idx}
                type="button"
                data-testid={`suggestion-chip-${idx}`}
                onClick={() => {
                  setQuery(suggestion)
                  if (textareaRef.current) {
                    textareaRef.current.focus()
                  }
                }}
                className="rounded-xl border border-surface-700/60 bg-surface-800/40 px-3 py-1.5 text-left text-xs text-slate-300 transition-all hover:border-primary-500/50 hover:bg-surface-700/50 hover:text-white"
              >
                {suggestion}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
