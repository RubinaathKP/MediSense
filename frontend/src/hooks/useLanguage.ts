/**
 * useLanguage — hook for managing internationalisation state (Tamil / English).
 *
 * Returns:
 *  - current_language: 'en' | 'ta'
 *  - is_tamil: boolean
 *  - toggle: () => void
 *  - change: (lang: 'en' | 'ta') => Promise<void>
 *
 * Syncs with authStore on mount and dispatches debounced PATCH /users/preferences.
 */
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuthStore } from '@/store/authStore'
import apiClient from '@/api/client'
import i18nInstance from '@/i18n/index'

export function useLanguage() {
  const { i18n } = useTranslation()
  const user = useAuthStore(s => s.user)
  const setLanguagePref = useAuthStore(s => s.setLanguagePref)
  const updateLanguage = useAuthStore(s => s.updateLanguage)
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const activeLang = i18n?.language || i18nInstance.language || 'en'
  const current_language = (activeLang.startsWith('ta') ? 'ta' : 'en') as 'en' | 'ta'
  const is_tamil = current_language === 'ta'

  // Sync with authStore user language on mount
  useEffect(() => {
    if (user?.language_pref && user.language_pref !== current_language) {
      if (i18n?.changeLanguage) i18n.changeLanguage(user.language_pref)
      if (i18nInstance?.changeLanguage) i18nInstance.changeLanguage(user.language_pref)
    }
  }, [user?.language_pref, current_language, i18n])

  const change = async (lang: 'en' | 'ta') => {
    if (i18n?.changeLanguage) await i18n.changeLanguage(lang)
    if (i18nInstance?.changeLanguage) await i18nInstance.changeLanguage(lang)
    if (setLanguagePref) setLanguagePref(lang)
    if (updateLanguage) updateLanguage(lang)

    // Debounced 1 second PATCH /users/preferences
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    debounceTimerRef.current = setTimeout(() => {
      if (user) {
        apiClient.patch('/users/preferences', { language_pref: lang }).catch(() => {
          // Gracefully ignore network/unauth error
        })
      }
    }, 1000)
  }

  const toggle = () => {
    const nextLang = current_language === 'en' ? 'ta' : 'en'
    change(nextLang)
  }

  return {
    current_language,
    toggle,
    is_tamil,
    change,
  }
}

export default useLanguage
