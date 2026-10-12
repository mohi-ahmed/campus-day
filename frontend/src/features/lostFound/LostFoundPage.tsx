import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import {
  Backpack, Bell, BookOpen, Check, ChevronLeft, CircleHelp, Headphones,
  KeyRound, MapPin, MessageCircle, Plus, RefreshCw, Search, Send, Shirt,
  WalletCards, X, type LucideIcon,
} from 'lucide-react'
import { useDemoUser } from '../../app/DemoUserContext'
import {
  lostFoundApi,
  type LostFoundCategory,
  type LostFoundComment,
  type LostFoundConversation,
  type LostFoundPost,
  type LostFoundThread,
  type LostFoundType,
} from './lostFoundApi'
import './lost-found.css'

type StatusFilter = 'all' | 'lost' | 'found'
type ModalName = 'report' | 'detail' | 'contact' | 'messages' | 'thread' | null
type ReportWhen = 'Today' | 'Yesterday' | 'Earlier'

const categoryLabels: Record<LostFoundCategory, string> = {
  ELECTRONICS: 'Electronics', ID_AND_CARDS: 'ID & Cards', BAGS: 'Bags', KEYS: 'Keys',
  CLOTHING: 'Clothing', BOOKS: 'Books', OTHER: 'Other',
}

const categoryIcons: Record<LostFoundCategory, LucideIcon> = {
  ELECTRONICS: Headphones, ID_AND_CARDS: WalletCards, BAGS: Backpack, KEYS: KeyRound,
  CLOTHING: Shirt, BOOKS: BookOpen, OTHER: CircleHelp,
}

const reportCategories = Object.keys(categoryLabels) as LostFoundCategory[]
const categories: Array<'ALL' | LostFoundCategory> = ['ALL', ...reportCategories]

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.'
}

function relativeTime(value: string | null) {
  if (!value) return 'No messages yet'
  const milliseconds = Date.now() - new Date(value).getTime()
  if (!Number.isFinite(milliseconds) || milliseconds < 60_000) return 'Just now'
  const minutes = Math.floor(milliseconds / 60_000)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return `${Math.floor(days / 7)}w ago`
}

function occurredOn(when: ReportWhen) {
  const date = new Date()
  if (when === 'Yesterday') date.setDate(date.getDate() - 1)
  if (when === 'Earlier') date.setDate(date.getDate() - 7)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function Modal({ children, onClose, labelledBy, compact = false }: { children: ReactNode; onClose: () => void; labelledBy: string; compact?: boolean }) {
  return <div className="lf-modal-layer" role="presentation" onMouseDown={onClose}>
    <section className={`lf-modal ${compact ? 'lf-modal--compact' : ''}`} role="dialog" aria-modal="true" aria-labelledby={labelledBy} onMouseDown={(event) => event.stopPropagation()}>
      <div className="lf-modal__handle" aria-hidden="true" />
      <button className="lf-round-button lf-modal__close" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button>
      {children}
    </section>
  </div>
}

function ItemGlyph({ type, category, size = 'normal' }: { type: LostFoundType; category: LostFoundCategory; size?: 'normal' | 'large' }) {
  const Icon = categoryIcons[category]
  return <span className={`lf-item-glyph lf-item-glyph--${type.toLowerCase()} ${size === 'large' ? 'is-large' : ''}`}><Icon size={size === 'large' ? 24 : 19} strokeWidth={1.8} /></span>
}

function LostFoundCard({ post, currentStudentId, onOpen, onContact }: {
  post: LostFoundPost
  currentStudentId: string
  onOpen: () => void
  onContact: () => void
}) {
  const ownPost = post.ownerStudentId === currentStudentId
  const status = post.type.toLowerCase()
  const action = ownPost ? 'Your post' : post.type === 'LOST' ? 'I found it' : 'Claim this'
  return <article className="lf-card" tabIndex={0} role="button" onClick={onOpen} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') onOpen() }}>
    <div className="lf-card__head">
      <ItemGlyph type={post.type} category={post.category} />
      <div className="lf-card__title"><span>{categoryLabels[post.category]}</span><h3>{post.title}</h3></div>
      <span className={`lf-status lf-status--${status}`}>{post.type === 'LOST' ? 'Lost' : 'Found'}</span>
    </div>
    {post.description ? <p>{post.description}</p> : null}
    <div className="lf-card__foot">
      <div><strong>{post.location}</strong><span>{relativeTime(post.createdAt)}</span></div>
      <div className="lf-card__actions">
        <button type="button" className="lf-comment-button" aria-label={`View ${post.commentCount} comments`} onClick={(event) => { event.stopPropagation(); onOpen() }}><MessageCircle size={17} />{post.commentCount}</button>
        <button type="button" disabled={ownPost} className={`lf-contact-button lf-contact-button--${status}`} onClick={(event) => { event.stopPropagation(); onContact() }}>{action}</button>
      </div>
    </div>
  </article>
}

function FeedSkeleton() {
  return <div className="lf-loading-list" aria-label="Loading lost and found posts">
    {[0, 1, 2].map((item) => <div className="lf-card lf-card--skeleton" key={item}><span /><div><i /><i /><i /></div></div>)}
  </div>
}

export default function LostFoundPage() {
  const { user } = useDemoUser()
  const [posts, setPosts] = useState<LostFoundPost[]>([])
  const [feedLoading, setFeedLoading] = useState(true)
  const [feedError, setFeedError] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | LostFoundCategory>('ALL')
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState<ModalName>(null)
  const [selectedPost, setSelectedPost] = useState<LostFoundPost | null>(null)
  const [comments, setComments] = useState<LostFoundComment[]>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [reportType, setReportType] = useState<LostFoundType>('LOST')
  const [reportCategory, setReportCategory] = useState<LostFoundCategory>('ELECTRONICS')
  const [reportWhen, setReportWhen] = useState<ReportWhen>('Today')
  const [postSuccess, setPostSuccess] = useState(false)
  const [formBusy, setFormBusy] = useState(false)
  const [actionError, setActionError] = useState('')
  const [comment, setComment] = useState('')
  const [message, setMessage] = useState('')
  const [reply, setReply] = useState('')
  const [conversations, setConversations] = useState<LostFoundConversation[]>([])
  const [inboxLoading, setInboxLoading] = useState(false)
  const [inboxError, setInboxError] = useState('')
  const [thread, setThread] = useState<LostFoundThread | null>(null)
  const [threadLoading, setThreadLoading] = useState(false)
  const [unread, setUnread] = useState(0)
  const [toast, setToast] = useState('')
  const reportForm = useRef<HTMLFormElement>(null)

  const loadPosts = useCallback(async (searchText = '') => {
    setFeedLoading(true)
    setFeedError('')
    try {
      const response = await lostFoundApi.listPosts({ query: searchText, size: 50 })
      setPosts(response.items)
    } catch (error) {
      setFeedError(errorMessage(error))
    } finally {
      setFeedLoading(false)
    }
  }, [])

  const loadUnread = useCallback(async () => {
    try {
      const response = await lostFoundApi.unreadCount(user.id)
      setUnread(response.unreadCount)
    } catch {
      setUnread(0)
    }
  }, [user.id])

  useEffect(() => {
    const timer = window.setTimeout(() => void loadPosts(query), query ? 280 : 0)
    return () => window.clearTimeout(timer)
  }, [loadPosts, query])

  useEffect(() => {
    const timer = window.setTimeout(() => void loadUnread(), 0)
    return () => window.clearTimeout(timer)
  }, [loadUnread])

  useEffect(() => {
    if (!postSuccess) return
    const timer = window.setTimeout(() => {
      setPostSuccess(false)
      setModal(null)
      reportForm.current?.reset()
    }, 1200)
    return () => window.clearTimeout(timer)
  }, [postSuccess])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 2200)
    return () => window.clearTimeout(timer)
  }, [toast])

  useEffect(() => {
    if (!modal) return
    const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') setModal(null) }
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [modal])

  const filteredPosts = useMemo(() => posts.filter((post) => {
    const statusMatches = statusFilter === 'all' || post.type.toLowerCase() === statusFilter
    const categoryMatches = categoryFilter === 'ALL' || post.category === categoryFilter
    return statusMatches && categoryMatches
  }), [categoryFilter, posts, statusFilter])

  const counts = {
    all: posts.length,
    lost: posts.filter((post) => post.type === 'LOST').length,
    found: posts.filter((post) => post.type === 'FOUND').length,
  }

  const openReport = (type: LostFoundType) => {
    setReportType(type)
    setPostSuccess(false)
    setActionError('')
    setModal('report')
  }

  const openItem = async (post: LostFoundPost) => {
    setSelectedPost(post)
    setComments([])
    setDetailError('')
    setActionError('')
    setDetailLoading(true)
    setModal('detail')
    try {
      const [freshPost, commentPage] = await Promise.all([
        lostFoundApi.getPost(post.id), lostFoundApi.listComments(post.id),
      ])
      setSelectedPost(freshPost)
      setComments(commentPage.items)
    } catch (error) {
      setDetailError(errorMessage(error))
    } finally {
      setDetailLoading(false)
    }
  }

  const openContact = (post: LostFoundPost) => {
    if (post.ownerStudentId === user.id) return
    setSelectedPost(post)
    setMessage('')
    setActionError('')
    setModal('contact')
  }

  const submitReport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormBusy(true)
    setActionError('')
    const data = new FormData(event.currentTarget)
    try {
      const created = await lostFoundApi.createPost({
        ownerStudentId: user.id,
        type: reportType,
        category: reportCategory,
        title: String(data.get('title') || '').trim(),
        description: String(data.get('details') || '').trim() || undefined,
        location: String(data.get('place') || '').trim(),
        occurredOn: occurredOn(reportWhen),
        contact: String(data.get('contact') || '').trim() || undefined,
      })
      setPosts((current) => [created, ...current])
      setPostSuccess(true)
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setFormBusy(false)
    }
  }

  const submitComment = async (event: FormEvent) => {
    event.preventDefault()
    if (!selectedPost || !comment.trim() || formBusy) return
    setFormBusy(true)
    setActionError('')
    try {
      const created = await lostFoundApi.createComment(selectedPost.id, user.id, comment.trim())
      setComments((current) => [created, ...current])
      setSelectedPost((current) => current ? { ...current, commentCount: current.commentCount + 1 } : current)
      setPosts((current) => current.map((post) => post.id === selectedPost.id ? { ...post, commentCount: post.commentCount + 1 } : post))
      setComment('')
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setFormBusy(false)
    }
  }

  const markReturned = async () => {
    if (!selectedPost || formBusy) return
    setFormBusy(true)
    setActionError('')
    try {
      await lostFoundApi.markReturned(selectedPost.id, user.id)
      setPosts((current) => current.filter((post) => post.id !== selectedPost.id))
      setModal(null)
      setToast('Item marked as returned')
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setFormBusy(false)
    }
  }

  const loadInbox = async () => {
    setInboxLoading(true)
    setInboxError('')
    setModal('messages')
    try {
      setConversations(await lostFoundApi.listConversations(user.id))
    } catch (error) {
      setInboxError(errorMessage(error))
    } finally {
      setInboxLoading(false)
    }
  }

  const openThread = async (conversation: LostFoundConversation) => {
    setThreadLoading(true)
    setThread(null)
    setActionError('')
    setModal('thread')
    try {
      const [loadedThread, readResult] = await Promise.all([
        lostFoundApi.getThread(conversation.id, user.id),
        lostFoundApi.markConversationRead(conversation.id, user.id),
      ])
      setThread(loadedThread)
      setUnread(readResult.unreadCount)
      setConversations((current) => current.map((entry) => entry.id === conversation.id ? { ...entry, unreadCount: 0 } : entry))
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setThreadLoading(false)
    }
  }

  const sendContactMessage = async (event: FormEvent) => {
    event.preventDefault()
    if (!selectedPost || !message.trim() || formBusy) return
    setFormBusy(true)
    setActionError('')
    try {
      const sent = await lostFoundApi.startConversation(selectedPost.id, user.id, message.trim())
      const loadedThread = await lostFoundApi.getThread(sent.conversationId, user.id)
      setThread(loadedThread)
      setMessage('')
      setToast('Message sent')
      setModal('thread')
      void loadUnread()
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setFormBusy(false)
    }
  }

  const submitReply = async (event: FormEvent) => {
    event.preventDefault()
    if (!thread || !reply.trim() || formBusy) return
    setFormBusy(true)
    setActionError('')
    try {
      const sent = await lostFoundApi.reply(thread.conversation.id, user.id, reply.trim())
      setThread((current) => current ? { ...current, messages: [...current.messages, sent], totalElements: current.totalElements + 1 } : current)
      setReply('')
      setToast('Reply sent')
      void loadUnread()
    } catch (error) {
      setActionError(errorMessage(error))
    } finally {
      setFormBusy(false)
    }
  }

  const ownSelectedPost = selectedPost?.ownerStudentId === user.id

  return <div className="lf-page">
    <header className="lf-header">
      <div className="lf-header__actions">
        <button type="button" className="lf-round-button" aria-label="Open messages" onClick={() => void loadInbox()}><Bell size={20} />{unread ? <span className="lf-notification-count">{unread}</span> : null}</button>
        <button type="button" className={`lf-round-button ${searchOpen ? 'is-active' : ''}`} aria-label="Search lost and found posts" aria-expanded={searchOpen} onClick={() => setSearchOpen((value) => !value)}><Search size={20} /></button>
        <button type="button" className="lf-round-button lf-round-button--primary" aria-label="Report an item" onClick={() => openReport('LOST')}><Plus size={23} /></button>
      </div>
    </header>

    {searchOpen ? <label className="lf-search"><Search size={18} /><span className="sr-only">Search posts</span><input autoFocus value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search items or places" /><button type="button" onClick={() => { setQuery(''); setSearchOpen(false) }} aria-label="Close search"><X size={18} /></button></label> : null}

    <section className="lf-hero">
      <div className="lf-hero__rings" aria-hidden="true" />
      <h1>Lost something? Someone<br />may have found it.</h1>
      <p>Post what you lost or found and help it get home.</p>
      <div className="lf-hero__actions">
        <button type="button" onClick={() => openReport('LOST')}><Search size={18} />I lost something</button>
        <button type="button" onClick={() => openReport('FOUND')}><MapPin size={18} />I found something</button>
      </div>
    </section>

    <nav className="lf-tabs" aria-label="Filter by item status">
      {(['all', 'lost', 'found'] as const).map((status) => <button type="button" key={status} className={statusFilter === status ? 'is-active' : ''} onClick={() => setStatusFilter(status)}>{status[0].toUpperCase() + status.slice(1)}<span>{counts[status]}</span></button>)}
    </nav>

    <div className="lf-category-row" aria-label="Filter by category">
      {categories.map((category) => <button type="button" key={category} className={categoryFilter === category ? 'is-active' : ''} onClick={() => setCategoryFilter(category)}>{category === 'ALL' ? 'All' : categoryLabels[category]}</button>)}
    </div>

    <div className="lf-results-label">{filteredPosts.length} {filteredPosts.length === 1 ? 'result' : 'results'}</div>
    <section className="lf-feed" aria-live="polite">
      {feedLoading ? <FeedSkeleton /> : feedError ? <div className="lf-api-error"><CircleHelp size={26} /><h2>Could not load posts</h2><p>{feedError}</p><button type="button" onClick={() => void loadPosts(query)}><RefreshCw size={16} />Try again</button></div> : filteredPosts.length ? filteredPosts.map((post) => <LostFoundCard key={post.id} post={post} currentStudentId={user.id} onOpen={() => void openItem(post)} onContact={() => openContact(post)} />) : <div className="lf-empty"><Search size={28} /><h2>No matching items</h2><p>Try another category or search term.</p><button type="button" onClick={() => { setCategoryFilter('ALL'); setStatusFilter('all'); setQuery('') }}>Clear filters</button></div>}
    </section>

    {modal === 'report' ? <Modal onClose={() => setModal(null)} labelledBy="lf-report-title">
      <form ref={reportForm} className="lf-form" onSubmit={(event) => void submitReport(event)}>
        <header><h2 id="lf-report-title">Report an item</h2><p>It takes under a minute.</p></header>
        <div className="lf-segmented"><button type="button" className={reportType === 'LOST' ? 'is-active is-lost' : ''} onClick={() => setReportType('LOST')}>I lost</button><button type="button" className={reportType === 'FOUND' ? 'is-active is-found' : ''} onClick={() => setReportType('FOUND')}>I found</button></div>
        <fieldset><legend>Category</legend><div className="lf-choice-row">{reportCategories.map((category) => <button type="button" key={category} className={reportCategory === category ? 'is-active' : ''} onClick={() => setReportCategory(category)}>{categoryLabels[category]}</button>)}</div></fieldset>
        <label><span>What is it?</span><input required name="title" maxLength={160} placeholder="e.g. Black leather wallet" /></label>
        <label><span>Details <em>(optional)</em></span><textarea name="details" maxLength={1200} className="lf-form__details" placeholder="Add color, identifying marks, or anything helpful" /></label>
        <label><span>Where?</span><input required name="place" maxLength={240} placeholder="e.g. Main Library, 3rd floor" /></label>
        <fieldset><legend>When</legend><div className="lf-choice-row">{(['Today', 'Yesterday', 'Earlier'] as ReportWhen[]).map((value) => <button type="button" key={value} className={reportWhen === value ? 'is-active is-dark' : ''} onClick={() => setReportWhen(value)}>{value}</button>)}</div></fieldset>
        <label><span>Posting as</span><input value={user.name} readOnly /></label>
        <label><span>Phone or email <em>(optional)</em></span><input name="contact" maxLength={160} placeholder={user.email} /></label>
        {actionError ? <p className="lf-form-error" role="alert">{actionError}</p> : null}
        <button type="submit" className={`lf-submit ${postSuccess ? 'is-success' : ''}`} disabled={formBusy || postSuccess}>{postSuccess ? <><Check size={22} strokeWidth={3} />Posted</> : formBusy ? 'Posting...' : 'Post item'}</button>
      </form>
    </Modal> : null}

    {modal === 'detail' && selectedPost ? <Modal onClose={() => setModal(null)} labelledBy="lf-detail-title">
      <div className="lf-detail">
        <header className="lf-detail__head"><ItemGlyph type={selectedPost.type} category={selectedPost.category} size="large" /><div><span>{categoryLabels[selectedPost.category]}</span><small className={`lf-status lf-status--${selectedPost.type.toLowerCase()}`}>{selectedPost.type === 'LOST' ? 'Lost' : 'Found'}</small></div></header>
        <h2 id="lf-detail-title">{selectedPost.title}</h2>
        {selectedPost.description ? <p className="lf-detail__description">{selectedPost.description}</p> : null}
        <dl className="lf-detail__meta"><div><dt>Owner</dt><dd>{selectedPost.ownerName}</dd></div><div><dt>Contact</dt><dd>{selectedPost.contact || 'Shared through messages'}</dd></div><div><dt>Place</dt><dd>{selectedPost.location}</dd></div><div><dt>Posted</dt><dd>{relativeTime(selectedPost.createdAt)}</dd></div></dl>
        {ownSelectedPost ? <button type="button" disabled={formBusy} className="lf-detail__secondary lf-detail__secondary--owner" onClick={() => void markReturned()}>{formBusy ? 'Updating...' : 'Mark as returned'}</button> : <button type="button" className={`lf-detail__primary lf-detail__primary--${selectedPost.type.toLowerCase()}`} onClick={() => openContact(selectedPost)}>{selectedPost.type === 'LOST' ? 'I found it' : 'Claim this'}</button>}
        {actionError ? <p className="lf-form-error" role="alert">{actionError}</p> : null}
        <section className="lf-comments"><h3>Comments <span>{selectedPost.commentCount}</span></h3>{detailLoading ? <p className="lf-inline-state">Loading comments...</p> : detailError ? <p className="lf-form-error" role="alert">{detailError}</p> : comments.length ? comments.map((entry) => <article key={entry.id}><span>{entry.authorName[0]}</span><div><strong>{entry.authorName} <small>{relativeTime(entry.createdAt)}</small></strong><p>{entry.content}</p></div></article>) : <p className="lf-inline-state">No comments yet.</p>}</section>
        <form className="lf-message-input" onSubmit={(event) => void submitComment(event)}><label><span className="sr-only">Write a comment</span><input value={comment} disabled={formBusy || detailLoading} maxLength={600} onChange={(event) => setComment(event.target.value)} placeholder="Write a comment" /></label><button type="submit" disabled={formBusy || detailLoading || !comment.trim()} aria-label="Send comment"><Send size={21} /></button></form>
      </div>
    </Modal> : null}

    {modal === 'contact' && selectedPost ? <Modal onClose={() => setModal(null)} labelledBy="lf-contact-title" compact>
      <form className="lf-form lf-contact-form" onSubmit={(event) => void sendContactMessage(event)}>
        <header><h2 id="lf-contact-title">{selectedPost.type === 'LOST' ? 'I found this item' : 'Claim this item'}</h2><p>{selectedPost.title}</p></header>
        <label><span>Message</span><textarea required maxLength={1200} value={message} onChange={(event) => setMessage(event.target.value)} placeholder={selectedPost.type === 'LOST' ? 'Where is it now? How can they get it back?' : 'Describe something only the owner would know.'} /></label>
        {actionError ? <p className="lf-form-error" role="alert">{actionError}</p> : null}
        <button type="submit" disabled={formBusy || !message.trim()} className={`lf-submit lf-submit--${selectedPost.type.toLowerCase()}`}>{formBusy ? 'Sending...' : 'Send message'}</button>
      </form>
    </Modal> : null}

    {modal === 'messages' ? <Modal onClose={() => setModal(null)} labelledBy="lf-messages-title">
      <div className="lf-inbox"><header><h2 id="lf-messages-title">Messages</h2><p>People who contacted you about your posts, and items you reached out about.</p></header>{inboxLoading ? <p className="lf-inline-state">Loading messages...</p> : inboxError ? <div className="lf-api-error lf-api-error--small"><p>{inboxError}</p><button type="button" onClick={() => void loadInbox()}><RefreshCw size={16} />Try again</button></div> : conversations.length ? <div className="lf-inbox__list">{conversations.map((entry) => <button type="button" key={entry.id} className={entry.unreadCount ? 'is-unread' : ''} onClick={() => void openThread(entry)}><ItemGlyph type={entry.postType} category={entry.postCategory} /><span><strong>{entry.postTitle}</strong><small>{entry.otherStudentName}: {entry.lastMessage || 'Start the conversation'}</small><em>{entry.onYourPost ? 'On your post' : 'Your message'}</em></span><time>{relativeTime(entry.lastMessageAt)}</time>{entry.unreadCount ? <b>{entry.unreadCount}</b> : null}</button>)}</div> : <p className="lf-inline-state">No messages yet.</p>}</div>
    </Modal> : null}

    {modal === 'thread' ? <Modal onClose={() => setModal(null)} labelledBy="lf-thread-title" compact>
      <div className="lf-thread"><button type="button" className="lf-thread__back" onClick={() => void loadInbox()}><ChevronLeft size={18} />Messages</button>{threadLoading ? <p className="lf-inline-state">Loading conversation...</p> : thread ? <><header><ItemGlyph type={thread.conversation.postType} category={thread.conversation.postCategory} /><div><span>{thread.conversation.onYourPost ? 'Your post' : 'Item you contacted about'}</span><h2 id="lf-thread-title">{thread.conversation.postTitle}</h2></div></header><div className="lf-thread__messages">{thread.messages.map((entry) => <div className={`lf-bubble ${entry.senderStudentId === user.id ? 'lf-bubble--outgoing' : 'lf-bubble--incoming'}`} key={entry.id}>{entry.senderStudentId === user.id ? null : <strong>{entry.senderName}</strong>}<p>{entry.content}</p><time>{relativeTime(entry.createdAt)}</time></div>)}</div><form className="lf-message-input" onSubmit={(event) => void submitReply(event)}><label><span className="sr-only">Write a reply</span><input value={reply} disabled={formBusy} maxLength={1200} onChange={(event) => setReply(event.target.value)} placeholder="Write a reply" /></label><button type="submit" disabled={formBusy || !reply.trim()} aria-label="Send reply"><Send size={21} /></button></form></> : <div className="lf-api-error lf-api-error--small"><p>{actionError || 'Could not load this conversation.'}</p><button type="button" onClick={() => setModal('messages')}>Back to messages</button></div>}</div>
    </Modal> : null}

    {toast ? <div className="lf-toast" role="status"><Check size={17} />{toast}</div> : null}
  </div>
}
