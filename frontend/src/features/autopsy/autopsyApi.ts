import { apiRequest } from '../../lib/api'

export type Category = 'feelings' | 'confession' | 'complaint' | 'roasting' | 'random'
export type ReactionKey = 'like' | 'unlike' | 'love' | 'broken' | 'haha' | 'wow' | 'angry' | 'cry' | 'fire'
export type ReportReason =
  | 'BULLYING_OR_HARASSMENT'
  | 'HATE_OR_ABUSIVE_CONTENT'
  | 'PERSONAL_INFORMATION'
  | 'SPAM_OR_MISLEADING'
  | 'OTHER'

type ApiCategory = 'FEELINGS' | 'CONFESSION' | 'COMPLAINT' | 'ROASTING' | 'RANDOM'
type ApiReaction = 'LIKE' | 'UNLIKE' | 'LOVE' | 'BROKEN_HEART' | 'HAHA' | 'WOW' | 'ANGRY' | 'CRY' | 'FIRE'

type ApiPost = {
  id: string
  category: ApiCategory
  toWhom: string | null
  thoughts: string
  nickname: string
  tag: string | null
  views: number
  comments: number
  totalReactions: number
  reposts: number
  reactions: Partial<Record<ApiReaction, number>>
  createdAt: string
  updatedAt: string
}

type ApiPostPage = {
  posts: ApiPost[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type ApiReactionResponse = {
  postId: string
  selectedReaction: ApiReaction | null
  totalReactions: number
  reactions: Partial<Record<ApiReaction, number>>
}

export type AutopsyPost = {
  id: string
  category: Category
  tag?: string
  toWhom?: string
  body: string
  nickname: string
  createdAt: string
  views: number
  comments: number
  totalReactions: number
  reposts: number
  reactions: Partial<Record<ApiReaction, number>>
}

export type AutopsyPostPage = {
  posts: AutopsyPost[]
  page: number
  totalElements: number
  totalPages: number
}

export type AutopsyComment = {
  id: string
  postId: string
  content: string
  nickname: string
  createdAt: string
}

type CommentPage = {
  comments: AutopsyComment[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type CreatePostInput = {
  category: Category
  toWhom?: string
  thoughts: string
  nickname?: string
  tag?: string
}

export type ReactionResult = {
  selectedReaction: ReactionKey | null
  totalReactions: number
  reactions: Partial<Record<ApiReaction, number>>
}

const reactionToApi: Record<ReactionKey, ApiReaction> = {
  like: 'LIKE',
  unlike: 'UNLIKE',
  love: 'LOVE',
  broken: 'BROKEN_HEART',
  haha: 'HAHA',
  wow: 'WOW',
  angry: 'ANGRY',
  cry: 'CRY',
  fire: 'FIRE',
}

const apiToReaction = Object.fromEntries(
  Object.entries(reactionToApi).map(([key, value]) => [value, key]),
) as Record<ApiReaction, ReactionKey>

function mapPost(post: ApiPost): AutopsyPost {
  return {
    id: post.id,
    category: post.category.toLowerCase() as Category,
    tag: post.tag ?? undefined,
    toWhom: post.toWhom ?? undefined,
    body: post.thoughts,
    nickname: post.nickname,
    createdAt: post.createdAt,
    views: post.views,
    comments: post.comments,
    totalReactions: post.totalReactions,
    reposts: post.reposts,
    reactions: post.reactions ?? {},
  }
}

export const autopsyApi = {
  async listPosts(category: 'all' | Category, page = 0): Promise<AutopsyPostPage> {
    const query = new URLSearchParams({ page: String(page), size: '20' })
    if (category !== 'all') query.set('category', category.toUpperCase())
    const response = await apiRequest<ApiPostPage>(`/api/autopsy/posts?${query}`, undefined, 'Autopsy feed')
    return {
      posts: response.posts.map(mapPost),
      page: response.page,
      totalElements: response.totalElements,
      totalPages: response.totalPages,
    }
  },

  async createPost(input: CreatePostInput, sessionId: string): Promise<AutopsyPost> {
    const response = await apiRequest<ApiPost>('/api/autopsy/posts', {
      method: 'POST',
      body: JSON.stringify({
        sessionId,
        category: input.category.toUpperCase(),
        toWhom: input.toWhom || null,
        thoughts: input.thoughts,
        nickname: input.nickname || null,
        tag: input.tag || null,
      }),
    }, 'Anonymous post')
    return mapPost(response)
  },

  recordView(postId: string) {
    return apiRequest<{ postId: string; views: number }>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/views`,
      { method: 'POST' },
      'Post view',
    )
  },

  listComments(postId: string) {
    return apiRequest<CommentPage>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/comments?page=0&size=50`,
      undefined,
      'Comments',
    )
  },

  createComment(postId: string, sessionId: string, content: string, nickname?: string) {
    return apiRequest<AutopsyComment>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/comments`,
      {
        method: 'POST',
        body: JSON.stringify({ sessionId, content, nickname: nickname || null }),
      },
      'Comment',
    )
  },

  async react(postId: string, sessionId: string, reaction: ReactionKey): Promise<ReactionResult> {
    const response = await apiRequest<ApiReactionResponse>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/reactions`,
      {
        method: 'POST',
        body: JSON.stringify({ sessionId, reaction: reactionToApi[reaction] }),
      },
      'Reaction',
    )
    return {
      selectedReaction: response.selectedReaction ? apiToReaction[response.selectedReaction] : null,
      totalReactions: response.totalReactions,
      reactions: response.reactions ?? {},
    }
  },

  toggleRepost(postId: string, sessionId: string) {
    return apiRequest<{ postId: string; reposted: boolean; reposts: number }>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/reposts`,
      {
        method: 'POST',
        body: JSON.stringify({ sessionId }),
      },
      'Repost',
    )
  },

  report(postId: string, sessionId: string, reason: ReportReason, details?: string) {
    return apiRequest<{ id: string; postId: string; status: string; createdAt: string }>(
      `/api/autopsy/posts/${encodeURIComponent(postId)}/reports`,
      {
        method: 'POST',
        body: JSON.stringify({ sessionId, reason, details: details || null }),
      },
      'Report',
    )
  },
}
