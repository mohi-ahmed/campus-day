import { apiRequest, apiUrl } from '../../lib/api'

export interface CalendarIntegrationStatus {
  googleConfigured: boolean
  googleConnected: boolean
  googleConnectedAt: string | null
  googleLastSyncedAt: string | null
  appleExportAvailable: boolean
}

export interface CalendarSyncResult {
  created: number
  updated: number
  deleted: number
  syncedAt: string
}

interface CalendarAuthorizationResponse {
  authorizationUrl: string
}

const base = '/api/study/calendar/integrations'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyCalendarIntegrationApi = {
  status: (studentId: string) =>
    apiRequest<CalendarIntegrationStatus>(
      `${base}/status?${student(studentId)}`,
      undefined,
      'Calendar integration API',
    ),

  googleAuthorizationUrl: (studentId: string) =>
    apiRequest<CalendarAuthorizationResponse>(
      `${base}/google/authorization-url?${student(studentId)}`,
      { method: 'POST' },
      'Google Calendar API',
    ),

  syncGoogle: (studentId: string, from: string, to: string) =>
    apiRequest<CalendarSyncResult>(
      `${base}/google/sync?${student(studentId)}`,
      { method: 'POST', body: JSON.stringify({ from, to }) },
      'Google Calendar API',
    ),

  disconnectGoogle: (studentId: string) =>
    apiRequest<void>(
      `${base}/google?${student(studentId)}`,
      { method: 'DELETE' },
      'Google Calendar API',
    ),

  downloadAppleCalendar: async (studentId: string, from: string, to: string) => {
    const path = `${base}/apple.ics?${student(studentId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    let response: Response
    try {
      response = await fetch(apiUrl(path), { headers: { Accept: 'text/calendar' } })
    } catch {
      throw new Error('Calendar integration API is unreachable. Check that the backend is running.')
    }
    if (!response.ok) {
      const body = await response.text().catch(() => '')
      try {
        const problem = JSON.parse(body) as { detail?: string; message?: string }
        throw new Error(problem.detail ?? problem.message ?? 'Apple Calendar export failed')
      } catch (error) {
        if (error instanceof SyntaxError) throw new Error(body || 'Apple Calendar export failed')
        throw error
      }
    }
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = 'campus-day-study.ics'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(downloadUrl)
  },
}
