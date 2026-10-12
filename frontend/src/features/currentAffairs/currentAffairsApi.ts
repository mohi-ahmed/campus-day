import { apiRequest } from '../../lib/api'

export type CurrentAffairsCategory =
  | 'Bangladesh'
  | 'International'
  | 'Education'
  | 'Science'
  | 'Technology'
  | 'Health'
  | 'Business'
  | 'Sports'
  | 'Politics'
  | 'Environment'

export type CurrentAffairsLanguage = 'bn' | 'en'
export type CurrentAffairsRegion = 'bangladesh' | 'international'
export type CurrentAffairsContentType = 'CURRENT_AFFAIRS' | 'NEWS'

export interface CurrentAffairsArticle {
  id: string
  title: string
  description: string
  body: string[]
  category: CurrentAffairsCategory
  language: CurrentAffairsLanguage
  region: CurrentAffairsRegion
  contentType: CurrentAffairsContentType
  sourceName: string
  sourceUrl: string
  author?: string
  publishedAt: string
  imageUrl?: string
  imageAlt: string
  readTimeMinutes: number
  featured?: boolean
  relatedGroup?: string
}

export interface CurrentAffairsArticleView extends CurrentAffairsArticle {
  isBookmarked: boolean
  isRead: boolean
  readAt?: string
}

export interface CurrentAffairsPageResponse {
  items: CurrentAffairsArticleView[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  nextCursor?: string
}

export interface CurrentAffairsQuery {
  query?: string
  category?: CurrentAffairsCategory
  region?: CurrentAffairsRegion
  language?: CurrentAffairsLanguage
  source?: string
  contentType?: CurrentAffairsContentType
  readStatus?: 'read' | 'unread'
  bookmarked?: boolean
  page?: number
  size?: number
  cursor?: string
}

type UserQuery = CurrentAffairsQuery & { studentId?: string }

function queryString(query: UserQuery = {}) {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  const value = params.toString()
  return value ? `?${value}` : ''
}

const base = '/api/current-affairs'

export const currentAffairsApi = {
  list: (studentId: string, query?: CurrentAffairsQuery) => apiRequest<CurrentAffairsPageResponse>(
    `${base}${queryString({ ...query, studentId } as UserQuery)}`,
    undefined,
    'Current affairs',
  ),

  highlights: (studentId: string) => apiRequest<CurrentAffairsArticleView[]>(
    `${base}/highlights${queryString({ studentId } as UserQuery)}`,
    undefined,
    'Current affairs highlights',
  ),

  getArticle: (studentId: string, articleId: string) => apiRequest<CurrentAffairsArticleView>(
    `${base}/${encodeURIComponent(articleId)}${queryString({ studentId } as UserQuery)}`,
    undefined,
    'Current affairs article',
  ),

  listBookmarks: (studentId: string, query?: CurrentAffairsQuery) => apiRequest<CurrentAffairsPageResponse>(
    `${base}/bookmarks${queryString({ ...query, studentId } as UserQuery)}`,
    undefined,
    'Current affairs bookmarks',
  ),

  bookmark: (studentId: string, articleId: string) => apiRequest<void>(
    `${base}/${encodeURIComponent(articleId)}/bookmark${queryString({ studentId } as UserQuery)}`,
    { method: 'POST' },
    'Bookmark current affairs article',
  ),

  removeBookmark: (studentId: string, articleId: string) => apiRequest<void>(
    `${base}/${encodeURIComponent(articleId)}/bookmark${queryString({ studentId } as UserQuery)}`,
    { method: 'DELETE' },
    'Remove current affairs bookmark',
  ),

  markRead: (studentId: string, articleId: string) => apiRequest<void>(
    `${base}/${encodeURIComponent(articleId)}/read${queryString({ studentId } as UserQuery)}`,
    { method: 'POST' },
    'Mark current affairs article as read',
  ),

  markUnread: (studentId: string, articleId: string) => apiRequest<void>(
    `${base}/${encodeURIComponent(articleId)}/read${queryString({ studentId } as UserQuery)}`,
    { method: 'DELETE' },
    'Mark current affairs article as unread',
  ),

  history: (studentId: string, query?: CurrentAffairsQuery) => apiRequest<CurrentAffairsPageResponse>(
    `${base}/history${queryString({ ...query, studentId } as UserQuery)}`,
    undefined,
    'Current affairs reading history',
  ),
}
