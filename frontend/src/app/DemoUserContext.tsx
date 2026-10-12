import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { demoUsers } from './demoUsers'
import type { DemoUser, StudentContext } from '../features/meal/types'
import { authSessionChangedEvent, readAuthSession } from '../features/public/authSession'
import { profileApi, toAppUser } from '../features/profile/profileApi'

interface DemoUserValue {
  user: DemoUser
  setContext: (context: StudentContext) => void
  authenticated: boolean
  profileLoading: boolean
  profileReady: boolean
  refreshProfile: () => Promise<void>
}

const DemoUserContext = createContext<DemoUserValue | null>(null)

export function DemoUserProvider({ children }: { children: ReactNode }) {
  const [context, setContext] = useState<StudentContext>('regular')
  const [authenticated, setAuthenticated] = useState(() => Boolean(readAuthSession()))
  const [profileUser, setProfileUser] = useState<DemoUser | null>(null)
  const [profileLoading, setProfileLoading] = useState(() => Boolean(readAuthSession()))

  const refreshProfile = useCallback(async () => {
    const session = readAuthSession()
    setAuthenticated(Boolean(session))
    if (!session?.account.profileComplete) {
      setProfileUser(null)
      setProfileLoading(false)
      return
    }
    setProfileLoading(true)
    try {
      setProfileUser(toAppUser(await profileApi.me()))
    } catch {
      setProfileUser(null)
    } finally {
      setProfileLoading(false)
    }
  }, [])

  useEffect(() => {
    void refreshProfile()
    const onAuthChange = () => { void refreshProfile() }
    window.addEventListener(authSessionChangedEvent, onAuthChange)
    return () => window.removeEventListener(authSessionChangedEvent, onAuthChange)
  }, [refreshProfile])

  const session = readAuthSession()
  const fallbackName = session?.account.email.split('@')[0].replace(/[._-]+/g, ' ') || 'Student'
  const authenticatedFallback: DemoUser = {
    id: 'profile-loading',
    name: fallbackName.replace(/\b\w/g, (letter) => letter.toUpperCase()),
    email: session?.account.email || '',
    initials: fallbackName.slice(0, 2).toUpperCase(),
    academicYear: 1,
    context: 'regular',
    isMealManager: false,
    profileImageUrl: null,
  }
  const user = authenticated ? profileUser ?? authenticatedFallback : demoUsers[context]
  const profileReady = !authenticated || Boolean(profileUser)
  const value = useMemo(() => ({ user, setContext, authenticated, profileLoading, profileReady, refreshProfile }),
    [user, authenticated, profileLoading, profileReady, refreshProfile])
  return <DemoUserContext.Provider value={value}>{children}</DemoUserContext.Provider>
}

// Context and its hook intentionally share this small module.
// eslint-disable-next-line react-refresh/only-export-components
export function useDemoUser() {
  const value = useContext(DemoUserContext)
  if (!value) throw new Error('useDemoUser must be used inside DemoUserProvider')
  return value
}
