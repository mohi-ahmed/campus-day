import { apiRequest } from '../../lib/api'

export interface StudyPreferencesInput {
  focusMinutes: number
  shortBreakMinutes: number
  longBreakMinutes: number
  dailyFocusGoalMinutes: number
  weeklySessionGoal: number
  topicCompletionGoal: number
  taskReminders: boolean
  routineReminders: boolean
  examReminders: boolean
  reminderLeadMinutes: number
  morningDigest: boolean
  quietHoursEnabled: boolean
  dailyReminderLimit: number
}

export interface StudyPreferencesRecord extends StudyPreferencesInput {
  id: string
  createdAt: string
  updatedAt: string
}

const base = '/api/study/preferences'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyPreferencesApi = {
  get: (studentId: string) =>
    apiRequest<StudyPreferencesRecord>(
      `${base}?${student(studentId)}`,
      undefined,
      'Study preferences API',
    ),

  update: (studentId: string, input: StudyPreferencesInput) =>
    apiRequest<StudyPreferencesRecord>(
      `${base}?${student(studentId)}`,
      { method: 'PUT', body: JSON.stringify(input) },
      'Study preferences API',
    ),
}
