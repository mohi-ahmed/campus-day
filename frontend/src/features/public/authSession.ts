export type AuthAccount = {
  id: string
  email: string
  studentId: string | null
  profileComplete: boolean
  providers: Array<'LOCAL' | 'GOOGLE'>
  roles: string[]
}

export type AuthTokenResponse = {
  tokenType: 'Bearer'
  accessToken: string
  accessTokenExpiresAt: string
  refreshToken: string
  refreshTokenExpiresAt: string
  account: AuthAccount
}

type StoredAuthSession = AuthTokenResponse & { version: 1 }

const sessionKey = 'campus-day-auth-session-v1'
const passwordResetKey = 'campus-day-password-reset-v1'
export const authSessionChangedEvent = 'campus-day-auth-session-changed'

function notifyAuthSessionChanged() {
  window.dispatchEvent(new Event(authSessionChangedEvent))
}

export function saveAuthSession(tokens: AuthTokenResponse) {
  const session: StoredAuthSession = { version: 1, ...tokens }
  sessionStorage.setItem(sessionKey, JSON.stringify(session))
  notifyAuthSessionChanged()
}

export function readAuthSession(): StoredAuthSession | null {
  const raw = sessionStorage.getItem(sessionKey)
  if (!raw) return null
  try {
    const session = JSON.parse(raw) as Partial<StoredAuthSession>
    if (session.version !== 1 || !session.accessToken || !session.refreshToken || !session.account) {
      sessionStorage.removeItem(sessionKey)
      return null
    }
    return session as StoredAuthSession
  } catch {
    sessionStorage.removeItem(sessionKey)
    return null
  }
}

export function clearAuthSession() {
  sessionStorage.removeItem(sessionKey)
  notifyAuthSessionChanged()
}

export function updateAuthAccount(account: AuthAccount) {
  const current = readAuthSession()
  if (!current) return
  saveAuthSession({ ...current, account })
}

export function savePasswordResetContext(email: string, otp: string) {
  sessionStorage.setItem(passwordResetKey, JSON.stringify({ email, otp }))
}

export function readPasswordResetContext(): { email: string; otp: string } | null {
  const raw = sessionStorage.getItem(passwordResetKey)
  if (!raw) return null
  try {
    const context = JSON.parse(raw) as { email?: string; otp?: string }
    return context.email && context.otp ? { email: context.email, otp: context.otp } : null
  } catch {
    return null
  }
}

export function clearPasswordResetContext() {
  sessionStorage.removeItem(passwordResetKey)
}
