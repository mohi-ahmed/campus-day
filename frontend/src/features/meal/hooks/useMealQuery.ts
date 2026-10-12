import { useEffect, useState } from 'react'

export function useMealQuery<T>(loader: () => Promise<T>, dependencies: readonly unknown[] = []) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    loader()
      .then((result) => { if (active) { setData(result); setError(null) } })
      .catch((cause) => {
        if (active) setError(cause instanceof Error
          ? cause.message
          : "We couldn't load this meal information. Please try again.")
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
    // The caller controls primitive query dependencies.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies)

  return { data, error, loading }
}
