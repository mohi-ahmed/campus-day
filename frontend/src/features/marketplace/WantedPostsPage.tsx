import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ArrowRight, BadgeCheck, MapPin, MessageCircleMore, PackageSearch, Plus, Search, Tag } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { MarketplaceDialog, MarketplaceFlowPage, MarketplacePageIntro } from './MarketplaceFlowUi'
import { listingFromApi, marketplaceApi, wantedPostFromApi } from './marketplaceApi'
import { listings as sampleListings, wantedPosts as sampleWantedPosts, type Listing, type WantedPost } from './marketplaceData'
import './marketplace.css'

export default function WantedPostsPage() {
  const { user } = useDemoUser()
  const [posts, setPosts] = useState<WantedPost[]>(sampleWantedPosts)
  const [sellerListings, setSellerListings] = useState<Listing[]>([])
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('All requests')
  const [selected, setSelected] = useState<WantedPost | null>(null)
  const [responded, setResponded] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [responseError, setResponseError] = useState('')

  useEffect(() => {
    let active = true
    Promise.allSettled([
      marketplaceApi.listWantedPosts(),
      marketplaceApi.listSellerListings(user.id),
    ]).then(([wantedResult, listingResult]) => {
      if (!active) return
      if (wantedResult.status === 'fulfilled' && wantedResult.value.items.length) {
        setPosts(wantedResult.value.items.map(wantedPostFromApi))
      }
      if (listingResult.status === 'fulfilled') {
        setSellerListings(listingResult.value.items.map(listingFromApi))
      }
    })
    return () => { active = false }
  }, [user.id])

  const visiblePosts = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return posts.filter((post) => {
      const matchesQuery = !normalized || `${post.title} ${post.location} ${post.author}`.toLowerCase().includes(normalized)
      const matchesFilter = filter === 'All requests' || (filter === 'Recent' && (post.posted === 'Recently' || post.posted.includes('hour'))) || (filter === 'Few responses' && post.responses <= 2)
      return matchesQuery && matchesFilter
    })
  }, [filter, posts, query])

  const responseListings = typeof selected?.id === 'string' ? sellerListings : sampleListings.slice(0, 3)

  const submitResponse = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selected) return
    setSubmitting(true)
    setResponseError('')
    const form = new FormData(event.currentTarget)
    try {
      if (typeof selected.id === 'string') {
        const listingId = String(form.get('listingId') ?? '')
        await marketplaceApi.respondToWantedPost(selected.id, {
          sellerStudentId: user.id,
          listingId: listingId || undefined,
          price: Number(form.get('price')),
          message: String(form.get('message') ?? ''),
        })
      }
      setResponded(true)
    } catch (cause) {
      setResponseError(cause instanceof Error ? cause.message : 'Could not send your response.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-wanted-page">
        <MarketplacePageIntro title="Wanted posts" description="See what students need and respond when you have a matching item." action={<Link className="marketplace-primary-action" to="/marketplace/wanted/create"><Plus size={17} />Create wanted post</Link>} />

        <section className="marketplace-wanted-toolbar">
          <label><Search size={17} /><span className="sr-only">Search wanted posts</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search wanted posts" /></label>
          <div>{['All requests', 'Recent', 'Few responses'].map((item) => <button key={item} type="button" className={filter === item ? 'is-active' : ''} onClick={() => setFilter(item)}>{item}</button>)}</div>
        </section>

        <div className="marketplace-wanted-page-count">{visiblePosts.length} active requests</div>
        {visiblePosts.length ? <section className="marketplace-wanted-grid">
          {visiblePosts.map((post) => <article key={post.id}>
            <header><div className="marketplace-avatar marketplace-avatar--small">{post.author.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div><div><h2>{post.title}</h2><span>{post.author}<BadgeCheck size={14} /></span></div><time>{post.posted}</time></header>
            <p>{post.description}</p>
            <dl><div><dt><Tag size={15} />Budget</dt><dd>{post.budget}</dd></div><div><dt><PackageSearch size={15} />Condition</dt><dd>{post.condition}</dd></div><div><dt><MapPin size={15} />Pickup</dt><dd>{post.location}</dd></div></dl>
            <footer><span><MessageCircleMore size={15} />{post.responses} responses</span><button type="button" onClick={() => { setSelected(post); setResponded(false); setResponseError('') }}>I have this item<ArrowRight size={16} /></button></footer>
          </article>)}
        </section> : <div className="marketplace-empty-state"><PackageSearch size={34} /><h3>No wanted posts found</h3><p>Try another search or publish a new wanted post.</p><button type="button" onClick={() => { setQuery(''); setFilter('All requests') }}>Clear filters</button></div>}
      </main>

      {selected ? <MarketplaceDialog title={responded ? 'Response sent' : `Respond to ${selected.title}`} description={responded ? `${selected.author} will be notified.` : selected.budget} onClose={() => setSelected(null)}>
        {responded ? <div className="marketplace-dialog-success"><BadgeCheck size={38} /><h3>Your item was shared</h3><p>The buyer can review your response and start a conversation.</p><button type="button" onClick={() => setSelected(null)}>Done</button></div> : <form className="marketplace-dialog-form" onSubmit={submitResponse}>
          <label><span>Select your listing (optional)</span><select name="listingId" defaultValue=""><option value="">Respond without linking a listing</option>{responseListings.map((listing) => <option key={listing.id} value={listing.id}>{listing.title}</option>)}</select></label>
          <label><span>Your asking price</span><div className="marketplace-money-input"><b>৳</b><input name="price" type="number" min="1" placeholder="1200" required /></div></label>
          <label><span>Message</span><textarea name="message" rows={3} placeholder="Share condition, availability or pickup details" required /></label>
          {responseError ? <p className="marketplace-form-error" role="alert">{responseError}</p> : null}
          <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Sending...' : 'Send response'}</button>
        </form>}
      </MarketplaceDialog> : null}
    </MarketplaceFlowPage>
  )
}
