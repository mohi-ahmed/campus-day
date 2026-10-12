import {
  clearAuthSession,
  readAuthSession,
  saveAuthSession,
  type AuthTokenResponse,
} from '../features/public/authSession'

const apiBaseUrlSetting = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/$/, '')
// Render exposes cross-service hostnames without a scheme. Accept both that
// format and a complete URL so custom domains continue to work.
const configuredApiBaseUrl = apiBaseUrlSetting && !/^https?:\/\//i.test(apiBaseUrlSetting)
  ? `https://${apiBaseUrlSetting}`
  : apiBaseUrlSetting
// Local development always uses Vite's same-origin /api proxy. This avoids
// origin/port differences when Vite selects 4174 because 4173 is already busy.
const apiBaseUrl = import.meta.env.DEV ? '' : configuredApiBaseUrl

export function apiUrl(path: string): string {
  if (!path.startsWith('/')) throw new Error(`API path must start with "/": ${path}`)
  return `${apiBaseUrl}${path}`
}

type ProblemResponse = {
  detail?: string
  title?: string
  message?: string
  error?: string
  errors?: string[]
}

let refreshRequest: Promise<boolean> | null = null

async function refreshAuthSession(): Promise<boolean> {
  const session = readAuthSession()
  if (!session?.refreshToken) return false
  if (!refreshRequest) {
    refreshRequest = fetch(apiUrl('/api/auth/refresh'), {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    }).then(async (response) => {
      if (!response.ok) {
        clearAuthSession()
        return false
      }
      saveAuthSession(await response.json() as AuthTokenResponse)
      return true
    }).catch(() => false).finally(() => { refreshRequest = null })
  }
  return refreshRequest
}

const refreshExcludedPaths = new Set([
  '/api/auth/register',
  '/api/auth/verify-email',
  '/api/auth/resend-verification',
  '/api/auth/login',
  '/api/auth/refresh',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/auth/oauth/exchange',
])

export async function apiRequest<T>(path: string, init?: RequestInit, label = 'API'): Promise<T> {
  let response: Response
  try {
    const execute = () => {
      const hasFormDataBody = typeof FormData !== 'undefined' && init?.body instanceof FormData
      const accessToken = readAuthSession()?.accessToken
      return fetch(apiUrl(path), {
        ...init,
        headers: init?.body == null
          ? { Accept: 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}), ...init?.headers }
          : hasFormDataBody
            ? { Accept: 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}), ...init.headers }
            : { Accept: 'application/json', 'Content-Type': 'application/json', ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}), ...init.headers },
      })
    }
    response = await execute()
    if (response.status === 401 && !refreshExcludedPaths.has(path) && await refreshAuthSession()) {
      response = await execute()
    }
  } catch {
    throw new Error(`${label} is unreachable. Check the backend URL and that the server is running.`)
  }

  if (!response.ok) {
    const responseText = await response.text().catch(() => '')
    let problem: ProblemResponse | null = null
    try {
      problem = responseText ? JSON.parse(responseText) as ProblemResponse : null
    } catch {
      problem = null
    }
    const validation = problem?.errors?.length ? ` ${problem.errors.join('; ')}` : ''
    const detail = problem?.detail
      ?? problem?.message
      ?? problem?.error
      ?? problem?.title
      ?? (responseText.trim() || `${label} request failed (${response.status})`)
    throw new Error(`${detail}${validation}`)
  }

  if (response.status === 204) return null as T
  return response.json() as Promise<T>
}
