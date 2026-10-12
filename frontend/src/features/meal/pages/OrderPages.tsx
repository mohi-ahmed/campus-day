import { useEffect, useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Clock3, Edit3, Minus, Plus } from 'lucide-react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { BackLink } from '../../../components/AppShell'
import { Button, Card, EmptyState, Field, Input, Notice, PageHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { formatDate } from '../../../lib/format'
import { useMealQuery } from '../hooks/useMealQuery'
import { addDays, dhakaToday, menuApi, type MenuItemInput } from '../services/menuApi'
import { orderApi, type OrderInput, type OrderRecord } from '../services/orderApi'
import { findPrice, priceApi, priceLabel } from '../services/priceApi'

type Choice = { enabled: boolean; fullMeal: boolean; quantities: Record<string, number> }
type Selections = { breakfast: Choice; lunch: Choice; dinner: Choice }

const emptyChoice = (): Choice => ({ enabled: false, fullMeal: true, quantities: {} })
const emptySelections = (): Selections => ({ breakfast: emptyChoice(), lunch: emptyChoice(), dinner: emptyChoice() })

function fromOrder(order: OrderRecord): Selections {
  const choice = (meal: OrderRecord['lunch']): Choice => ({
    enabled: meal.kind !== 'NOT_EATING', fullMeal: meal.kind === 'FULL_MEAL',
    quantities: Object.fromEntries(meal.items.map((item) => [item.itemId, item.quantity])),
  })
  return { breakfast: { enabled: order.breakfast, fullMeal: true, quantities: {} },
    lunch: choice(order.lunch), dinner: choice(order.dinner) }
}

function toRequest(date: string, selection: Selections): OrderInput {
  const choice = (meal: Choice): OrderInput['lunch'] => !meal.enabled
    ? { kind: 'NOT_EATING', items: [] }
    : meal.fullMeal ? { kind: 'FULL_MEAL', items: [] }
      : { kind: 'ITEMS', items: Object.entries(meal.quantities)
          .filter(([, quantity]) => quantity > 0).map(([itemId, quantity]) => ({ itemId, quantity })) }
  return { mealDate: date, breakfast: selection.breakfast.enabled,
    lunch: choice(selection.lunch), dinner: choice(selection.dinner) }
}

function mealLabel(meal: OrderRecord['lunch']): string {
  if (meal.kind === 'NOT_EATING') return 'Not eating'
  if (meal.kind === 'FULL_MEAL') return 'Full meal'
  return meal.items.map((item) => `${item.name} × ${item.quantity}`).join(', ')
}

function orderStatus(order: OrderRecord): string {
  if (order.status === 'CANCELLED') return 'Cancelled'
  return order.canEdit || order.canCancel ? 'Ordered' : 'Locked'
}

function dhakaDeadline(value: string): string {
  return new Intl.DateTimeFormat('en-BD', { timeZone: 'Asia/Dhaka',
    day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }).format(new Date(value))
}

function OrderMeal({ title, packageOnly = false, items, available, orderingEnabled,
  fullMealAvailable = false, value, onChange, fullMealPrice, itemPrice }: {
  title: string; packageOnly?: boolean; items: MenuItemInput[]; available: boolean
  orderingEnabled: boolean; fullMealAvailable?: boolean; value: Choice; onChange: (value: Choice) => void
  fullMealPrice: string; itemPrice: (itemId: string | null) => string
}) {
  const selectable = available && orderingEnabled && (packageOnly || fullMealAvailable || items.some((item) => item.available))
  const setQty = (id: string, delta: number) => onChange({ ...value, enabled: true, fullMeal: false,
    quantities: { ...value.quantities, [id]: Math.max(0, (value.quantities[id] ?? 0) + delta) } })
  return <Card className={`order-meal ${value.enabled ? 'is-selected' : ''}`}><div className="order-meal__title"><div><h2>{title}</h2><p>{packageOnly ? 'Complete package' : 'Choose a full meal or individual items'}</p></div><label className="switch"><input type="checkbox" checked={value.enabled} disabled={!selectable} onChange={(event) => onChange({ ...value, enabled: event.target.checked, fullMeal: packageOnly || fullMealAvailable })} /><span /><b>{value.enabled ? 'Eating' : 'Not Eating'}</b></label></div>
    {!selectable ? <Notice title="Ordering unavailable">This meal is not open for ordering on the published menu.</Notice> : null}
    {value.enabled && selectable ? <div className="order-meal__body">{!packageOnly ? <div className="segmented"><button className={value.fullMeal ? 'is-active' : ''} disabled={!fullMealAvailable} onClick={() => onChange({ ...value, fullMeal: true })} type="button">Full meal</button><button className={!value.fullMeal ? 'is-active' : ''} disabled={!items.some((item) => item.available)} onClick={() => onChange({ ...value, fullMeal: false })} type="button">Individual items</button></div> : null}
      {packageOnly || !value.fullMeal ? <div className="order-items">{items.map((item) => <div className={`order-item ${!item.available ? 'is-disabled' : ''}`} key={item.id}><div><strong>{item.name}</strong><span>{itemPrice(item.id ?? null)}</span></div>{packageOnly ? <StatusBadge status="Available" /> : <div className="quantity"><button type="button" aria-label={`Remove ${item.name}`} disabled={!item.available || !(value.quantities[item.id ?? ''] > 0)} onClick={() => setQty(item.id ?? '', -1)}><Minus size={15} /></button><span>{value.quantities[item.id ?? ''] ?? 0}</span><button type="button" aria-label={`Add ${item.name}`} disabled={!item.available} onClick={() => setQty(item.id ?? '', 1)}><Plus size={15} /></button></div>}</div>)}</div> : <Notice tone="success" title="Full meal selected">Includes the published meal set for this service. {fullMealPrice}</Notice>}
    </div> : null}</Card>
}

function OrderEditor({ date, studentId }: { date: string; studentId: string }) {
  const { data: menus, loading: menuLoading, error: menuError } = useMealQuery(() => menuApi.published(date, date), [date])
  const { data: cutoffs, loading: cutoffLoading, error: cutoffError } = useMealQuery(() => orderApi.cutoffs(date), [date])
  const { data: found, loading: orderLoading, error: orderError } = useMealQuery(() => orderApi.byDate(studentId, date), [date, studentId])
  const { data: prices } = useMealQuery(() => priceApi.applicable(date), [date])
  const activePrices = prices?.mealDate === date ? prices : null
  const [changedOrder, setChangedOrder] = useState<OrderRecord | null>(null)
  const [editedSelection, setEditedSelection] = useState<Selections | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000)
    return () => window.clearInterval(timer)
  }, [])
  const order = changedOrder ?? found
  const selection = editedSelection ?? (order ? fromOrder(order) : emptySelections())
  const menu = menus?.[0]
  const selectedCount = Object.values(selection).filter((meal) => meal.enabled).length
  const quantitiesValid = (['lunch', 'dinner'] as const).every((meal) => !selection[meal].enabled
    || selection[meal].fullMeal || Object.values(selection[meal].quantities).some((quantity) => quantity > 0))
  const createOpen = !!cutoffs && now < new Date(cutoffs.orderCutoffAt).getTime()
  const editOpen = !!order && order.status === 'SUBMITTED' && now < new Date(order.editCutoffAt).getTime()
  const cancelOpen = !!order && order.status === 'SUBMITTED' && now < new Date(order.cancelCutoffAt).getTime()
  const canSave = !!menu && !!cutoffs && !orderError && quantitiesValid
    && (order ? selectedCount ? editOpen : cancelOpen : createOpen)
  const save = async () => {
    setFailure(null)
    if (!selectedCount && !order) { setMessage('No order placed. Your state remains Not Eating.'); return }
    if (!canSave) return
    setSubmitting(true)
    try {
      const updated = selectedCount === 0 && order ? await orderApi.cancel(studentId, order.id)
        : order ? await orderApi.update(studentId, order.id, toRequest(date, selection))
          : await orderApi.create(studentId, toRequest(date, selection))
      setChangedOrder(updated)
      setMessage(updated.status === 'CANCELLED' ? 'Order cancelled.' : 'Order saved.')
    } catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not save the order.') }
    finally { setSubmitting(false) }
  }
  return <>
    {message ? <Notice tone="success" title={message} /> : null}
    {failure ? <Notice tone="error" title="Order action failed">{failure}</Notice> : null}
    <div className="order-toolbar"><div><Clock3 size={18} /><span><strong>Asia/Dhaka cutoffs</strong><small>{cutoffs ? `Order ${dhakaDeadline(cutoffs.orderCutoffAt)} · Edit ${dhakaDeadline(cutoffs.editCutoffAt)} · Cancel ${dhakaDeadline(cutoffs.cancelCutoffAt)}` : 'Cutoff configuration is required'}</small></span></div></div>
    <Notice title="Prices can be added after you order.">Items showing Price Pending remain orderable. Your bill waits for required prices.</Notice>
    {menuLoading || cutoffLoading || orderLoading ? <Skeleton lines={8} /> : menuError || !menu ? <EmptyState title="No published menu for this date" description="Choose a date with a published menu before ordering." /> : cutoffError ? <Notice tone="warning" title="Ordering is not configured">The meal team must configure the order, edit and cancel cutoffs.</Notice> : orderError ? <Notice tone="error" title="Student order context unavailable">A matching Final Year Student record is required before using the order API.</Notice> : <>
      {order?.status === 'CANCELLED' ? <Notice title="Order cancelled">This order remains in your history.</Notice> : null}
      <div className="order-stack"><OrderMeal title="Breakfast" packageOnly items={[{ id: 'breakfast-package', name: 'Breakfast package', available: menu.breakfast.available }]} available={menu.breakfast.available} orderingEnabled={menu.breakfast.orderingEnabled} value={selection.breakfast} onChange={(breakfast) => setEditedSelection({ ...selection, breakfast })} fullMealPrice={priceLabel(findPrice(activePrices, 'BREAKFAST_PACKAGE'))} itemPrice={() => priceLabel(findPrice(activePrices, 'BREAKFAST_PACKAGE'))} />
        <OrderMeal title="Lunch" items={menu.lunch.items} available={menu.lunch.available} orderingEnabled={menu.lunch.orderingEnabled} fullMealAvailable={menu.lunch.fullMealAvailable} value={selection.lunch} onChange={(lunch) => setEditedSelection({ ...selection, lunch })} fullMealPrice={priceLabel(findPrice(activePrices, 'LUNCH_FULL_MEAL'))} itemPrice={(id) => priceLabel(findPrice(activePrices, 'LUNCH_MENU_ITEM', id))} />
        <OrderMeal title="Dinner" items={menu.dinner.items} available={menu.dinner.available} orderingEnabled={menu.dinner.orderingEnabled} fullMealAvailable={menu.dinner.fullMealAvailable} value={selection.dinner} onChange={(dinner) => setEditedSelection({ ...selection, dinner })} fullMealPrice={priceLabel(findPrice(activePrices, 'DINNER_FULL_MEAL'))} itemPrice={(id) => priceLabel(findPrice(activePrices, 'DINNER_MENU_ITEM', id))} /></div>
      <div className="sticky-submit"><div><strong>{selectedCount ? `${selectedCount} meals selected` : 'Not Eating'}</strong><span>{!quantitiesValid ? 'Choose at least one item quantity' : order ? `Current order: ${orderStatus(order)}` : 'No order placed for this date'}</span></div><Button onClick={save} disabled={submitting || (!canSave && !!selectedCount) || order?.status === 'CANCELLED'}>{submitting ? 'Saving…' : !selectedCount && order ? 'Cancel order' : order ? 'Update order' : 'Place order'}</Button></div>
    </>}
  </>
}

export function OrderPage() {
  const { user } = useDemoUser()
  const [params] = useSearchParams()
  const today = useMemo(() => dhakaToday(), [])
  const [date, setDate] = useState(params.get('date') ?? addDays(today, 1))
  return <div className="page-enter student-width"><PageHeader title="Place order" description="Choose breakfast, lunch and dinner from the published menu." />
    <Field label="Meal date"><Input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></Field>
    <OrderEditor key={date} date={date} studentId={user.id} />
  </div>
}

export function OrdersPage() {
  const { user } = useDemoUser()
  const today = useMemo(() => dhakaToday(), [])
  const { data, loading, error } = useMealQuery(() => orderApi.history(user.id, addDays(today, -335), addDays(today, 30)), [user.id, today])
  const [filter, setFilter] = useState<'All orders' | 'Upcoming' | 'Past' | 'Cancelled'>('All orders')
  const visible = data?.filter((order) => filter === 'All orders' || filter === 'Cancelled' && order.status === 'CANCELLED'
    || filter === 'Upcoming' && order.mealDate >= today && order.status === 'SUBMITTED'
    || filter === 'Past' && order.mealDate < today && order.status === 'SUBMITTED')
  return <div className="page-enter student-width"><PageHeader title="My orders" description="Review upcoming and previous Final Year meal orders." actions={<Link className="button button--primary" to="/meal/order">Place order</Link>} />
    <div className="filter-pills">{(['All orders', 'Upcoming', 'Past', 'Cancelled'] as const).map((option) => <button key={option} className={filter === option ? 'is-active' : ''} onClick={() => setFilter(option)}>{option}</button>)}</div>
    {loading ? <Skeleton lines={6} /> : error ? <Notice tone="error" title="Orders unavailable">A matching Final Year Student record is required.</Notice> : !visible?.length ? <EmptyState title="No matching orders" description="Without an order, your default state is Not Eating." action={<Link className="button button--primary" to="/meal/order">Place an order</Link>} /> : <div className="order-list">{visible.map((order) => <Link className="order-row" to={`/meal/orders/${order.id}`} key={order.id}><div className="date-tile"><strong>{Number(order.mealDate.slice(8))}</strong><span>{formatDate(order.mealDate, 'en-BD', { month: 'short' })}</span></div><div className="order-row__meals"><strong>{formatDate(order.mealDate, 'en-BD', { weekday: 'long', day: 'numeric', month: 'long' })}</strong><span>B: {order.breakfast ? 'Package' : 'Not eating'} · L: {mealLabel(order.lunch)} · D: {mealLabel(order.dinner)}</span></div><StatusBadge status={orderStatus(order)} /><span className="amount">Price pending</span></Link>)}</div>}
  </div>
}

export function OrderDetailPage() {
  const { user } = useDemoUser()
  const { orderId } = useParams()
  const { data, loading, error } = useMealQuery(() => orderApi.byId(user.id, orderId ?? ''), [user.id, orderId])
  const [changed, setChanged] = useState<OrderRecord | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const order = changed ?? data
  const cancel = async () => {
    if (!order) return
    try { setChanged(await orderApi.cancel(user.id, order.id)); setFailure(null) }
    catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not cancel the order.') }
  }
  if (loading) return <Skeleton lines={8} />
  if (error || !order) return <EmptyState title="Order not found" description="This order may no longer be available." action={<Link className="button button--secondary" to="/meal/orders">Back to orders</Link>} />
  return <div className="page-enter form-width"><BackLink to="/meal/orders" label="My orders" /><PageHeader title={formatDate(order.mealDate, 'en-BD', { day: 'numeric', month: 'long', year: 'numeric' })} description={`Order ${order.id}`} actions={<StatusBadge status={orderStatus(order)} />} />
    {failure ? <Notice tone="error" title="Cancellation failed">{failure}</Notice> : null}
    <Card><div className="detail-list"><div><span>Breakfast</span><strong>{order.breakfast ? 'Complete package' : 'Not eating'}</strong></div><div><span>Lunch</span><strong>{mealLabel(order.lunch)}</strong></div><div><span>Dinner</span><strong>{mealLabel(order.dinner)}</strong></div></div><div className="divider" /><div className="total-row"><span>Order price</span><strong>Pending required prices</strong></div></Card>
    {order.status === 'SUBMITTED' ? <div className="page-actions">{order.canEdit ? <Link className="button button--secondary" to={`/meal/order?date=${order.mealDate}`}><Edit3 size={17} />Edit order</Link> : null}{order.canCancel ? <Button variant="danger" onClick={cancel}>Cancel order</Button> : null}</div> : null}
    {order.status === 'SUBMITTED' && !order.canEdit && !order.canCancel ? <Notice title="This order is locked.">The configured edit and cancel cutoffs have passed.</Notice> : null}
  </div>
}
