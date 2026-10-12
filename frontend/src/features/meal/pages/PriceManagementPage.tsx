import { useMemo, useState, type FormEvent } from 'react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { formatDate } from '../../../lib/format'
import { useMealQuery } from '../hooks/useMealQuery'
import { dhakaToday } from '../services/menuApi'
import { priceApi, priceLabel, type ApplicablePrice } from '../services/priceApi'

function mealName(price: ApplicablePrice): string {
  if (price.target.startsWith('BREAKFAST')) return 'Breakfast'
  return price.target.startsWith('LUNCH') ? 'Lunch' : 'Dinner'
}

export default function PriceManagementPage() {
  const { user } = useDemoUser()
  const today = useMemo(() => dhakaToday(), [])
  const [date, setDate] = useState(today)
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState<ApplicablePrice | null>(null)
  const [amount, setAmount] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const { data, loading, error } = useMealQuery(() => priceApi.applicable(date), [date, revision])
  const { data: history } = useMealQuery(
    () => selected ? priceApi.history(date, selected.target, selected.itemId) : Promise.resolve([]),
    [date, selected?.target, selected?.itemId, revision],
  )

  const choose = (price: ApplicablePrice) => {
    setSelected(price)
    setAmount(price.amount == null ? '' : String(price.amount))
    setConfirming(false)
    setFailure(null)
    setMessage(null)
  }

  const review = (event: FormEvent) => {
    event.preventDefault()
    if (!selected) return
    const value = Number(amount)
    if (!Number.isFinite(value) || value < 0 || !/^\d+(?:\.\d{1,2})?$/.test(amount)) {
      setFailure('Enter a nonnegative price with at most two decimal places.')
      return
    }
    setFailure(null)
    setConfirming(true)
  }

  const save = async () => {
    if (!selected) return
    setSaving(true)
    setFailure(null)
    try {
      await priceApi.set(user.id, date, selected.target, selected.itemId, amount)
      setSelected(null)
      setConfirming(false)
      setRevision((value) => value + 1)
      setMessage('Price saved and now applicable for this menu date.')
    } catch (cause) {
      setFailure(cause instanceof Error ? cause.message : 'Could not save the price.')
    } finally { setSaving(false) }
  }

  return <div className="page-enter wide-page">
    <Breadcrumbs items={['Meal Management', 'Price management']} />
    <PageHeader title="Final Year prices" description="Set prices for a published daily menu. A saved price applies immediately for that menu date." />
    <Field label="Meal date"><Input type="date" value={date} onChange={(event) => { setDate(event.target.value); setSelected(null); setConfirming(false); setMessage(null) }} /></Field>
    {message ? <Notice tone="success" title={message} /> : null}
    {failure ? <Notice tone="error" title="Price action failed">{failure}</Notice> : null}
    {loading || data && data.mealDate !== date ? <Skeleton lines={6} /> : error || !data ? <EmptyState title="No published menu or prices available" description="Choose a date with a published menu to manage its prices." /> : <>
      <Card className="table-card"><SectionHeader title="Applicable prices" description={formatDate(date, 'en-BD', { day: 'numeric', month: 'long', year: 'numeric' })} />
        <div className="responsive-table"><table><thead><tr><th>Meal</th><th>Package or item</th><th>Price</th><th>Status</th><th></th></tr></thead><tbody>
          {data.prices.map((price) => <tr key={`${price.target}:${price.itemId ?? ''}`}><td>{mealName(price)}</td><td><strong>{price.label}</strong></td><td>{priceLabel(price.amount)}</td><td><StatusBadge status={price.amount == null ? 'Price Pending' : 'Available'} /></td><td><Button type="button" variant="secondary" onClick={() => choose(price)}>{price.amount == null ? 'Set price' : 'Change price'}</Button></td></tr>)}
        </tbody></table></div>
      </Card>
      {selected ? <Card><SectionHeader title={`${selected.amount == null ? 'Set' : 'Change'} ${selected.label}`} description={`${mealName(selected)} · ${date}`} />
        {confirming ? <div className="form-stack"><Notice title="Confirm price">{selected.label} · {mealName(selected)} · {date} · {priceLabel(Number(amount))}. Saving makes this price applicable immediately.</Notice><div className="form-actions"><Button type="button" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Confirm and save'}</Button><Button type="button" variant="secondary" onClick={() => setConfirming(false)}>Edit amount</Button></div></div>
          : <form className="form-stack" onSubmit={review}><Field label="Amount (BDT)"><Input type="number" min="0" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required /></Field><div className="form-actions"><Button type="submit">Review price</Button><Button type="button" variant="secondary" onClick={() => setSelected(null)}>Cancel</Button></div></form>}
        <SectionHeader title="Price history" description="Earlier saved amounts remain available for historical lookup." />
        {history?.length ? <div className="responsive-table"><table><thead><tr><th>Applicable from</th><th>Amount</th></tr></thead><tbody>{history.map((version) => <tr key={version.id}><td>{new Intl.DateTimeFormat('en-BD', { timeZone: 'Asia/Dhaka', dateStyle: 'medium', timeStyle: 'short' }).format(new Date(version.effectiveAt))}</td><td>{priceLabel(version.amount)}</td></tr>)}</tbody></table></div> : <p>No earlier prices for this selection.</p>}
      </Card> : null}
    </>}
  </div>
}
