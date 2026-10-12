import { apiRequest } from '../../../lib/api'

export type MealChoiceKind = 'NOT_EATING' | 'FULL_MEAL' | 'ITEMS'

export interface OrderMealChoice {
  kind: MealChoiceKind
  items: { itemId: string; name: string; quantity: number }[]
}

export interface OrderInput {
  mealDate: string
  breakfast: boolean
  lunch: { kind: MealChoiceKind; items: { itemId: string; quantity: number }[] }
  dinner: { kind: MealChoiceKind; items: { itemId: string; quantity: number }[] }
}

export interface OrderRecord {
  id: string
  studentId: string
  mealDate: string
  menuId: string
  breakfast: boolean
  lunch: OrderMealChoice
  dinner: OrderMealChoice
  status: 'SUBMITTED' | 'CANCELLED'
  canEdit: boolean
  canCancel: boolean
  editCutoffAt: string
  cancelCutoffAt: string
  createdAt: string
  updatedAt: string
}

export interface OrderCutoffs {
  mealDate: string
  orderCutoffAt: string
  editCutoffAt: string
  cancelCutoffAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal${path}`, init, 'Order API')
}

const student = (studentId: string) => `studentId=${encodeURIComponent(studentId)}`

export const orderApi = {
  cutoffs: (mealDate: string) => request<OrderCutoffs>(`/order-configuration?mealDate=${encodeURIComponent(mealDate)}`),
  byDate: (studentId: string, mealDate: string) => request<OrderRecord | null>(`/orders/by-date?${student(studentId)}&mealDate=${encodeURIComponent(mealDate)}`),
  byId: (studentId: string, id: string) => request<OrderRecord>(`/orders/${encodeURIComponent(id)}?${student(studentId)}`),
  history: (studentId: string, from: string, to: string) => request<OrderRecord[]>(`/orders?${student(studentId)}&from=${from}&to=${to}`),
  create: (studentId: string, input: OrderInput) => request<OrderRecord>(`/orders?${student(studentId)}`, { method: 'POST', body: JSON.stringify(input) }),
  update: (studentId: string, id: string, input: OrderInput) => request<OrderRecord>(`/orders/${encodeURIComponent(id)}?${student(studentId)}`, { method: 'PUT', body: JSON.stringify(input) }),
  cancel: (studentId: string, id: string) => request<OrderRecord>(`/orders/${encodeURIComponent(id)}/cancel?${student(studentId)}`, { method: 'POST' }),
}
