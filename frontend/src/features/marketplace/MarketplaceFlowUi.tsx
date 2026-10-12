import { useEffect, useRef, type ReactNode } from 'react'
import { ArrowLeft, Heart, Plus, Search, ShieldCheck, Store, X } from 'lucide-react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { listingImageSource, type Listing } from './marketplaceData'

export function MarketplaceFlowHeader() {
  const navigate = useNavigate()

  const submitSearch = (formData: FormData) => {
    const query = String(formData.get('marketplaceSearch') ?? '').trim()
    navigate(query ? `/marketplace?search=${encodeURIComponent(query)}#marketplace-listings` : '/marketplace')
  }

  return (
    <header className="marketplace-flow-nav">
      <Link className="marketplace-brand" to="/marketplace" aria-label="Campus Day Marketplace home">
        <span><Store size={20} /></span>
        <span><strong>Campus Day</strong><small>Marketplace</small></span>
      </Link>
      <form action={submitSearch} className="marketplace-flow-search">
        <Search size={17} />
        <label className="sr-only" htmlFor="marketplace-flow-search">Search marketplace</label>
        <input id="marketplace-flow-search" name="marketplaceSearch" placeholder="Search marketplace" />
      </form>
      <nav aria-label="Marketplace pages">
        <NavLink to="/marketplace" end>Explore</NavLink>
        <NavLink to="/marketplace/wanted">Wanted posts</NavLink>
        <NavLink to="/marketplace/me"><Heart size={16} />My marketplace</NavLink>
        <Link className="marketplace-sell-button" to="/marketplace/sell"><Plus size={17} />Sell an item</Link>
      </nav>
    </header>
  )
}

export function MarketplaceFlowPage({ children }: { children: ReactNode }) {
  return <div className="marketplace-page marketplace-flow-page"><MarketplaceFlowHeader />{children}<MarketplaceFlowFooter /></div>
}

export function MarketplaceFlowFooter() {
  return (
    <footer className="marketplace-footer marketplace-flow-footer">
      <p><ShieldCheck size={16} />Trade with verified students and meet in public campus spaces.</p>
      <div><Link to="/marketplace/wanted">Wanted posts</Link><Link to="/marketplace">Explore listings</Link><Link to="/marketplace/safety">Safety guide</Link></div>
    </footer>
  )
}

export function MarketplacePageIntro({ backTo = '/marketplace', backLabel = 'Back to marketplace', title, description, action }: { backTo?: string; backLabel?: string; title: string; description: string; action?: ReactNode }) {
  return (
    <div className="marketplace-page-intro">
      <Link to={backTo}><ArrowLeft size={16} />{backLabel}</Link>
      <div>
        <span><h1>{title}</h1><p>{description}</p></span>
        {action}
      </div>
    </div>
  )
}

export function MarketplaceListingImage({ listing, className = '' }: { listing: Listing; className?: string }) {
  if (listing.image === 'sprite') {
    return <img className={`marketplace-product-sprite ${listing.sprite ?? ''} ${className}`} src="/marketplace/product-grid.png" alt={listing.title} />
  }

  return <img className={`marketplace-product-photo ${className}`} src={listingImageSource(listing)} alt={listing.title} />
}

export function MarketplaceDialog({ title, description, onClose, children }: { title: string; description?: string; onClose: () => void; children: ReactNode }) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    closeButtonRef.current?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [])

  return (
    <div className="marketplace-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="marketplace-modal" role="dialog" aria-modal="true" aria-labelledby="marketplace-dialog-title">
        <header>
          <div><h2 id="marketplace-dialog-title">{title}</h2>{description ? <p>{description}</p> : null}</div>
          <button ref={closeButtonRef} type="button" onClick={onClose} aria-label="Close dialog"><X size={18} /></button>
        </header>
        {children}
      </section>
    </div>
  )
}
