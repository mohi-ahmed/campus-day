import {
  ArrowLeft, Eye, Flag, Flame, Heart, LockKeyhole, Megaphone, MessageCircle,
  MessageCircleHeart, PenLine, RefreshCw, Repeat2, Send, Smile, SmilePlus,
  Sparkles, X,
} from 'lucide-react'
import {
  useCallback, useEffect, useRef, useState, type CSSProperties, type FormEvent,
} from 'react'
import {
  autopsyApi,
  type AutopsyComment,
  type AutopsyPost,
  type Category,
  type CreatePostInput,
  type ReactionKey,
  type ReactionResult,
  type ReportReason,
} from './autopsyApi'
import {
  anonymousSessionId,
  rememberReaction,
  rememberRepost,
  storedReaction,
  storedRepost,
} from './autopsySession'
import './autopsy.css'

const categoryMeta: Record<Category, { label: string; prompt: string; icon: typeof Heart }> = {
  feelings: { label: 'Feelings', prompt: 'Share what is sitting on your mind.', icon: Heart },
  confession: { label: 'Confession', prompt: 'Say the thing you have been holding back.', icon: LockKeyhole },
  complaint: { label: 'Complaint', prompt: 'Call out an issue without calling out yourself.', icon: Megaphone },
  roasting: { label: 'Roasting', prompt: 'Keep it clever, campus-safe and never cruel.', icon: Flame },
  random: { label: 'Random', prompt: 'For thoughts that refuse to fit a category.', icon: Sparkles },
}

const reactions: { key: ReactionKey; symbol: string; label: string }[] = [
  { key: 'like', symbol: '👍', label: 'Like' },
  { key: 'unlike', symbol: '👎', label: 'Unlike' },
  { key: 'love', symbol: '❤️', label: 'Love' },
  { key: 'broken', symbol: '💔', label: 'Broken heart' },
  { key: 'haha', symbol: '😂', label: 'Haha' },
  { key: 'wow', symbol: '😮', label: 'Wow' },
  { key: 'angry', symbol: '😡', label: 'Angry' },
  { key: 'cry', symbol: '😢', label: 'Cry' },
  { key: 'fire', symbol: '🔥', label: 'Fire' },
]

const reportReasons: { value: ReportReason; label: string }[] = [
  { value: 'BULLYING_OR_HARASSMENT', label: 'Bullying or harassment' },
  { value: 'HATE_OR_ABUSIVE_CONTENT', label: 'Hate or abusive content' },
  { value: 'PERSONAL_INFORMATION', label: 'Personal information' },
  { value: 'SPAM_OR_MISLEADING', label: 'Spam or misleading' },
  { value: 'OTHER', label: 'Something else' },
]

const viewedInSession = new Set<string>()

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.'
}

function useRateLimitCountdown() {
  const [cooldownUntil, setCooldownUntil] = useState<number | null>(null)
  const [cooldownSeconds, setCooldownSeconds] = useState(0)

  useEffect(() => {
    if (cooldownUntil === null) return
    const updateCountdown = () => {
      const remaining = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000))
      setCooldownSeconds(remaining)
      if (remaining === 0) setCooldownUntil(null)
    }
    updateCountdown()
    const interval = window.setInterval(updateCountdown, 1000)
    return () => window.clearInterval(interval)
  }, [cooldownUntil])

  const startCooldown = useCallback((message: string) => {
    const match = message.match(/^Please wait (\d+) seconds before creating another (?:post|comment)$/i)
    if (!match) return false
    const seconds = Number(match[1])
    setCooldownSeconds(seconds)
    setCooldownUntil(Date.now() + seconds * 1000)
    return true
  }, [])

  return { cooldownSeconds, startCooldown }
}

function relativeTime(timestamp: string, reference: number) {
  const createdAt = Date.parse(timestamp)
  if (!Number.isFinite(createdAt)) return 'now'
  const seconds = Math.max(0, Math.floor((reference - createdAt) / 1000))
  if (seconds < 60) return 'now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

function ReactionControl({
  postId,
  count,
  onReact,
  onError,
}: {
  postId: string
  count: number
  onReact: (reaction: ReactionKey) => Promise<ReactionResult>
  onError: (message: string) => void
}) {
  const [selected, setSelected] = useState<ReactionKey | null>(() => storedReaction(postId))
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const controlRef = useRef<HTMLDivElement>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const longPressed = useRef(false)

  useEffect(() => () => {
    if (timer.current) clearTimeout(timer.current)
  }, [])

  useEffect(() => {
    if (!open) return
    const closeOutside = (event: PointerEvent) => {
      if (!controlRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', closeWithEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', closeWithEscape)
    }
  }, [open])

  const startPress = () => {
    if (busy) return
    longPressed.current = false
    timer.current = setTimeout(() => {
      longPressed.current = true
      setOpen(true)
    }, 420)
  }

  const stopPress = () => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
  }

  const submit = async (reaction: ReactionKey) => {
    if (busy) return
    setBusy(true)
    try {
      const result = await onReact(reaction)
      setSelected(result.selectedReaction)
      rememberReaction(postId, result.selectedReaction)
      setOpen(false)
    } catch (error) {
      onError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  const clickMain = () => {
    if (longPressed.current) {
      longPressed.current = false
      return
    }
    void submit(selected ?? 'like')
  }

  const active = reactions.find((reaction) => reaction.key === selected)

  return <div ref={controlRef} className="autopsy-reaction-control">
    {open ? <div className="autopsy-reaction-tray" role="menu" aria-label="Choose a reaction">
      {reactions.map((reaction) => <button
        type="button"
        role="menuitem"
        aria-label={reaction.label}
        title={reaction.label}
        key={reaction.key}
        disabled={busy}
        className={selected === reaction.key ? 'is-selected' : ''}
        onClick={() => void submit(reaction.key)}
      >{reaction.symbol}</button>)}
    </div> : null}
    <button
      type="button"
      disabled={busy}
      className={`autopsy-card-action autopsy-react-button ${selected ? 'is-reacted' : ''}`}
      aria-label={active ? `Remove ${active.label} reaction` : 'React. Press and hold for more reactions'}
      aria-expanded={open}
      onPointerDown={startPress}
      onPointerUp={stopPress}
      onPointerCancel={stopPress}
      onContextMenu={(event) => { event.preventDefault(); setOpen(true) }}
      onClick={clickMain}
    >
      {active ? <span className="autopsy-active-reaction">{active.symbol}</span> : <SmilePlus size={19} />}
      <span>{count}</span>
    </button>
  </div>
}

function AutopsyCard({
  post,
  featured,
  clock,
  onComments,
  onReport,
  onReact,
  onRepost,
  onViewed,
  onFeedback,
}: {
  post: AutopsyPost
  featured: boolean
  clock: number
  onComments: () => void
  onReport: () => void
  onReact: (reaction: ReactionKey) => Promise<ReactionResult>
  onRepost: () => Promise<{ reposted: boolean; reposts: number }>
  onViewed: () => Promise<void>
  onFeedback: (message: string, tone?: 'success' | 'error') => void
}) {
  const [reposted, setReposted] = useState(() => storedRepost(post.id))
  const [repostBusy, setRepostBusy] = useState(false)
  const cardRef = useRef<HTMLElement>(null)
  const meta = categoryMeta[post.category]
  const CategoryIcon = meta.icon

  useEffect(() => {
    const element = cardRef.current
    if (!element || viewedInSession.has(post.id)) return
    let timer: ReturnType<typeof setTimeout> | null = null
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && entry.intersectionRatio >= 0.45) {
        observer.disconnect()
        timer = setTimeout(() => {
          viewedInSession.add(post.id)
          void onViewed().catch(() => viewedInSession.delete(post.id))
        }, 700)
      }
    }, { threshold: [0.45] })
    observer.observe(element)
    return () => {
      observer.disconnect()
      if (timer) clearTimeout(timer)
    }
  }, [onViewed, post.id])

  const toggleRepost = async () => {
    if (repostBusy) return
    setRepostBusy(true)
    try {
      const result = await onRepost()
      setReposted(result.reposted)
      rememberRepost(post.id, result.reposted)
      onFeedback(result.reposted ? 'Reposted to your campus circle.' : 'Repost removed.')
    } catch (error) {
      onFeedback(errorMessage(error), 'error')
    } finally {
      setRepostBusy(false)
    }
  }

  return <article ref={cardRef} className={`autopsy-card autopsy-card--${post.category} ${featured ? 'autopsy-card--featured' : ''}`}>
    <header className="autopsy-card-header">
      <div className="autopsy-card-labels">
        <span className="autopsy-category"><CategoryIcon size={14} />{meta.label}</span>
        {post.tag ? <span className="autopsy-tag">{post.tag}</span> : null}
      </div>
      <span className="autopsy-time">{relativeTime(post.createdAt, clock)}</span>
    </header>

    <div className="autopsy-card-copy">
      {post.toWhom ? <p className="autopsy-to-whom">{post.toWhom}</p> : null}
      <p className="autopsy-post-text">“{post.body}”</p>
    </div>

    <p className="autopsy-author">by <strong>{post.nickname}</strong></p>

    <footer className="autopsy-card-footer">
      <div className="autopsy-card-metrics">
        <span title="Views"><Eye size={18} />{post.views}</span>
        <button type="button" className="autopsy-card-action" onClick={onComments}><MessageCircle size={18} />{post.comments}</button>
      </div>
      <div className="autopsy-card-tools">
        <ReactionControl postId={post.id} count={post.totalReactions} onReact={onReact} onError={(message) => onFeedback(message, 'error')} />
        <button
          type="button"
          disabled={repostBusy}
          className={`autopsy-card-action ${reposted ? 'is-active' : ''}`}
          aria-label={reposted ? 'Remove repost' : 'Repost'}
          onClick={() => void toggleRepost()}
        ><Repeat2 size={18} /><span>{post.reposts}</span></button>
        <button type="button" className="autopsy-card-action" aria-label="Report post" onClick={onReport}><Flag size={18} /></button>
      </div>
    </footer>
  </article>
}

function Composer({
  onClose,
  onPublish,
}: {
  onClose: () => void
  onPublish: (input: CreatePostInput) => Promise<void>
}) {
  const [step, setStep] = useState<1 | 2>(1)
  const [category, setCategory] = useState<Category | null>(null)
  const [toWhom, setToWhom] = useState('')
  const [body, setBody] = useState('')
  const [nickname, setNickname] = useState('')
  const [tag, setTag] = useState('')
  const [attempted, setAttempted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const { cooldownSeconds, startCooldown } = useRateLimitCountdown()
  const [switchingCategory, setSwitchingCategory] = useState(false)
  const categoryTransitionTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const toWhomRequired = category === 'complaint'
  const toWhomUsed = category === 'complaint' || category === 'roasting'
  const bodyError = attempted && (body.trim().length < 3 || body.trim().length > 1000)
  const invalidToWhom = toWhomUsed && (toWhomRequired
    ? toWhom.trim().length < 2
    : toWhom.trim().length > 0 && toWhom.trim().length < 2)
  const toWhomError = attempted && invalidToWhom

  useEffect(() => () => {
    if (categoryTransitionTimer.current) clearTimeout(categoryTransitionTimer.current)
  }, [])

  const chooseCategory = (nextCategory: Category) => {
    if (switchingCategory) return
    setCategory(nextCategory)
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setStep(2)
      return
    }
    setSwitchingCategory(true)
    categoryTransitionTimer.current = setTimeout(() => {
      setStep(2)
      setSwitchingCategory(false)
      categoryTransitionTimer.current = null
    }, 170)
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setAttempted(true)
    setSubmitError('')
    if (!category || body.trim().length < 3 || body.trim().length > 1000 || invalidToWhom) return
    setSubmitting(true)
    try {
      await onPublish({
        category,
        toWhom: toWhom.trim() || undefined,
        thoughts: body.trim(),
        nickname: nickname.trim() || undefined,
        tag: tag.trim() || undefined,
      })
    } catch (error) {
      const message = errorMessage(error)
      if (startCooldown(message)) {
        setSubmitError('')
      } else {
        setSubmitError(message)
      }
      setSubmitting(false)
    }
  }

  return <div className="autopsy-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="autopsy-composer" role="dialog" aria-modal="true" aria-labelledby="autopsy-composer-title">
      <header className="autopsy-composer-header">
        <div>
          <span>Anonymous posting</span>
          <h2 key={`${step}-${category ?? 'none'}`} className="autopsy-composer-title" id="autopsy-composer-title">{step === 1 ? 'What kind of post is this?' : category ? categoryMeta[category].label : 'Write a post'}</h2>
        </div>
        <button type="button" className="autopsy-close" aria-label="Close composer" onClick={onClose}><X size={20} /></button>
      </header>

      {step === 1 ? <>
        <div className={`autopsy-category-grid autopsy-composer-stage ${switchingCategory ? 'is-transitioning' : ''}`}>
          {(Object.keys(categoryMeta) as Category[]).map((key, index) => {
            const item = categoryMeta[key]
            const Icon = item.icon
            return <button
              type="button"
              key={key}
              data-category={key}
              aria-pressed={category === key}
              className={category === key ? 'is-selected' : ''}
              style={{ animationDelay: `${index * 45}ms` } as CSSProperties}
              onClick={() => chooseCategory(key)}
            >
              <span><Icon size={23} strokeWidth={1.8} /></span>
              <strong>{item.label}</strong>
              <small>{item.prompt}</small>
            </button>
          })}
        </div>
      </> : <form className="autopsy-form autopsy-composer-stage autopsy-form-stage" onSubmit={(event) => void submit(event)}>
        {toWhomUsed ? <label>
          <span>To whom {toWhomRequired ? <b>Required</b> : <small>Optional</small>}</span>
          <input value={toWhom} maxLength={120} onChange={(event) => setToWhom(event.target.value)} placeholder={category === 'complaint' ? 'Who or what is this complaint for?' : 'Who is getting roasted?'} />
          <em className={toWhomError ? 'is-error' : ''}>{toWhomError ? 'Use at least 2 characters.' : `${toWhom.length}/120`}</em>
        </label> : null}
        <label>
          <span>Your thoughts <b>Required</b></span>
          <textarea autoFocus value={body} maxLength={1000} onChange={(event) => setBody(event.target.value)} placeholder="Write honestly. Your identity is not stored with the post." rows={8} />
          <em className={bodyError ? 'is-error' : ''}>{bodyError ? 'Your post must be between 3 and 1000 characters.' : `${body.length}/1000`}</em>
        </label>
        <div className="autopsy-form-row">
          <label>
            <span>Nickname <small>Optional</small></span>
            <input value={nickname} maxLength={48} onChange={(event) => setNickname(event.target.value)} placeholder="Anonymous" />
          </label>
          <label>
            <span>Tag <small>Optional</small></span>
            <input value={tag} maxLength={32} onChange={(event) => setTag(event.target.value)} placeholder="#campuslife" />
          </label>
        </div>
        <div className="autopsy-safety-note"><LockKeyhole size={17} /><span><strong>Your identity is not saved with this post.</strong> Avoid sharing personal information or targeting someone with abuse.</span></div>
        {cooldownSeconds > 0
          ? <p className="autopsy-inline-error" role="status">Please wait {cooldownSeconds} seconds before creating another post</p>
          : submitError ? <p className="autopsy-inline-error" role="alert">{submitError}</p> : null}
        <div className="autopsy-composer-actions autopsy-composer-actions--split">
          <button type="button" className="autopsy-button autopsy-button--quiet" disabled={submitting} onClick={() => { setAttempted(false); setSubmitError(''); setStep(1) }}><ArrowLeft size={17} />Back</button>
          <button type="submit" disabled={submitting || cooldownSeconds > 0} className="autopsy-button autopsy-button--primary">{submitting ? 'Posting...' : cooldownSeconds > 0 ? `Wait ${cooldownSeconds}s` : 'Post anonymously'} <Send size={17} /></button>
        </div>
      </form>}
    </section>
  </div>
}

function CommentDrawer({
  post,
  clock,
  onClose,
  onAdded,
}: {
  post: AutopsyPost
  clock: number
  onClose: () => void
  onAdded: (comment: AutopsyComment) => void
}) {
  const [comments, setComments] = useState<AutopsyComment[]>([])
  const [comment, setComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [reload, setReload] = useState(0)
  const { cooldownSeconds, startCooldown } = useRateLimitCountdown()

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError('')
    void autopsyApi.listComments(post.id)
      .then((page) => { if (active) setComments(page.comments) })
      .catch((error) => { if (active) setLoadError(errorMessage(error)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [post.id, reload])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!comment.trim() || submitting) return
    setSubmitting(true)
    setSubmitError('')
    try {
      const created = await autopsyApi.createComment(
        post.id,
        anonymousSessionId(),
        comment.trim(),
      )
      setComments((current) => [created, ...current])
      setComment('')
      onAdded(created)
    } catch (error) {
      const message = errorMessage(error)
      setSubmitError(startCooldown(message) ? '' : message)
    } finally {
      setSubmitting(false)
    }
  }

  const count = loading ? post.comments : comments.length

  return <div className="autopsy-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <aside className="autopsy-comment-drawer" role="dialog" aria-modal="true" aria-labelledby="autopsy-comments-title">
      <header><div><span>Discussion</span><h2 id="autopsy-comments-title">{count} comments</h2></div><button type="button" className="autopsy-close" aria-label="Close comments" onClick={onClose}><X size={20} /></button></header>
      <div className="autopsy-comment-context"><span>{categoryMeta[post.category].label}</span><p>“{post.body}”</p></div>
      <div className="autopsy-comments" aria-busy={loading}>
        {loading ? <CommentSkeleton /> : loadError ? <div className="autopsy-comments-state" role="alert"><strong>Comments could not load</strong><p>{loadError}</p><button type="button" onClick={() => setReload((value) => value + 1)}><RefreshCw size={15} />Try again</button></div> : comments.length ? comments.map((item) => <div className="autopsy-comment" key={item.id}><span><MessageCircleHeart size={16} /></span><div><strong>{item.nickname}</strong><p>{item.content}</p><small>{relativeTime(item.createdAt, clock)}</small></div></div>) : <div className="autopsy-no-comments"><MessageCircle size={28} /><strong>Start the conversation</strong><p>Keep it kind. Your comment will also be anonymous.</p></div>}
      </div>
      <form className="autopsy-comment-form" onSubmit={(event) => void submit(event)}>
        <label><span className="sr-only">Add an anonymous comment</span><textarea value={comment} onChange={(event) => setComment(event.target.value)} maxLength={500} rows={2} placeholder="Add an anonymous comment..." /></label>
        <button type="submit" aria-label={cooldownSeconds > 0 ? `Wait ${cooldownSeconds} seconds` : 'Post comment'} disabled={!comment.trim() || submitting || cooldownSeconds > 0}><Send size={18} /></button>
        {cooldownSeconds > 0
          ? <p className="autopsy-inline-error" role="status">Please wait {cooldownSeconds} seconds before creating another comment</p>
          : submitError ? <p className="autopsy-inline-error" role="alert">{submitError}</p> : null}
      </form>
    </aside>
  </div>
}

function CommentSkeleton() {
  return <div className="autopsy-comment-skeleton" aria-label="Loading comments">
    {[0, 1, 2].map((item) => <div key={item}><span /><section><i /><b /></section></div>)}
  </div>
}

function ReportDialog({
  onClose,
  onSubmit,
}: {
  onClose: () => void
  onSubmit: (reason: ReportReason, details: string) => Promise<void>
}) {
  const [reason, setReason] = useState<ReportReason>('BULLYING_OR_HARASSMENT')
  const [details, setDetails] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const submit = async () => {
    if (submitting) return
    setSubmitting(true)
    setSubmitError('')
    try {
      await onSubmit(reason, details.trim())
    } catch (error) {
      setSubmitError(errorMessage(error))
      setSubmitting(false)
    }
  }

  return <div className="autopsy-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="autopsy-report" role="dialog" aria-modal="true" aria-labelledby="autopsy-report-title">
      <header><span><Flag size={18} /></span><div><small>Community safety</small><h2 id="autopsy-report-title">Report this post</h2></div><button type="button" className="autopsy-close" aria-label="Close report" onClick={onClose}><X size={20} /></button></header>
      <p>Choose the reason that best describes the problem. Reports are reviewed privately.</p>
      <div className="autopsy-report-options">{reportReasons.map((item) => <label key={item.value}><input type="radio" name="report-reason" value={item.value} checked={reason === item.value} onChange={() => setReason(item.value)} /><span>{item.label}</span></label>)}</div>
      <label className="autopsy-report-details"><span>Additional details <small>Optional</small></span><textarea rows={3} maxLength={500} value={details} onChange={(event) => setDetails(event.target.value)} placeholder="Add context for the reviewer" /><em>{details.length}/500</em></label>
      {submitError ? <p className="autopsy-inline-error autopsy-report-error" role="alert">{submitError}</p> : null}
      <div className="autopsy-composer-actions autopsy-composer-actions--split"><button type="button" disabled={submitting} className="autopsy-button autopsy-button--quiet" onClick={onClose}>Cancel</button><button type="button" disabled={submitting} className="autopsy-button autopsy-button--danger" onClick={() => void submit()}>{submitting ? 'Submitting...' : 'Submit report'}</button></div>
    </section>
  </div>
}

function FeedSkeleton() {
  return <section className="autopsy-grid autopsy-grid--loading" aria-label="Loading anonymous posts">
    {[0, 1, 2, 3].map((item) => <article className="autopsy-card autopsy-card-skeleton" key={item}><span /><i /><i /><b /></article>)}
  </section>
}

type Toast = { message: string; tone: 'success' | 'error' }

export default function AutopsyPage() {
  const [posts, setPosts] = useState<AutopsyPost[]>([])
  const [filter, setFilter] = useState<'all' | Category>('all')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [feedError, setFeedError] = useState('')
  const [reload, setReload] = useState(0)
  const [composerOpen, setComposerOpen] = useState(false)
  const [commentPostId, setCommentPostId] = useState<string | null>(null)
  const [reportPostId, setReportPostId] = useState<string | null>(null)
  const [toast, setToast] = useState<Toast | null>(null)
  const [clock, setClock] = useState(Date.now())

  const notify = useCallback((message: string, tone: Toast['tone'] = 'success') => {
    setToast({ message, tone })
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setFeedError('')
    void autopsyApi.listPosts(filter)
      .then((result) => {
        if (!active) return
        setPosts(result.posts)
        setPage(result.page)
        setTotalPages(result.totalPages)
      })
      .catch((error) => { if (active) setFeedError(errorMessage(error)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filter, reload])

  useEffect(() => {
    const interval = setInterval(() => setClock(Date.now()), 60 * 1000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (!composerOpen && commentPostId === null && reportPostId === null) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setComposerOpen(false)
        setCommentPostId(null)
        setReportPostId(null)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [composerOpen, commentPostId, reportPostId])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 3200)
    return () => clearTimeout(timer)
  }, [toast])

  const activeCommentPost = posts.find((post) => post.id === commentPostId)

  const publish = async (input: CreatePostInput) => {
    const created = await autopsyApi.createPost(input, anonymousSessionId())
    setFilter('all')
    setPosts((current) => [created, ...current])
    setComposerOpen(false)
    notify('Your anonymous post is live.')
  }

  const loadMore = async () => {
    if (loadingMore || page + 1 >= totalPages) return
    setLoadingMore(true)
    try {
      const result = await autopsyApi.listPosts(filter, page + 1)
      setPosts((current) => [...current, ...result.posts.filter((post) => !current.some((item) => item.id === post.id))])
      setPage(result.page)
      setTotalPages(result.totalPages)
    } catch (error) {
      notify(errorMessage(error), 'error')
    } finally {
      setLoadingMore(false)
    }
  }

  const react = useCallback(async (postId: string, reaction: ReactionKey) => {
    const result = await autopsyApi.react(postId, anonymousSessionId(), reaction)
    setPosts((current) => current.map((post) => post.id === postId ? {
      ...post,
      totalReactions: result.totalReactions,
      reactions: result.reactions,
    } : post))
    return result
  }, [])

  const repost = useCallback(async (postId: string) => {
    const result = await autopsyApi.toggleRepost(postId, anonymousSessionId())
    setPosts((current) => current.map((post) => post.id === postId ? { ...post, reposts: result.reposts } : post))
    return result
  }, [])

  const recordView = useCallback(async (postId: string) => {
    const result = await autopsyApi.recordView(postId)
    setPosts((current) => current.map((post) => post.id === postId ? { ...post, views: result.views } : post))
  }, [])

  const commentAdded = (comment: AutopsyComment) => {
    setPosts((current) => current.map((post) => post.id === comment.postId
      ? { ...post, comments: post.comments + 1 }
      : post))
  }

  const submitReport = async (reason: ReportReason, details: string) => {
    if (!reportPostId) return
    await autopsyApi.report(reportPostId, anonymousSessionId(), reason, details)
    setReportPostId(null)
    notify('Report submitted for private review.')
  }

  return <div className="autopsy-page">
    <section className="autopsy-hero">
      <div className="autopsy-hero-mark"><MessageCircleHeart size={22} /></div>
      <div>
        <p>Campus thoughts, no names attached</p>
        <h1>Autopsy</h1>
        <span>Feelings, confessions, complaints, roasts and every thought in between.</span>
      </div>
      <button type="button" className="autopsy-button autopsy-button--primary autopsy-write-button" onClick={() => setComposerOpen(true)}><PenLine size={18} />Write a post</button>
    </section>

    <div className="autopsy-toolbar">
      <div className="autopsy-filters" role="tablist" aria-label="Filter anonymous posts">
        <button type="button" role="tab" aria-selected={filter === 'all'} className={filter === 'all' ? 'is-active' : ''} onClick={() => setFilter('all')}>All posts</button>
        {(Object.keys(categoryMeta) as Category[]).map((key) => {
          const Icon = categoryMeta[key].icon
          return <button type="button" role="tab" data-category={key} aria-selected={filter === key} className={filter === key ? 'is-active' : ''} key={key} onClick={() => setFilter(key)}><Icon size={15} />{categoryMeta[key].label}</button>
        })}
      </div>
    </div>

    {loading ? <FeedSkeleton /> : feedError ? <section className="autopsy-feed-state" role="alert"><RefreshCw size={28} /><h2>Autopsy could not load</h2><p>{feedError}</p><button type="button" className="autopsy-button autopsy-button--primary" onClick={() => setReload((value) => value + 1)}>Try again</button></section> : posts.length ? <>
      <section className="autopsy-grid" aria-live="polite">
        {posts.map((post, index) => <AutopsyCard
            key={post.id}
            post={post}
            featured={index === 0}
            clock={clock}
            onComments={() => setCommentPostId(post.id)}
            onReport={() => setReportPostId(post.id)}
            onReact={(reaction) => react(post.id, reaction)}
            onRepost={() => repost(post.id)}
            onViewed={() => recordView(post.id)}
            onFeedback={notify}
          />)}
      </section>
      {page + 1 < totalPages ? <div className="autopsy-load-more"><button type="button" className="autopsy-button autopsy-button--quiet" disabled={loadingMore} onClick={() => void loadMore()}>{loadingMore ? 'Loading...' : 'Load more posts'}</button></div> : null}
    </> : <section className="autopsy-empty"><Smile size={30} /><h2>No posts here yet</h2><p>Be the first person to share something in this category.</p><button type="button" className="autopsy-button autopsy-button--primary" onClick={() => setComposerOpen(true)}>Write the first post</button></section>}

    <button type="button" className="autopsy-mobile-fab" onClick={() => setComposerOpen(true)}><PenLine size={19} /><span>Write</span></button>
    {composerOpen ? <Composer onClose={() => setComposerOpen(false)} onPublish={publish} /> : null}
    {activeCommentPost ? <CommentDrawer post={activeCommentPost} clock={clock} onClose={() => setCommentPostId(null)} onAdded={commentAdded} /> : null}
    {reportPostId ? <ReportDialog onClose={() => setReportPostId(null)} onSubmit={submitReport} /> : null}
    {toast ? <div className={`autopsy-toast ${toast.tone === 'error' ? 'is-error' : ''}`} role={toast.tone === 'error' ? 'alert' : 'status'}><MessageCircleHeart size={17} />{toast.message}</div> : null}
  </div>
}
