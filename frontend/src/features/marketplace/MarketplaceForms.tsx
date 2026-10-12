import { useState, type FormEvent } from 'react'
import { BadgeCheck, Camera, ChevronRight, ImagePlus, MapPin, PackageCheck, ShieldCheck, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDemoUser } from '../../app/DemoUserContext'
import { MarketplaceFlowPage, MarketplacePageIntro } from './MarketplaceFlowUi'
import { marketplaceApi, marketplaceApiValues } from './marketplaceApi'
import { categories } from './marketplaceData'
import './marketplace.css'

function FormSuccess({ type, resourceId }: { type: 'listing' | 'wanted'; resourceId?: string }) {
  const isListing = type === 'listing'
  return (
    <div className="marketplace-form-success">
      <span><BadgeCheck size={42} /></span>
      <h2>{isListing ? 'Your listing is ready' : 'Your wanted post is live'}</h2>
      <p>{isListing ? 'Students can now find your item and send offers.' : 'Sellers can now respond with matching items.'}</p>
      <div><Link className="marketplace-primary-action" to={isListing && resourceId ? `/marketplace/listings/${resourceId}` : '/marketplace/wanted'}>{isListing ? 'View listing' : 'View wanted posts'}<ChevronRight size={17} /></Link><Link className="marketplace-secondary-action" to="/marketplace/me">Go to My Marketplace</Link></div>
    </div>
  )
}

export function SellItemPage() {
  const { user } = useDemoUser()
  const [submitted, setSubmitted] = useState(false)
  const [createdId, setCreatedId] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [condition, setCondition] = useState('Good')
  const [saleType, setSaleType] = useState('Used')
  const [negotiable, setNegotiable] = useState(true)
  const [photos, setPhotos] = useState<File[]>([])

  if (submitted) return <MarketplaceFlowPage><FormSuccess type="listing" resourceId={createdId} /></MarketplaceFlowPage>

  const submitListing = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!photos.length) {
      setError('Add at least one product photo.')
      return
    }
    setSubmitting(true)
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      const uploaded = await Promise.all(photos.map((file) => marketplaceApi.uploadImage(user.id, file)))
      const created = await marketplaceApi.createListing({
        sellerStudentId: user.id,
        title: String(form.get('title') ?? ''),
        description: String(form.get('description') ?? ''),
        category: marketplaceApiValues.category(String(form.get('category') ?? '')),
        price: Number(form.get('price')),
        condition: marketplaceApiValues.condition(condition),
        listingType: saleType === 'Used' ? 'USED' : 'STUDENT_BUSINESS',
        quantity: saleType === 'Used' ? 1 : Number(form.get('quantity')),
        negotiable,
        pickupZone: marketplaceApiValues.pickup(String(form.get('pickup') ?? '')),
        imageIds: uploaded.map((image) => image.id),
      })
      setCreatedId(created.id)
      setSubmitted(true)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not publish the listing.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-form-page">
        <MarketplacePageIntro title="Sell an item" description="Create a clear listing and meet a verified buyer at a campus pickup point." />
        <form className="marketplace-listing-form" onSubmit={submitListing}>
          <div className="marketplace-form-main">
            <section className="marketplace-form-card">
              <header><span><Camera size={19} /></span><div><h2>Product photos</h2><p>Add up to 8 clear photos. The first image becomes the cover.</p></div></header>
              <label className="marketplace-upload-zone">
                <ImagePlus size={28} />
                <strong>Add product photos</strong>
                <span>PNG, JPG or WEBP, up to 8 images</span>
                <input name="photos" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={(event) => setPhotos(Array.from(event.target.files ?? []).slice(0, 8))} />
              </label>
              {photos.length ? <div className="marketplace-upload-list">{photos.map((photo, index) => <span key={`${photo.name}-${photo.lastModified}`}><ImagePlus size={15} />{photo.name}<button type="button" aria-label={`Remove ${photo.name}`} onClick={() => setPhotos((current) => current.filter((_, itemIndex) => itemIndex !== index))}><X size={14} /></button></span>)}</div> : null}
            </section>

            <section className="marketplace-form-card">
              <header><span><PackageCheck size={19} /></span><div><h2>Item details</h2><p>Use specific information so buyers know exactly what you are offering.</p></div></header>
              <div className="marketplace-form-grid">
                <label className="marketplace-field marketplace-field--wide"><span>Title</span><input name="title" placeholder="Example: Casio scientific calculator" required maxLength={80} /><small>Keep it short and descriptive.</small></label>
                <label className="marketplace-field"><span>Category</span><select name="category" required defaultValue=""><option value="" disabled>Select category</option>{categories.map((category) => <option key={category.label}>{category.label}</option>)}</select></label>
                <label className="marketplace-field"><span>Price</span><div className="marketplace-money-input"><b>৳</b><input name="price" type="number" min="0" placeholder="0" required /></div></label>
                <label className="marketplace-field marketplace-field--wide"><span>Description</span><textarea name="description" rows={5} placeholder="Describe condition, included accessories and anything the buyer should know" required /></label>
              </div>
            </section>

            <section className="marketplace-form-card">
              <header><span><ShieldCheck size={19} /></span><div><h2>Condition and availability</h2><p>Set accurate expectations for buyers.</p></div></header>
              <fieldset className="marketplace-choice-field"><legend>Item type</legend><div>{['Used', 'New / Student business'].map((item) => <button key={item} type="button" className={saleType === item ? 'is-active' : ''} onClick={() => setSaleType(item)}>{item}</button>)}</div></fieldset>
              <input type="hidden" name="saleType" value={saleType} />
              <fieldset className="marketplace-choice-field"><legend>Condition</legend><div>{['New', 'Like new', 'Good', 'Fair'].map((item) => <button key={item} type="button" className={condition === item ? 'is-active' : ''} onClick={() => setCondition(item)}>{item}</button>)}</div></fieldset>
              <input type="hidden" name="condition" value={condition} />
              <div className="marketplace-form-grid">
                <label className="marketplace-field"><span>Quantity</span><input name="quantity" type="number" min="1" defaultValue={saleType === 'Used' ? 1 : 5} required /></label>
                <label className="marketplace-switch-field"><span><strong>Open to offers</strong><small>Buyers can suggest a different price.</small></span><input name="negotiable" type="checkbox" checked={negotiable} onChange={(event) => setNegotiable(event.target.checked)} /><i /></label>
              </div>
            </section>

            <section className="marketplace-form-card">
              <header><span><MapPin size={19} /></span><div><h2>Campus pickup</h2><p>Choose a familiar public area. Share the exact point after reservation.</p></div></header>
              <div className="marketplace-form-grid">
                <label className="marketplace-field"><span>Pickup zone</span><select name="pickup" defaultValue="" required><option value="" disabled>Select pickup zone</option><option>Main Gate</option><option>Library</option><option>Cafeteria</option><option>CSE Building</option><option>Hall Lobby</option><option>Student Centre</option></select></label>
                <label className="marketplace-field"><span>Preferred time</span><select name="time" defaultValue="Flexible"><option>Flexible</option><option>Morning</option><option>Afternoon</option><option>Evening</option></select></label>
              </div>
            </section>
          </div>

          <aside className="marketplace-form-summary">
            <h2>Before you publish</h2>
            <ul><li>Use your own clear photos</li><li>Describe any damage honestly</li><li>Do not list prohibited items</li><li>Meet in a public campus area</li></ul>
            <div><ShieldCheck size={18} /><span><strong>Student-only marketplace</strong>Your verified Campus Day profile appears on the listing.</span></div>
            {error ? <p className="marketplace-form-error" role="alert">{error}</p> : null}
            <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Publishing...' : 'Publish listing'}</button>
            <Link to="/marketplace">Save as draft</Link>
          </aside>
        </form>
      </main>
    </MarketplaceFlowPage>
  )
}

export function CreateWantedPostPage() {
  const { user } = useDemoUser()
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [condition, setCondition] = useState('Used is fine')

  if (submitted) return <MarketplaceFlowPage><FormSuccess type="wanted" /></MarketplaceFlowPage>

  const submitWantedPost = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      await marketplaceApi.createWantedPost({
        authorStudentId: user.id,
        title: String(form.get('title') ?? ''),
        description: String(form.get('description') ?? ''),
        category: marketplaceApiValues.category(String(form.get('category') ?? '')),
        maximumBudget: Number(form.get('budget')),
        acceptableCondition: marketplaceApiValues.condition(condition),
        pickupZone: marketplaceApiValues.pickup(String(form.get('pickup') ?? '')),
      })
      setSubmitted(true)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not publish the wanted post.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <MarketplaceFlowPage>
      <main className="marketplace-form-page marketplace-wanted-form-page">
        <MarketplacePageIntro backTo="/marketplace/wanted" backLabel="Back to wanted posts" title="Post what you need" description="Tell campus sellers what you are looking for and the budget you have in mind." />
        <form className="marketplace-wanted-form" onSubmit={submitWantedPost}>
          <section className="marketplace-form-card">
            <div className="marketplace-form-grid">
              <label className="marketplace-field marketplace-field--wide"><span>What are you looking for?</span><input name="title" placeholder="Example: Scientific calculator" maxLength={80} required /></label>
              <label className="marketplace-field"><span>Category</span><select name="category" defaultValue="" required><option value="" disabled>Select category</option>{categories.map((category) => <option key={category.label}>{category.label}</option>)}</select></label>
              <label className="marketplace-field"><span>Maximum budget</span><div className="marketplace-money-input"><b>৳</b><input name="budget" type="number" min="1" placeholder="1500" required /></div></label>
              <label className="marketplace-field marketplace-field--wide"><span>Details</span><textarea name="description" rows={5} placeholder="Mention model, size, edition or any other useful detail" required /></label>
            </div>
            <fieldset className="marketplace-choice-field"><legend>Acceptable condition</legend><div>{['New only', 'Like new', 'Good or better', 'Used is fine'].map((item) => <button key={item} type="button" className={condition === item ? 'is-active' : ''} onClick={() => setCondition(item)}>{item}</button>)}</div></fieldset>
            <input type="hidden" name="condition" value={condition} />
            <label className="marketplace-field"><span>Preferred pickup zone</span><select name="pickup" defaultValue="" required><option value="" disabled>Select pickup zone</option><option>Main Gate</option><option>Library</option><option>Cafeteria</option><option>CSE Building</option><option>Hall Lobby</option></select></label>
          </section>
          <aside className="marketplace-form-summary">
            <h2>Your request</h2>
            <p>Verified students with a matching item can respond. You decide which offer to continue with.</p>
            <div><ShieldCheck size={18} /><span><strong>Privacy protected</strong>Your contact details are not shown publicly.</span></div>
            {error ? <p className="marketplace-form-error" role="alert">{error}</p> : null}
            <button type="submit" className="marketplace-primary-action" disabled={submitting}>{submitting ? 'Publishing...' : 'Publish wanted post'}</button>
          </aside>
        </form>
      </main>
    </MarketplaceFlowPage>
  )
}
