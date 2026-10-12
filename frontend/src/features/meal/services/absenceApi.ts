import { apiRequest } from '../../../lib/api'

export type AbsenceMeal = 'BREAKFAST' | 'LUNCH' | 'DINNER'

export interface AbsenceInput {
  startDate: string
  endDate: string
  meals: AbsenceMeal[]
  note?: string
}

export interface AbsenceNotice extends AbsenceInput {
  id: string
  createdAt: string
  updatedAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal/absences${path}`, init, 'Absence API')
}

const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const absenceApi = {
  create: (studentId: string, input: AbsenceInput) => request<AbsenceNotice>(`?${student(studentId)}`,
    { method: 'POST', body: JSON.stringify(input) }),
  list: (studentId: string, from: string, to: string) => request<AbsenceNotice[]>(
    `?${student(studentId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),
  byId: (studentId: string, id: string) => request<AbsenceNotice>(
    `/${encodeURIComponent(id)}?${student(studentId)}`),
}
