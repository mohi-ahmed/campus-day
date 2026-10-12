import { apiRequest } from '../../lib/api'
import type { StudyTaskPriority, StudyTaskRecord, StudyTaskRepeat } from './studyTaskApi'

export type TaskbotConversationRole = 'USER' | 'ASSISTANT'
export type TaskbotIntent = 'STUDY_STATUS' | 'TASK_DRAFT' | 'STUDY_PLAN' | 'RESCHEDULE' | 'GENERAL_STUDY_HELP'
export type TaskbotAction = 'NONE' | 'CREATE_TASK_DRAFT'
export type TaskbotPlanItemType = 'STUDY' | 'TASK' | 'SHORT_BREAK' | 'LONG_BREAK'

export interface TaskbotConversationMessage {
  role: TaskbotConversationRole
  content: string
}

export interface TaskbotTaskDraft {
  title: string
  subject: string
  taskType: string
  priority: StudyTaskPriority
  dueDate: string
  dueTime?: string | null
  estimatedMinutes?: number | null
  repeat: StudyTaskRepeat
  note?: string | null
}

export interface TaskbotPlanItem {
  type: TaskbotPlanItemType
  title: string
  subject?: string | null
  date?: string | null
  startTime?: string | null
  durationMinutes?: number | null
  sourceTaskId?: string | null
  reason?: string | null
}

export interface TaskbotChatResponse {
  reply: string
  intent: TaskbotIntent
  proposedAction: TaskbotAction
  taskDraft?: TaskbotTaskDraft | null
  plan: TaskbotPlanItem[]
}

export interface TaskbotStatusResponse {
  available: boolean
  provider: string
  model: string
}

const base = '/api/study/assistant'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyTaskbotApi = {
  status: () => apiRequest<TaskbotStatusResponse>(`${base}/status`, undefined, 'Taskbot API'),

  chat: (studentId: string, message: string, history: TaskbotConversationMessage[]) =>
    apiRequest<TaskbotChatResponse>(`${base}/chat?${student(studentId)}`, {
      method: 'POST',
      body: JSON.stringify({ message, history }),
    }, 'Taskbot API'),

  confirmTask: (studentId: string, draft: TaskbotTaskDraft) =>
    apiRequest<StudyTaskRecord>(`${base}/tasks/confirm?${student(studentId)}`, {
      method: 'POST',
      body: JSON.stringify(draft),
    }, 'Taskbot API'),
}
