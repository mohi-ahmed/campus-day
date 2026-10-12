import { apiRequest } from '../../../lib/api'

export type IngredientUnit = 'KG' | 'LITER' | 'PIECE' | 'PACKET'
export type StockMovementType = 'OPENING' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT' | 'KITCHEN_ISSUE' | 'PURCHASE_IN'
export type StockMealType = 'BREAKFAST' | 'LUNCH' | 'DINNER'

export interface IngredientStock {
  id: string
  name: string
  unit: IngredientUnit
  currentStock: number
  openingAllowed: boolean
  createdAt: string
  updatedAt: string
}

export interface StockMovement {
  id: string
  ingredientId: string
  sequence: number
  type: StockMovementType
  quantity: number
  balanceAfter: number
  unit: IngredientUnit
  businessDate: string
  occurredAt: string
  mealType: StockMealType | null
  note: string | null
  managerStudentId: string
  sourceReference: string | null
}

export interface StockPage<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface MovementPage extends StockPage<StockMovement> { ingredientId: string }
export interface IssuePage extends StockPage<IssueLine> { date: string }

export interface IssueLine {
  id: string
  ingredientId: string
  ingredientName: string
  unit: IngredientUnit
  quantity: number
  mealType: StockMealType
  date: string
  note: string | null
  occurredAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal${path}`, init, 'Stock API')
}

const manager = (studentId: string) => `?managerStudentId=${encodeURIComponent(studentId)}`

export const stockApi = {
  ingredients: () => request<IngredientStock[]>('/ingredients'),
  createIngredient: (studentId: string, name: string, unit: IngredientUnit) =>
    request<IngredientStock>(`/ingredients${manager(studentId)}`, { method: 'POST', body: JSON.stringify({ name, unit }) }),
  updateIngredient: (studentId: string, id: string, name: string, unit: IngredientUnit) =>
    request<IngredientStock>(`/ingredients/${encodeURIComponent(id)}${manager(studentId)}`, { method: 'PUT', body: JSON.stringify({ name, unit }) }),
  opening: (studentId: string, ingredient: IngredientStock, quantity: string) =>
    request<StockMovement>(`/stock/opening${manager(studentId)}`, { method: 'POST',
      body: JSON.stringify({ ingredientId: ingredient.id, unit: ingredient.unit, quantity }) }),
  adjustment: (studentId: string, ingredient: IngredientStock, type: 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT', quantity: string, note: string) =>
    request<StockMovement>(`/stock/adjustments${manager(studentId)}`, { method: 'POST',
      body: JSON.stringify({ ingredientId: ingredient.id, unit: ingredient.unit, type, quantity, note }) }),
  issue: (studentId: string, ingredient: IngredientStock, date: string, mealType: StockMealType, quantity: string, note: string) =>
    request<StockMovement>(`/stock/issues${manager(studentId)}`, { method: 'POST',
      body: JSON.stringify({ ingredientId: ingredient.id, unit: ingredient.unit, date, mealType, quantity, note: note || null }) }),
  movements: (ingredientId: string, page: number) =>
    request<MovementPage>(`/stock/${encodeURIComponent(ingredientId)}/movements?page=${page}&size=20`),
  issues: (date: string, page: number) =>
    request<IssuePage>(`/stock/issues?date=${encodeURIComponent(date)}&page=${page}&size=20`),
}
