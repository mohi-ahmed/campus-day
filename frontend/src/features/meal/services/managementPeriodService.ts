import type { ManagementPeriodContext, ManagementPeriodHistory } from '../types'
import { apiRequest } from '../../../lib/api'

const path = '/api/meal/management-periods'

export const managementPeriodService = {
  history: (page = 0) => apiRequest<ManagementPeriodHistory>(`${path}?page=${page}&size=20`, undefined, 'Management period API'),
  context: () => apiRequest<ManagementPeriodContext>(`${path}/context`, undefined, 'Management period API'),
}
