import { useEffect, useMemo, useState } from 'react'
import {
  ArrowRight,
  Bell,
  ChevronLeft,
  ChevronRight,
  Heart,
  LampDesk,
  MapPin,
  MessageCircleMore,
  PackageSearch,
  Plus,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Store,
  Tag,
  Verified,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { listingFromApi, marketplaceApi, wantedPostFromApi } from './marketplaceApi'
import { ads, categories, listingImageSource, listings as sampleListings, money, wantedPosts as sampleWantedPosts, type Listing, type WantedPost } from './marketplaceData'
import './marketplace.css'

const tabs = ['All', 'Recent', 'Popular', 'Nearby'] as const
type ListingTab = (typeof tabs)[number]

function ListingImage({ listing }: { listing: Listing }) {
  if (listing.image === 'sprite') {
    return <img className={`marketplace-product-sprite ${listing.sprite ?? ''}`} src="/marketplace/product-grid.png" alt={listing.title} />
  }

  return <img className="marketplace-product-photo" src={listingImageSource(listing)} alt={listing.title} />
}

function ListingCard({ listing, saved, onSave }: { listing: Listing; saved: boolean; onSave: () => void }) {
  return (
    <article className={`marketplace-listing-card ${listing.soldOut ? 'is-sold-out' : ''}`}>
      <div className="marketplace-listing-media">
        <Link className="marketplace-listing-image-link" to={`/marketplace/listings/${listing.id}`} aria-label={`View ${listing.title}`}><ListingImage listing={listing} /></Link>
        <span className={`marketplace-condition ${listing.soldOut ? 'marketplace-condition--sold-out' : `marketplace-condition--${listing.condition.toLowerCase().replace(' ', '-')}`}`}>{listing.soldOut ? 'Sold out' : listing.condition}</span>
        {listing.soldOut ? null : <button type="button" className={`marketplace-save ${saved ? 'is-saved' : ''}`} aria-label={saved ? `Remove ${listing.title} from saved items` : `Save ${listing.title}`} aria-pressed={saved} onClick={onSave}>
          <Heart size={18} fill={saved ? 'currentColor' : 'none'} />
        </button>}
      </div>
      <div className="marketplace-listing-body">
        <span className="marketplace-listing-category">{listing.category}</span>
        <h3><Link to={`/marketplace/listings/${listing.id}`}>{listing.title}</Link></h3>
        <div className="marketplace-listing-price-row">
          <strong>{money(listing.price)}</strong>
          {listing.soldOut ? <span className="marketplace-sold-price-label">Sold</span> : listing.negotiable ? <span>Negotiable</span> : null}
        </div>
        <Link className="marketplace-seller" to={`/marketplace/sellers/${listing.sellerId}`}>
          <span>{listing.seller}</span>
          <Verified size={14} aria-label="Verified student" />
        </Link>
        <footer>
          <span><MapPin size={14} />{listing.location}</span>
          <time>{listing.posted}</time>
        </footer>
      </div>
    </article>
  )
}

export default function MarketplacePage() {
  const { user } = useDemoUser()
  const [searchParams] = useSearchParams()
  const [liveListings, setLiveListings] = useState<Listing[]>([])
  const [wantedPosts, setWantedPosts] = useState<WantedPost[]>(sampleWantedPosts)
  const [activeAd, setActiveAd] = useState(0)
  const [adPaused, setAdPaused] = useState(false)
  const [reduceMotion, setReduceMotion] = useState(false)
  const [query, setQuery] = useState(() => searchParams.get('search') ?? '')
  const [activeTab, setActiveTab] = useState<ListingTab>('All')
  const [activeCategory, setActiveCategory] = useState(() => searchParams.get('category') ?? 'All')
  const [sort, setSort] = useState('newest')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [condition, setCondition] = useState('All conditions')
  const [saved, setSaved] = useState<Set<Listing['id']>>(() => new Set([3]))

  useEffect(() => {
    let active = true
    marketplaceApi.listListings().then((page) => {
      if (active) setLiveListings(page.items.map(listingFromApi))
    }).catch(() => undefined)
    marketplaceApi.listWantedPosts().then((page) => {
      if (active && page.items.length) setWantedPosts(page.items.map(wantedPostFromApi))
    }).catch(() => undefined)
    return () => { active = false }
  }, [])

  useEffect(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)')
    const syncPreference = () => setReduceMotion(media.matches)
    syncPreference()
    media.addEventListener('change', syncPreference)
    return () => media.removeEventListener('change', syncPreference)
  }, [])

  useEffect(() => {
    setQuery(searchParams.get('search') ?? '')
    const requestedCategory = searchParams.get('category')
    setActiveCategory(requestedCategory && categories.some((item) => item.label === requestedCategory) ? requestedCategory : 'All')
  }, [searchParams])

  useEffect(() => {
    if (adPaused || reduceMotion) return undefined
    const timer = window.setInterval(() => setActiveAd((current) => (current + 1) % ads.length), 5200)
    return () => window.clearInterval(timer)
  }, [adPaused, reduceMotion])

  const visibleListings = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    const filtered = [...liveListings, ...sampleListings].filter((listing) => {
      const matchesQuery = !normalized || `${listing.title} ${listing.category} ${listing.location}`.toLowerCase().includes(normalized)
      const matchesCategory = activeCategory === 'All' || listing.category === activeCategory
      const matchesCondition = condition === 'All conditions' || listing.condition === condition
      const matchesTab = activeTab === 'All'
        || (activeTab === 'Recent' && (listing.posted === 'Recently' || listing.posted.includes('hour') || listing.posted.includes('day')))
        || (activeTab === 'Popular' && listing.popularity >= 85)
        || (activeTab === 'Nearby' && ['Main Gate', 'Main Campus', 'CSE Building', 'Library'].includes(listing.location))
      return matchesQuery && matchesCategory && matchesCondition && matchesTab
    })

    return [...filtered].sort((a, b) => {
      if (sort === 'price-low') return a.price - b.price
      if (sort === 'price-high') return b.price - a.price
      if (sort === 'popular' || activeTab === 'Popular') return b.popularity - a.popularity
      return 0
    })
  }, [activeCategory, activeTab, condition, liveListings, query, sort])

  const availableListings = visibleListings.filter((listing) => !listing.soldOut)
  const soldListings = visibleListings.filter((listing) => listing.soldOut)

  const currentAd = ads[activeAd]

  const moveAd = (direction: number) => {
    setActiveAd((current) => (current + direction + ads.length) % ads.length)
  }

  const toggleSaved = (id: Listing['id']) => {
    const wasSaved = saved.has(id)
    setSaved((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
    if (typeof id !== 'string') return
    const request = wasSaved
      ? marketplaceApi.removeSavedListing(id, user.id)
      : marketplaceApi.saveListing(id, user.id)
    request.catch(() => setSaved((current) => {
      const next = new Set(current)
      if (wasSaved) next.add(id)
      else next.delete(id)
      return next
    }))
  }

  const chooseCategory = (label: string) => {
    setActiveCategory(label)
    document.getElementById('marketplace-listings')?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' })
  }

  const showAllListings = () => {
    setQuery('')
    setActiveTab('All')
    setActiveCategory('All')
    setCondition('All conditions')
    setSort('newest')
    document.getElementById('marketplace-listings')?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' })
  }

  const openCurrentPromotion = () => {
    const promotionCategory = activeAd === 0 ? 'Bicycle' : activeAd === 1 ? 'Art & Crafts' : 'All'
    chooseCategory(promotionCategory)
  }

  return (
    <div className="marketplace-page">
      <header className="marketplace-subnav">
        <Link className="marketplace-brand" to="/marketplace" aria-label="Campus Day Marketplace home">
          <span><Store size={20} /></span>
          <span><strong>Campus Day</strong><small>Marketplace</small></span>
        </Link>
        <label className="marketplace-header-search">
          <Search size={17} />
          <span className="sr-only">Search marketplace</span>
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search books, gadgets, room essentials..." />
        </label>
        <nav aria-label="Marketplace navigation">
          <a href="#marketplace-listings">Explore</a>
          <Link to="/marketplace/wanted">Wanted posts</Link>
          <Link to="/marketplace/me"><Heart size={17} /><span>Saved</span><b>{saved.size}</b></Link>
          <Link className="marketplace-bell" to="/marketplace/me" aria-label="Marketplace notifications"><Bell size={18} /></Link>
          <Link className="marketplace-sell-button" to="/marketplace/sell"><Plus size={18} />Sell an item</Link>
        </nav>
      </header>

      <section id="marketplace-top" className="marketplace-hero" aria-labelledby="marketplace-title">
        <div className="marketplace-hero-copy">
          <span className="marketplace-kicker"><Sparkles size={15} />Made for campus life</span>
          <h1 id="marketplace-title">Find it nearby.<br /><span>Keep it moving.</span></h1>
          <p>Buy, sell and discover useful things from verified students across your campus.</p>
          <div className="marketplace-hero-actions">
            <a className="marketplace-primary-action" href="#marketplace-listings">Explore marketplace<ArrowRight size={17} /></a>
            <Link className="marketplace-secondary-action" to="/marketplace/sell"><Plus size={17} />Sell an item</Link>
          </div>
        </div>

        <div
          className="marketplace-ad-shell"
          onMouseEnter={() => setAdPaused(true)}
          onMouseLeave={() => setAdPaused(false)}
          onFocusCapture={() => setAdPaused(true)}
          onBlurCapture={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setAdPaused(false)
          }}
          aria-roledescription="carousel"
          aria-label="Marketplace advertising"
        >
          <div className="marketplace-ad-visual" key={`image-${activeAd}`}>
            <img src={currentAd.image} alt="" />
            <span className="marketplace-ad-label">{currentAd.label}</span>
            <button type="button" className="marketplace-ad-arrow marketplace-ad-arrow--left" onClick={() => moveAd(-1)} aria-label="Previous advertisement"><ChevronLeft size={21} /></button>
            <button type="button" className="marketplace-ad-arrow marketplace-ad-arrow--right" onClick={() => moveAd(1)} aria-label="Next advertisement"><ChevronRight size={21} /></button>
          </div>
          <div className="marketplace-ad-copy" key={`copy-${activeAd}`} aria-live="polite">
            <div>
              <small>Advertising space</small>
              <h2>{currentAd.title}</h2>
              <p>{currentAd.copy}</p>
            </div>
            <button type="button" onClick={openCurrentPromotion}>{currentAd.action}<ArrowRight size={16} /></button>
          </div>
          <div className="marketplace-ad-progress" aria-label={`Advertisement ${activeAd + 1} of ${ads.length}`}>
            {ads.map((ad, index) => (
              <button key={ad.title} type="button" className={index === activeAd ? 'is-active' : ''} onClick={() => setActiveAd(index)} aria-label={`Show advertisement ${index + 1}`} aria-current={index === activeAd ? 'true' : undefined} />
            ))}
          </div>
        </div>
      </section>

      <div className="marketplace-trust-row" aria-label="Marketplace benefits">
        <span><ShieldCheck size={18} /><strong>Verified students</strong><small>Trade within the Campus Day community</small></span>
        <span><MapPin size={18} /><strong>Easy campus pickup</strong><small>Choose familiar public meeting points</small></span>
      </div>

      <section className="marketplace-categories" aria-labelledby="marketplace-category-title">
        <div className="marketplace-section-heading">
          <div>
            <h2 id="marketplace-category-title">Shop by category</h2>
            <p>Move your pointer over the categories to pause.</p>
          </div>
          <button type="button" onClick={() => setActiveCategory('All')}>View all categories<ArrowRight size={16} /></button>
        </div>
        <div className="marketplace-category-viewport">
          <div className="marketplace-category-track">
            {[...categories, ...categories].map(({ label, icon: Icon }, index) => (
              <button key={`${label}-${index}`} type="button" className={activeCategory === label ? 'is-active' : ''} onClick={() => chooseCategory(label)} aria-label={`Browse ${label}`} aria-hidden={index >= categories.length ? 'true' : undefined} tabIndex={index >= categories.length ? -1 : 0}>
                <span><Icon size={22} /></span>
                <strong>{label}</strong>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section id="marketplace-listings" className="marketplace-listings" aria-labelledby="marketplace-listings-title">
        <div className="marketplace-listing-heading">
          <div>
            <h2 id="marketplace-listings-title">Fresh finds, close to you</h2>
            <p>Student-listed essentials available for campus pickup.</p>
          </div>
          <button type="button" onClick={showAllListings}>View all<ArrowRight size={16} /></button>
        </div>

        <div className="marketplace-tabs" role="tablist" aria-label="Listing views">
          {tabs.map((tab) => <button key={tab} type="button" role="tab" aria-selected={activeTab === tab} className={activeTab === tab ? 'is-active' : ''} onClick={() => setActiveTab(tab)}>{tab}</button>)}
        </div>

        <div className="marketplace-toolbar">
          <label>
            <Search size={17} />
            <span className="sr-only">Search current listings</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search marketplace listings" />
          </label>
          <div>
            <button type="button" className={activeCategory !== 'All' ? 'is-active' : ''} onClick={() => setActiveCategory('All')}><MapPin size={16} />{activeCategory === 'All' ? 'All items' : activeCategory}</button>
            <button type="button" className={filtersOpen ? 'is-active' : ''} aria-expanded={filtersOpen} onClick={() => setFiltersOpen((value) => !value)}><SlidersHorizontal size={16} />Filters</button>
            <label className="marketplace-sort">
              <span className="sr-only">Sort listings</span>
              <select value={sort} onChange={(event) => setSort(event.target.value)}>
                <option value="newest">Newest first</option>
                <option value="popular">Most popular</option>
                <option value="price-low">Price low to high</option>
                <option value="price-high">Price high to low</option>
              </select>
            </label>
          </div>
        </div>

        {filtersOpen ? (
          <div className="marketplace-filter-panel">
            <span>Condition</span>
            {['All conditions', 'New', 'Like new', 'Good', 'Fair'].map((item) => <button key={item} type="button" className={condition === item ? 'is-active' : ''} onClick={() => setCondition(item)}>{item}</button>)}
            <button type="button" className="marketplace-filter-reset" onClick={() => { setCondition('All conditions'); setActiveCategory('All'); setQuery('') }}>Reset filters</button>
          </div>
        ) : null}

        <div className="marketplace-result-count">{availableListings.length} available · {soldListings.length} sold</div>

        {visibleListings.length ? (
          <>
            {availableListings.length ? <div className="marketplace-product-grid">
              {availableListings.map((listing) => <ListingCard key={listing.id} listing={listing} saved={saved.has(listing.id)} onSave={() => toggleSaved(listing.id)} />)}
            </div> : null}
            {soldListings.length ? <section className="marketplace-sold-section" aria-labelledby="marketplace-sold-title">
              <header><div><span>Past campus finds</span><h3 id="marketplace-sold-title">Recently sold</h3></div><p>These items have found a new home.</p></header>
              <div className="marketplace-product-grid">
                {soldListings.map((listing) => <ListingCard key={listing.id} listing={listing} saved={false} onSave={() => undefined} />)}
              </div>
            </section> : null}
          </>
        ) : (
          <div className="marketplace-empty-state">
            <PackageSearch size={34} />
            <h3>No listings found</h3>
            <p>Try a different search or clear the filters to see more campus finds.</p>
            <button type="button" onClick={() => { setQuery(''); setActiveCategory('All'); setCondition('All conditions') }}>Clear filters</button>
          </div>
        )}
      </section>

      <section id="wanted-posts" className="marketplace-wanted" aria-labelledby="wanted-title">
        <div className="marketplace-wanted-intro">
          <span><PackageSearch size={21} /></span>
          <h2 id="wanted-title">Students are looking for...</h2>
          <p>Have something another student needs? Respond to a wanted post and make a quick campus deal.</p>
          <div className="marketplace-wanted-actions">
            <Link to="/marketplace/wanted">Browse wanted posts<ArrowRight size={16} /></Link>
            <Link to="/marketplace/wanted/create"><Plus size={16} />Post a wanted item</Link>
          </div>
        </div>
        <div className="marketplace-wanted-list">
          {wantedPosts.slice(0, 3).map((post) => (
            <article key={post.id}>
              <div>
                <h3>{post.title}</h3>
                <span><Tag size={14} />{post.budget}</span>
              </div>
              <div>
                <span><MapPin size={14} />{post.location}</span>
                <span><MessageCircleMore size={14} />{post.responses} responses</span>
              </div>
              <Link to="/marketplace/wanted" aria-label={`Respond to wanted post for ${post.title}`}><ArrowRight size={17} /></Link>
            </article>
          ))}
        </div>
      </section>

      <section className="marketplace-selling-cta">
        <span><LampDesk size={25} /></span>
        <div>
          <h2>Something sitting around? Give it a new home.</h2>
          <p>List it in minutes and meet a verified buyer at a familiar campus pickup point.</p>
        </div>
        <Link to="/marketplace/sell"><Plus size={17} />Start selling<ArrowRight size={16} /></Link>
      </section>

      <footer className="marketplace-footer">
        <p><ShieldCheck size={16} />Trade with verified students and meet in public campus spaces.</p>
        <div><Link to="/marketplace/wanted">Wanted posts</Link><a href="#marketplace-listings">Explore listings</a><Link to="/marketplace/safety">Safety guide</Link></div>
      </footer>
    </div>
  )
}
