import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import TopBar from './TopBar'
import Disclaimer from './Disclaimer'
import { useTokenRefresh } from '@/hooks/useTokenRefresh'

export default function DashboardLayout() {
  // Proactive token refresh scheduling for entire authenticated session
  useTokenRefresh()

  return (
    <div className="flex h-screen overflow-hidden bg-surface-900 text-slate-100">
      {/* Sidebar (Desktop left + Mobile bottom) */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Sticky Persistent Non-Dismissible Disclaimer (NFR-17) */}
        <Disclaimer />

        {/* TopBar with Page Title, Language Toggle, and Avatar */}
        <TopBar />

        {/* Scrollable Page Outlet */}
        <main className="flex-1 overflow-y-auto px-4 py-6 md:px-8 pb-24 md:pb-8" id="main-content">
          <div className="mx-auto max-w-5xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
