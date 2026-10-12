import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  BadgeCheck,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Flag,
  Heart,
  MapPin,
  MessageCircleMore,
  PackageCheck,
  ShieldCheck,
  Star,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { MarketplaceDialog, MarketplaceFlowPage, MarketplaceListingImage } from './MarketplaceFlowUi'
import { listingFromApi, marketplaceApi } from './marketplaceApi'
import { listings as sampleListings, money, type Listing } from './marketplaceData'
import './marketplace.css'

type DialogName = 'offer' | 'message' | 'report' | null

export default function ProductDetailsPage() {
  const { user } = useDemoUser()
  const { listingId } = useParams()
  const initialListing = sampleListings.find((item) => String(item.id) === listingId) ?? sampleListings[0]
  const [listing, setListing] = useState<Listing>(initialListing)
  const [photo, setPhoto] = useState(0)
  const [saved, setSaved] = useState(false)
  const [dialog, setDialog] = useState<DialogName>(null)
  const [offer, setOffer] = useState(String(Math.round(listing.price * .88)))
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [actionError, setActionError] = useState('')
  const [message, setMessage] = useState('Hi, is this still available?')
  const related = useMemo(() => sampleListings.filter((item) => item.id !== listing.id && (item.category === listing.category || item.location === listing.location)).slice(0, 3), [listing])

  useEffect(() => {
    const fallback = sampleListings.find((item) => String(item.id) === listingId)
    if (fallback) {
      setListing(fallback)
      return
    }
    if (!listingId) return
    let active = true
    marketplaceApi.getListing(listingId)
      .then((item) => { if (active) setListing(listingFromApi(item)) })
      .catch(() => undefined)
    return () => { active = false }
  }, [listingId])

  useEffect(() => {
    setOffer(String(Math.round(listing.price * .88)))
  }, [listing.price])

  const changePhoto = (direction: number) => setPhoto((current) => (current + direction + 3) % 3)

  const toggleSaved = async () => {
    const next = !saved
    setSaved(next)
    setActionError('')
    if (typeof listing.id !== 'string') return
    try {
      if (next) await marketplaceApi.saveListing(listing.id, user.id)
      else await marketplaceApi.removeSavedListing(listing.id, user.id)
    } catch (cause) {
      setSaved(!next)
      setActionError(cause instanceof Error ? cause.message : 'Could not update saved items.')
    }
  }

  const submitOffer = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setActionError('')
    const form = new FormData(event.currentTarget)
    try {
      if (typeof listing.id === 'string') {
        await marketplaceApi.makeOffer(listing.id, {
          buyerStudentId: user.id,
          amount: Number(offer),
          message: String(form.get('message') ?? ''),
        })
      }
      setSubmitted(true)
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not send the offer.')
    } finally {
      setSubmitting(false)
    }
  }

  const submitReport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setActionError('')
    const form = new FormData(event.currentTarget)
    try {
      if (typeof listing.id === 'string') {
        await marketplaceApi.report({
          reporterStudentId: user.id,
          targetType: 'LISTING',
          targetId: listing.id,
          reason: String(form.get('reason')),
          details: String(form.get('details') ?? ''),
        })
      }
      setDialog(null)
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not submit the report.')
    } finally {
      setSubmitting(false)
    }
  }

  const submitMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setActionError('')
    try {
      if (typeof listing.id === 'string') {
        await marketplaceApi.sendMessage({
          senderStudentId: user.id,
          recipientStudentId: listing.sellerId,
          listingId: listing.id,
          body: message,
        })
      }
      setDialog(null)
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Could not send the message.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-detail-page">
        <nav className="marketplace-breadcrumbs" aria-label="Breadcrumb">
          <Link to="/marketplace">Marketplace</Link><span>/</span><Link to={`/marketplace?category=${encodeURIComponent(listing.category)}`}>{listing.category}</Link><span>/</span><strong>{listing.title}</strong>
        </nav>

        <div className="marketplace-detail-layout">
          <section className="marketplace-gallery" aria-label={`${listing.title} photos`}>
            <div className={`marketplace-gallery-main marketplace-gallery-main--${photo}`}>
              <MarketplaceListingImage listing={listing} />
              <button type="button" onClick={() => changePhoto(-1)} aria-label="Previous photo"><ChevronLeft size={20} /></button>
              <button type="button" onClick={() => changePhoto(1)} aria-label="Next photo"><ChevronRight size={20} /></button>
            </div>
            <div className="marketplace-gallery-thumbnails">
              {[0, 1, 2].map((index) => <button key={index} type="button" className={photo === index ? 'is-active' : ''} onClick={() => setPhoto(index)} aria-label={`Show product photo ${index + 1}`}><span className={`marketplace-gallery-crop marketplace-gallery-crop--${index}`}><MarketplaceListingImage listing={listing} /></span></button>)}
            </div>
          </section>

          <aside className="marketplace-purchase-panel">
            <div className="marketplace-detail-labels"><span className={listing.soldOut ? 'is-sold-out' : ''}>{listing.soldOut ? 'Sold out' : listing.condition}</span><span>{listing.category}</span></div>
            <h1>{listing.title}</h1>
            <div className="marketplace-detail-price"><strong>{money(listing.price)}</strong>{listing.soldOut ? <span>Previously listed price</span> : listing.negotiable ? <span>Negotiable</span> : <span>Fixed price</span>}</div>
            <p>{listing.description}</p>

            <dl className="marketplace-detail-facts">
              <div><dt><PackageCheck size={17} />Availability</dt><dd>{listing.soldOut ? 'Sold out' : listing.quantity > 1 ? `${listing.quantity} available` : '1 available'}</dd></div>
              <div><dt><MapPin size={17} />Pickup zone</dt><dd>{listing.location}</dd></div>
              <div><dt><Clock3 size={17} />Posted</dt><dd>{listing.posted}</dd></div>
            </dl>

            {listing.soldOut ? <div className="marketplace-detail-sold-note"><PackageCheck size={18} /><span><strong>This item has been sold</strong>Browse current listings to find a similar campus deal.</span><Link to="/marketplace">Browse available items</Link></div> : <div className="marketplace-detail-actions">
              <button type="button" className="marketplace-primary-action" onClick={() => { setSubmitted(false); setActionError(''); setDialog('offer') }}>Make an offer</button>
              <button type="button" className="marketplace-secondary-action" onClick={() => { setActionError(''); setDialog('message') }}><MessageCircleMore size={17} />Message seller</button>
              <button type="button" className={`marketplace-detail-save ${saved ? 'is-saved' : ''}`} onClick={toggleSaved} aria-pressed={saved}><Heart size={18} fill={saved ? 'currentColor' : 'none'} />{saved ? 'Saved' : 'Save item'}</button>
            </div>}
            {actionError && dialog === null ? <p className="marketplace-form-error" role="alert">{actionError}</p> : null}

            <div className="marketplace-safety-note"><ShieldCheck size={19} /><span><strong>Meet safely on campus</strong>Use a public pickup zone and check the item before paying.</span></div>
            <button type="button" className="marketplace-report-link" onClick={() => { setActionError(''); setDialog('report') }}><Flag size={14} />Report listing</button>
          </aside>
        </div>

        <div className="marketplace-detail-lower">
          <section className="marketplace-seller-panel">
            <div className="marketplace-avatar">{listing.seller.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div>
            <div>
              <span>Seller</span>
              <h2>{listing.seller}<BadgeCheck size={18} aria-label="Verified student" /></h2>
              <p>Verified student, joined Campus Day in 2025</p>
              <div><span><Star size={15} fill="currentColor" />4.8 rating</span><span>17 completed deals</span></div>
            </div>
            <Link to={`/marketplace/sellers/${listing.sellerId}`}>View seller profile</Link>
          </section>

          <section className="marketplace-pickup-panel">
            <div>
              <span><MapPin size={20} /></span>
              <h2>Pickup at {listing.location}</h2>
              <p>The seller shares the exact meeting point after an offer is accepted.</p>
            </div>
            <div className="marketplace-pickup-preview">
              <MapPin size={27} />
              <span>Exact map location stays private until reservation</span>
            </div>
          </section>
        </div>

        {related.length ? <section className="marketplace-related">
          <header><h2>More from campus</h2><Link to="/marketplace">View all listings</Link></header>
          <div>{related.map((item) => <Link key={item.id} to={`/marketplace/listings/${item.id}`}><span className="marketplace-related-image"><MarketplaceListingImage listing={item} /></span><span><small>{item.soldOut ? 'Sold out' : item.category}</small><strong>{item.title}</strong><b>{money(item.price)}</b></span><ChevronRight size={18} /></Link>)}</div>
        </section> : null}
      </main>

      {dialog === 'offer' ? <MarketplaceDialog title={submitted ? 'Offer sent' : 'Make an offer'} description={submitted ? 'The seller will be notified.' : `Listed price ${money(listing.price)}`} onClose={() => setDialog(null)}>
        {submitted ? <div className="marketplace-dialog-success"><BadgeCheck size={38} /><h3>Your offer of {money(Number(offer))} was sent</h3><p>You can track the response from My Marketplace.</p><button type="button" onClick={() => setDialog(null)}>Done</button></div> : <form className="marketplace-dialog-form" onSubmit={submitOffer}>
          <label><span>Your offer</span><div className="marketplace-money-input"><b>৳</b><input type="number" min="1" max={listing.price} value={offer} onChange={(event) => setOffer(event.target.value)} required /></div><small>Choose a fair amount. The seller can accept, counter or reject it.</small></label>
          <label><span>Message (optional)</span><textarea name="message" rows={3} placeholder="Add a short note for the seller" /></label>
          {actionError ? <p className="marketplace-form-error" role="alert">{actionError}</p> : null}
          <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Sending...' : 'Send offer'}</button>
        </form>}
      </MarketplaceDialog> : null}

      {dialog === 'message' ? <MarketplaceDialog title={`Message ${listing.seller}`} description={`About ${listing.title}`} onClose={() => setDialog(null)}>
        <form className="marketplace-dialog-form" onSubmit={submitMessage}>
          <div className="marketplace-quick-messages">{['Is this still available?', 'When can I pick it up?', 'Can I see another photo?'].map((text) => <button key={text} type="button" onClick={() => setMessage(text)}>{text}</button>)}</div>
          <label><span>Your message</span><textarea name="message" rows={4} value={message} onChange={(event) => setMessage(event.target.value)} required /></label>
          {actionError ? <p className="marketplace-form-error" role="alert">{actionError}</p> : null}
          <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Sending...' : 'Start conversation'}</button>
        </form>
      </MarketplaceDialog> : null}

      {dialog === 'report' ? <MarketplaceDialog title="Report this listing" description="Reports are reviewed by Campus Day moderators." onClose={() => setDialog(null)}>
        <form className="marketplace-dialog-form" onSubmit={submitReport}>
          <fieldset><legend>Reason</legend>{[['Fake product', 'FAKE_PRODUCT'], ['Misleading information', 'MISLEADING_INFORMATION'], ['Prohibited item', 'PROHIBITED_ITEM'], ['Scam or spam', 'SCAM'], ['Other', 'OTHER']].map(([label, value]) => <label className="marketplace-radio" key={value}><input type="radio" name="reason" value={value} required />{label}</label>)}</fieldset>
          <label><span>Details (optional)</span><textarea name="details" rows={3} placeholder="Tell us what happened" /></label>
          {actionError ? <p className="marketplace-form-error" role="alert">{actionError}</p> : null}
          <button type="submit" className="marketplace-danger-action" disabled={submitting}>{submitting ? 'Submitting...' : 'Submit report'}</button>
        </form>
      </MarketplaceDialog> : null}
    </MarketplaceFlowPage>
  )
}
