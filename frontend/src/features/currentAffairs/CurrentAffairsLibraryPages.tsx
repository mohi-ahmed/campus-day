import { useDeferredValue, useEffect, useState } from 'react'
import { Bookmark, History } from 'lucide-react'
import { currentAffairsCategories, formatHistoryDate } from './currentAffairsData'
import { currentAffairsApi, type CurrentAffairsArticleView } from './currentAffairsApi'
import { useCurrentAffairsState } from './CurrentAffairsState'
import { CurrentAffairsEmpty, CurrentAffairsSearch, CurrentAffairsSubnav, ReadingListItem } from './CurrentAffairsUi'
import { useDemoUser } from '../../app/DemoUserContext'
import './current-affairs.css'

function LibraryToolbar({ query, onQuery, category, onCategory }: {
  query: string
  onQuery: (value: string) => void
  category: string
  onCategory: (value: string) => void
}) {
  return <div className="ca-library-toolbar">
    <CurrentAffairsSearch value={query} onChange={onQuery} placeholder="Search this collection" />
    <label><span>Category</span><select value={category} onChange={(event) => onCategory(event.target.value)}>{currentAffairsCategories.map((item) => <option key={item}>{item}</option>)}</select></label>
  </div>
}

export function CurrentAffairsBookmarksPage() {
  const { user } = useDemoUser()
  const { revision, mutationError, toggleBookmark } = useCurrentAffairsState()
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const [category, setCategory] = useState('All stories')
  const [articles, setArticles] = useState<CurrentAffairsArticleView[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    currentAffairsApi.listBookmarks(user.id, {
      query: deferredQuery.trim() || undefined,
      category: category === 'All stories' ? undefined : category as CurrentAffairsArticleView['category'],
      size: 50,
    }).then((response) => {
      if (active) setArticles(response.items)
    }).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'Bookmarks could not be loaded.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [category, deferredQuery, revision, user.id])

  return <div className="ca-page ca-library-page">
    <CurrentAffairsSubnav />
    <header className="ca-library-header">
      <span><Bookmark size={22} /></span>
      <div><h1>My bookmarks</h1><p>Stories you saved for revision, reference or another quiet reading moment.</p></div>
      <strong>{articles.length}</strong>
    </header>
    <LibraryToolbar query={query} onQuery={setQuery} category={category} onCategory={setCategory} />
    {articles.length ? <div className="ca-reading-list">
      {articles.map((article) => <ReadingListItem
        key={article.id}
        article={article}
        saved
        read={article.isRead}
        onBookmark={() => { void toggleBookmark(article.id, true).catch(() => undefined) }}
      />)}
    </div> : <CurrentAffairsEmpty title={error ? 'Bookmarks unavailable' : loading ? 'Loading bookmarks' : 'No bookmarks here'} description={error ?? (loading ? 'Fetching your saved stories.' : 'Save useful stories from the current affairs feed and they will appear here.')} />}
    {mutationError ? <p role="alert" className="ca-results-meta">{mutationError}</p> : null}
  </div>
}

export function CurrentAffairsHistoryPage() {
  const { user } = useDemoUser()
  const { revision, mutationError, toggleBookmark, markUnread } = useCurrentAffairsState()
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const [category, setCategory] = useState('All stories')
  const [articles, setArticles] = useState<CurrentAffairsArticleView[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    currentAffairsApi.history(user.id, {
      query: deferredQuery.trim() || undefined,
      category: category === 'All stories' ? undefined : category as CurrentAffairsArticleView['category'],
      size: 50,
    }).then((response) => {
      if (active) setArticles(response.items)
    }).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'Reading history could not be loaded.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [category, deferredQuery, revision, user.id])

  return <div className="ca-page ca-library-page">
    <CurrentAffairsSubnav />
    <header className="ca-library-header">
      <span><History size={22} /></span>
      <div><h1>Reading history</h1><p>Your private trail of recently opened current-affairs stories.</p></div>
      <strong>{articles.length}</strong>
    </header>
    <LibraryToolbar query={query} onQuery={setQuery} category={category} onCategory={setCategory} />
    {articles.length ? <div className="ca-reading-list">
      {articles.map((article) => <ReadingListItem
        key={article.id}
        article={article}
        saved={article.isBookmarked}
        read
        readAt={article.readAt ? formatHistoryDate(article.readAt) : undefined}
        onBookmark={() => { void toggleBookmark(article.id, article.isBookmarked).catch(() => undefined) }}
        onRemoveHistory={() => { void markUnread(article.id).catch(() => undefined) }}
      />)}
    </div> : <CurrentAffairsEmpty title={error ? 'History unavailable' : loading ? 'Loading reading history' : 'No reading history'} description={error ?? (loading ? 'Fetching your recent stories.' : 'Open a story from the current affairs feed to begin your private reading history.')} />}
    {mutationError ? <p role="alert" className="ca-results-meta">{mutationError}</p> : null}
  </div>
}
