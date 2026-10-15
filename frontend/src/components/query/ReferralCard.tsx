import { useMemo } from 'react'
import { Stethoscope, HelpCircle, ArrowRight, ShieldAlert } from 'lucide-react'

interface ReferralCardProps {
  responseText: string
  classifierFlag?: 'PRESCRIPTIVE' | 'DIAGNOSTIC' | 'DRUG_INTERACTION' | 'EMERGENCY' | null
}

export default function ReferralCard({ responseText, classifierFlag }: ReferralCardProps) {
  // Client-side alternative suggestions based on classifierFlag
  const safeAlternatives = useMemo(() => {
    switch (classifierFlag) {
      case 'PRESCRIPTIVE':
        return [
          'What is this medication class generally used for?',
          'How does this class of medication work in the body?',
          'What are the typical non-pharmacological lifestyle recommendations for this condition?',
        ]
      case 'DIAGNOSTIC':
        return [
          'What clinical guidelines exist for evaluating these lab values?',
          'What general factors can cause these test markers to be elevated or reduced?',
          'What questions should I ask my doctor to clarify my diagnosis?',
        ]
      case 'DRUG_INTERACTION':
        return [
          'What is the standard mechanism of action for this drug class?',
          'What general information is available in pharmacology guidelines for this medication?',
          'What should I disclose to my physician or pharmacist about my current prescriptions?',
        ]
      case 'EMERGENCY':
        return [
          'What are standard warning signs requiring immediate emergency medical evaluation?',
          'How do healthcare providers assess acute symptoms?',
        ]
      default:
        return [
          'Can you explain what this medical term means in plain language?',
          'What does this test generally measure in a standard lab panel?',
          'What questions should I bring to my next physician consultation?',
        ]
    }
  }, [classifierFlag])

  return (
    <div
      data-testid="referral-card"
      className="rounded-2xl border border-amber-500/40 bg-amber-500/10 p-5 shadow-glass space-y-4"
    >
      {/* Header with Stethoscope Icon and subtle notice */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/30 shrink-0">
            <Stethoscope className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-amber-200">
              Doctor Referral Required
            </h3>
            <span
              data-testid="clinical-judgment-tag"
              className="inline-block mt-0.5 rounded-md bg-amber-400/10 px-2 py-0.5 text-[11px] font-medium text-amber-300 border border-amber-400/20"
            >
              This question requires clinical judgment
            </span>
          </div>
        </div>

        <ShieldAlert className="h-5 w-5 text-amber-400/70 shrink-0" />
      </div>

      {/* Referral Explanation Text */}
      <div className="rounded-xl border border-amber-500/20 bg-surface-900/60 p-4 text-sm leading-relaxed text-slate-200 whitespace-pre-wrap">
        {responseText}
      </div>

      {/* "What can I ask instead?" Safe Alternative Suggestions */}
      <div className="space-y-2 border-t border-amber-500/20 pt-3">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-300">
          <HelpCircle className="h-3.5 w-3.5" />
          <span>What can I ask instead?</span>
        </div>
        <ul className="space-y-1.5">
          {safeAlternatives.map((alt, idx) => (
            <li
              key={idx}
              data-testid={`safe-alt-${idx}`}
              className="flex items-start gap-2 text-xs text-slate-300 bg-surface-800/40 rounded-lg p-2 border border-surface-700/40"
            >
              <ArrowRight className="h-3.5 w-3.5 text-amber-400 shrink-0 mt-0.5" />
              <span>{alt}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
