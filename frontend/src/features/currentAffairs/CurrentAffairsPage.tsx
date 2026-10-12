import { useDeferredValue, useEffect, useMemo, useState } from 'react'
import { ArrowRight, ChevronDown, Filter, RotateCcw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { currentAffairsApi, type CurrentAffairsArticleView, type CurrentAffairsContentType, type CurrentAffairsLanguage, type CurrentAffairsPageResponse, type CurrentAffairsRegion } from './currentAffairsApi'
import { currentAffairsCategories } from './currentAffairsData'
import { useDemoUser } from '../../app/DemoUserContext'
import { useCurrentAffairsState } from './CurrentAffairsState'
import {
  CurrentAffairsEmpty,
  CurrentAffairsSearch,
  CurrentAffairsSubnav,
  EditorialArticleCard,
} from './CurrentAffairsUi'
import './current-affairs.css'

type FeedMode = 'news' | 'current-affairs'
type ReadFilter = 'all' | 'read' | 'unread'

const modeToType: Record<FeedMode, CurrentAffairsContentType> = {
  news: 'NEWS',
  'current-affairs': 'CURRENT_AFFAIRS',
}

export default function CurrentAffairsPage() {
  const { user } = useDemoUser()
  const { revision, mutationError, toggleBookmark } = useCurrentAffairsState()
  const [mode, setMode] = useState<FeedMode>('current-affairs')
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const [category, setCategory] = useState('All stories')
  const [region, setRegion] = useState<'all' | CurrentAffairsRegion>('all')
  const [language, setLanguage] = useState<'all' | CurrentAffairsLanguage>('all')
  const [source, setSource] = useState('all')
  const [readFilter, setReadFilter] = useState<ReadFilter>('all')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [visibleCount, setVisibleCount] = useState(6)
  const [feed, setFeed] = useState<CurrentAffairsPageResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const articles = feed?.items ?? []
  const sources = useMemo(() => [...new Set(articles.map((article) => article.sourceName))], [articles])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    currentAffairsApi.list(user.id, {
      query: deferredQuery.trim() || undefined,
      category: category === 'All stories' ? undefined : category as CurrentAffairsArticleView['category'],
      region: region === 'all' ? undefined : region,
      language: language === 'all' ? undefined : language,
      source: source === 'all' ? undefined : source,
      contentType: modeToType[mode],
      readStatus: readFilter === 'all' ? undefined : readFilter,
      page: 0,
      size: 50,
    }).then((response) => {
      if (active) setFeed(response)
    }).catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'Current affairs could not be loaded.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [category, deferredQuery, language, mode, readFilter, region, revision, source, user.id])

  useEffect(() => setVisibleCount(6), [category, language, mode, query, readFilter, region, source])

  const highlights = articles.slice(0, 3)
  const visibleArticles = articles.slice(0, visibleCount)
  const editionDate = new Intl.DateTimeFormat('en-BD', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())
  const activeFilterCount = [region !== 'all', language !== 'all', source !== 'all', readFilter !== 'all'].filter(Boolean).length

  const resetFilters = () => {
    setCategory('All stories')
    setRegion('all')
    setLanguage('all')
    setSource('all')
    setReadFilter('all')
  }

  return <div className="ca-page">
    <CurrentAffairsSubnav />

    <header className="ca-hero">
      <div className="ca-hero__copy">
        <span>CampusDay Current Affairs</span>
        <h1>See the world beyond your syllabus.</h1>
        <p>Recent stories, clear context and reliable source links in one focused student feed.</p>
        <div className="ca-mode-switch" aria-label="Choose content type">
          <button type="button" className={mode === 'current-affairs' ? 'is-active' : ''} onClick={() => setMode('current-affairs')}>Current affairs</button>
          <button type="button" className={mode === 'news' ? 'is-active' : ''} onClick={() => setMode('news')}>News</button>
        </div>
      </div>
      <div className="ca-edition-card">
        <span>Latest available edition</span>
        <strong>{editionDate}</strong>
        <p>Sample frontend data until the Spring Boot API is connected.</p>
        <div><span>{feed?.totalElements ?? 0} stories</span><span>{articles.filter((article) => !article.isRead).length} unread here</span></div>
      </div>
    </header>

    <section className="ca-highlights" aria-labelledby="ca-highlights-title">
      <div className="ca-section-heading">
        <div><h2 id="ca-highlights-title">Today's highlights</h2><p>A few stories worth your attention.</p></div>
        <a href="#daily-reading">Browse the edition <ArrowRight size={16} /></a>
      </div>
      <div className="ca-highlight-grid">
        {highlights.map((article, index) => <EditorialArticleCard
          key={article.id}
          article={article}
          saved={article.isBookmarked}
          read={article.isRead}
          onBookmark={() => { void toggleBookmark(article.id, article.isBookmarked).catch(() => undefined) }}
          variant={index === 0 ? 'lead' : 'compact'}
          index={index}
        />)}
      </div>
    </section>

    <section className="ca-feed" id="daily-reading" aria-labelledby="ca-feed-title">
      <div className="ca-section-heading ca-section-heading--feed">
        <div><h2 id="ca-feed-title">Fresh perspectives</h2><p>Search the latest stories stored by CampusDay.</p></div>
        <button type="button" className={`ca-filter-toggle ${filtersOpen ? 'is-active' : ''}`} onClick={() => setFiltersOpen((value) => !value)} aria-expanded={filtersOpen}>
          <Filter size={17} />Filters{activeFilterCount ? <span>{activeFilterCount}</span> : null}<ChevronDown size={15} />
        </button>
      </div>

      <div className="ca-discovery-bar">
        <CurrentAffairsSearch value={query} onChange={setQuery} placeholder="Search topics, sources or keywords" />
        <div className="ca-topic-strip" aria-label="Filter by topic">
          {currentAffairsCategories.map((item) => <button type="button" key={item} className={category === item ? 'is-active' : ''} onClick={() => setCategory(item)}>{item}</button>)}
        </div>
      </div>

      {filtersOpen ? <div className="ca-filter-panel">
        <label><span>Region</span><select value={region} onChange={(event) => setRegion(event.target.value as typeof region)}><option value="all">All regions</option><option value="bangladesh">Bangladesh</option><option value="international">International</option></select></label>
        <label><span>Language</span><select value={language} onChange={(event) => setLanguage(event.target.value as typeof language)}><option value="all">All languages</option><option value="bn">বাংলা</option><option value="en">English</option></select></label>
        <label><span>Source</span><select value={source} onChange={(event) => setSource(event.target.value)}><option value="all">All sources</option>{sources.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
        <label><span>Reading status</span><select value={readFilter} onChange={(event) => setReadFilter(event.target.value as ReadFilter)}><option value="all">Read and unread</option><option value="unread">Unread only</option><option value="read">Read only</option></select></label>
        <button type="button" onClick={resetFilters}><RotateCcw size={15} />Reset filters</button>
      </div> : null}

      <div className="ca-results-meta">
        <span>{feed?.totalElements ?? 0} {(feed?.totalElements ?? 0) === 1 ? 'story' : 'stories'} in this view</span>
        <span>Showing the latest available updates</span>
      </div>

      {error ? <CurrentAffairsEmpty title="Current affairs unavailable" description={error} /> : loading && !feed ? <CurrentAffairsEmpty title="Loading the latest edition" description="Fetching the newest stored stories." /> : visibleArticles.length ? <div className="ca-feed-grid">
        {visibleArticles.map((article, index) => <EditorialArticleCard
          key={article.id}
          article={article}
          saved={article.isBookmarked}
          read={article.isRead}
          onBookmark={() => { void toggleBookmark(article.id, article.isBookmarked).catch(() => undefined) }}
          index={index}
        />)}
      </div> : <CurrentAffairsEmpty title="No stories found" description="Try another keyword, topic or filter combination." />}

      {mutationError ? <p role="alert" className="ca-results-meta">{mutationError}</p> : null}
      {visibleCount < articles.length ? <button type="button" className="ca-load-more" onClick={() => setVisibleCount((value) => value + 6)}>Load more stories <ArrowRight size={16} /></button> : null}
    </section>

    <section className="ca-reading-note">
      <div><span>Build a small daily habit</span><h2>A little reading creates a bigger perspective.</h2></div>
      <p>Open one useful story, follow its source and save what you want to revisit.</p>
      <Link to="/current-affairs/bookmarks">Open my bookmarks <ArrowRight size={17} /></Link>
    </section>
  </div>
}
