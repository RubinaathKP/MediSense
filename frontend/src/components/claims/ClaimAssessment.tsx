import { FlaskConical, Leaf, Info, ShieldAlert } from 'lucide-react'
import type { ClaimAssessment } from '@/api/types'
import EvidenceLabel from './EvidenceLabel'

interface ClaimAssessmentProps {
  assessment: ClaimAssessment
  claimText?: string
}

export default function ClaimAssessmentCard({
  assessment,
  claimText,
}: ClaimAssessmentProps) {
  const { allopathic_position, traditional_position, closing_note } = assessment

  return (
    <div
      data-testid="claim-assessment-card"
      className="space-y-5 rounded-2xl border border-surface-700/60 bg-surface-800/60 p-6 shadow-glass animate-scale-in"
    >
      {claimText && (
        <div className="border-b border-surface-700/40 pb-3">
          <p className="text-xs font-semibold uppercase tracking-wider text-surface-400">
            Health Claim Under Verification:
          </p>
          <p className="text-sm font-bold text-white mt-1">"{claimText}"</p>
        </div>
      )}

      {/* Two-Column Evidence Comparison Layout */}
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {/* Left Column (Clinical Evidence) */}
        <div
          data-testid="clinical-evidence-column"
          className="flex flex-col justify-between rounded-xl border border-blue-500/30 bg-blue-950/20 p-5 space-y-4 shadow-sm"
        >
          <div className="space-y-3">
            <div className="flex items-center gap-2 border-b border-blue-500/20 pb-2.5">
              <FlaskConical className="h-5 w-5 text-blue-400 shrink-0" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-blue-300">
                According to verified clinical guidelines:
              </h3>
            </div>

            <p className="text-sm leading-relaxed text-slate-200">
              {allopathic_position.summary}
            </p>
          </div>

          <div className="pt-2 border-t border-blue-500/20 flex items-center justify-between gap-2 flex-wrap">
            <EvidenceLabel evidenceBasis={allopathic_position.evidence_basis} />
            <span className="text-xs font-medium text-slate-300">
              Source: <strong className="text-blue-300">{allopathic_position.source}</strong>
            </span>
          </div>
        </div>

        {/* Right Column (Traditional Medicine) */}
        <div
          data-testid="traditional-evidence-column"
          className="flex flex-col justify-between rounded-xl border border-amber-500/30 bg-amber-950/20 p-5 space-y-4 shadow-sm"
        >
          {traditional_position ? (
            <>
              <div className="space-y-3">
                <div className="flex items-center gap-2 border-b border-amber-500/20 pb-2.5">
                  <Leaf className="h-5 w-5 text-amber-400 shrink-0" />
                  <h3 className="text-xs font-bold uppercase tracking-wider text-amber-300">
                    According to recognized traditional medicine sources:
                  </h3>
                </div>

                <p className="text-sm leading-relaxed text-slate-200">
                  {traditional_position.summary}
                </p>
              </div>

              <div className="pt-2 border-t border-amber-500/20 flex items-center justify-between gap-2 flex-wrap">
                <EvidenceLabel evidenceBasis={traditional_position.evidence_basis} />
                <span className="text-xs font-medium text-slate-300">
                  Source: <strong className="text-amber-300">{traditional_position.source}</strong>
                </span>
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center justify-center h-full text-center py-6 text-surface-400">
              <Leaf className="h-8 w-8 text-surface-500 mb-2 opacity-50" />
              <p className="text-xs text-slate-400">
                Not found in traditional medicine sources we track
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Closing Note Section (Full Width Below) */}
      {closing_note && (
        <div
          data-testid="claim-closing-note"
          className="flex items-start gap-3 rounded-xl border border-surface-700/60 bg-surface-900/60 p-4"
        >
          <Info className="h-5 w-5 text-primary-400 shrink-0 mt-0.5" />
          <div className="text-xs leading-relaxed text-slate-300">
            <p className="italic">{closing_note}</p>
            <p className="mt-2 text-[11px] font-semibold text-amber-400/90 flex items-center gap-1.5">
              <ShieldAlert className="h-3.5 w-3.5" />
              Always discuss individual treatment decisions and remedies with a qualified medical professional.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
