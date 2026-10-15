/**
 * ReportUpload — PDF upload with all states.
 *
 * Backend integration (ReportController.java):
 *  POST /reports/upload  multipart/form-data
 *    params: file (PDF), report_type, session_id?
 *
 * Error codes from GlobalExceptionHandler:
 *  FILE_TOO_LARGE (400) → show error state
 *  INVALID_FILE_TYPE (400) → show error state
 *  DOCUMENT_QUALITY_FAILED (400) → OCR confidence < 0.85, show error
 *
 * Success with requires_confirmation=true (OCR 0.85–0.90) → trigger ValueConfirmation
 *
 * Rate limit: 10/hr/user (Tier 2) — pre-check from rateLimitStore before uploading
 */
import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { motion, AnimatePresence } from 'framer-motion'
import { Upload, FileText, CheckCircle, AlertCircle, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AxiosError } from 'axios'

import { reportsApi } from '@/api/reports'
import { useSessionStore } from '@/store/sessionStore'
import { useRateLimit } from '@/hooks/useRateLimit'
import type { ApiError, ReportUploadResponse } from '@/api/types'
import RateLimitBanner from '@/components/common/RateLimitBanner'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ValueConfirmation from './ValueConfirmation'
import { cn } from '@/utils/cn'

const MAX_SIZE_BYTES = 10 * 1024 * 1024  // matches backend MAX_FILE_SIZE_BYTES

interface Props {
  onUploadSuccess: (response: ReportUploadResponse, reportType: 'lab_report' | 'prescription') => void
}

type UploadState = 'idle' | 'drag-active' | 'file-selected' | 'uploading' | 'success'
  | 'error-size' | 'error-type' | 'error-ocr' | 'error-rate-limit' | 'error-generic'
  | 'confirming'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function ReportUpload({ onUploadSuccess }: Props) {
  const { t } = useTranslation()
  const { session_id } = useSessionStore()
  const uploadRateLimit = useRateLimit('reports-upload')

  const [uploadState, setUploadState] = useState<UploadState>('idle')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [reportType, setReportType] = useState<'lab_report' | 'prescription'>('lab_report')
  const [uploadResponse, setUploadResponse] = useState<ReportUploadResponse | null>(null)
  const [rateLimitSeconds, setRateLimitSeconds] = useState(0)

  const onDrop = useCallback((acceptedFiles: File[], rejectedFiles: any[]) => {
    if (rejectedFiles && rejectedFiles.length > 0) {
      const errorCode = rejectedFiles[0]?.errors?.[0]?.code
      if (errorCode === 'file-too-large') { setUploadState('error-size'); return }
      if (errorCode === 'file-invalid-type') { setUploadState('error-type'); return }
    }
    if (acceptedFiles[0]) {
      setSelectedFile(acceptedFiles[0])
      setUploadState('file-selected')
    }
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'] },
    maxSize: MAX_SIZE_BYTES,
    multiple: false,
    disabled: uploadState === 'uploading',
  })

  const handleUpload = async () => {
    if (!selectedFile) return

    // Pre-check rate limit quota
    if (uploadRateLimit.is_limited) {
      setRateLimitSeconds(uploadRateLimit.countdown)
      setUploadState('error-rate-limit')
      return
    }

    setUploadState('uploading')
    try {
      const response = await reportsApi.uploadReport(selectedFile, reportType, session_id ?? undefined)
      const data = response.data

      if (data.requires_confirmation) {
        // OCR confidence 0.85–0.90 — enter confirmation flow
        setUploadResponse(data)
        setUploadState('confirming')
      } else {
        setUploadState('success')
        onUploadSuccess(data, reportType)
      }
    } catch (err) {
      const axiosErr = err as AxiosError<ApiError>
      const errorCode = axiosErr.response?.data?.error_code

      if (errorCode === 'FILE_TOO_LARGE')        { setUploadState('error-size'); return }
      if (errorCode === 'INVALID_FILE_TYPE')      { setUploadState('error-type'); return }
      if (errorCode === 'DOCUMENT_QUALITY_FAILED') { setUploadState('error-ocr'); return }
      if (axiosErr.response?.status === 429) {
        setRateLimitSeconds(axiosErr.response.data?.retry_after_seconds ?? 3600)
        setUploadState('error-rate-limit')
        return
      }
      setUploadState('error-generic')
    }
  }

  const reset = () => {
    setSelectedFile(null)
    setUploadState('idle')
    setUploadResponse(null)
  }

  // ValueConfirmation flow
  if (uploadState === 'confirming' && uploadResponse) {
    return (
      <ValueConfirmation
        uploadResponse={uploadResponse}
        reportType={reportType}
        onConfirmed={() => {
          setUploadState('success')
          onUploadSuccess(uploadResponse, reportType)
        }}
      />
    )
  }

  return (
    <div className="space-y-4">
      {/* Report type toggle */}
      <div className="flex rounded-xl border border-surface-700 bg-surface-900/50 p-1">
        {(['lab_report', 'prescription'] as const).map(type => (
          <button
            key={type}
            id={`report-type-${type}`}
            type="button"
            onClick={() => setReportType(type)}
            className={cn(
              'flex-1 rounded-lg py-2 text-sm font-medium transition',
              reportType === type
                ? 'bg-primary-600 text-white shadow-glow'
                : 'text-surface-600 hover:text-slate-300'
            )}
          >
            {type === 'lab_report' ? t('report.report_type_lab') : t('report.report_type_prescription')}
          </button>
        ))}
      </div>

      {/* Rate limit banner */}
      {uploadState === 'error-rate-limit' && (
        <RateLimitBanner
          retryAfterSeconds={rateLimitSeconds}
          onExpired={() => setUploadState('idle')}
        />
      )}

      {/* Drop zone */}
      <div
        {...getRootProps()}
        id="report-dropzone"
        className={cn(
          'relative cursor-pointer rounded-2xl border-2 border-dashed transition-all duration-200 focus:outline-none',
          isDragActive
            ? 'border-primary-400 bg-primary-600/10 shadow-glow'
            : uploadState === 'success'
            ? 'border-health-safe/50 bg-health-safe/5'
            : uploadState.startsWith('error')
            ? 'border-health-abnormal/50 bg-health-abnormal/5'
            : 'border-surface-600 bg-surface-800/30 hover:border-primary-500 hover:bg-primary-600/5',
        )}
      >
        <input {...getInputProps({ 'data-testid': 'file-input' } as any)} />

        <div className="flex flex-col items-center justify-center px-6 py-10 text-center">
          <AnimatePresence mode="wait">
            {uploadState === 'idle' && (
              <motion.div key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-700">
                  <Upload className="h-6 w-6 text-primary-400" />
                </div>
                <p className="font-medium text-slate-300">{t('report.upload_idle')}</p>
                <p className="text-xs text-surface-600">{t('report.upload_idle_subtitle')}</p>
              </motion.div>
            )}

            {isDragActive && (
              <motion.div key="drag" initial={{ scale: 0.95 }} animate={{ scale: 1 }} className="space-y-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-600/20">
                  <Upload className="h-6 w-6 text-primary-400 animate-bounce" />
                </div>
                <p className="font-medium text-primary-300">{t('report.upload_drag_active')}</p>
              </motion.div>
            )}

            {uploadState === 'file-selected' && selectedFile && (
              <motion.div key="selected" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} className="space-y-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-600/20">
                  <FileText className="h-6 w-6 text-primary-400" />
                </div>
                <p className="font-medium text-slate-300">
                  {t('report.upload_selected', { filename: selectedFile.name, size: formatBytes(selectedFile.size) })}
                </p>
                <button
                  type="button"
                  onClick={e => { e.stopPropagation(); reset() }}
                  className="text-xs text-surface-600 hover:text-slate-400 flex items-center gap-1 mx-auto"
                >
                  <X className="h-3 w-3" /> Remove
                </button>
              </motion.div>
            )}

            {uploadState === 'uploading' && (
              <motion.div key="uploading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
                <LoadingSpinner size="lg" className="mx-auto border-primary-500 border-t-primary-200" />
                <p className="font-medium text-slate-300">{t('report.upload_uploading')}</p>
              </motion.div>
            )}

            {uploadState === 'success' && (
              <motion.div key="success" initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="space-y-2">
                <CheckCircle className="mx-auto h-12 w-12 text-health-safe" />
                <p className="font-medium text-health-safe">{t('report.upload_success')}</p>
              </motion.div>
            )}

            {uploadState.startsWith('error-') && uploadState !== 'error-rate-limit' && (
              <motion.div key="error" initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} className="space-y-2">
                <AlertCircle className="mx-auto h-10 w-10 text-health-abnormal" />
                <p className="font-medium text-health-abnormal">
                  {uploadState === 'error-size' && t('report.error_file_too_large')}
                  {uploadState === 'error-type' && t('report.error_invalid_type')}
                  {uploadState === 'error-ocr'  && t('report.error_ocr_failed')}
                  {uploadState === 'error-generic' && t('errors.generic')}
                </p>
                <button type="button" onClick={reset} className="text-xs text-primary-400 hover:text-primary-300">
                  {t('common.retry')}
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Upload button — only show when file selected */}
      {uploadState === 'file-selected' && (
        <motion.button
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          id="upload-submit-button"
          type="button"
          onClick={handleUpload}
          disabled={uploadRateLimit.is_limited}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-primary py-2.5 text-sm font-semibold text-white shadow-glow transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Upload className="h-4 w-4" />
          {t('report.upload_button')}
        </motion.button>
      )}

      {/* Rate limit remaining indicator */}
      {!uploadRateLimit.is_limited && uploadRateLimit.remaining < Infinity && (
        <p className="text-center text-xs text-surface-600">
          {t('rate_limit.uploads_remaining', { remaining: uploadRateLimit.remaining })}
        </p>
      )}
    </div>
  )
}
