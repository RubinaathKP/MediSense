import { ShieldCheck, BookOpen, HelpCircle } from 'lucide-react'
import { cn } from '@/utils/cn'

interface EvidenceLabelProps {
  evidenceBasis: 'clinical_trial' | 'traditional_recognized' | 'not_found' | 'not_verified' | string
}

export default function EvidenceLabel({ evidenceBasis }: EvidenceLabelProps) {
  const getBadgeDetails = () => {
    switch (evidenceBasis) {
      case 'clinical_trial':
        return {
          label: 'Clinical Trial Evidence',
          tooltip: 'Supported by peer-reviewed human clinical trials or standard clinical practice guidelines.',
          cls: 'bg-blue-500/15 text-blue-300 border-blue-500/30',
          icon: ShieldCheck,
        }
      case 'traditional_recognized':
        return {
          label: 'Traditional Medicine',
          tooltip: 'Formally recognized in documented traditional systems of medicine (e.g. AYUSH, WHO Traditional Medicine).',
          cls: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
          icon: BookOpen,
        }
      case 'not_found':
      case 'not_verified':
      default:
        return {
          label: 'Not in verified sources',
          tooltip: 'Absent from peer-reviewed clinical guidelines and recognized traditional medicine pharmacopoeias.',
          cls: 'bg-slate-500/15 text-slate-400 border-slate-500/30',
          icon: HelpCircle,
        }
    }
  }

  const { label, tooltip, cls, icon: IconComponent } = getBadgeDetails()

  return (
    <div className="relative group inline-block">
      <span
        data-testid="evidence-label-badge"
        className={cn(
          'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium cursor-help transition-colors',
          cls
        )}
      >
        <IconComponent className="h-3.5 w-3.5" />
        <span>{label}</span>
      </span>

      {/* Tooltip on hover */}
      <div
        data-testid="evidence-label-tooltip"
        className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:flex flex-col items-center z-50 w-56 p-2 rounded-xl border border-surface-700 bg-surface-900/95 text-[11px] leading-relaxed text-slate-200 shadow-xl backdrop-blur-md pointer-events-none"
      >
        <span>{tooltip}</span>
        <div className="w-2 h-2 bg-surface-900 rotate-45 border-r border-b border-surface-700 -mb-3 mt-1" />
      </div>
    </div>
  )
}
