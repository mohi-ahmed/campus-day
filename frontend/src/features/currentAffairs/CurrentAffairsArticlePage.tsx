import { useEffect, useState } from 'react'
import { ArrowLeft, ArrowUpRight, Check, Clock3, EyeOff } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { formatArticleDate } from './currentAffairsData'
import { currentAffairsApi, type CurrentAffairsArticleView } from './currentAffairsApi'
import { useCurrentAffairsState } from './CurrentAffairsState'
import { BookmarkAction, CurrentAffairsEmpty, CurrentAffairsSubnav, EditorialArticleCard } from './CurrentAffairsUi'
import { useDemoUser } from '../../app/DemoUserContext'
import './current-affairs.css'

export default function CurrentAffairsArticlePage() {
  const { articleId } = useParams()
  const { user } = useDemoUser()
  const { mutationError, toggleBookmark, markRead, markUnread } = useCurrentAffairsState()
  const [article, setArticle] = useState<CurrentAffairsArticleView | null>(null)
  const [related, setRelated] = useState<CurrentAffairsArticleView[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!articleId) return
    let active = true
    setLoading(true)
    setError(null)
    const load = async () => {
      const articleRequest = currentAffairsApi.getArticle(user.id, articleId)
      const markReadRequest = markRead(articleId).catch(() => undefined)
      const [loaded] = await Promise.all([articleRequest, markReadRequest])
      if (!active) return
      setArticle({ ...loaded, isRead: true, readAt: loaded.readAt ?? new Date().toISOString() })
      const relatedPage = await currentAffairsApi.list(user.id, { category: loaded.category, size: 4 })
      if (active) setRelated(relatedPage.items.filter((item) => item.id !== loaded.id).slice(0, 3))
    }
    load().catch((reason: unknown) => {
      if (active) setError(reason instanceof Error ? reason.message : 'This story could not be loaded.')
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [articleId, markRead, user.id])

  if (!article) return <div className="ca-page"><CurrentAffairsSubnav /><CurrentAffairsEmpty title={error ? 'Article unavailable' : loading ? 'Loading article' : 'Article not found'} description={error ?? (loading ? 'Fetching the story and source details.' : 'This story may have moved or is no longer available.')} /></div>

  const saved = article.isBookmarked
  const isRead = article.isRead

  const handleBookmark = async () => {
    await toggleBookmark(article.id, article.isBookmarked)
    setArticle((current) => current ? { ...current, isBookmarked: !current.isBookmarked } : current)
  }

  const handleRead = async () => {
    if (article.isRead) {
      await markUnread(article.id)
      setArticle((current) => current ? { ...current, isRead: false, readAt: undefined } : current)
      return
    }
    await markRead(article.id)
    setArticle((current) => current ? { ...current, isRead: true, readAt: new Date().toISOString() } : current)
  }

  return <div className="ca-page ca-article-page">
    <CurrentAffairsSubnav />
    <Link className="ca-back" to="/current-affairs"><ArrowLeft size={17} />Back to current affairs</Link>

    <article className="ca-story" lang={article.language === 'bn' ? 'bn' : 'en'}>
      <header className="ca-story__header">
        <span className="ca-category">{article.category}</span>
        <h1>{article.title}</h1>
        <p>{article.description}</p>
        <div className="ca-story__meta">
          <span>{article.sourceName}</span>
          <time dateTime={article.publishedAt}>{formatArticleDate(article.publishedAt, article.language)}</time>
          <span><Clock3 size={14} />{article.readTimeMinutes} min read</span>
        </div>
        <div className="ca-story__actions">
          <BookmarkAction article={article} saved={saved} onToggle={() => { void handleBookmark().catch(() => undefined) }} />
          <button type="button" className="ca-story__read-toggle" onClick={() => { void handleRead().catch(() => undefined) }}>
            {isRead ? <EyeOff size={17} /> : <Check size={17} />}{isRead ? 'Mark unread' : 'Mark as read'}
          </button>
          <a href={article.sourceUrl} target="_blank" rel="noreferrer">Open source website <ArrowUpRight size={17} /></a>
        </div>
      </header>

      <figure className="ca-story__media">
        {article.imageUrl ? <img src={article.imageUrl} alt={article.imageAlt} /> : null}
      </figure>

      <div className="ca-story__layout">
        <div className="ca-story__body">
          {article.body.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
        </div>
        <aside className="ca-story__source">
          <span>Source information</span>
          <strong>{article.sourceName}</strong>
          <p>This is sample UI content. The production API will provide the original article URL, publication metadata and source attribution.</p>
          <a href={article.sourceUrl} target="_blank" rel="noreferrer">Visit source <ArrowUpRight size={15} /></a>
        </aside>
      </div>
      {mutationError ? <p role="alert" className="ca-results-meta">{mutationError}</p> : null}
    </article>

    {related.length ? <section className="ca-related" aria-labelledby="related-coverage-title">
      <div className="ca-section-heading"><div><h2 id="related-coverage-title">Related coverage</h2><p>Continue with another perspective on this topic.</p></div></div>
      <div className="ca-related-grid">
        {related.map((item, index) => <EditorialArticleCard
          key={item.id}
          article={item}
          saved={item.isBookmarked}
          read={item.isRead}
          onBookmark={() => { void toggleBookmark(item.id, item.isBookmarked).catch(() => undefined) }}
          variant="standard"
          index={index}
        />)}
      </div>
    </section> : null}
  </div>
}
