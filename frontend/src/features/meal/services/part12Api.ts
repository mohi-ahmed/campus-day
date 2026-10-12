import type { IngredientUnit, IngredientStock } from './stockApi'
import type { ManagementPeriod } from '../types'
import { apiRequest } from '../../../lib/api'

export interface HandoverLine { ingredientId: string; ingredientName: string; unit: IngredientUnit; quantity: string }
export interface Handover { id: string; requestId: string; outgoingPeriodId: string; incomingPeriodId: string; handoverDate: string; lines: HandoverLine[]; recordedValue: string; paidAmount: string; paymentDate: string | null; paymentRequestId: string | null; recordedByStudentId: string; note: string | null; createdAt: string }
export interface Report { year: number; month: number; periodId: string; periodStatus: string; managers: { id: string; studentId: string; name: string }[]; orders: { validOrders: number; cancelledOrders: number; breakfastPackages: number; lunchFullMeals: number; dinnerFullMeals: number; items: { meal: string; itemId: string; name: string; quantity: number }[] }; purchases: { count: number; total: string; cashTotal: string; creditTotal: string }; suppliers: { paymentTotal: string; currentOutstanding: string }; inventory: { movements: { ingredientId: string; type: string; unit: IngredientUnit; quantity: string }[]; liveStock: IngredientStock[]; transferredStock: HandoverLine[] }; billing: { fixedMonthlyCharge: string | null; officeCollected: string; finalizedFinalYearBills: number; finalYearBilled: string; finalYearCollected: string; finalYearOutstanding: string }; handover: Handover | null }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`/api/meal${path}`, init, 'Month closing API')
}
export const part12Api = {
  handovers: () => request<Handover[]>('/stock-handovers'),
  createHandover: (managerStudentId: string, input: { requestId: string; outgoingPeriodId: string; incomingPeriodId: string; handoverDate: string; lines: { ingredientId: string; unit: IngredientUnit; quantity: string }[]; recordedValue: string; note: string | null }) => request<Handover>(`/stock-handovers?managerStudentId=${encodeURIComponent(managerStudentId)}`, { method: 'POST', body: JSON.stringify(input) }),
  payHandover: (managerStudentId: string, id: string, requestId: string, amount: string, paymentDate: string) => request<Handover>(`/stock-handovers/${encodeURIComponent(id)}/payment?managerStudentId=${encodeURIComponent(managerStudentId)}`, { method: 'POST', body: JSON.stringify({ requestId, amount, paymentDate }) }),
  report: (year: number, month: number) => request<Report>(`/reports/monthly?year=${year}&month=${month}`),
  close: (period: ManagementPeriod, managerStudentId: string) => request<ManagementPeriod>(`/management-periods/${encodeURIComponent(period.id)}/close?managerStudentId=${encodeURIComponent(managerStudentId)}`, { method: 'POST' }),
}
