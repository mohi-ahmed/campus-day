export type SubjectKey = 'ANA' | 'PHY' | 'BIO' | 'PHA' | 'PAT' | 'GEN'
export type TaskType = 'Study' | 'Assignment' | 'Revision' | 'Exam' | 'Personal'
export type TaskPriority = 'High' | 'Med' | 'Low'
export type RoutineKind = 'Class' | 'Self-study'

export type StudyTask = {
  id: string
  source?: 'fixture' | 'user'
  title: string
  subject: SubjectKey
  subjectLabel?: string
  type: TaskType
  typeLabel?: string
  priority: TaskPriority
  due: string
  time?: string
  estimate?: number
  repeat?: 'daily' | 'weekly'
  note?: string
  done?: boolean
}

export type RoutineBlock = {
  id: string
  days: number[]
  start: string
  end: string
  subject: SubjectKey
  subjectLabel?: string
  topic: string
  location: string
  kind: RoutineKind
  kindLabel?: string
}

export type FocusSession = {
  id: string
  date: string
  subject: SubjectKey
  subjectLabel?: string
  minutes: number
  topic: string
}

export const subjects: Record<SubjectKey, { name: string; color: string }> = {
  ANA: { name: 'Anatomy', color: '#d4527a' },
  PHY: { name: 'Physiology', color: '#3e73d8' },
  BIO: { name: 'Biochemistry', color: '#8a63d2' },
  PHA: { name: 'Pharmacology', color: '#0f8f82' },
  PAT: { name: 'Pathology', color: '#d9912b' },
  GEN: { name: 'General', color: '#91a09d' },
}

export const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

export function startOfDay(date = new Date()) {
  const next = new Date(date)
  next.setHours(0, 0, 0, 0)
  return next
}

export function addDays(date: Date, amount: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  return next
}

export function dateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function fromDateKey(value: string) {
  return new Date(`${value}T00:00:00`)
}

export function mondayIndex(date = new Date()) {
  return (date.getDay() + 6) % 7
}

export function minuteValue(time = '23:59') {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

export function formatTime(time?: string) {
  if (!time) return 'Any time'
  const [hours, minutes] = time.split(':').map(Number)
  const displayHour = ((hours + 11) % 12) + 1
  return `${displayHour}${minutes ? `:${String(minutes).padStart(2, '0')}` : ''} ${hours >= 12 ? 'PM' : 'AM'}`
}

export function daysFromToday(key: string, today = startOfDay()) {
  return Math.round((fromDateKey(key).getTime() - today.getTime()) / 86_400_000)
}

export function relativeDate(key: string, today = startOfDay()) {
  const difference = daysFromToday(key, today)
  if (difference === 0) return 'Today'
  if (difference === 1) return 'Tomorrow'
  if (difference === -1) return 'Yesterday'
  if (difference < 0) return `${Math.abs(difference)} days late`
  if (difference < 7) return dayNames[mondayIndex(fromDateKey(key))]
  return fromDateKey(key).toLocaleDateString('en-BD', { day: 'numeric', month: 'short' })
}

