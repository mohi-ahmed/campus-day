import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight, CircleDollarSign, ClipboardCheck, CookingPot, PackagePlus, Printer, ShoppingBasket, Warehouse } from 'lucide-react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Metric, Notice, PageHeader, SearchField, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { cookingPlanApi } from '../services/cookingPlanApi'
import { fractionSheetApi, type FractionMealSelection, type FractionStudentRow } from '../services/fractionSheetApi'
import { dhakaToday } from '../services/menuApi'
import { purchaseApi } from '../services/purchaseApi'
import { stockApi } from '../services/stockApi'

function mealText(choice: FractionMealSelection): string {
  if (choice.kind === 'NOT_EATING') return 'Not eating'
  if (choice.kind === 'FULL_MEAL') return 'Full meal'
  return choice.items.map((item) => `${item.name} × ${item.quantity}`).join(', ')
}

function selectionText(row: FractionStudentRow): string {
  if (row.status === 'CANCELLED') return 'Cancelled'
  return [
    row.breakfast ? 'Breakfast package' : null,
    row.lunch.kind === 'NOT_EATING' ? null : `Lunch: ${mealText(row.lunch)}`,
    row.dinner.kind === 'NOT_EATING' ? null : `Dinner: ${mealText(row.dinner)}`,
  ].filter(Boolean).join(' · ') || 'Not eating'
}

function statusLabel(status: FractionStudentRow['status']): string {
  return { SUBMITTED: 'Ordered', LOCKED: 'Locked', CANCELLED: 'Cancelled', NOT_EATING: 'Not Eating' }[status]
}

export function ManagerDashboardPage() {
  const { user } = useDemoUser()
  const date = useMemo(() => dhakaToday(), [])
  const planQuery = useMealQuery(() => cookingPlanApi.get(date, user.id), [date, user.id])
  const sheetQuery = useMealQuery(() => fractionSheetApi.get(date, user.id), [date, user.id])
  const ingredientsQuery = useMealQuery(() => stockApi.ingredients(), [])
  const suppliersQuery = useMealQuery(() => purchaseApi.allSuppliers(), [])
  const loading = planQuery.loading || sheetQuery.loading || ingredientsQuery.loading || suppliersQuery.loading
  const allUnavailable = [planQuery, sheetQuery, ingredientsQuery, suppliersQuery]
    .every((query) => query.error && !query.data)
  const unavailableSections = [
    planQuery.error ? 'cooking plan' : null,
    sheetQuery.error ? 'Final Year orders' : null,
    ingredientsQuery.error ? 'stock' : null,
    suppliersQuery.error ? 'supplier dues' : null,
  ].filter(Boolean).join(', ')

  const plan = planQuery.data
  const sheet = sheetQuery.data
  const ingredients = ingredientsQuery.data
  const suppliers = suppliersQuery.data
  const supplierDue = suppliers?.reduce((sum, supplier) => sum + Number(supplier.outstanding), 0) ?? null
  const suppliersWithDue = suppliers?.filter((supplier) => Number(supplier.outstanding) > 0).length ?? null
  const zeroStock = ingredients?.filter((ingredient) => Number(ingredient.currentStock) === 0).length ?? null
  const editableOrders = sheet ? sheet.totals.submitted - sheet.totals.locked : null

  return <div className="page-enter"><Breadcrumbs items={['Meal Management', 'Dashboard']} /><PageHeader eyebrow={`Live operations · ${date}`} title="Manager dashboard" description="Today’s operational picture for the assigned Meal Managers." actions={<><StatusBadge status="Active" /><Button variant="secondary" icon={<Printer size={17} />} disabled={!plan} onClick={() => window.print()}>Print cooking brief</Button></>} />
    {loading ? <Skeleton lines={8} /> : allUnavailable ? <Notice tone="error" title="Manager dashboard unavailable">Confirm that the backend is running and this Student is assigned to the current management period.</Notice> : <>
      {unavailableSections ? <Notice tone="warning" title="Some dashboard information is not ready">Unavailable: {unavailableSections}. The available sections are shown below.</Notice> : null}
      <div className="metric-grid">
        <Metric label="Lunch to prepare" value={plan ? String(plan.lunch.totalPackagesOrFullMeals) : '—'} detail={plan ? `${plan.lunch.yearsOneToFourPlanned} baseline + ${plan.lunch.finalYearPackagesOrFullMeals} Final Year` : 'Publish today’s menu to calculate demand'} tone="brand" />
        <Metric label="Final Year orders" value={sheet ? String(sheet.totals.submitted) : '—'} detail={editableOrders === null ? 'Order data unavailable' : `${editableOrders} orders still editable`} tone="info" />
        <Metric label="Stock records" value={ingredients ? String(ingredients.length) : '—'} detail={zeroStock === null ? 'Stock data unavailable' : `${zeroStock} items currently at zero`} tone={zeroStock ? 'warning' : 'success'} />
        <Metric label="Supplier dues" value={supplierDue === null ? '—' : `BDT ${supplierDue.toLocaleString('en-BD', { minimumFractionDigits: 2 })}`} detail={suppliersWithDue === null ? 'Supplier data unavailable' : `Across ${suppliersWithDue} suppliers`} tone={supplierDue ? 'error' : 'success'} />
      </div>
      <div className="manager-grid"><Card><SectionHeader title="Today’s cooking requirement" description="Derived from current absences and Final Year orders" action={<Link className="text-link" to="/meal/manage/cooking-plan">Open plan <ChevronRight size={15} /></Link>} />
        {plan ? <><p>Breakfast: {plan.breakfast.totalPackagesOrFullMeals} · Lunch: {plan.lunch.totalPackagesOrFullMeals} · Dinner: {plan.dinner.totalPackagesOrFullMeals}</p><Notice title="Planning counts are operational.">They do not calculate student bills.</Notice></> : <Notice tone="warning" title="Cooking plan is not ready">Publish today’s menu, then return here to see the cooking requirement.</Notice>}
      </Card><Card><SectionHeader title="Needs attention" /><div className="task-list"><Link to="/meal/manage/prices"><span className="task-icon warning"><CircleDollarSign /></span><span><strong>Review Final Year prices</strong><small>Set prices for published daily menus</small></span><ChevronRight /></Link><Link to="/meal/manage/stock"><span className="task-icon info"><Warehouse /></span><span><strong>Review ingredient stock</strong><small>{zeroStock === null ? 'Stock data unavailable' : `${zeroStock} items currently at zero`}</small></span><ChevronRight /></Link><Link to="/meal/manage/orders"><span className="task-icon info"><ClipboardCheck /></span><span><strong>{editableOrders === null ? 'Review orders' : `${editableOrders} editable orders`}</strong><small>Current configured cutoff has not passed</small></span><ChevronRight /></Link></div></Card></div>
      <Card><SectionHeader title="Operations" description="Continue with today’s meal work." /><div className="quick-links">{[
        ['/meal/manage/fraction', ClipboardCheck, 'Fraction sheet', 'Review Final Year selections'], ['/meal/manage/cooking-plan', CookingPot, 'Cooking plan', 'Prepare kitchen quantities'], ['/meal/manage/kitchen-issue', PackagePlus, 'Kitchen issue', 'Record ingredient issues'], ['/meal/manage/purchases', ShoppingBasket, 'Purchases', 'Add cash or credit purchase'],
      ].map(([to, Icon, title, detail]) => { const IconComponent = Icon as typeof ClipboardCheck; return <Link key={to as string} to={to as string}><IconComponent /><span><strong>{title as string}</strong><small>{detail as string}</small></span><ChevronRight /></Link> })}</div></Card>
    </>}
  </div>
}

export function OperationPage({ kind }: { kind: 'orders' }) {
  void kind
  const { user } = useDemoUser()
  const [date, setDate] = useState(() => dhakaToday())
  const [query, setQuery] = useState('')
  const { data: sheet, loading, error } = useMealQuery(() => fractionSheetApi.get(date, user.id), [date, user.id])
  const rows = useMemo(() => sheet?.students.filter((row) => `${row.name} ${row.studentId}`.toLowerCase().includes(query.toLowerCase())) ?? [], [sheet, query])

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Final Year order overview']} /><PageHeader title="Final Year order overview" description="Monitor submitted, editable, locked and cancelled meal orders." />
    {sheet ? <div className="metric-grid metric-grid--compact"><Metric label="Final Year students" value={String(sheet.totals.finalYearStudents)} detail="Current cohort" /><Metric label="Ordered" value={String(sheet.totals.submitted)} detail="At least one meal" tone="success" /><Metric label="Not Eating" value={String(sheet.totals.notEating)} detail="Default without an order" /><Metric label="Locked" value={String(sheet.totals.locked)} detail="Configured cutoff passed" tone="warning" /></div> : null}
    <div className="toolbar"><SearchField value={query} onChange={setQuery} placeholder="Search Final Year orders" /><Field label="Meal date"><Input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></Field></div>
    {error ? <Notice tone="error" title="Order overview unavailable">Confirm the backend connection and current Meal Manager assignment.</Notice> : loading || !sheet ? <Skeleton lines={8} /> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th className="sticky-col">Student</th><th>Meal date</th><th>Selections</th><th>Order status</th></tr></thead><tbody>{rows.map((row) => <tr key={row.studentId}><td className="sticky-col"><strong>{row.name}</strong><small>{row.studentId}</small></td><td>{sheet.mealDate}</td><td>{selectionText(row)}</td><td><StatusBadge status={statusLabel(row.status)} /></td></tr>)}</tbody></table></div>{rows.length ? <div className="table-footer"><span>Showing {rows.length} of {sheet.totals.finalYearStudents} students</span></div> : <EmptyState title="No matching records" description="Try changing your search or date." />}</Card>}
  </div>
}
