import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

export type Locale = 'en-BD' | 'bn-BD'

const messages = {
  'en-BD': { dashboard: 'Dashboard', menu: 'Menu', bills: 'Bills & Payments', greeting: 'Good afternoon', today: "Today's menu", management: 'Meal Management' },
  'bn-BD': { dashboard: 'ড্যাশবোর্ড', menu: 'মেনু', bills: 'বিল ও পেমেন্ট', greeting: 'শুভ অপরাহ্ণ', today: 'আজকের মেনু', management: 'মিল ব্যবস্থাপনা' },
} as const

interface LocaleValue {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: keyof typeof messages['en-BD']) => string
}

const LocaleContext = createContext<LocaleValue | null>(null)

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => localStorage.getItem('campus-day-locale') === 'bn-BD' ? 'bn-BD' : 'en-BD')
  const value = useMemo(() => ({
    locale,
    setLocale: (next: Locale) => { localStorage.setItem('campus-day-locale', next); setLocaleState(next) },
    t: (key: keyof typeof messages['en-BD']) => messages[locale][key],
  }), [locale])
  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
}

// Context and its hook intentionally share this small module.
// eslint-disable-next-line react-refresh/only-export-components
export function useLocale() {
  const value = useContext(LocaleContext)
  if (!value) throw new Error('useLocale must be used inside LocaleProvider')
  return value
}
