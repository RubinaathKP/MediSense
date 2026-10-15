import { useState } from 'react'
import { X, CheckCircle2, ChevronRight } from 'lucide-react'
import type { EvalRunSummary } from '@/api/types'
import { useEvalFailures } from '@/hooks/useEvalData'
import LoadingSpinner from '@/components/common/LoadingSpinner'

interface RunDetailModalProps {
  run: EvalRunSummary
  onClose: () => void
}

export default function RunDetailModal({ run, onClose }: RunDetailModalProps) {
  const [showFailures, setShowFailures] = useState(false)
  const { data: failures = [], isLoading: isFailuresLoading } = useEvalFailures(
    showFailures ? run.run_id : null
  )

  const passedCases = run.summary_metrics?.passed_cases ?? 0
  const totalCases = run.summary_metrics?.total_cases ?? 100
  const failedCases = totalCases - passedCases

  return (
    <div
      data-testid="run-detail-modal"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm animate-fade-in"
    >
      <div className="relative w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-3xl border border-surface-700 bg-surface-800 p-6 shadow-2xl space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-surface-700/60 pb-3">
          <div className="space-y-0.5">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <span>Evaluation Run Detail</span>
              {run.passed ? (
                <span className="rounded-md bg-emerald-500/20 px-2 py-0.5 text-xs font-bold text-emerald-400 border border-emerald-500/30">
                  PASS
                </span>
              ) : (
                <span className="rounded-md bg-rose-500/20 px-2 py-0.5 text-xs font-bold text-rose-400 border border-rose-500/30">
                  FAIL
                </span>
              )}
            </h3>
            <p className="font-mono text-xs text-surface-400">ID: {run.run_id}</p>
          </div>

          <button
            type="button"
            data-testid="modal-close-btn"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-xl bg-surface-700/60 text-surface-400 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Summary Stats */}
        <div className="grid grid-cols-3 gap-3">
          <div className="rounded-xl border border-surface-700/40 bg-surface-900/40 p-3 text-center">
            <p className="text-[10px] uppercase font-bold text-surface-400">Total Cases</p>
            <p className="font-mono text-xl font-extrabold text-white mt-0.5">{totalCases}</p>
          </div>
          <div className="rounded-xl border border-emerald-500/20 bg-emerald-950/20 p-3 text-center">
            <p className="text-[10px] uppercase font-bold text-emerald-400">Passed</p>
            <p className="font-mono text-xl font-extrabold text-emerald-300 mt-0.5">{passedCases}</p>
          </div>
          <div className="rounded-xl border border-rose-500/20 bg-rose-950/20 p-3 text-center">
            <p className="text-[10px] uppercase font-bold text-rose-400">Failed</p>
            <p className="font-mono text-xl font-extrabold text-rose-300 mt-0.5">{failedCases}</p>
          </div>
        </div>

        {/* Toggle Failures View */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold uppercase tracking-wider text-surface-400">
              Failure Audit Analysis
            </h4>
            <button
              type="button"
              data-testid="toggle-failures-btn"
              onClick={() => setShowFailures((v) => !v)}
              className="flex items-center gap-1.5 text-xs font-semibold text-primary-400 hover:text-primary-300 transition"
            >
              <span>{showFailures ? 'Hide Failures' : 'View Failures List'}</span>
              <ChevronRight
                className={`h-3.5 w-3.5 transition-transform ${showFailures ? 'rotate-90' : ''}`}
              />
            </button>
          </div>

          {showFailures && (
            <div data-testid="failures-table-section" className="space-y-2 animate-fade-in">
              {isFailuresLoading ? (
                <div className="flex py-8 justify-center">
                  <LoadingSpinner size="md" />
                </div>
              ) : failures.length === 0 ? (
                <div className="rounded-xl border border-emerald-500/20 bg-emerald-950/10 p-4 text-center text-xs text-emerald-300 flex items-center justify-center gap-2">
                  <CheckCircle2 className="h-4 w-4" />
                  <span>No failures logged for this evaluation run. All test cases passed!</span>
                </div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-surface-700">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-surface-900/80 text-[10px] font-bold uppercase tracking-wider text-surface-400 border-b border-surface-700">
                      <tr>
                        <th className="px-3 py-2">Test Case ID</th>
                        <th className="px-3 py-2">Failure Reason</th>
                        <th className="px-3 py-2">Expected vs Actual</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-surface-700/50 font-mono">
                      {failures.map((f, i) => (
                        <tr key={f.test_case_id || i} className="hover:bg-surface-700/30">
                          <td className="px-3 py-2 font-bold text-rose-400">{f.test_case_id}</td>
                          <td className="px-3 py-2 text-slate-200 font-sans">{f.failure_reason}</td>
                          <td className="px-3 py-2 text-surface-400">{f.actual_vs_expected}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
