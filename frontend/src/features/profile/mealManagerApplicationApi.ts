import { apiRequest } from '../../lib/api'

export type MealManagerApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'
export type MealManagerDecision = 'APPROVE' | 'REJECT'

export type MealManagerApplication = {
  id: string
  studentId: string
  studentName: string
  studentEmail: string
  managementPeriodId: string
  periodStartDate: string | null
  periodEndDate: string | null
  status: MealManagerApplicationStatus
  submittedAt: string
  expiresAt: string
  decidedAt: string | null
}

export type MealManagerApplicationOverview = {
  eligible: boolean
  currentAccess: boolean
  managementPeriodId: string | null
  periodStartDate: string | null
  periodEndDate: string | null
  application: MealManagerApplication | null
}

export const mealManagerApplicationApi = {
  current: () => apiRequest<MealManagerApplicationOverview>(
    '/api/meal-manager-applications/me', undefined, 'Meal Manager application'),

  apply: () => apiRequest<MealManagerApplication>(
    '/api/meal-manager-applications', { method: 'POST' }, 'Meal Manager application'),

  review: (token: string) => apiRequest<MealManagerApplication>(
    `/api/public/meal-manager-applications/review?token=${encodeURIComponent(token)}`,
    undefined,
    'Meal Manager application'),

  decide: (token: string, decision: MealManagerDecision) => apiRequest<MealManagerApplication>(
    '/api/public/meal-manager-applications/decision', {
      method: 'POST',
      body: JSON.stringify({ token, decision }),
    }, 'Meal Manager decision'),
}
