import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { useDemoUser } from '../../app/DemoUserContext'
import { currentAffairsApi } from './currentAffairsApi'

interface CurrentAffairsStateValue {
  revision: number
  mutationError: string | null
  toggleBookmark: (articleId: string, bookmarked: boolean) => Promise<void>
  markRead: (articleId: string) => Promise<void>
  markUnread: (articleId: string) => Promise<void>
}

const CurrentAffairsStateContext = createContext<CurrentAffairsStateValue | null>(null)

export function CurrentAffairsStateProvider({ children }: { children: ReactNode }) {
  const { user } = useDemoUser()
  const [revision, setRevision] = useState(0)
  const [mutationError, setMutationError] = useState<string | null>(null)

  const mutate = useCallback(async (request: () => Promise<void>) => {
    setMutationError(null)
    try {
      await request()
      setRevision((value) => value + 1)
    } catch (error) {
      setMutationError(error instanceof Error ? error.message : 'Could not update this story.')
      throw error
    }
  }, [])

  const toggleBookmark = useCallback((articleId: string, bookmarked: boolean) => mutate(
    () => bookmarked
      ? currentAffairsApi.removeBookmark(user.id, articleId)
      : currentAffairsApi.bookmark(user.id, articleId),
  ), [mutate, user.id])

  const markRead = useCallback((articleId: string) => mutate(
    () => currentAffairsApi.markRead(user.id, articleId),
  ), [mutate, user.id])

  const markUnread = useCallback((articleId: string) => mutate(
    () => currentAffairsApi.markUnread(user.id, articleId),
  ), [mutate, user.id])

  const value = useMemo<CurrentAffairsStateValue>(() => ({
    revision,
    mutationError,
    toggleBookmark,
    markRead,
    markUnread,
  }), [markRead, markUnread, mutationError, revision, toggleBookmark])

  return <CurrentAffairsStateContext.Provider value={value}>{children}</CurrentAffairsStateContext.Provider>
}

// Context and hook share this feature-level module by design.
// eslint-disable-next-line react-refresh/only-export-components
export function useCurrentAffairsState() {
  const value = useContext(CurrentAffairsStateContext)
  if (!value) throw new Error('useCurrentAffairsState must be used inside CurrentAffairsStateProvider')
  return value
}
