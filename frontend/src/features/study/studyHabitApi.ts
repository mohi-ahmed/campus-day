import { apiRequest } from '../../lib/api'
import type { RoutineDay } from './studyRoutineApi'

export interface StudyHabitProgressRecord {
  id: string
  name: string
  description: string | null
  repeatDays: RoutineDay[]
  displayOrder: number
  active: boolean
  completedDates: string[]
  createdAt: string
  updatedAt: string
}

const base = '/api/study/habits'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyHabitApi = {
  progress: (studentId: string, from: string, to: string) =>
    apiRequest<StudyHabitProgressRecord[]>(
      `${base}/progress?${student(studentId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&activeOnly=true`,
      undefined,
      'Study habit API',
    ),

  complete: (studentId: string, habitId: string, completedOn: string) =>
    apiRequest(
      `${base}/${encodeURIComponent(habitId)}/completions?${student(studentId)}`,
      { method: 'POST', body: JSON.stringify({ completedOn }) },
      'Study habit API',
    ),

  uncomplete: (studentId: string, habitId: string, completedOn: string) =>
    apiRequest<void>(
      `${base}/${encodeURIComponent(habitId)}/completions/${encodeURIComponent(completedOn)}?${student(studentId)}`,
      { method: 'DELETE' },
      'Study habit API',
    ),
}
