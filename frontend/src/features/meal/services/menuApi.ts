import { apiRequest } from '../../../lib/api'

export interface MenuItemInput {
  id?: string
  name: string
  nameBn?: string
  quantity?: string
  available: boolean
}

export interface MainMealInput {
  description: string
  available: boolean
  orderingEnabled: boolean
  fullMealAvailable: boolean
  fullMealDescription: string | null
  items: MenuItemInput[]
}

export interface MenuInput {
  mealDate: string
  breakfast: { packageDescription: string; available: boolean; orderingEnabled: boolean }
  lunch: MainMealInput
  dinner: MainMealInput
}

export interface MenuRecord extends MenuInput {
  id: string
  status: 'DRAFT' | 'PUBLISHED'
  createdAt: string
  updatedAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal/menus${path}`, init, 'Menu API')
}

function rangePath(from: string, to: string) {
  return `?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
}

export function dhakaToday(): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Dhaka', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date())
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? ''
  return `${value('year')}-${value('month')}-${value('day')}`
}

export function addDays(date: string, days: number): string {
  const value = new Date(`${date}T00:00:00Z`)
  value.setUTCDate(value.getUTCDate() + days)
  return value.toISOString().slice(0, 10)
}

export const menuApi = {
  published: (from: string, to: string) => request<MenuRecord[]>(rangePath(from, to)),
  managed: (from: string, to: string) => request<MenuRecord[]>(`/manage${rangePath(from, to)}`),
  create: (input: MenuInput) => request<MenuRecord>('', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: MenuInput) => request<MenuRecord>(`/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  publish: (id: string) => request<MenuRecord>(`/${id}/publish`, { method: 'POST' }),
  delete: (id: string) => request<void>(`/${id}`, { method: 'DELETE' }),
}
