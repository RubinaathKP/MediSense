import { BookOpen, ShieldCheck, HelpCircle } from 'lucide-react'
import type { Citation } from '@/api/types'
import { cn } from '@/utils/cn'

interface CitationCardProps {
  citation: Citation
  index?: number
}

export default function CitationCard({ citation, index }: CitationCardProps) {
  // Evidence basis color schemes:
  // clinical_trial: blue "Clinical Evidence"
  // traditional_recognized: amber "Traditional Medicine"
  // not_found / not_verified: gray "Unverified"
  const getEvidenceBadge = (basis: string) => {
    switch (basis) {
      case 'clinical_trial':
        return {
          label: 'Clinical Evidence',
          bg: 'bg-blue-500/15 text-blue-300 border-blue-500/30',
          icon: ShieldCheck,
        }
      case 'traditional_recognized':
        return {
          label: 'Traditional Medicine',
          bg: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
          icon: BookOpen,
        }
      case 'not_found':
      case 'not_verified':
      default:
        return {
          label: 'Unverified',
          bg: 'bg-slate-500/15 text-slate-400 border-slate-500/30',
          icon: HelpCircle,
        }
    }
  }

  const badge = getEvidenceBadge(citation.evidence_basis)
  const IconComponent = badge.icon

  return (
    <div
      data-testid={`citation-card-${index ?? 0}`}
      className="flex flex-col justify-between rounded-xl border border-surface-700/60 bg-surface-800/40 p-3.5 shadow-sm transition-all hover:border-surface-600 hover:bg-surface-800/70"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-xs text-white">
              {citation.source_name}
            </span>
            {citation.source_year && (
              <span className="rounded bg-surface-700/60 px-1.5 py-0.5 font-mono text-[10px] text-surface-300">
                {citation.source_year}
              </span>
            )}
          </div>
          {citation.chunk_id && (
            <p className="font-mono text-[10px] text-surface-500 truncate max-w-[200px]">
              Ref: {citation.chunk_id}
            </p>
          )}
        </div>

        {/* Evidence basis tag */}
        <span
          data-testid="evidence-basis-badge"
          className={cn(
            'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-medium tracking-wide shrink-0',
            badge.bg
          )}
        >
          <IconComponent className="h-3 w-3" />
          <span>{badge.label}</span>
        </span>
      </div>
    </div>
  )
}
