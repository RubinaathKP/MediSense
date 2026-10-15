/**
 * ErrorBoundary — Catches all unhandled React rendering errors.
 *
 * Displays a clean user-facing fallback with:
 * - Clear error message
 * - Support error ID (UUID generated client-side)
 * - Refresh button to reload page
 * - Error logging to console (ready for telemetry integration)
 */
import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
  errorId: string | null
}

export default class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorId: null,
  }

  public static getDerivedStateFromError(error: Error): State {
    const errorId = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `err-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`
    return { hasError: true, error, errorId }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error(`[ErrorBoundary] Client-side unhandled exception (ID: ${this.state.errorId}):`, error, errorInfo)
    // Future: sendToTelemetryService({ errorId: this.state.errorId, error, errorInfo })
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, errorId: null })
    if (typeof window !== 'undefined') {
      window.location.reload()
    }
  }

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center p-6 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-danger-500/10 text-danger-400 mb-4 border border-danger-500/20">
            <AlertTriangle className="h-7 w-7" />
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Something went wrong. Please refresh the page.</h2>
          {this.state.errorId && (
            <p className="text-xs font-mono text-surface-400 mb-4">
              Error ID: <span className="text-surface-300 font-semibold">{this.state.errorId}</span>
            </p>
          )}
          <p className="text-sm text-surface-400 max-w-md mb-6">
            An unexpected error occurred. Please try reloading the page to continue.
          </p>
          <button
            type="button"
            onClick={this.handleReset}
            className="flex items-center gap-2 rounded-xl bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-500 transition-all shadow-glow cursor-pointer"
          >
            <RefreshCw className="h-4 w-4" />
            Refresh
          </button>
        </div>
      )
    }

    return this.props.children
  }
}
