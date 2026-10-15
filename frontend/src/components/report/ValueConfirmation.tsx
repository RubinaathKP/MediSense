/**
 * ValueConfirmation — shown when OCR confidence is 0.85–0.90 (requires_confirmation=true)
 *
 * Backend integration:
 *  POST /reports/confirm-value  { value_id, confirmed_value, confirmed_unit }
 *  Rate limit: 30/hr/user (Tier 2)
 *
 * LabValue.requiresUserConfirmation=true means that specific value needs confirmation.
 * We only show the confirm UI for values where requiresUserConfirmation===true.
 */
import { useState } from 'react'
import { motion } from 'framer-motion'
import { CheckCircle, Edit2, AlertCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'

import { reportsApi } from '@/api/reports'
import type { ReportUploadResponse, LabValue } from '@/api/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'

interface Props {
  uploadResponse: ReportUploadResponse
  reportType?: 'lab_report' | 'prescription'
  onConfirmed: () => void
}

interface ValueState {
  confirmed: boolean
  editing: boolean
  editValue: string
  editUnit: string
  error: string | null
  saving: boolean
}

// Common lab units for the dropdown
const COMMON_UNITS = [
  'mg/dL', 'mmol/L', 'g/dL', 'g/L', 'mEq/L', 'U/L', 'IU/L',
  'μg/dL', 'ng/mL', 'pg/mL', '%', 'cells/μL', 'K/μL', 'mg', 'mcg',
]

export default function ValueConfirmation({ uploadResponse, onConfirmed }: Props) {
  const { t } = useTranslation()

  // Filter values that need confirmation
  const valuesNeedingConfirmation: LabValue[] = (uploadResponse.extracted_values as LabValue[])
    .filter(v => v.requiresUserConfirmation)

  const [states, setStates] = useState<Record<string, ValueState>>(() => {
    const init: Record<string, ValueState> = {}
    valuesNeedingConfirmation.forEach(v => {
      init[v.testName] = {
        confirmed: false,
        editing: false,
        editValue: v.extractedValue,
        editUnit: v.unit,
        error: null,
        saving: false,
      }
    })
    return init
  })

  const updateState = (key: string, patch: Partial<ValueState>) =>
    setStates(prev => ({ ...prev, [key]: { ...prev[key], ...patch } }))

  const handleConfirm = async (value: LabValue) => {
    updateState(value.testName, { saving: true, error: null })
    try {
      await reportsApi.confirmValue({
        value_id: uploadResponse.report_id, // value_id not yet on LabValue; use report_id as proxy
        confirmed_value: states[value.testName].editValue,
        confirmed_unit: states[value.testName].editUnit,
      })
      updateState(value.testName, { confirmed: true, saving: false })
    } catch {
      updateState(value.testName, { error: t('errors.generic'), saving: false })
    }
  }

  const allConfirmed = valuesNeedingConfirmation.every(v => states[v.testName]?.confirmed)

  // If nothing needs confirmation, proceed immediately
  if (valuesNeedingConfirmation.length === 0) {
    onConfirmed()
    return null
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-4"
    >
      <div>
        <h3 className="font-semibold text-white">{t('report.confirm_title')}</h3>
        <p className="mt-1 text-sm text-surface-600">{t('report.confirm_subtitle')}</p>
      </div>

      {valuesNeedingConfirmation.map(value => {
        const s = states[value.testName]
        return (
          <div
            key={value.testName}
            className="rounded-xl border border-surface-700/60 bg-surface-800/40 p-4 space-y-3"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs text-surface-600">{t('report.confirm_value_read')}</p>
                <p className="font-mono text-lg font-semibold text-white">
                  {value.extractedValue} <span className="text-primary-400">{value.unit}</span>
                </p>
                <p className="text-sm text-slate-400">{value.testName}</p>
              </div>
              {s.confirmed && (
                <CheckCircle className="h-5 w-5 text-health-safe shrink-0 mt-1" />
              )}
            </div>

            {!s.confirmed && (
              <>
                <p className="text-sm text-slate-300">{t('report.confirm_question')}</p>

                {!s.editing ? (
                  <div className="flex gap-2">
                    <button
                      id={`confirm-yes-${value.testName}`}
                      type="button"
                      onClick={() => handleConfirm(value)}
                      disabled={s.saving}
                      className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50 transition"
                    >
                      {s.saving ? <LoadingSpinner size="sm" /> : <CheckCircle className="h-4 w-4" />}
                      {t('report.confirm_yes')}
                    </button>
                    <button
                      id={`confirm-edit-${value.testName}`}
                      type="button"
                      onClick={() => updateState(value.testName, { editing: true })}
                      className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-surface-600 py-2 text-sm font-medium text-slate-300 hover:border-primary-500 transition"
                    >
                      <Edit2 className="h-4 w-4" />
                      {t('report.confirm_edit')}
                    </button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <div className="flex-1">
                        <label className="mb-1 block text-xs text-surface-600">
                          {t('report.confirm_edit_label')}
                        </label>
                        <input
                          id={`edit-value-input-${value.testName}`}
                          type="number"
                          step="any"
                          value={s.editValue}
                          onChange={e => updateState(value.testName, { editValue: e.target.value })}
                          className="w-full rounded-lg border border-surface-700 bg-surface-900/50 px-3 py-2 text-sm text-white font-mono outline-none focus:border-primary-500"
                        />
                      </div>
                      <div className="w-28">
                        <label className="mb-1 block text-xs text-surface-600">{t('report.confirm_unit_label')}</label>
                        <select
                          id={`edit-unit-${value.testName}`}
                          value={s.editUnit}
                          onChange={e => updateState(value.testName, { editUnit: e.target.value })}
                          className="w-full rounded-lg border border-surface-700 bg-surface-900 px-2 py-2 text-sm text-white outline-none focus:border-primary-500"
                        >
                          {COMMON_UNITS.map(u => <option key={u} value={u}>{u}</option>)}
                        </select>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleConfirm(value)}
                      disabled={s.saving}
                      className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50 transition"
                    >
                      {s.saving ? <LoadingSpinner size="sm" /> : null}
                      {t('common.confirm')}
                    </button>
                  </div>
                )}

                {s.error && (
                  <div className="flex items-center gap-2 text-xs text-health-abnormal">
                    <AlertCircle className="h-3.5 w-3.5" />
                    {s.error}
                  </div>
                )}
              </>
            )}
          </div>
        )
      })}

      {allConfirmed && (
        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          id="confirm-all-done"
          type="button"
          onClick={onConfirmed}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-primary py-2.5 text-sm font-semibold text-white shadow-glow"
        >
          <CheckCircle className="h-4 w-4" />
          {t('report.confirm_done')}
        </motion.button>
      )}
    </motion.div>
  )
}
