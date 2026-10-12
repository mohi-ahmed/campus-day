import type { CSSProperties } from 'react'
import { Bookmark, BookOpen, Clock3, History, Newspaper, Search } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import type { CurrentAffairsArticle } from './currentAffairsApi'
import { formatArticleDate } from './currentAffairsData'

export function CurrentAffairsSubnav() {
  return <nav className="ca-subnav" aria-label="Current affairs navigation">
    <NavLink to="/current-affairs" end><Newspaper size={17} />Current affairs</NavLink>
    <NavLink to="/current-affairs/bookmarks"><Bookmark size={17} />My bookmarks</NavLink>
    <NavLink to="/current-affairs/history"><History size={17} />Reading history</NavLink>
  </nav>
}

export function CurrentAffairsSearch({ value, onChange, placeholder = 'Search current affairs' }: {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}) {
  return <label className="ca-search">
    <Search size={18} />
    <span className="sr-only">{placeholder}</span>
    <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    {value ? <button type="button" onClick={() => onChange('')} aria-label="Clear search">Clear</button> : null}
  </label>
}

export function BookmarkAction({ article, saved, onToggle, compact = false }: {
  article: CurrentAffairsArticle
  saved: boolean
  onToggle: () => void
  compact?: boolean
}) {
  return <button
    type="button"
    className={`ca-bookmark ${saved ? 'is-saved' : ''} ${compact ? 'is-compact' : ''}`}
    onClick={(event) => { event.preventDefault(); event.stopPropagation(); onToggle() }}
    aria-label={saved ? `Remove ${article.title} from bookmarks` : `Bookmark ${article.title}`}
    aria-pressed={saved}
  >
    <Bookmark size={compact ? 17 : 19} fill={saved ? 'currentColor' : 'none'} />
    {compact ? null : <span>{saved ? 'Saved' : 'Save'}</span>}
  </button>
}

function ArticleMeta({ article, read }: { article: CurrentAffairsArticle; read: boolean }) {
  return <div className="ca-article-meta">
    <span>{article.sourceName}</span>
    <span><Clock3 size={13} />{article.readTimeMinutes} min read</span>
    {!read ? <span className="ca-new-label">New</span> : null}
  </div>
}

export function EditorialArticleCard({ article, saved, read, onBookmark, variant = 'standard', index = 0 }: {
  article: CurrentAffairsArticle
  saved: boolean
  read: boolean
  onBookmark: () => void
  variant?: 'lead' | 'compact' | 'standard'
  index?: number
}) {
  const style = { '--ca-index': index } as CSSProperties
  return <article className={`ca-article-card ca-article-card--${variant} ${read ? 'is-read' : ''}`} style={style}>
    <Link className="ca-article-card__media" to={`/current-affairs/${article.id}`} aria-label={`Read ${article.title}`}>
      {article.imageUrl ? <img src={article.imageUrl} alt={article.imageAlt} /> : <span className="ca-image-fallback"><Newspaper size={30} /></span>}
    </Link>
    <div className="ca-article-card__body" lang={article.language === 'bn' ? 'bn' : 'en'}>
      <div className="ca-article-card__topline">
        <span className="ca-category">{article.category}</span>
        <BookmarkAction article={article} saved={saved} onToggle={onBookmark} compact />
      </div>
      <h3><Link to={`/current-affairs/${article.id}`}>{article.title}</Link></h3>
      <p>{article.description}</p>
      <ArticleMeta article={article} read={read} />
    </div>
  </article>
}

export function ReadingListItem({ article, saved, read, onBookmark, readAt, onRemoveHistory }: {
  article: CurrentAffairsArticle
  saved: boolean
  read: boolean
  onBookmark: () => void
  readAt?: string
  onRemoveHistory?: () => void
}) {
  return <article className="ca-reading-row" lang={article.language === 'bn' ? 'bn' : 'en'}>
    <Link className="ca-reading-row__media" to={`/current-affairs/${article.id}`}>
      {article.imageUrl ? <img src={article.imageUrl} alt="" /> : <span><BookOpen size={22} /></span>}
    </Link>
    <div className="ca-reading-row__content">
      <span className="ca-category">{article.category}</span>
      <h3><Link to={`/current-affairs/${article.id}`}>{article.title}</Link></h3>
      <p>{article.description}</p>
      <div className="ca-article-meta">
        <span>{article.sourceName}</span>
        <time dateTime={article.publishedAt}>{formatArticleDate(article.publishedAt, article.language)}</time>
        {readAt ? <span>Read {readAt}</span> : null}
      </div>
    </div>
    <div className="ca-reading-row__actions">
      <BookmarkAction article={article} saved={saved} onToggle={onBookmark} compact />
      {onRemoveHistory ? <button type="button" onClick={onRemoveHistory}>Remove</button> : null}
      {!read ? <span className="ca-new-label">New</span> : null}
    </div>
  </article>
}

export function CurrentAffairsEmpty({ title, description }: { title: string; description: string }) {
  return <div className="ca-empty">
    <span><BookOpen size={24} /></span>
    <h2>{title}</h2>
    <p>{description}</p>
    <Link to="/current-affairs">Browse current affairs</Link>
  </div>
}
