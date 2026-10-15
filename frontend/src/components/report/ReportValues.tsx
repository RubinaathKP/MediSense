/**
 * ReportValues — displays extracted lab values or prescriptions
 *
 * FlagStatus from backend (LabReportDto.java / FlagStatus enum):
 *   NORMAL | BORDERLINE | ABNORMAL
 *
 * "Ask about this →" pre-fills QueryInput using sessionStore navigation context.
 * Prescription display shows both brand and generic names (NFR-18).
 */
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, Pill, FlaskConical } from 'lucide-react'

import type { LabValue, Prescription, FlagStatus } from '@/api/types'
import { cn } from '@/utils/cn'

// Status badge configuration
const statusConfig: Record<FlagStatus, { label: string; bg: string; text: string; rowHighlight: string }> = {
  NORMAL: {
    label: 'report.status_normal',
    bg: 'bg-health-safe/15',
    text: 'text-health-safe',
    rowHighlight: '',
  },
  BORDERLINE: {
    label: 'report.status_borderline',
    bg: 'bg-health-borderline/15',
    text: 'text-health-borderline',
    rowHighlight: 'bg-health-borderline/5 border-l-2 border-health-borderline/50',
  },
  ABNORMAL: {
    label: 'report.status_abnormal',
    bg: 'bg-health-abnormal/15',
    text: 'text-health-abnormal',
    rowHighlight: 'bg-health-abnormal/5 border-l-2 border-health-abnormal/50',
  },
}

interface LabTableProps { values: LabValue[] }
interface PrescriptionCardsProps { prescriptions: Prescription[] }

function StatusBadge({ status }: { status: FlagStatus }) {
  const { t } = useTranslation()
  const cfg = statusConfig[status]
  return (
    <span className={cn('rounded-full px-2.5 py-0.5 text-xs font-semibold', cfg.bg, cfg.text)}>
      {t(cfg.label)}
    </span>
  )
}

function LabTable({ values }: LabTableProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()

  const handleAsk = (value: LabValue) => {
    const prefill = t('report.ask_prefill_lab', {
      test_name: value.testName,
      value: value.extractedValue,
      unit: value.unit,
    })
    navigate('/query', { state: { prefillQuery: prefill } })
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-surface-700/50">
      <table className="w-full text-sm" role="table" aria-label="Lab report values">
        <thead>
          <tr className="border-b border-surface-700/50 bg-surface-800/60">
            {['report.values_column_test', 'report.values_column_value', 'report.values_column_unit',
              'report.values_column_range', 'report.values_column_status', ''].map((key, i) => (
              <th key={i} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-surface-600">
                {key ? t(key) : ''}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-surface-700/30">
          {values.map((v, idx) => {
            const cfg = statusConfig[v.flagStatus]
            return (
              <motion.tr
                key={`${v.testName}-${idx}`}
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.04 }}
                className={cn('transition-colors hover:bg-surface-700/20', cfg.rowHighlight)}
              >
                <td className="px-4 py-3 font-medium text-slate-200">{v.testName}</td>
                <td className="px-4 py-3 font-mono font-semibold text-white">{v.extractedValue}</td>
                <td className="px-4 py-3 text-surface-600">{v.unit}</td>
                <td className="px-4 py-3 text-surface-600">{v.referenceRange}</td>
                <td className="px-4 py-3"><StatusBadge status={v.flagStatus} /></td>
                <td className="px-4 py-3">
                  <button
                    id={`ask-about-${v.testName.replace(/\s/g, '-')}`}
                    type="button"
                    onClick={() => handleAsk(v)}
                    className="flex items-center gap-1 text-xs text-primary-400 hover:text-primary-300 font-medium transition whitespace-nowrap"
                  >
                    {t('report.ask_about')} <ArrowRight className="h-3 w-3" />
                  </button>
                </td>
              </motion.tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function PrescriptionCards({ prescriptions }: PrescriptionCardsProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()

  const handleAsk = (p: Prescription) => {
    const prefill = t('report.ask_prefill_prescription', { generic_name: p.genericName })
    navigate('/query', { state: { prefillQuery: prefill } })
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {prescriptions.map((p, idx) => (
        <motion.div
          key={`${p.brandName}-${idx}`}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: idx * 0.06 }}
          className="rounded-xl border border-surface-700/50 bg-surface-800/40 p-4 space-y-3"
        >
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-600/20">
              <Pill className="h-5 w-5 text-primary-400" />
            </div>
            <div>
              {/* NFR-18: Always show both brand and generic name */}
              <p className="font-semibold text-white">
                {p.brandName}
                {p.genericName && p.genericName !== p.brandName && (
                  <span className="ml-1.5 text-sm font-normal text-surface-600">({p.genericName})</span>
                )}
              </p>
              <span className="inline-block rounded-full bg-primary-600/15 px-2 py-0.5 text-xs font-medium text-primary-400">
                {p.drugClass}
              </span>
            </div>
          </div>

          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            {[
              { label: t('report.prescription_dosage_label'), value: p.dosage },
              { label: t('report.prescription_frequency_label'), value: p.frequency },
              { label: t('report.prescription_timing_label'), value: p.timing },
              { label: t('report.prescription_duration_label'), value: p.duration },
            ].map(({ label, value }) => value ? (
              <div key={label}>
                <dt className="text-xs text-surface-600">{label}</dt>
                <dd className="font-medium text-slate-300">{value}</dd>
              </div>
            ) : null)}
          </dl>

          <button
            id={`ask-about-prescription-${idx}`}
            type="button"
            onClick={() => handleAsk(p)}
            className="flex items-center gap-1 text-xs text-primary-400 hover:text-primary-300 font-medium transition"
          >
            {t('report.ask_about')} <ArrowRight className="h-3 w-3" />
          </button>
        </motion.div>
      ))}
    </div>
  )
}

interface Props {
  labValues: LabValue[]
  prescriptions: Prescription[]
  reportType?: 'lab_report' | 'prescription'
}

export default function ReportValues({ labValues, prescriptions }: Props) {
  const { t } = useTranslation()

  const hasLab = labValues.length > 0
  const hasPrescriptions = prescriptions.length > 0

  return (
    <div className="space-y-6 animate-fade-in">
      {hasLab && (
        <section>
          <div className="mb-3 flex items-center gap-2">
            <FlaskConical className="h-4 w-4 text-primary-400" />
            <h2 className="font-semibold text-white">{t('report.values_title')}</h2>
          </div>
          <LabTable values={labValues} />
        </section>
      )}

      {hasPrescriptions && (
        <section>
          <div className="mb-3 flex items-center gap-2">
            <Pill className="h-4 w-4 text-primary-400" />
            <h2 className="font-semibold text-white">{t('report.prescription_title')}</h2>
          </div>
          <PrescriptionCards prescriptions={prescriptions} />
        </section>
      )}

      {!hasLab && !hasPrescriptions && (
        <div className="rounded-xl border border-surface-700/30 bg-surface-800/20 px-6 py-10 text-center">
          <p className="text-surface-600">{t('report.empty_state_subtitle')}</p>
        </div>
      )}
    </div>
  )
}
