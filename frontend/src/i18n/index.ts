import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import en from './en.json'
import ta from './ta.json'

// Determine initial language from authStore state in localStorage if present
let savedUserLang: string | null = null
try {
  const authData = localStorage.getItem('auth-storage')
  if (authData) {
    const parsed = JSON.parse(authData)
    savedUserLang = parsed?.state?.user?.language_pref ?? null
  }
} catch {
  // Ignore localStorage read error
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      ta: { translation: ta },
    },
    lng: savedUserLang || undefined, // Use user preference if detected, else fallback to detector
    fallbackLng: 'en',
    ns: ['translation'],
    defaultNS: 'translation',
    interpolation: {
      escapeValue: false, // React already escapes values
    },
    detection: {
      // 1. User preference / localStorage 2. Browser language (navigator) 3. fallback 'en'
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'i18nextLng',
    },
  })

export default i18n
