import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, Check, ChevronRight, Clock3, Download, Edit3, FileDown, Utensils } from 'lucide-react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge, SubmittingLabel } from '../../../components/ui'
import { useLocale } from '../../../i18n/LocaleContext'
import { formatDate } from '../../../lib/format'
import { useMealQuery } from '../hooks/useMealQuery'
import { absenceApi, type AbsenceMeal } from '../services/absenceApi'
import { addDays, dhakaToday, menuApi, type MenuRecord } from '../services/menuApi'
import { orderApi } from '../services/orderApi'
import { findPrice, priceApi, priceLabel, type ApplicablePrices } from '../services/priceApi'
import { billingApi, currentBillingMonth, moneyLabel } from '../services/billingApi'
import { billCurrentOutstanding, billCurrentTotal, exportBillCsv, exportBillPdf } from '../services/billExport'
import type { MealType } from '../types'

function MenuCards({ menu, prices, finalYear }: { menu: MenuRecord; prices: ApplicablePrices | null; finalYear: boolean }) {
  const { locale } = useLocale()
  return <div className="meal-grid">
    <Card className="meal-card"><div className="meal-card__header"><div className="meal-icon meal-icon--breakfast"><Utensils size={18} /></div><div><h3>Breakfast</h3><StatusBadge status={menu.breakfast.available ? 'Available' : 'Unavailable'} /></div></div><ul><li><span><strong>Complete package</strong><small>{menu.breakfast.packageDescription}</small>{finalYear ? <small>{priceLabel(findPrice(prices, 'BREAKFAST_PACKAGE'))}</small> : null}</span>{!menu.breakfast.orderingEnabled ? <em>Ordering disabled</em> : null}</li></ul></Card>
    {(['lunch', 'dinner'] as const).map((meal) => {
      const section = menu[meal]
      return <Card key={meal} className="meal-card"><div className="meal-card__header"><div className={`meal-icon meal-icon--${meal}`}><Utensils size={18} /></div><div><h3>{meal[0].toUpperCase() + meal.slice(1)}</h3><StatusBadge status={section.available ? 'Available' : 'Unavailable'} /></div></div><p>{section.description}</p>{!section.orderingEnabled ? <small>Ordering disabled</small> : null}<ul>
        {section.fullMealAvailable ? <li><span><strong>Full meal</strong><small>{section.fullMealDescription}</small>{finalYear ? <small>{priceLabel(findPrice(prices, meal === 'lunch' ? 'LUNCH_FULL_MEAL' : 'DINNER_FULL_MEAL'))}</small> : null}</span></li> : null}
        {section.items.map((item) => <li key={item.id}><span><strong>{locale === 'bn-BD' ? item.nameBn || item.name : item.name}</strong><small>{item.quantity}</small>{finalYear ? <small>{priceLabel(findPrice(prices, meal === 'lunch' ? 'LUNCH_MENU_ITEM' : 'DINNER_MENU_ITEM', item.id ?? null))}</small> : null}</span>{!item.available ? <em>Unavailable</em> : null}</li>)}
      </ul></Card>
    })}
  </div>
}

function PriceAwareMenuCards({ menu }: { menu: MenuRecord }) {
  const { user } = useDemoUser()
  const finalYear = user.context === 'final-year'
  const { data: prices } = useMealQuery(
    () => finalYear ? priceApi.applicable(menu.mealDate) : Promise.resolve(null),
    [menu.mealDate, finalYear],
  )
  return <MenuCards menu={menu} prices={prices?.mealDate === menu.mealDate ? prices : null} finalYear={finalYear} />
}

function MenuPreview() {
  const today = useMemo(() => dhakaToday(), [])
  const { data, loading, error } = useMealQuery(() => menuApi.published(today, today), [today])
  if (loading) return <Card><Skeleton lines={4} /></Card>
  if (error || !data) return <Notice tone="error" title="We couldn't load today's menu.">Please try again.</Notice>
  return data[0] ? <PriceAwareMenuCards menu={data[0]} /> : <EmptyState title="No published menu for today" description="The meal team has not published today's menu yet." action={<Link className="button button--secondary" to="/meal/menu">Browse upcoming menu <ChevronRight size={16} /></Link>} />
}

function NextOrderCard({ studentId }: { studentId: string }) {
  const tomorrow = useMemo(() => addDays(dhakaToday(), 1), [])
  const { data: order, loading, error } = useMealQuery(() => orderApi.byDate(studentId, tomorrow), [studentId, tomorrow])
  if (loading) return <Card><Skeleton lines={4} /></Card>
  return <Card className="action-card"><div className="action-card__icon"><Clock3 /></div><span>Next order</span><h2>Tomorrow’s meals</h2>
    {error ? <p>Order information is unavailable for this Student record.</p> : order?.status === 'SUBMITTED' ? <><p>Your order is saved for {formatDate(tomorrow, 'en-BD', { day: 'numeric', month: 'long' })}.</p><StatusBadge status={order.canEdit || order.canCancel ? 'Ordered' : 'Locked'} /></> : <p>No order placed. Your default state is Not Eating.</p>}
    <Link className="button button--primary" to={`/meal/order?date=${tomorrow}`}><Edit3 size={17} />{order?.status === 'SUBMITTED' ? 'Review order' : 'Place order'}</Link>
    {order?.status === 'SUBMITTED' ? <small>Edit cutoff: {new Intl.DateTimeFormat('en-BD', { timeZone: 'Asia/Dhaka', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }).format(new Date(order.editCutoffAt))} · Asia/Dhaka</small> : null}
  </Card>
}

export function DashboardPage() {
  const { user } = useDemoUser()
  const { t } = useLocale()
  const finalYear = user.context === 'final-year'
  const billingMonth = useMemo(() => currentBillingMonth(), [])
  const { data: bill, loading: billLoading } = useMealQuery(
    () => finalYear ? billingApi.bill(user.id, billingMonth.year, billingMonth.month) : Promise.resolve(null),
    [finalYear, user.id, billingMonth.year, billingMonth.month],
  )
  return <div className="page-enter dashboard-page">
    <div className="dashboard-intro"><PageHeader title={`${t('greeting')}, ${user.name.split(' ')[0]}`} description={finalYear ? 'Review today’s menu and make sure your next order is ready.' : 'Here’s what matters for your meal today.'} actions={<StatusBadge status={user.isMealManager ? 'Manager access active' : `${user.academicYear === 5 ? 'Final Year' : `Year ${user.academicYear}`} student`} tone="brand" />} /></div>
    {user.isMealManager ? <Link className="manager-banner" to="/meal/manage"><span><strong>Meal Management workspace</strong><small>September period · 6 operational items need attention</small></span><span>Open workspace <ChevronRight size={17} /></span></Link> : null}
    <section className="dashboard-lead">
      <section className="today-panel"><SectionHeader title={t('today')} description="Published menu for today" action={<Link className="text-link" to="/meal/menu">View full menu <ChevronRight size={15} /></Link>} /><MenuPreview /></section>
      <aside className="dashboard-aside">
        {finalYear ? <NextOrderCard studentId={user.id} />
          : <Card className="action-card"><div className="action-card__icon"><CalendarDays /></div><span>Planning to miss a meal?</span><h2>Send an absence notice</h2><p>It helps the kitchen prepare the correct number of meals.</p><Link className="button button--primary" to="/meal/absence">Create notice</Link><small>No penalty if you do not submit one</small></Card>}
        {finalYear ? billLoading || !bill ? <Card><Skeleton /></Card> : <Card className="bill-card"><div><span>{bill.month}/{bill.year}</span><StatusBadge status={bill.status === 'FINALIZED' ? Number(billCurrentOutstanding(bill)) === 0 ? 'Paid' : 'Outstanding' : 'Current month'} /></div><h3>{moneyLabel(billCurrentTotal(bill))}</h3><p>Daily meal orders</p><div className="divider" /><small>Payment method</small><strong>Pay Meal Manager after month end</strong><Link className="text-link" to="/meal/bills">View monthly bill <ChevronRight size={15} /></Link></Card> : null}
      </aside>
    </section>
  </div>
}

export function MenuPage() {
  const { user } = useDemoUser()
  const today = useMemo(() => dhakaToday(), [])
  const dates = useMemo(() => [today, addDays(today, 1), addDays(today, 2)], [today])
  const { data, loading, error } = useMealQuery(() => menuApi.published(dates[0], dates[2]), [dates[0], dates[2]])
  const [day, setDay] = useState(0)
  const selected = data?.find((menu) => menu.mealDate === dates[day])
  return <div className="page-enter student-width"><PageHeader title="Meal menu" description="Plan ahead with the published JIMC hall menu." actions={user.context === 'final-year' && selected ? <Link className="button button--primary" to={`/meal/order?date=${selected.mealDate}`}>Place order</Link> : undefined} />
    <div className="date-tabs" role="tablist" aria-label="Menu date">{dates.map((date, index) => <button key={date} role="tab" aria-selected={day === index} onClick={() => setDay(index)}><strong>{index === 0 ? 'Today' : index === 1 ? 'Tomorrow' : formatDate(date, 'en-BD', { weekday: 'long' })}</strong><span>{formatDate(date, 'en-BD', { day: 'numeric', month: 'short' })}</span></button>)}</div>
    {loading ? <Skeleton lines={7} /> : error || !data ? <Notice tone="error" title="We couldn't load the menu.">Please try again.</Notice> : <>
      {selected ? <PriceAwareMenuCards menu={selected} /> : <EmptyState title="No published menu for this date" description="The meal team has not published this day's menu yet." />}
    </>}
  </div>
}

export function AbsencePage() {
  const { user } = useDemoUser()
  const today = useMemo(() => dhakaToday(), [])
  const [startDate, setStartDate] = useState(addDays(today, 1))
  const [endDate, setEndDate] = useState(addDays(today, 1))
  const [meals, setMeals] = useState<MealType[]>(['Lunch'])
  const [note, setNote] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [saved, setSaved] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const { data: notices, loading, error } = useMealQuery(
    () => absenceApi.list(user.id, today, addDays(today, 90)), [user.id, today, revision])
  const toggle = (meal: MealType) => setMeals((current) => current.includes(meal) ? current.filter((item) => item !== meal) : [...current, meal])
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    setSaved(false)
    setFailure(null)
    try {
      await absenceApi.create(user.id, { startDate, endDate,
        meals: meals.map((meal) => meal.toUpperCase() as AbsenceMeal), note: note.trim() || undefined })
      setSaved(true)
      setRevision((value) => value + 1)
    } catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not save the notice.') }
    finally { setSubmitting(false) }
  }
  return <div className="page-enter form-width"><PageHeader title="Absence notice" description="Tell the kitchen which meals you expect to miss." />
    <Notice title="This notice is for cooking planning only.">It helps the kitchen estimate meal quantities. There is no penalty if you do not submit a notice.</Notice>
    {saved ? <Notice tone="success" title="Absence notice saved.">Your notice has been recorded for cooking planning.</Notice> : null}
    {failure ? <Notice tone="error" title="Absence notice not saved">{failure}</Notice> : null}
    <Card><form onSubmit={submit} className="form-stack"><SectionHeader title="Notice details" description="Choose a date range and at least one meal." />
      <div className="form-grid"><Field label="From date"><Input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} required /></Field><Field label="To date"><Input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} required /></Field></div>
      <fieldset><legend>Meals you will miss</legend><div className="choice-grid">{(['Breakfast', 'Lunch', 'Dinner'] as MealType[]).map((meal) => <button type="button" className={`choice-card ${meals.includes(meal) ? 'is-selected' : ''}`} onClick={() => toggle(meal)} key={meal}><span className="check-box">{meals.includes(meal) ? <Check size={15} /> : null}</span><span><strong>{meal}</strong><small>Select for the full date range</small></span></button>)}</div></fieldset>
      <Field label="Note (optional)" hint="Keep it short; do not include medical details."><textarea className="input" rows={3} maxLength={1000} value={note} onChange={(event) => setNote(event.target.value)} placeholder="Anything the meal team should know" /></Field>
      <div className="form-actions"><Button type="submit" disabled={submitting || meals.length === 0 || !startDate || !endDate || startDate > endDate}><SubmittingLabel submitting={submitting} idle="Save absence notice" /></Button></div>
    </form></Card>
    <SectionHeader title="Upcoming notices" />
    {loading ? <Skeleton lines={4} /> : error ? <Notice tone="error" title="Notices unavailable">A matching Years 1–4 Student record is required.</Notice> : !notices?.length ? <EmptyState title="No upcoming absence notices" description="Submitting an absence notice is optional." /> : <Card className="list-card">{notices.map((notice) => <div className="list-row" key={notice.id}><div className="date-tile"><strong>{Number(notice.startDate.slice(8))}</strong><span>{formatDate(notice.startDate, 'en-BD', { month: 'short' })}</span></div><div><strong>{formatDate(notice.startDate, 'en-BD', { day: 'numeric', month: 'long' })}{notice.startDate === notice.endDate ? '' : ` – ${formatDate(notice.endDate, 'en-BD', { day: 'numeric', month: 'long' })}`}</strong><span>{notice.meals.map((meal) => meal[0] + meal.slice(1).toLowerCase()).join(' · ')}{notice.note ? ` · ${notice.note}` : ''}</span></div></div>)}</Card>}
  </div>
}

export function BillsPage() {
  const { user } = useDemoUser()
  const initial = useMemo(() => currentBillingMonth(), [])
  const [period, setPeriod] = useState(`${initial.year}-${String(initial.month).padStart(2, '0')}`)
  const [year, month] = period.split('-').map(Number)
  const { data: bill, loading, error } = useMealQuery(() => billingApi.bill(user.id, year, month), [user.id, year, month])
  const currentTotal = bill ? billCurrentTotal(bill) : null
  const currentOutstanding = bill ? billCurrentOutstanding(bill) : null
  return <div className="page-enter student-width print-document"><PageHeader title="Monthly meal bill" description="Your Final Year daily meal orders and offline payment history." actions={<>
    <Button variant="secondary" icon={<Download size={17} />} disabled={!bill} onClick={() => bill && exportBillCsv(bill)}>Export CSV</Button>
    <Button variant="secondary" icon={<FileDown size={17} />} disabled={!bill} onClick={() => bill && exportBillPdf(bill)}>Save PDF</Button>
  </>} />
    <div className="print-hidden"><Field label="Billing month"><Input type="month" value={period} onChange={(event) => setPeriod(event.target.value)} /></Field></div>
    {loading ? <Skeleton lines={8} /> : error || !bill ? <Notice tone="error" title="Bill unavailable">Check the Student record or try again.</Notice> : <>
    <div className="bill-summary"><Card><span>This month's orders</span><h2>{moneyLabel(currentTotal)}</h2><StatusBadge status={bill.status === 'FINALIZED' ? 'Finalized' : 'Current month'} /><small>{period}</small></Card><Card><span>Paid</span><h2>{moneyLabel(bill.paid)}</h2><small>Recorded by Meal Manager</small></Card><Card><span>Remaining</span><h2>{moneyLabel(currentOutstanding)}</h2><small>{bill.credit && Number(bill.credit) > 0 ? `Overpaid: ${moneyLabel(bill.credit)}` : 'Ordered meals'}</small></Card></div>
    {bill.lines.some((line) => line.pricePending) ? <Notice tone="warning" title="Price Pending">Required prices are missing. The complete monthly total will appear after prices are added.</Notice> : bill.status !== 'FINALIZED' ? <Notice title="Current month bill">Submitted daily orders are added automatically. After the month ends, a Meal Manager finalizes the bill and records your offline payment.</Notice> : <Notice title="Offline payment">Pay the outstanding amount to a current Meal Manager. The manager will record the received payment here.</Notice>}
    <SectionHeader title="Ordered meal breakdown" /><Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Date</th><th>Meal / item</th><th>Quantity</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>{bill.lines.map((line, index) => <tr key={`${line.orderId}-${index}`}><td>{line.mealDate}</td><td>{line.label}</td><td>{line.quantity}</td><td>{line.pricePending ? 'Price Pending' : moneyLabel(line.unitPrice)}</td><td>{moneyLabel(line.lineTotal)}</td></tr>)}</tbody></table></div>{!bill.lines.length ? <EmptyState title="No meal orders this month" description="Submitted daily orders will appear here automatically." /> : null}</Card>
    <SectionHeader title="Offline payment history" /><Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Date</th><th>Reference</th><th>Recorded by</th><th className="numeric">Amount</th></tr></thead><tbody>{bill.paymentHistory.map((payment) => <tr key={payment.id}><td>{payment.paymentDate}</td><td>{payment.reference || 'Not provided'}</td><td>Meal Manager</td><td className="numeric">{moneyLabel(payment.amount)}</td></tr>)}</tbody></table></div>{!bill.paymentHistory.length ? <EmptyState title="No offline payments recorded" description="Payments received by a Meal Manager will appear here." /> : null}</Card>
    </>}
  </div>
}
