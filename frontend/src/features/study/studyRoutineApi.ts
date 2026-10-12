import { apiRequest } from '../../lib/api'

export type RoutineDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface StudyRoutineInput {
  subject: string
  blockType: string
  topic: string
  location?: string
  days: RoutineDay[]
  startTime: string
  endTime: string
}

export interface StudyRoutineRecord extends StudyRoutineInput {
  id: string
  active: boolean
  createdAt: string
  updatedAt: string
}

const base = '/api/study/routines'
const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const studyRoutineApi = {
  list: (studentId: string) =>
    apiRequest<StudyRoutineRecord[]>(
      `${base}?${student(studentId)}&activeOnly=true`,
      undefined,
      'Study routine API',
    ),

  create: (studentId: string, input: StudyRoutineInput) =>
    apiRequest<StudyRoutineRecord>(`${base}?${student(studentId)}`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Study routine API'),

  update: (studentId: string, blockId: string, input: StudyRoutineInput) =>
    apiRequest<StudyRoutineRecord>(
      `${base}/${encodeURIComponent(blockId)}?${student(studentId)}`,
      { method: 'PUT', body: JSON.stringify(input) },
      'Study routine API',
    ),

  remove: (studentId: string, blockId: string) =>
    apiRequest<void>(
      `${base}/${encodeURIComponent(blockId)}?${student(studentId)}`,
      { method: 'DELETE' },
      'Study routine API',
    ),
}
