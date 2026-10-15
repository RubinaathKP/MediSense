/**
 * sessionStore — session-scoped state
 * Tracks the active session_id, loaded report, and language selection.
 * session_id must be passed to every /query and /claims/verify call.
 */
import { create } from 'zustand'
import type { LabValue, Prescription } from '@/api/types'

interface SessionState {
  session_id: string | null
  report_id: string | null
  report_type: 'lab_report' | 'prescription' | null
  lab_values: LabValue[]
  prescriptions: Prescription[]
  report_loaded: boolean
  consent_request_id: string | null   // set after ABDM pull, cleared on report load
  consent_status: 'idle' | 'pending' | 'granted' | 'denied' | 'timeout'

  setSession: (sessionId: string) => void
  setReportLoaded: (
    reportId: string,
    type: 'lab_report' | 'prescription',
    labValues: LabValue[],
    prescriptions: Prescription[]
  ) => void
  setConsentRequest: (consentRequestId: string) => void
  setConsentStatus: (status: SessionState['consent_status']) => void
  clearSession: () => void
}

export const useSessionStore = create<SessionState>((set) => ({
  session_id: null,
  report_id: null,
  report_type: null,
  lab_values: [],
  prescriptions: [],
  report_loaded: false,
  consent_request_id: null,
  consent_status: 'idle',

  setSession: (sessionId) => set({ session_id: sessionId }),

  setReportLoaded: (reportId, type, labValues, prescriptions) =>
    set({
      report_id: reportId,
      report_type: type,
      lab_values: labValues,
      prescriptions: prescriptions,
      report_loaded: true,
      consent_status: 'granted',
    }),

  setConsentRequest: (consentRequestId) =>
    set({ consent_request_id: consentRequestId, consent_status: 'pending' }),

  setConsentStatus: (status) => set({ consent_status: status }),

  clearSession: () =>
    set({
      session_id: null,
      report_id: null,
      report_type: null,
      lab_values: [],
      prescriptions: [],
      report_loaded: false,
      consent_request_id: null,
      consent_status: 'idle',
    }),
}))
