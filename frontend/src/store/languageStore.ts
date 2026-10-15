import { create } from 'zustand'

interface LanguageState {
  language: 'en' | 'ta'
  setLanguage: (lang: 'en' | 'ta') => void
}

export const useLanguageStore = create<LanguageState>((set) => ({
  language: 'en',
  setLanguage: (lang) => set({ language: lang }),
}))
