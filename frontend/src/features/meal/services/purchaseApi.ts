import type { IngredientUnit } from './stockApi'
import { apiRequest } from '../../../lib/api'

export interface Supplier {
  id: string
  name: string
  contact: string | null
  note: string | null
  outstanding: number
}
export interface SupplierPage { suppliers: Supplier[]; page: number; totalElements: number; totalPages: number }
export interface PurchaseLine { ingredientId: string; ingredientName: string; unit: IngredientUnit; quantity: number; unitCost: number; lineTotal: number }
export interface Purchase {
  id: string; requestId: string; supplierId: string; supplierName: string; purchaseDate: string
  lines: PurchaseLine[]; paymentMode: 'CASH' | 'CREDIT'; total: number; initialPaid: number
  initialDue: number; status: 'PROCESSING' | 'COMPLETED'; note: string | null
  managerStudentId: string; createdAt: string; updatedAt: string
}
export interface PurchasePage { purchases: Purchase[]; page: number; totalElements: number; totalPages: number }
export interface Payment { id: string; supplierId: string; amount: number; balanceAfter: number; date: string; note: string | null; createdAt: string }
export interface PaymentPage { payments: Payment[]; page: number; totalElements: number; totalPages: number }
export interface PurchaseInput {
  requestId: string; supplierId: string; purchaseDate: string; paymentMode: 'CASH' | 'CREDIT'
  initialPaid: string | null; note: string | null
  lines: { ingredientId: string; unit: IngredientUnit; quantity: string; unitCost: string }[]
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal${path}`, init, 'Purchase API')
}

const actor = (id: string) => `managerStudentId=${encodeURIComponent(id)}`
const body = (value: unknown) => JSON.stringify(value)

export const purchaseApi = {
  suppliers: (page = 0, size = 20) => request<SupplierPage>(`/suppliers?page=${page}&size=${size}`),
  allSuppliers: async () => {
    const first = await request<SupplierPage>('/suppliers?page=0&size=100')
    const rest = await Promise.all(Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
      request<SupplierPage>(`/suppliers?page=${index + 1}&size=100`)))
    return [first, ...rest].flatMap((page) => page.suppliers)
  },
  supplier: (id: string) => request<Supplier>(`/suppliers/${encodeURIComponent(id)}`),
  createSupplier: (studentId: string, name: string, contact: string, note: string) =>
    request<Supplier>(`/suppliers?${actor(studentId)}`, { method: 'POST', body: body({ name, contact, note }) }),
  updateSupplier: (studentId: string, id: string, name: string, contact: string, note: string) =>
    request<Supplier>(`/suppliers/${encodeURIComponent(id)}?${actor(studentId)}`, { method: 'PUT', body: body({ name, contact, note }) }),
  due: (id: string) => request<{ outstanding: number }>(`/suppliers/${encodeURIComponent(id)}/dues`),
  payments: (id: string, page = 0) => request<PaymentPage>(`/suppliers/${encodeURIComponent(id)}/payments?page=${page}&size=20`),
  pay: (studentId: string, id: string, requestId: string, amount: string, paymentDate: string, note: string) =>
    request<Payment>(`/suppliers/${encodeURIComponent(id)}/payments?${actor(studentId)}`, {
      method: 'POST', body: body({ requestId, amount, paymentDate, note: note || null }),
    }),
  purchases: (page = 0, supplierId = '', paymentMode = '', from = '', to = '') => request<PurchasePage>(
    `/purchases?page=${page}&size=20${supplierId ? `&supplierId=${encodeURIComponent(supplierId)}` : ''}${paymentMode ? `&paymentMode=${paymentMode}` : ''}${from ? `&from=${from}` : ''}${to ? `&to=${to}` : ''}`),
  purchase: (id: string) => request<Purchase>(`/purchases/${encodeURIComponent(id)}`),
  resumePurchase: (studentId: string, id: string) =>
    request<Purchase>(`/purchases/${encodeURIComponent(id)}/resume?${actor(studentId)}`, { method: 'POST' }),
  createPurchase: (studentId: string, input: PurchaseInput) =>
    request<Purchase>(`/purchases?${actor(studentId)}`, { method: 'POST', body: body(input) }),
}
