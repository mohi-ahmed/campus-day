import { useMemo, useState } from 'react'
import { ArrowDownToLine, Printer } from 'lucide-react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SearchField, Select, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { dhakaToday } from '../services/menuApi'
import { fractionSheetApi, type FractionMealSelection, type FractionSheet, type FractionStudentRow } from '../services/fractionSheetApi'

function mealText(choice: FractionMealSelection): string {
  if (choice.kind === 'NOT_EATING') return 'Not eating'
  if (choice.kind === 'FULL_MEAL') return 'Full meal'
  return choice.items.map((item) => `${item.name} × ${item.quantity}`).join(', ')
}

function rowStatus(row: FractionStudentRow): string {
  return { SUBMITTED: 'Ordered', LOCKED: 'Locked', CANCELLED: 'Cancelled', NOT_EATING: 'Not Eating' }[row.status]
}

function mealCell(row: FractionStudentRow, meal: 'breakfast' | 'lunch' | 'dinner'): string {
  if (row.status === 'CANCELLED') return 'Cancelled'
  if (meal === 'breakfast') return row.breakfast ? 'Complete package' : 'Not eating'
  return mealText(row[meal])
}

function exportCsv(sheet: FractionSheet) {
  const quote = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`
  const rows = [
    ['Student ID', 'Student', 'Breakfast', 'Lunch', 'Dinner', 'Status'],
    ...sheet.students.map((row) => [row.studentId, row.name, mealCell(row, 'breakfast'),
      mealCell(row, 'lunch'), mealCell(row, 'dinner'), rowStatus(row)]),
  ]
  const csv = '\uFEFF' + rows.map((row) => row.map(quote).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `fraction-sheet-${sheet.mealDate}.csv`
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export default function FractionSheetPage() {
  const { user } = useDemoUser()
  const [date, setDate] = useState(() => dhakaToday())
  const [query, setQuery] = useState('')
  const [mealFilter, setMealFilter] = useState('all')
  const { data, loading, error } = useMealQuery(() => fractionSheetApi.get(date, user.id), [date, user.id])
  const sheet = data?.mealDate === date ? data : null
  const rows = useMemo(() => sheet?.students.filter((row) => {
    const matchesQuery = `${row.name} ${row.studentId}`.toLowerCase().includes(query.toLowerCase())
    if (!matchesQuery) return false
    if (mealFilter === 'breakfast') return row.status !== 'CANCELLED' && row.breakfast
    if (mealFilter === 'lunch') return row.status !== 'CANCELLED' && row.lunch.kind !== 'NOT_EATING'
    if (mealFilter === 'dinner') return row.status !== 'CANCELLED' && row.dinner.kind !== 'NOT_EATING'
    return true
  }) ?? [], [sheet, query, mealFilter])

  return <div className="page-enter wide-page">
    <Breadcrumbs items={['Meal Management', 'Fraction Sheet']} />
    <PageHeader title="Digital fraction sheet" description="Final Year orders for the selected meal date." actions={<>
      <Button variant="secondary" icon={<ArrowDownToLine size={17} />} disabled={!sheet} onClick={() => sheet && exportCsv(sheet)}>Export CSV</Button>
      <Button variant="secondary" icon={<Printer size={17} />} disabled={!sheet} onClick={() => window.print()}>Print</Button>
    </>} />
    <div className="toolbar"><SearchField value={query} onChange={setQuery} placeholder="Search student or ID" />
      <Field label="Meal date"><Input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></Field>
      <Field label="Show"><Select value={mealFilter} onChange={(event) => setMealFilter(event.target.value)}><option value="all">All students</option><option value="breakfast">Eating breakfast</option><option value="lunch">Eating lunch</option><option value="dinner">Eating dinner</option></Select></Field>
    </div>
    {error ? <Notice tone="error" title="Fraction Sheet unavailable">A matching current Meal Manager record is required to view this sheet.</Notice> : loading || data && !sheet ? <Skeleton lines={8} /> : !sheet ? <Notice tone="error" title="Fraction Sheet unavailable">Please try again.</Notice> : <>
      <div className="summary-strip"><span><b>{sheet.totals.finalYearStudents}</b> Final Year students</span><span><b>{sheet.totals.breakfastPackages}</b> Breakfast packages</span><span><b>{sheet.totals.lunchFullMeals}</b> Lunch full meals</span><span><b>{sheet.totals.dinnerFullMeals}</b> Dinner full meals</span></div>
      <Card className="table-card dense-table"><div className="responsive-table"><table><thead><tr><th className="sticky-col">Student</th><th>Breakfast</th><th>Lunch</th><th>Dinner</th><th>Status</th></tr></thead><tbody>{rows.map((row) => <tr key={row.studentId}><td className="sticky-col"><strong>{row.name}</strong><small>{row.studentId}</small></td><td>{mealCell(row, 'breakfast')}</td><td>{mealCell(row, 'lunch')}</td><td>{mealCell(row, 'dinner')}</td><td><StatusBadge status={rowStatus(row)} /></td></tr>)}</tbody></table></div>
        {rows.length ? <div className="table-footer"><span>Showing {rows.length} of {sheet.totals.finalYearStudents} students</span></div> : <EmptyState title="No matching students" description="Try changing your search or meal filter." />}</Card>
      <Card><h2>Operational totals</h2><div className="summary-strip"><span><b>{sheet.totals.submitted}</b> Submitted</span><span><b>{sheet.totals.cancelled}</b> Cancelled</span><span><b>{sheet.totals.notEating}</b> Not Eating</span><span><b>{sheet.totals.locked}</b> Locked</span></div>
        <div className="detail-grid"><div><h3>Lunch items</h3>{sheet.totals.lunchItems.length ? <ul>{sheet.totals.lunchItems.map((item) => <li key={item.itemId}>{item.name} × {item.quantity}</li>)}</ul> : <p>No individual items ordered.</p>}</div>
          <div><h3>Dinner items</h3>{sheet.totals.dinnerItems.length ? <ul>{sheet.totals.dinnerItems.map((item) => <li key={item.itemId}>{item.name} × {item.quantity}</li>)}</ul> : <p>No individual items ordered.</p>}</div></div>
      </Card>
    </>}
  </div>
}
