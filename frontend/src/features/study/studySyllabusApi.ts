import { apiRequest } from '../../lib/api'

export type SyllabusTopicStatus = 'NOT_STARTED' | 'LEARNING' | 'REVISION' | 'COMPLETED'

export interface SyllabusTopicRecord {
  id: string
  syllabusSubjectId: string
  title: string
  note: string | null
  status: SyllabusTopicStatus
  displayOrder: number
  targetDate: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface SyllabusSubjectProgressRecord {
  subject: {
    id: string
    name: string
    description: string | null
    color: string | null
    displayOrder: number
    active: boolean
    createdAt: string
    updatedAt: string
  }
  completedTopics: number
  totalTopics: number
  topics: SyllabusTopicRecord[]
}

const base = '/api/study/syllabus'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studySyllabusApi = {
  progress: (studentId: string) =>
    apiRequest<SyllabusSubjectProgressRecord[]>(
      `${base}/subjects/progress?${student(studentId)}&activeOnly=true`,
      undefined,
      'Study syllabus API',
    ),

  updateStatus: (studentId: string, subjectId: string, topicId: string, status: SyllabusTopicStatus) =>
    apiRequest<SyllabusTopicRecord>(
      `${base}/subjects/${encodeURIComponent(subjectId)}/topics/${encodeURIComponent(topicId)}/status?${student(studentId)}`,
      { method: 'PUT', body: JSON.stringify({ status }) },
      'Study syllabus API',
    ),
}
