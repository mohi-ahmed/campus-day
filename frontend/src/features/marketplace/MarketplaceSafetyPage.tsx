import { BadgeCheck, CheckCircle2, MapPin, MessageCircleMore, ShieldCheck, Smartphone, WalletCards } from 'lucide-react'
import { Link } from 'react-router-dom'
import { MarketplaceFlowPage, MarketplacePageIntro } from './MarketplaceFlowUi'
import './marketplace.css'

const safetyTips = [
  { icon: BadgeCheck, title: 'Check the student profile', copy: 'Look for a verified Campus Day account and review the seller activity before making an offer.' },
  { icon: MessageCircleMore, title: 'Keep the conversation here', copy: 'Use marketplace messaging so the offer, item details and pickup decision stay together.' },
  { icon: MapPin, title: 'Meet in a public campus zone', copy: 'Choose a busy, familiar place during campus hours. Avoid private rooms or isolated areas.' },
  { icon: WalletCards, title: 'Inspect before paying', copy: 'Confirm the item, accessories and condition in person before you complete payment.' },
]

export default function MarketplaceSafetyPage() {
  return (
    <MarketplaceFlowPage>
      <main className="marketplace-safety-page">
        <MarketplacePageIntro title="Trade safely on campus" description="A simple pickup flow designed for student-to-student marketplace deals." />

        <section className="marketplace-safety-hero">
          <div><span><ShieldCheck size={22} /></span><h2>Public pickup, private details</h2><p>The listing shows only a general campus zone. The exact meeting point and map directions become available after the seller accepts an offer.</p></div>
          <div className="marketplace-safety-map" aria-label="Example campus pickup map">
            <i /><i /><i /><i />
            <span><MapPin size={26} /><b>Library entrance</b><small>Example accepted pickup point</small></span>
          </div>
        </section>

        <section className="marketplace-safety-tips" aria-labelledby="marketplace-safety-tips-title">
          <header><h2 id="marketplace-safety-tips-title">Before you meet</h2><p>Four habits that make campus trading easier and safer.</p></header>
          <div>{safetyTips.map(({ icon: Icon, title, copy }) => <article key={title}><span><Icon size={20} /></span><div><h3>{title}</h3><p>{copy}</p></div></article>)}</div>
        </section>

        <section className="marketplace-pickup-steps">
          <header><h2>How campus pickup works</h2><p>The backend will later connect these states to offers and notifications.</p></header>
          <ol>
            <li><span>1</span><div><strong>Buyer makes an offer</strong><p>The exact seller location stays hidden.</p></div></li>
            <li><span>2</span><div><strong>Seller accepts and selects a point</strong><p>A public campus meeting point is shared with the buyer.</p></div></li>
            <li><span>3</span><div><strong>Buyer opens directions</strong><p>A free map provider can guide the buyer to the agreed pickup spot.</p></div></li>
            <li><span><CheckCircle2 size={18} /></span><div><strong>Both confirm the handoff</strong><p>The deal moves to completed after the item is checked.</p></div></li>
          </ol>
        </section>

        <section className="marketplace-safety-contact"><span><Smartphone size={22} /></span><div><h2>Something feels wrong?</h2><p>Leave the meeting, keep your messages, and report the listing or seller profile from Marketplace.</p></div><Link to="/marketplace">Return to Marketplace</Link></section>
      </main>
    </MarketplaceFlowPage>
  )
}
