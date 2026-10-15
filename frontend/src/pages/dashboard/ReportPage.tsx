/**
 * ReportPage — main Report tab
 * Tabs: Upload PDF | Connect ABHA
 * After report loaded: shows ReportValues + "Go to Query" CTA
 */
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Upload, Wifi, ArrowRight } from 'lucide-react'

import { useSessionStore } from '@/store/sessionStore'
import type { ReportUploadResponse } from '@/api/types'
import ReportUpload from '@/components/report/ReportUpload'
import ReportValues from '@/components/report/ReportValues'
import ABHAConnect from '@/components/abdm/ABHAConnect'
import { cn } from '@/utils/cn'

type ActiveTab = 'upload' | 'abdm'

export default function ReportPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const {
    report_loaded, lab_values, prescriptions, report_type,
    setReportLoaded, session_id,
  } = useSessionStore()

  const [activeTab, setActiveTab] = useState<ActiveTab>('upload')

  const handleUploadSuccess = (response: ReportUploadResponse, type: 'lab_report' | 'prescription') => {
    if (session_id) {
      setReportLoaded(
        session_id,
        type,
        type === 'lab_report' ? (response.extracted_values as any) : [],
        type === 'prescription' ? (response.extracted_values as any) : [],
      )
    }
  }

  const handleAbdmRecordReceived = () => {
    // session store is updated by ConsentStatus when data arrives
  }

  const tabs: { key: ActiveTab; label: string; icon: typeof Upload }[] = [
    { key: 'upload', label: t('report.upload_tab'), icon: Upload },
    { key: 'abdm',   label: t('report.abdm_tab'),  icon: Wifi },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">{t('report.page_title')}</h1>
        <p className="mt-1 text-sm text-surface-600">{t('report.empty_state_subtitle')}</p>
      </div>

      {/* If report is loaded, show values */}
      {report_loaded ? (
        <AnimatePresence>
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <ReportValues
              labValues={lab_values}
              prescriptions={prescriptions}
              reportType={report_type ?? 'lab_report'}
            />
            <button
              id="go-to-query-button"
              type="button"
              onClick={() => navigate('/query')}
              className="flex items-center gap-2 rounded-xl bg-gradient-primary px-6 py-3 text-sm font-semibold text-white shadow-glow hover:opacity-90 transition"
            >
              {t('report.go_to_query')} <ArrowRight className="h-4 w-4" />
            </button>
          </motion.div>
        </AnimatePresence>
      ) : (
        <div className="rounded-2xl border border-surface-700/50 bg-surface-800/40 p-6 shadow-glass backdrop-blur-sm">
          {/* Tab selector */}
          <div className="mb-6 flex rounded-xl border border-surface-700 bg-surface-900/50 p-1">
            {tabs.map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                id={`report-tab-${key}`}
                type="button"
                onClick={() => setActiveTab(key)}
                className={cn(
                  'flex flex-1 items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-medium transition',
                  activeTab === key
                    ? 'bg-primary-600 text-white shadow-glow'
                    : 'text-surface-600 hover:text-slate-300'
                )}
              >
                <Icon className="h-4 w-4" />
                {label}
              </button>
            ))}
          </div>

          <AnimatePresence mode="wait">
            {activeTab === 'upload' && (
              <motion.div
                key="upload"
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 8 }}
              >
                <ReportUpload onUploadSuccess={handleUploadSuccess} />
              </motion.div>
            )}
            {activeTab === 'abdm' && (
              <motion.div
                key="abdm"
                initial={{ opacity: 0, x: 8 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -8 }}
              >
                <ABHAConnect
                  onRecordReceived={handleAbdmRecordReceived}
                  onFallbackToPdf={() => setActiveTab('upload')}
                />
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}
    </div>
  )
}
