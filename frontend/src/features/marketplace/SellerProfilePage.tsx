import { useEffect, useState, type FormEvent } from 'react'
import { BadgeCheck, CalendarDays, CheckCircle2, MapPin, MessageCircleMore, ShieldCheck, Star } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { MarketplaceDialog, MarketplaceFlowPage, MarketplaceListingImage, MarketplacePageIntro } from './MarketplaceFlowUi'
import { listingFromApi, marketplaceApi } from './marketplaceApi'
import { listings as sampleListings, money, type Listing } from './marketplaceData'
import './marketplace.css'

export default function SellerProfilePage() {
  const { user } = useDemoUser()
  const { sellerId } = useParams()
  const [dialog, setDialog] = useState<'message' | 'report' | null>(null)
  const [sent, setSent] = useState(false)
  const fallbackListing = sampleListings.find((item) => item.sellerId === sellerId) ?? sampleListings[0]
  const [sellerListings, setSellerListings] = useState<Listing[]>(() => sampleListings.filter((item) => item.sellerId === fallbackListing.sellerId || item.category === fallbackListing.category).slice(0, 4))
  const [backendSeller, setBackendSeller] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const primaryListing = sellerListings[0] ?? fallbackListing

  useEffect(() => {
    if (!sellerId) return
    let active = true
    marketplaceApi.listSellerListings(sellerId).then((page) => {
      if (!active || !page.items.length) return
      setSellerListings(page.items.map(listingFromApi))
      setBackendSeller(true)
    }).catch(() => undefined)
    return () => { active = false }
  }, [sellerId])

  const submitReport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      if (backendSeller) {
        await marketplaceApi.report({
          reporterStudentId: user.id,
          targetType: 'SELLER',
          targetId: primaryListing.sellerId,
          reason: String(form.get('reason')),
          details: String(form.get('details') ?? ''),
        })
      }
      setSent(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not submit the report.')
    } finally {
      setSubmitting(false)
    }
  }

  const submitMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      if (backendSeller) {
        await marketplaceApi.sendMessage({
          senderStudentId: user.id,
          recipientStudentId: primaryListing.sellerId,
          body: String(form.get('message') ?? ''),
        })
      }
      setSent(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not send the message.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-seller-page">
        <MarketplacePageIntro title="Seller profile" description="Marketplace activity and verified student reputation." />
        <section className="marketplace-profile-hero">
          <div className="marketplace-profile-identity">
            <div className="marketplace-avatar marketplace-avatar--large">{primaryListing.seller.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div>
            <div><span>Verified student seller</span><h2>{primaryListing.seller}<BadgeCheck size={22} /></h2><p>Computer Science, 3rd year</p><small><CalendarDays size={14} />Joined Campus Day in February 2025</small></div>
          </div>
          <div className="marketplace-profile-actions"><button type="button" className="marketplace-primary-action" onClick={() => { setSent(false); setError(''); setDialog('message') }}><MessageCircleMore size={17} />Message seller</button><button type="button" className="marketplace-secondary-action" onClick={() => { setSent(false); setError(''); setDialog('report') }}>Report profile</button></div>
        </section>

        <section className="marketplace-profile-stats">
          <div><strong>4.8</strong><span><Star size={15} fill="currentColor" />Seller rating</span></div>
          <div><strong>17</strong><span>Completed deals</span></div>
          <div><strong>{sellerListings.filter((item) => !item.soldOut).length}</strong><span>Active listings</span></div>
          <div><strong>92%</strong><span>Positive feedback</span></div>
        </section>

        <div className="marketplace-profile-content">
          <section className="marketplace-profile-listings">
            <header><h2>Marketplace listings</h2><span>{sellerListings.length} items</span></header>
            <div>{sellerListings.map((item) => <Link key={item.id} to={`/marketplace/listings/${item.id}`}><span className="marketplace-profile-product-image"><MarketplaceListingImage listing={item} /></span><span><small>{item.soldOut ? 'Sold out' : item.condition}</small><strong>{item.title}</strong><b>{money(item.price)}</b><em><MapPin size={13} />{item.location}</em></span></Link>)}</div>
          </section>
          <aside className="marketplace-reputation-panel">
            <h2>Student reputation</h2>
            <div><ShieldCheck size={19} /><span><strong>Identity verified</strong>Campus Day student account</span></div>
            <div><BadgeCheck size={19} /><span><strong>Reliable seller</strong>Consistent completed pickups</span></div>
            <blockquote>“Item matched the description and pickup was easy.”<footer>Recent buyer review</footer></blockquote>
          </aside>
        </div>
      </main>

      {dialog === 'message' ? <MarketplaceDialog title={`Message ${primaryListing.seller}`} description="Ask about an item or arrange a safe campus pickup." onClose={() => setDialog(null)}>
        {sent ? <div className="marketplace-dialog-success"><CheckCircle2 size={42} /><h3>Message sent</h3><p>The seller can reply through Campus Day marketplace messages.</p><button type="button" onClick={() => setDialog(null)}>Done</button></div> : <form className="marketplace-dialog-form" onSubmit={submitMessage}>
          <label><span>Your message</span><textarea name="message" rows={5} required defaultValue={`Hi, is ${primaryListing.title} still available?`} /></label>
          <small>Do not share passwords, OTP codes or payment details in messages.</small>
          {error ? <p className="marketplace-form-error" role="alert">{error}</p> : null}
          <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Sending...' : 'Send message'}</button>
        </form>}
      </MarketplaceDialog> : null}

      {dialog === 'report' ? <MarketplaceDialog title="Report seller profile" description="Reports help keep student trading safer." onClose={() => setDialog(null)}>
        {sent ? <div className="marketplace-dialog-success"><CheckCircle2 size={42} /><h3>Report recorded</h3><p>Campus Day moderators can now review your report.</p><button type="button" onClick={() => setDialog(null)}>Done</button></div> : <form className="marketplace-dialog-form" onSubmit={submitReport}>
          <label><span>Reason</span><select name="reason" required defaultValue=""><option value="" disabled>Select a reason</option><option value="SCAM">Suspicious behaviour</option><option value="MISLEADING_INFORMATION">Misleading listings</option><option value="HARASSMENT">Harassment</option><option value="OTHER">Other</option></select></label>
          <label><span>Details</span><textarea name="details" rows={4} required placeholder="Tell us what happened" /></label>
          {error ? <p className="marketplace-form-error" role="alert">{error}</p> : null}
          <button type="submit" className="marketplace-danger-action" disabled={submitting}>{submitting ? 'Submitting...' : 'Submit report'}</button>
        </form>}
      </MarketplaceDialog> : null}
    </MarketplaceFlowPage>
  )
}
