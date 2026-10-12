import { apiRequest } from '../../lib/api'

export type StudyTaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'
export type StudyTaskRepeat = 'NONE' | 'DAILY' | 'WEEKLY'
export type StudyTaskStatus = 'PENDING' | 'COMPLETED'

export interface StudyTaskInput {
  title: string
  subject: string
  taskType: string
  priority: StudyTaskPriority
  dueDate: string
  dueTime?: string
  estimatedMinutes?: number
  repeat: StudyTaskRepeat
  note?: string
}

export interface StudyTaskRecord extends StudyTaskInput {
  id: string
  status: StudyTaskStatus
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

const base = '/api/study/tasks'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyTaskApi = {
  list: (studentId: string) =>
    apiRequest<StudyTaskRecord[]>(`${base}?${student(studentId)}`, undefined, 'Study task API'),

  create: (studentId: string, input: StudyTaskInput) =>
    apiRequest<StudyTaskRecord>(`${base}?${student(studentId)}`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Study task API'),

  update: (studentId: string, taskId: string, input: StudyTaskInput) =>
    apiRequest<StudyTaskRecord>(
      `${base}/${encodeURIComponent(taskId)}?${student(studentId)}`,
      { method: 'PUT', body: JSON.stringify(input) },
      'Study task API',
    ),

  complete: (studentId: string, taskId: string) =>
    apiRequest<StudyTaskRecord>(
      `${base}/${encodeURIComponent(taskId)}/complete?${student(studentId)}`,
      { method: 'POST' },
      'Study task API',
    ),

  reopen: (studentId: string, taskId: string) =>
    apiRequest<StudyTaskRecord>(
      `${base}/${encodeURIComponent(taskId)}/reopen?${student(studentId)}`,
      { method: 'POST' },
      'Study task API',
    ),

  remove: (studentId: string, taskId: string) =>
    apiRequest<void>(
      `${base}/${encodeURIComponent(taskId)}?${student(studentId)}`,
      { method: 'DELETE' },
      'Study task API',
    ),
}
