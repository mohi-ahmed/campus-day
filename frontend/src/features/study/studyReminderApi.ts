import { apiRequest } from '../../lib/api'

export type StudyReminderType = 'TASK_DEADLINE' | 'ROUTINE_BLOCK' | 'SYLLABUS_TARGET' | 'MORNING_DIGEST'
export type StudyReminderStatus = 'SCHEDULED' | 'DELIVERED' | 'READ' | 'DISMISSED' | 'CANCELLED'

export interface StudyReminderRecord {
  id: string
  type: StudyReminderType
  sourceId: string
  title: string
  message: string
  actionPath: string | null
  scheduledAt: string
  status: StudyReminderStatus
  deliveredAt: string | null
  readAt: string | null
  dismissedAt: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface StudyReminderInbox {
  unreadCount: number
  scheduledCount: number
  reminders: StudyReminderRecord[]
}

const base = '/api/study/reminders'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyReminderApi = {
  inbox: (studentId: string) =>
    apiRequest<StudyReminderInbox>(`${base}?${student(studentId)}`, undefined, 'Study reminder API'),

  markRead: (studentId: string, reminderId: string) =>
    apiRequest<StudyReminderRecord>(
      `${base}/${encodeURIComponent(reminderId)}/read?${student(studentId)}`,
      { method: 'PUT' },
      'Study reminder API',
    ),

  dismiss: (studentId: string, reminderId: string) =>
    apiRequest<StudyReminderRecord>(
      `${base}/${encodeURIComponent(reminderId)}/dismiss?${student(studentId)}`,
      { method: 'PUT' },
      'Study reminder API',
    ),

  cancel: (studentId: string, reminderId: string) =>
    apiRequest<void>(
      `${base}/${encodeURIComponent(reminderId)}?${student(studentId)}`,
      { method: 'DELETE' },
      'Study reminder API',
    ),
}
