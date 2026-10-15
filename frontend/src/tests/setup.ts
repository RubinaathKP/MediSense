import React from 'react'
import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// Clean up after each test
afterEach(() => {
  cleanup()
})

const mockI18n = {
  language: 'en',
  changeLanguage: vi.fn((lang: string) => {
    mockI18n.language = lang
    return Promise.resolve()
  }),
}

// Mock i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) => {
      // Return interpolated key for testing
      if (opts) {
        return Object.entries(opts).reduce(
          (str, [k, v]) => str.replace(`{{${k}}}`, String(v)),
          key
        )
      }
      return key
    },
    i18n: mockI18n,
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
  initReactI18next: { type: '3rdParty', init: vi.fn() },
}))

// Mock framer-motion for test performance
vi.mock('framer-motion', () => ({
  motion: {
    div: ({ children, ...props }: any) => React.createElement('div', props, children),
    button: ({ children, ...props }: any) => React.createElement('button', props, children),
    form: ({ children, ...props }: any) => React.createElement('form', props, children),
  },
  AnimatePresence: ({ children }: any) => children,
}))

// Mock window.location
Object.defineProperty(window, 'location', {
  value: { href: '', assign: vi.fn() },
  writable: true,
})
