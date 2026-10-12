import { apiRequest } from '../../lib/api'

export type LostFoundType = 'LOST' | 'FOUND'
export type LostFoundCategory = 'ELECTRONICS' | 'ID_AND_CARDS' | 'BAGS' | 'KEYS' | 'CLOTHING' | 'BOOKS' | 'OTHER'
export type LostFoundPostStatus = 'OPEN' | 'RETURNED'

export interface LostFoundPost {
  id: string
  ownerStudentId: string
  ownerName: string
  contact: string | null
  type: LostFoundType
  category: LostFoundCategory
  title: string
  description: string | null
  location: string
  occurredOn: string
  status: LostFoundPostStatus
  commentCount: number
  returnedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface LostFoundPage<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface LostFoundComment {
  id: string
  postId: string
  authorStudentId: string
  authorName: string
  content: string
  createdAt: string
}

export interface LostFoundConversation {
  id: string
  postId: string
  postTitle: string
  postType: LostFoundType
  postCategory: LostFoundCategory
  otherStudentId: string
  otherStudentName: string
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
  onYourPost: boolean
}

export interface LostFoundMessage {
  id: string
  conversationId: string
  senderStudentId: string
  senderName: string
  content: string
  createdAt: string
}

export interface LostFoundThread {
  conversation: LostFoundConversation
  messages: LostFoundMessage[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface PostListParams {
  query?: string
  page?: number
  size?: number
}

interface CreatePostInput {
  ownerStudentId: string
  type: LostFoundType
  category: LostFoundCategory
  title: string
  description?: string
  location: string
  occurredOn: string
  contact?: string
}

const base = '/api/lost-found'

function queryString(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

export const lostFoundApi = {
  listPosts: ({ query, page = 0, size = 50 }: PostListParams = {}) =>
    apiRequest<LostFoundPage<LostFoundPost>>(
      `${base}/posts${queryString({ query: query?.trim(), page, size })}`,
      undefined,
      'Lost and found posts',
    ),

  getPost: (postId: string) =>
    apiRequest<LostFoundPost>(`${base}/posts/${postId}`, undefined, 'Lost and found post'),

  createPost: (input: CreatePostInput) =>
    apiRequest<LostFoundPost>(`${base}/posts`, {
      method: 'POST',
      body: JSON.stringify(input),
    }, 'Create lost and found post'),

  markReturned: (postId: string, studentId: string) =>
    apiRequest<LostFoundPost>(
      `${base}/posts/${postId}/returned${queryString({ studentId })}`,
      { method: 'PATCH' },
      'Mark item as returned',
    ),

  listComments: (postId: string, page = 0, size = 50) =>
    apiRequest<LostFoundPage<LostFoundComment>>(
      `${base}/posts/${postId}/comments${queryString({ page, size })}`,
      undefined,
      'Post comments',
    ),

  createComment: (postId: string, authorStudentId: string, content: string) =>
    apiRequest<LostFoundComment>(`${base}/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ authorStudentId, content }),
    }, 'Create comment'),

  startConversation: (postId: string, senderStudentId: string, content: string) =>
    apiRequest<LostFoundMessage>(`${base}/posts/${postId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ senderStudentId, content }),
    }, 'Send message'),

  listConversations: (studentId: string) =>
    apiRequest<LostFoundConversation[]>(
      `${base}/conversations${queryString({ studentId })}`,
      undefined,
      'Lost and found messages',
    ),

  unreadCount: (studentId: string) =>
    apiRequest<{ unreadCount: number }>(
      `${base}/conversations/unread-count${queryString({ studentId })}`,
      undefined,
      'Unread message count',
    ),

  getThread: (conversationId: string, studentId: string, page = 0, size = 50) =>
    apiRequest<LostFoundThread>(
      `${base}/conversations/${conversationId}${queryString({ studentId, page, size })}`,
      undefined,
      'Message thread',
    ),

  reply: (conversationId: string, senderStudentId: string, content: string) =>
    apiRequest<LostFoundMessage>(`${base}/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ senderStudentId, content }),
    }, 'Send reply'),

  markConversationRead: (conversationId: string, studentId: string) =>
    apiRequest<{ unreadCount: number }>(
      `${base}/conversations/${conversationId}/read${queryString({ studentId })}`,
      { method: 'PATCH' },
      'Mark conversation as read',
    ),
}
