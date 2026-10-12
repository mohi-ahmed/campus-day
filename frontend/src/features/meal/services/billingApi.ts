import { dhakaToday } from './menuApi'
import { apiRequest } from '../../../lib/api'

export interface BillLine { mealDate: string; orderId: string; target: string; itemId: string | null; label: string; quantity: number; unitPrice: string | null; lineTotal: string | null; pricePending: boolean }
export interface Payment { id: string; studentId: string; year: number; month: number; amount: string; paymentDate: string; channel: string; recordedByStudentId: string | null; reference: string | null; recordedAt: string }
export interface Bill { studentId: string; year: number; month: number; model: 'FIXED' | 'FINAL_YEAR'; status: 'CHARGE_PENDING' | 'PENDING_FINALIZATION' | 'FINALIZED'; amount: string | null; paid: string; outstanding: string | null; credit: string | null; lines: BillLine[]; paymentHistory: Payment[] }
export interface Charge { id: string; year: number; month: number; amount: string; recordedByStudentId: string; effectiveAt: string }
export interface OfficeSummary { year: number; month: number; charge: string | null; collected: string; payments: Payment[] }
export interface CollectionRow { studentId: string; studentName: string; status: 'PENDING_FINALIZATION' | 'FINALIZED'; total: string | null; paid: string; outstanding: string | null; pricePending: boolean }

const base = '/api/meal/billing'
export const currentBillingMonth = () => { const [year, month] = dhakaToday().split('-').map(Number); return { year, month } }
export const moneyLabel = (amount: string | null | undefined) => amount == null ? 'Pending' : `BDT ${Number(amount).toLocaleString('en-BD', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(`${base}${path}`, init, 'Billing API')
}
const period = (year: number, month: number) => `year=${year}&month=${month}`
export const billingApi = {
  bill: (studentId: string, year: number, month: number) => request<Bill>(`/bills/${encodeURIComponent(studentId)}?${period(year, month)}`),
  finalize: (managerStudentId: string, studentId: string, year: number, month: number) => request<Bill>(`/bills/${encodeURIComponent(studentId)}/finalize?managerStudentId=${encodeURIComponent(managerStudentId)}&${period(year, month)}`, { method: 'POST' }),
  charges: (year: number, month: number) => request<Charge[]>(`/fixed-charge/history?${period(year, month)}`),
  setCharge: (managerStudentId: string, year: number, month: number, amount: string) => request<Charge>(`/fixed-charge?managerStudentId=${encodeURIComponent(managerStudentId)}&${period(year, month)}`, { method: 'PUT', body: JSON.stringify({ amount }) }),
  officeSummary: (year: number, month: number) => request<OfficeSummary>(`/office-payments?${period(year, month)}`),
  officePayment: (studentId: string, year: number, month: number, amount: string, paymentDate: string, reference: string) => request<Payment>('/office-payments', { method: 'POST', body: JSON.stringify({ studentId, year, month, amount, paymentDate, reference }) }),
  collections: (managerStudentId: string, year: number, month: number) => request<CollectionRow[]>(`/final-year-collections/summary?managerStudentId=${encodeURIComponent(managerStudentId)}&${period(year, month)}`),
  collect: (managerStudentId: string, studentId: string, year: number, month: number, amount: string, paymentDate: string, reference: string) => request<Payment>(`/final-year-collections?managerStudentId=${encodeURIComponent(managerStudentId)}`, { method: 'POST', body: JSON.stringify({ studentId, year, month, amount, paymentDate, reference }) }),
}
