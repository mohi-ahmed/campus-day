import type { Locale } from '../i18n/LocaleContext'

const timeZone = 'Asia/Dhaka'

export function formatDate(date: string | Date, locale: Locale = 'en-BD', options: Intl.DateTimeFormatOptions = {}) {
  return new Intl.DateTimeFormat(locale, { timeZone, day: 'numeric', month: 'short', year: 'numeric', ...options }).format(new Date(date))
}

export function formatCurrency(amount: number, accounting = false) {
  const formatted = new Intl.NumberFormat('en-BD', { maximumFractionDigits: 0 }).format(amount)
  return accounting ? `BDT ${formatted}` : `৳${formatted}`
}

export const businessTimeZone = timeZone
