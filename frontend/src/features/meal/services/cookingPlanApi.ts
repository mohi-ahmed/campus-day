import { apiRequest } from '../../../lib/api'

export interface CookingItemDemand {
  itemId: string
  name: string
  menuQuantity: string | null
  orderedQuantity: number
}

export interface CookingMealDemand {
  mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER'
  available: boolean
  menuDescription: string
  fullMealDescription: string | null
  yearsOneToFourBaseline: number
  reportedAbsent: number
  yearsOneToFourPlanned: number
  finalYearPackagesOrFullMeals: number
  totalPackagesOrFullMeals: number
  finalYearItems: CookingItemDemand[]
}

export interface CookingPlan {
  mealDate: string
  breakfast: CookingMealDemand
  lunch: CookingMealDemand
  dinner: CookingMealDemand
}

export const cookingPlanApi = {
  async get(date: string, managerStudentId: string): Promise<CookingPlan> {
    const query = new URLSearchParams({ date, managerStudentId })
    return apiRequest<CookingPlan>(`/api/meal/cooking-plan?${query}`, undefined, 'Cooking plan API')
  },
}
