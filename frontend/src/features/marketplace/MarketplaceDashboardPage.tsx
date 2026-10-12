import { useEffect, useState } from 'react'
import { ArrowRight, BadgeCheck, Heart, Inbox, PackageCheck, Plus, Tag } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { MarketplaceFlowPage, MarketplaceListingImage, MarketplacePageIntro } from './MarketplaceFlowUi'
import { listingFromApi, marketplaceApi, wantedPostFromApi } from './marketplaceApi'
import { listings as sampleListings, money, wantedPosts as sampleWantedPosts, type Listing, type WantedPost } from './marketplaceData'
import './marketplace.css'

const dashboardTabs = ['My listings', 'Offers', 'Saved', 'Wanted posts'] as const
type DashboardTab = (typeof dashboardTabs)[number]
type DashboardOffer = { id: string | number; listingId: string | number; listingTitle: string; buyer: string; amount: number }

export default function MarketplaceDashboardPage() {
  const { user } = useDemoUser()
  const [tab, setTab] = useState<DashboardTab>('My listings')
  const [myListings, setMyListings] = useState<Listing[]>(sampleListings.slice(0, 3))
  const [savedListings, setSavedListings] = useState<Listing[]>(sampleListings.filter((listing) => [3, 4, 7].includes(Number(listing.id))))
  const [wantedPosts, setWantedPosts] = useState<WantedPost[]>(sampleWantedPosts.slice(0, 2))
  const [offers, setOffers] = useState<DashboardOffer[]>([])
  const [offerResponses, setOfferResponses] = useState<Record<string, string>>({})
  const [actionError, setActionError] = useState('')

  useEffect(() => {
    let active = true
    Promise.allSettled([
      marketplaceApi.listSellerListings(user.id),
      marketplaceApi.listSavedListings(user.id),
      marketplaceApi.listReceivedOffers(user.id),
      marketplaceApi.listWantedPosts(),
    ]).then(([listingResult, favoriteResult, offerResult, wantedResult]) => {
      if (!active) return
      if (listingResult.status === 'fulfilled' && listingResult.value.items.length) setMyListings(listingResult.value.items.map(listingFromApi))
      if (favoriteResult.status === 'fulfilled' && favoriteResult.value.items.length) setSavedListings(favoriteResult.value.items.map(listingFromApi))
      if (offerResult.status === 'fulfilled' && offerResult.value.length) {
        setOffers(offerResult.value.map((offer) => ({ id: offer.id, listingId: offer.listingId, listingTitle: offer.listingTitle, buyer: offer.buyerName, amount: Number(offer.offeredAmount) })))
      }
      if (wantedResult.status === 'fulfilled') {
        const ownPosts = wantedResult.value.items.filter((post) => post.authorStudentId === user.id)
        if (ownPosts.length) setWantedPosts(ownPosts.map(wantedPostFromApi))
      }
    })
    return () => { active = false }
  }, [user.id])

  const respondToOffer = async (offer: DashboardOffer, action: 'accept' | 'reject' | 'counter') => {
    const key = String(offer.id)
    setActionError('')
    try {
      if (typeof offer.id === 'string') {
        await marketplaceApi.actOnOffer(offer.id, action, user.id, action === 'counter' ? Math.ceil(offer.amount * 1.1) : undefined)
      }
      setOfferResponses((current) => ({ ...current, [key]: action === 'accept' ? 'Offer accepted' : action === 'counter' ? 'Counter sent' : 'Offer rejected' }))
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not update the offer.')
    }
  }

  const removeSaved = async (listing: Listing) => {
    setActionError('')
    setSavedListings((current) => current.filter((item) => item.id !== listing.id))
    if (typeof listing.id !== 'string') return
    try {
      await marketplaceApi.removeSavedListing(listing.id, user.id)
    } catch (cause) {
      setSavedListings((current) => current.some((item) => item.id === listing.id) ? current : [...current, listing])
      setActionError(cause instanceof Error ? cause.message : 'Could not remove the saved item.')
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-dashboard-page">
        <MarketplacePageIntro title="My Marketplace" description="Manage listings, offers, saved items and wanted posts." action={<Link className="marketplace-primary-action" to="/marketplace/sell"><Plus size={17} />Sell an item</Link>} />
        <section className="marketplace-dashboard-summary">
          <div><span><PackageCheck size={19} /></span><strong>{myListings.filter((item) => !item.soldOut).length}</strong><small>Active listings</small></div>
          <div><span><Inbox size={19} /></span><strong>{offers.length}</strong><small>Open offers</small></div>
          <div><span><Heart size={19} /></span><strong>{savedListings.length}</strong><small>Saved items</small></div>
          <div><span><BadgeCheck size={19} /></span><strong>12</strong><small>Completed deals</small></div>
        </section>

        <div className="marketplace-dashboard-tabs" role="tablist" aria-label="Marketplace dashboard views">{dashboardTabs.map((item) => <button key={item} type="button" role="tab" aria-selected={tab === item} className={tab === item ? 'is-active' : ''} onClick={() => setTab(item)}>{item}</button>)}</div>
        {actionError ? <p className="marketplace-form-error" role="alert">{actionError}</p> : null}

        {tab === 'My listings' ? <section className="marketplace-dashboard-list">
          <header><div><h2>Your listings</h2><p>Keep availability accurate so buyers know what they can reserve.</p></div><Link to="/marketplace/sell"><Plus size={16} />New listing</Link></header>
          {myListings.map((item) => <article key={item.id}><span className="marketplace-dashboard-thumb"><MarketplaceListingImage listing={item} /></span><div><small>{item.category}</small><h3>{item.title}</h3><span>{money(item.price)}</span></div><div className="marketplace-dashboard-status"><b className={item.soldOut ? 'is-reserved' : ''}>{item.soldOut ? 'Sold out' : 'Active'}</b><span>{item.soldOut ? 'Listing closed' : 'Manage listing'}</span></div><Link to={`/marketplace/listings/${item.id}`}>View<ArrowRight size={15} /></Link></article>)}
        </section> : null}

        {tab === 'Offers' ? <section className="marketplace-offer-stack">
          <header><h2>Offers needing your response</h2><p>Accept, counter or reject each offer.</p></header>
          {offers.length ? offers.map((offer) => { const itemKey = String(offer.id); const item = myListings.find((listing) => String(listing.id) === String(offer.listingId)) ?? sampleListings[0]; return <article key={offer.id}><span className="marketplace-dashboard-thumb"><MarketplaceListingImage listing={item} /></span><div><small>{offer.buyer} offered</small><h3>{money(offer.amount)}</h3><p>For {offer.listingTitle}</p></div>{offerResponses[itemKey] ? <span className="marketplace-offer-response">{offerResponses[itemKey]}</span> : <div><button type="button" onClick={() => respondToOffer(offer, 'reject')}>Reject</button><button type="button" onClick={() => respondToOffer(offer, 'counter')}>Counter</button><button type="button" className="is-accept" onClick={() => respondToOffer(offer, 'accept')}>Accept</button></div>}</article> }) : <div className="marketplace-empty-state"><Inbox size={30} /><h3>No open offers</h3><p>New buyer offers will appear here.</p></div>}
        </section> : null}

        {tab === 'Saved' ? <section className="marketplace-saved-grid">
          <header><h2>Saved items</h2><p>Items you want to revisit.</p></header>
          <div>{savedListings.map((item) => <article key={item.id}><Link to={`/marketplace/listings/${item.id}`}><span className="marketplace-dashboard-thumb"><MarketplaceListingImage listing={item} /></span><span><small>{item.soldOut ? 'Sold out' : item.condition}</small><strong>{item.title}</strong><b>{money(item.price)}</b></span></Link><button type="button" aria-label={`Remove ${item.title} from saved items`} onClick={() => removeSaved(item)}><Heart size={17} fill="currentColor" /></button></article>)}</div>
        </section> : null}

        {tab === 'Wanted posts' ? <section className="marketplace-dashboard-list">
          <header><div><h2>Your wanted posts</h2><p>Review responses from student sellers.</p></div><Link to="/marketplace/wanted/create"><Plus size={16} />New request</Link></header>
          {wantedPosts.map((post) => <article key={post.id}><span className="marketplace-dashboard-icon"><Tag size={20} /></span><div><small>{post.condition}</small><h3>{post.title}</h3><span>{post.budget}</span></div><div className="marketplace-dashboard-status"><b>{post.responses} responses</b><span>{post.posted}</span></div><Link to="/marketplace/wanted">Review<ArrowRight size={15} /></Link></article>)}
        </section> : null}
      </main>
    </MarketplaceFlowPage>
  )
}
