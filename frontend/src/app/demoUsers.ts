import type { DemoUser } from '../features/meal/types'

export const demoUsers: Record<DemoUser['context'], DemoUser> = {
  regular: { id: 'jimc-24018', name: 'Nusrat Jahan', email: 'nusrat.jahan@campusday.test', initials: 'NJ', academicYear: 3, context: 'regular', isMealManager: false },
  'final-year': { id: 'jimc-22034', name: 'Samiul Hasan', email: 'samiul.hasan@campusday.test', initials: 'SH', academicYear: 5, context: 'final-year', isMealManager: false },
  manager: { id: 'jimc-23011', name: 'Arafat Rahman', email: 'arafat.rahman@campusday.test', initials: 'AR', academicYear: 4, context: 'manager', isMealManager: true },
}
