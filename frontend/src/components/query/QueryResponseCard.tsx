import ReactMarkdown from 'react-markdown'
import {
  AlertTriangle,
  PhoneCall,
  Info,
  BookOpen,
  Languages,
} from 'lucide-react'
import type { QueryResponse } from '@/api/types'
import CitationCard from './CitationCard'
import ReferralCard from './ReferralCard'

interface QueryResponseProps {
  response: QueryResponse
  queryText?: string
}

export default function QueryResponseCard({ response, queryText }: QueryResponseProps) {
  const isEmergency = response.classifier_flag === 'EMERGENCY'

  // 1. Emergency Response State (Urgent Red Card)
  if (isEmergency) {
    return (
      <div
        data-testid="emergency-response-card"
        className="rounded-2xl border-2 border-red-500/60 bg-red-950/40 p-6 shadow-glow space-y-4 animate-scale-in"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-red-600 text-white shadow-lg shrink-0 animate-pulse">
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-red-100 uppercase tracking-wider">
                Emergency Alert
              </h3>
              <p className="text-xs text-red-300">
                Immediate clinical assistance required
              </p>
            </div>
          </div>

          <a
            href="tel:112"
            data-testid="call-112-btn"
            className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 text-xs font-bold text-white shadow-md hover:bg-red-500 transition-all active:scale-95 shrink-0"
          >
            <PhoneCall className="h-4 w-4" />
            <span>Call 112</span>
          </a>
        </div>

        <div className="rounded-xl border border-red-500/30 bg-red-900/30 p-4 text-sm font-medium leading-relaxed text-red-100">
          <p className="font-semibold mb-1">
            Please call 112 immediately or go to your nearest emergency room.
          </p>
          <p className="text-xs text-red-200/90 whitespace-pre-wrap">
            {response.response_text}
          </p>
        </div>
      </div>
    )
  }

  // 2. Doctor Referral State (is_referral: true)
  if (response.is_referral) {
    return (
      <ReferralCard
        responseText={response.response_text}
        classifierFlag={response.classifier_flag}
      />
    )
  }

  // 3. Safe Response State (is_referral: false)
  return (
    <div
      data-testid="safe-response-card"
      className="rounded-2xl border border-surface-700/60 bg-surface-800/60 p-6 shadow-glass space-y-5"
    >
      {/* Optional query text header for context */}
      {queryText && (
        <div className="border-b border-surface-700/40 pb-3">
          <p className="text-xs font-medium text-surface-400">Question:</p>
          <p className="text-sm font-semibold text-slate-100 mt-0.5">{queryText}</p>
        </div>
      )}

      {/* Translation unavailable banner */}
      {response.translation_unavailable && (
        <div
          data-testid="translation-unavailable-banner"
          className="flex items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3.5 py-2.5 text-xs text-amber-300"
        >
          <Languages className="h-4 w-4 shrink-0" />
          <span>
            Tamil translation temporarily unavailable. Showing response in English.
          </span>
        </div>
      )}

      {/* Insufficient context warning */}
      {response.insufficient_context ? (
        <div
          data-testid="insufficient-context-box"
          className="flex items-start gap-3 rounded-xl border border-surface-700/60 bg-surface-800/40 p-4 text-sm text-surface-300"
        >
          <Info className="h-5 w-5 text-primary-400 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-slate-200">
              No Specific Guideline Match Found
            </p>
            <p className="mt-1 text-xs leading-relaxed text-surface-400">
              We couldn't find specific information about this in our verified sources.
              For this question, please consult your doctor directly.
            </p>
          </div>
        </div>
      ) : (
        /* Markdown rendered explanation response */
        <div
          data-testid="markdown-response"
          className="prose prose-invert prose-sm max-w-none text-sm leading-relaxed text-slate-200"
        >
          <ReactMarkdown>{response.response_text}</ReactMarkdown>
        </div>
      )}

      {/* Citations section (only if context exists and citations present) */}
      {!response.insufficient_context &&
        response.citations &&
        response.citations.length > 0 && (
          <div data-testid="citations-section" className="space-y-2.5 border-t border-surface-700/40 pt-4">
            <div className="flex items-center gap-2">
              <BookOpen className="h-4 w-4 text-primary-400" />
              <h4 className="text-xs font-bold text-slate-200 uppercase tracking-wider">
                Sources
              </h4>
            </div>

            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              {response.citations.map((citation, idx) => (
                <CitationCard key={citation.chunk_id || idx} citation={citation} index={idx} />
              ))}
            </div>
          </div>
        )}
    </div>
  )
}
