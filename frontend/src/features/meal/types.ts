export type StudentContext = 'regular' | 'final-year' | 'manager'
export type MealType = 'Breakfast' | 'Lunch' | 'Dinner'
export type Tone = 'success' | 'warning' | 'error' | 'info' | 'neutral' | 'brand'

export interface DemoUser {
  id: string
  name: string
  email: string
  initials: string
  academicYear: 1 | 2 | 3 | 4 | 5
  context: StudentContext
  isMealManager: boolean
  profileImageUrl?: string | null
}

export interface ManagementPeriod {
  id: string
  year: number
  month: number
  startDate: string
  endDate: string
  status: 'ACTIVE' | 'CLOSED'
  managers: { id: string; studentId: string; name: string; academicYear: 'FOURTH_YEAR' }[]
  createdAt: string
  updatedAt: string
}

export interface ManagementPeriodHistory {
  content: ManagementPeriod[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ManagementPeriodContext {
  current: ManagementPeriod | null
  previous: ManagementPeriod | null
  next: ManagementPeriod | null
}
