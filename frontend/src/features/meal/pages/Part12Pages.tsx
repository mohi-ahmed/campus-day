import { useMemo, useState, type FormEvent } from 'react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { managementPeriodService } from '../services/managementPeriodService'
import { stockApi } from '../services/stockApi'
import { part12Api, type Handover } from '../services/part12Api'
import { currentBillingMonth, moneyLabel } from '../services/billingApi'
import { dhakaToday } from '../services/menuApi'
import type { ManagementPeriod } from '../types'

function periodName(period: ManagementPeriod | undefined | null) { return period ? `${period.year}-${String(period.month).padStart(2, '0')} · ${period.managers.map((manager) => manager.name).join(' & ')}` : 'Not assigned' }

export function StockHandoverPage() {
  const { user } = useDemoUser()
  const [revision, setRevision] = useState(0)
  const [requestId, setRequestId] = useState(() => crypto.randomUUID())
  const [paymentRequestId, setPaymentRequestId] = useState(() => crypto.randomUUID())
  const [date, setDate] = useState(dhakaToday())
  const [value, setValue] = useState('')
  const [note, setNote] = useState('')
  const [quantities, setQuantities] = useState<Record<string, string>>({})
  const [selected, setSelected] = useState<Handover | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const context = useMealQuery(() => managementPeriodService.context(), [revision])
  const stock = useMealQuery(() => stockApi.ingredients(), [revision])
  const history = useMealQuery(() => part12Api.handovers(), [revision])
  const periods = [context.data?.previous, context.data?.current, context.data?.next].filter((period): period is ManagementPeriod => Boolean(period)).sort((a, b) => a.year - b.year || a.month - b.month)
  const outgoing = context.data?.next && context.data?.current ? context.data.current : context.data?.previous ?? context.data?.current ?? periods[0]
  const incoming = periods.find((period) => outgoing && (period.year * 12 + period.month) === outgoing.year * 12 + outgoing.month + 1)
  const submit = async (event: FormEvent) => { event.preventDefault(); if (!outgoing || !incoming) return; setBusy(true); setError(null); try {
    const lines = (stock.data ?? []).filter((ingredient) => quantities[ingredient.id] && Number(quantities[ingredient.id]) > 0).map((ingredient) => ({ ingredientId: ingredient.id, unit: ingredient.unit, quantity: quantities[ingredient.id] }))
    if (!lines.length) throw new Error('Enter at least one transferred quantity.')
    const saved = await part12Api.createHandover(user.id, { requestId, outgoingPeriodId: outgoing.id, incomingPeriodId: incoming.id, handoverDate: date, lines, recordedValue: value, note: note || null })
    setSelected(saved); setRequestId(crypto.randomUUID()); setRevision((number) => number + 1)
  } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not record handover') } finally { setBusy(false) } }
  const pay = async () => { if (!selected) return; setBusy(true); setError(null); try { const saved = await part12Api.payHandover(user.id, selected.id, paymentRequestId, selected.recordedValue, dhakaToday()); setSelected(saved); setPaymentRequestId(crypto.randomUUID()); setRevision((number) => number + 1) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not record payment') } finally { setBusy(false) } }
  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Stock handover']} /><PageHeader title="Stock handover" description="Record remaining stock transferred between management teams." />
    <Notice tone="warning" title="Value is entered manually">The authority must confirm the handover value. No stock valuation formula is applied, and inventory balances stay unchanged.</Notice>
    {error ? <Notice tone="error" title="Handover action failed">{error}</Notice> : null}
    {context.loading || stock.loading ? <Skeleton lines={6} /> : <Card><SectionHeader title="Record handover" description={`${periodName(outgoing)} → ${periodName(incoming)}`} />
      {!outgoing || !incoming ? <EmptyState title="Two consecutive teams are required" description="Assign the incoming management period before recording handover." /> : <form className="form-stack" onSubmit={submit}>
        <Field label="Handover date"><Input type="date" max={dhakaToday()} value={date} onChange={(event) => setDate(event.target.value)} required /></Field>
        <div className="responsive-table"><table><thead><tr><th>Ingredient</th><th>Available</th><th>Transferred quantity</th></tr></thead><tbody>{stock.data?.map((ingredient) => <tr key={ingredient.id}><td>{ingredient.name}</td><td>{ingredient.currentStock} {ingredient.unit}</td><td><Input type="number" min="0" max={ingredient.currentStock} step="0.001" value={quantities[ingredient.id] ?? ''} onChange={(event) => setQuantities({ ...quantities, [ingredient.id]: event.target.value })} /></td></tr>)}</tbody></table></div>
        <Field label="Confirmed handover value (BDT)"><Input type="number" min="0" step="0.01" value={value} onChange={(event) => setValue(event.target.value)} required /></Field>
        <Field label="Note (optional)"><Input value={note} onChange={(event) => setNote(event.target.value)} /></Field><Button type="submit" disabled={busy}>Record handover</Button>
      </form>}</Card>}
    <SectionHeader title="Handover history" />{history.loading ? <Skeleton lines={4} /> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Date</th><th>Outgoing → Incoming</th><th>Ingredients</th><th>Recorded value</th><th>Payment</th><th></th></tr></thead><tbody>{history.data?.map((handover) => <tr key={handover.id}><td>{handover.handoverDate}</td><td>{handover.outgoingPeriodId} → {handover.incomingPeriodId}</td><td>{handover.lines.length}</td><td>{moneyLabel(handover.recordedValue)}</td><td><StatusBadge status={handover.paymentRequestId ? 'Paid' : 'Unpaid'} /></td><td><Button type="button" variant="secondary" onClick={() => setSelected(handover)}>View</Button></td></tr>)}</tbody></table></div>{!history.data?.length ? <EmptyState title="No handovers recorded" description="Recorded handovers appear here." /> : null}</Card>}
    {selected ? <Card><SectionHeader title={`Handover · ${selected.handoverDate}`} description={`Recorded value ${moneyLabel(selected.recordedValue)} · paid ${moneyLabel(selected.paidAmount)}`} /><div className="responsive-table"><table><thead><tr><th>Ingredient</th><th>Quantity</th></tr></thead><tbody>{selected.lines.map((line) => <tr key={line.ingredientId}><td>{line.ingredientName}</td><td>{line.quantity} {line.unit}</td></tr>)}</tbody></table></div>{selected.paymentRequestId ? <Notice title="Payment recorded">Paid on {selected.paymentDate}.</Notice> : context.data?.current?.id === selected.incomingPeriodId ? <Button type="button" onClick={pay} disabled={busy}>Record full payment</Button> : <Notice title="Payment belongs to the incoming team">The incoming team's current Meal Managers record the full payment.</Notice>}</Card> : null}
  </div>
}

export function MonthlyReportsPage() {
  const initial = useMemo(() => currentBillingMonth(), [])
  const [period, setPeriod] = useState(`${initial.year}-${String(initial.month).padStart(2, '0')}`)
  const [year, month] = period.split('-').map(Number)
  const { data, loading, error } = useMealQuery(() => part12Api.report(year, month), [year, month])
  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Reports']} /><PageHeader title="Monthly review" description="Operational and financial summary from recorded meal activity." />
    <Field label="Management month"><Input type="month" value={period} onChange={(event) => setPeriod(event.target.value)} /></Field>
    {loading ? <Skeleton lines={8} /> : error || !data ? <EmptyState title="Monthly report unavailable" description="Select a month with a management period." /> : <>
      <Card><SectionHeader title="Management" /><p>{data.managers.map((manager) => `${manager.name} (${manager.studentId})`).join(' & ')}</p><StatusBadge status={data.periodStatus === 'CLOSED' ? 'Closed' : 'Active'} /></Card>
      <div className="metric-grid metric-grid--compact"><Card><span>Valid Final Year orders</span><h2>{data.orders.validOrders}</h2><small>{data.orders.cancelledOrders} cancelled</small></Card><Card><span>Purchases</span><h2>{moneyLabel(data.purchases.total)}</h2><small>{data.purchases.count} completed purchases</small></Card><Card><span>Supplier dues now</span><h2>{moneyLabel(data.suppliers.currentOutstanding)}</h2><small>Current balance, not a month-end snapshot</small></Card></div>
      <Card><SectionHeader title="Final Year order quantities" /><p>Breakfast packages: {data.orders.breakfastPackages} · Full lunches: {data.orders.lunchFullMeals} · Full dinners: {data.orders.dinnerFullMeals}</p><div className="responsive-table"><table><thead><tr><th>Meal</th><th>Item</th><th>Quantity</th></tr></thead><tbody>{data.orders.items.map((item) => <tr key={`${item.meal}-${item.itemId}`}><td>{item.meal}</td><td>{item.name}</td><td>{item.quantity}</td></tr>)}</tbody></table></div></Card>
      <Card><SectionHeader title="Purchases and suppliers" /><div className="detail-list"><div><span>Cash purchases</span><strong>{moneyLabel(data.purchases.cashTotal)}</strong></div><div><span>Credit purchases</span><strong>{moneyLabel(data.purchases.creditTotal)}</strong></div><div><span>Supplier payments this month</span><strong>{moneyLabel(data.suppliers.paymentTotal)}</strong></div></div></Card>
      <Card><SectionHeader title="Stock movements and remaining stock" description="Current stock is live; recorded handover quantities preserve the transferred snapshot." /><div className="responsive-table"><table><thead><tr><th>Ingredient</th><th>Movement</th><th>Quantity</th></tr></thead><tbody>{data.inventory.movements.map((movement) => <tr key={`${movement.ingredientId}-${movement.type}`}><td>{data.inventory.liveStock.find((ingredient) => ingredient.id === movement.ingredientId)?.name ?? movement.ingredientId}</td><td>{movement.type}</td><td>{movement.quantity} {movement.unit}</td></tr>)}</tbody></table></div><SectionHeader title="Live remaining stock" /><div className="responsive-table"><table><thead><tr><th>Ingredient</th><th>Current quantity</th></tr></thead><tbody>{data.inventory.liveStock.map((ingredient) => <tr key={ingredient.id}><td>{ingredient.name}</td><td>{ingredient.currentStock} {ingredient.unit}</td></tr>)}</tbody></table></div></Card>
      <Card><SectionHeader title="Billing and collections" /><div className="detail-list"><div><span>Years 1–4 fixed monthly charge per student</span><strong>{moneyLabel(data.billing.fixedMonthlyCharge)}</strong></div><div><span>College Office collections</span><strong>{moneyLabel(data.billing.officeCollected)}</strong></div><div><span>Finalized Final Year bills</span><strong>{data.billing.finalizedFinalYearBills} · {moneyLabel(data.billing.finalYearBilled)}</strong></div><div><span>Final Year collected</span><strong>{moneyLabel(data.billing.finalYearCollected)}</strong></div><div><span>Final Year outstanding</span><strong>{moneyLabel(data.billing.finalYearOutstanding)}</strong></div></div></Card>
      <Card><SectionHeader title="Stock handover" />{data.handover ? <><p>{data.handover.lines.length} ingredients transferred · {moneyLabel(data.handover.recordedValue)}</p><StatusBadge status={data.handover.paymentRequestId ? 'Paid' : 'Unpaid'} /><div className="responsive-table"><table><thead><tr><th>Ingredient</th><th>Transferred quantity</th></tr></thead><tbody>{data.handover.lines.map((line) => <tr key={line.ingredientId}><td>{line.ingredientName}</td><td>{line.quantity} {line.unit}</td></tr>)}</tbody></table></div></> : <p>No handover recorded for this outgoing period.</p>}</Card>
    </>}
  </div>
}
