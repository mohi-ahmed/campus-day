import { apiRequest } from '../../../lib/api'

export type PriceTarget = 'BREAKFAST_PACKAGE' | 'LUNCH_FULL_MEAL' | 'DINNER_FULL_MEAL' | 'LUNCH_MENU_ITEM' | 'DINNER_MENU_ITEM'

export interface ApplicablePrice {
  target: PriceTarget
  itemId: string | null
  label: string
  amount: number | null
  effectiveAt: string | null
}

export interface ApplicablePrices {
  menuId: string
  mealDate: string
  prices: ApplicablePrice[]
}

export interface PriceVersion {
  id: string
  menuId: string
  mealDate: string
  target: PriceTarget
  itemId: string | null
  amount: number
  setByStudentId: string
  effectiveAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal/prices${path}`, init, 'Price API')
}

export const priceApi = {
  applicable: (mealDate: string) => request<ApplicablePrices>(`?mealDate=${encodeURIComponent(mealDate)}`),
  history: (mealDate: string, target: PriceTarget, itemId: string | null) => {
    const query = new URLSearchParams({ mealDate, target })
    if (itemId) query.set('itemId', itemId)
    return request<PriceVersion[]>(`/history?${query}`)
  },
  set: (managerStudentId: string, mealDate: string, target: PriceTarget, itemId: string | null, amount: string) =>
    request<PriceVersion>(`?managerStudentId=${encodeURIComponent(managerStudentId)}`, {
      method: 'PUT', body: JSON.stringify({ mealDate, target, itemId, amount }),
    }),
}

export function priceLabel(amount: number | null | undefined): string {
  return amount == null ? 'Price Pending' : `BDT ${Number(amount).toFixed(2)}`
}

export function findPrice(prices: ApplicablePrices | null, target: PriceTarget, itemId: string | null = null): number | null {
  return prices?.prices.find((price) => price.target === target && price.itemId === itemId)?.amount ?? null
}
