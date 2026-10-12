import { apiRequest } from '../../lib/api'

export type FocusSessionOutcome = 'COMPLETED' | 'STOPPED'

export interface FocusSessionInput {
  taskId: string
  focusDate: string
  startedAt: string
  endedAt: string
  plannedMinutes: number
  focusedMinutes: number
  outcome: FocusSessionOutcome
}

export interface FocusSessionRecord extends FocusSessionInput {
  id: string
  taskTitle: string
  subject: string
  createdAt: string
}

const base = '/api/study/focus-sessions'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyFocusApi = {
  list: (studentId: string, from: string, to: string) =>
    apiRequest<FocusSessionRecord[]>(
      `${base}?${student(studentId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      undefined,
      'Study focus API',
    ),

  record: (studentId: string, input: FocusSessionInput) =>
    apiRequest<FocusSessionRecord>(
      `${base}?${student(studentId)}`,
      { method: 'POST', body: JSON.stringify(input) },
      'Study focus API',
    ),
}
