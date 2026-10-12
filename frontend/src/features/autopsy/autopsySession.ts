import type { ReactionKey } from './autopsyApi'

const sessionKey = 'campusday.autopsy.interaction-session.v1'
const reactionPrefix = 'campusday.autopsy.reaction.'
const repostPrefix = 'campusday.autopsy.repost.'

let memorySessionId: string | null = null

function createSessionId() {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  return Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
}

export function anonymousSessionId() {
  if (memorySessionId) return memorySessionId
  try {
    const stored = localStorage.getItem(sessionKey)
    if (stored) return stored
    const created = createSessionId()
    localStorage.setItem(sessionKey, created)
    return created
  } catch {
    memorySessionId = createSessionId()
    return memorySessionId
  }
}

export function storedReaction(postId: string): ReactionKey | null {
  try {
    return localStorage.getItem(`${reactionPrefix}${postId}`) as ReactionKey | null
  } catch {
    return null
  }
}

export function rememberReaction(postId: string, reaction: ReactionKey | null) {
  try {
    if (reaction) localStorage.setItem(`${reactionPrefix}${postId}`, reaction)
    else localStorage.removeItem(`${reactionPrefix}${postId}`)
  } catch {
    // Interaction still works when browser storage is unavailable.
  }
}

export function storedRepost(postId: string) {
  try {
    return localStorage.getItem(`${repostPrefix}${postId}`) === 'true'
  } catch {
    return false
  }
}

export function rememberRepost(postId: string, reposted: boolean) {
  try {
    if (reposted) localStorage.setItem(`${repostPrefix}${postId}`, 'true')
    else localStorage.removeItem(`${repostPrefix}${postId}`)
  } catch {
    // Interaction still works when browser storage is unavailable.
  }
}
