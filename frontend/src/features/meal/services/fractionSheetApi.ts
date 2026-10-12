import { apiRequest } from '../../../lib/api'

export type FractionRowStatus = 'SUBMITTED' | 'LOCKED' | 'CANCELLED' | 'NOT_EATING'
export type FractionChoiceKind = 'NOT_EATING' | 'FULL_MEAL' | 'ITEMS'

export interface FractionItemQuantity { itemId: string; name: string; quantity: number }
export interface FractionMealSelection { kind: FractionChoiceKind; items: FractionItemQuantity[] }
export interface FractionStudentRow {
  studentId: string
  name: string
  status: FractionRowStatus
  breakfast: boolean
  lunch: FractionMealSelection
  dinner: FractionMealSelection
}
export interface FractionTotals {
  finalYearStudents: number
  submitted: number
  cancelled: number
  notEating: number
  locked: number
  breakfastPackages: number
  lunchFullMeals: number
  dinnerFullMeals: number
  lunchItems: FractionItemQuantity[]
  dinnerItems: FractionItemQuantity[]
}
export interface FractionSheet {
  mealDate: string
  students: FractionStudentRow[]
  totals: FractionTotals
}

export const fractionSheetApi = {
  async get(date: string, managerStudentId: string): Promise<FractionSheet> {
    const query = new URLSearchParams({ date, managerStudentId })
    return apiRequest<FractionSheet>(`/api/meal/fraction-sheet?${query}`, undefined, 'Fraction sheet API')
  },
}
