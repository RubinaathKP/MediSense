/**
 * App.tsx — Root router + QueryClient + store injection + ErrorBoundary + Toaster
 *
 * IMPORTANT: injectStores() is called here before any API calls can happen.
 * This wires the Axios interceptors to the Zustand stores without circular imports.
 */
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { Suspense, lazy } from 'react'

import { injectStores } from '@/api/client'
import { useAuthStore } from '@/store/authStore'
import { useRateLimitStore } from '@/store/rateLimitStore'
import { ProtectedRoute } from '@/components/common/ProtectedRoute'
import DashboardLayout from '@/components/layout/DashboardLayout'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ErrorBoundary from '@/components/common/ErrorBoundary'
import Toaster from '@/components/ui/Toaster'
import { AdminRoute } from '@/components/common/AdminRoute'

// Lazy-load pages for code splitting
const Login        = lazy(() => import('@/pages/auth/Login'))
const Register     = lazy(() => import('@/pages/auth/Register'))
const DashboardHome = lazy(() => import('@/pages/dashboard/DashboardHome'))
const ReportPage   = lazy(() => import('@/pages/dashboard/ReportPage'))
const QueryPage    = lazy(() => import('@/pages/dashboard/QueryPage'))
const ClaimsPage   = lazy(() => import('@/pages/dashboard/ClaimsPage'))
const HistoryPage  = lazy(() => import('@/pages/dashboard/HistoryPage'))
const EvalDashboard = lazy(() => import('@/pages/admin/EvalDashboard'))
const AuditLog      = lazy(() => import('@/pages/admin/AuditLog'))

// Wire Axios interceptors to Zustand stores
injectStores(
  () => useAuthStore.getState(),
  () => useRateLimitStore.getState()
)

// TanStack Query client — retry only non-auth errors
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: unknown) => {
        const status = (error as { response?: { status?: number } })?.response?.status
        if (status === 401 || status === 403 || status === 404) return false
        return failureCount < 2
      },
      staleTime: 30_000,
    },
    mutations: {
      retry: 0,
    },
  },
})

const PageLoader = () => (
  <div className="flex min-h-screen items-center justify-center bg-surface-900">
    <LoadingSpinner size="lg" />
  </div>
)

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Public auth routes */}
              <Route path="/login"    element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* Protected dashboard routes */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <DashboardLayout />
                  </ProtectedRoute>
                }
              >
                <Route index          element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<DashboardHome />} />
                <Route path="report"    element={<ReportPage />} />
                <Route path="query"     element={<QueryPage />} />
                <Route path="claims"    element={<ClaimsPage />} />
                <Route path="history"   element={<HistoryPage />} />
              </Route>

              {/* Protected admin routes */}
              <Route
                path="/admin"
                element={
                  <AdminRoute>
                    <DashboardLayout />
                  </AdminRoute>
                }
              >
                <Route index      element={<Navigate to="/admin/eval" replace />} />
                <Route path="eval"  element={<EvalDashboard />} />
                <Route path="audit" element={<AuditLog />} />
              </Route>

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>

        <Toaster />
        {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      </QueryClientProvider>
    </ErrorBoundary>
  )
}
