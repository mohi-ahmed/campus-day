import { useMemo, useState, type FormEvent } from 'react'
import { Download, FileDown } from 'lucide-react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { billingApi, currentBillingMonth, moneyLabel, type CollectionRow } from '../services/billingApi'
import { exportCollectionsCsv, exportCollectionsPdf } from '../services/collectionExport'
import { dhakaToday } from '../services/menuApi'

function useBillingPeriod() {
  const initial = useMemo(() => currentBillingMonth(), [])
  const [period, setPeriod] = useState(`${initial.year}-${String(initial.month).padStart(2, '0')}`)
  const [year, month] = period.split('-').map(Number)
  return { period, setPeriod, year, month }
}

function Payments({ payments }: { payments: { id: string; paymentDate: string; studentId: string; reference: string | null; amount: string }[] }) {
  return <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Date</th><th>Student record</th><th>Reference</th><th>Amount</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>{payment.paymentDate}</td><td>{payment.studentId}</td><td>{payment.reference || '—'}</td><td>{moneyLabel(payment.amount)}</td></tr>)}</tbody></table></div>{!payments.length ? <EmptyState title="No payments recorded" description="Records for the selected month will appear here." /> : null}</Card>
}

export function MonthlyBillsPage() {
  const { user } = useDemoUser()
  const { period, setPeriod, year, month } = useBillingPeriod()
  const [revision, setRevision] = useState(0)
  const [chargeAmount, setChargeAmount] = useState('')
  const [studentId, setStudentId] = useState('')
  const [selectedStudent, setSelectedStudent] = useState('')
  const [paymentAmount, setPaymentAmount] = useState('')
  const [reference, setReference] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const { data: charges, loading } = useMealQuery(() => billingApi.charges(year, month), [year, month, revision])
  const { data: office } = useMealQuery(() => billingApi.officeSummary(year, month), [year, month, revision])
  const { data: bill } = useMealQuery(() => selectedStudent ? billingApi.bill(selectedStudent, year, month) : Promise.resolve(null), [selectedStudent, year, month, revision])
  const saveCharge = async (event: FormEvent) => { event.preventDefault(); setSaving(true); setError(null); try { await billingApi.setCharge(user.id, year, month, chargeAmount); setChargeAmount(''); setRevision((value) => value + 1) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not save charge') } finally { setSaving(false) } }
  const savePayment = async (event: FormEvent) => { event.preventDefault(); setSaving(true); setError(null); try { await billingApi.officePayment(selectedStudent, year, month, paymentAmount, dhakaToday(), reference); setPaymentAmount(''); setReference(''); setRevision((value) => value + 1) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not record office payment') } finally { setSaving(false) } }
  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Monthly bills']} /><PageHeader title="Monthly bills" description="Set the authority-defined Years 1–4 charge and review College Office payment records." />
    <Field label="Billing month"><Input type="month" value={period} onChange={(event) => setPeriod(event.target.value)} /></Field>
    {error ? <Notice tone="error" title="Billing action failed">{error}</Notice> : null}
    <Card><SectionHeader title="Years 1–4 fixed charge" description="Either current Meal Manager may record the authority-defined amount. Earlier revisions remain in history." />
      <h2>{moneyLabel(charges?.[0]?.amount)}</h2>
      <form className="form-stack" onSubmit={saveCharge}><Field label="Authority-defined amount (BDT)"><Input type="number" min="0" step="0.01" value={chargeAmount} onChange={(event) => setChargeAmount(event.target.value)} required /></Field><Button type="submit" disabled={saving}>Save monthly charge</Button></form>
      {loading ? <Skeleton /> : <div className="responsive-table"><table><thead><tr><th>Revision</th><th>Amount</th><th>Recorded by</th></tr></thead><tbody>{charges?.map((charge) => <tr key={charge.id}><td>{new Date(charge.effectiveAt).toLocaleString('en-BD', { timeZone: 'Asia/Dhaka' })}</td><td>{moneyLabel(charge.amount)}</td><td>{charge.recordedByStudentId}</td></tr>)}</tbody></table></div>}
    </Card>
    <Card><SectionHeader title="Student bill" description="Look up a Years 1–4 Student ID to review its fixed bill and office payments." /><form className="form-stack" onSubmit={(event) => { event.preventDefault(); setSelectedStudent(studentId.trim()) }}><Field label="Student ID"><Input value={studentId} onChange={(event) => setStudentId(event.target.value)} required /></Field><Button type="submit">Review bill</Button></form>
      {bill ? <><div className="bill-summary"><div>Charge: <strong>{moneyLabel(bill.amount)}</strong></div><div>Paid: <strong>{moneyLabel(bill.paid)}</strong></div><div>Remaining: <strong>{moneyLabel(bill.outstanding)}</strong></div></div>{bill.credit && Number(bill.credit) > 0 ? <Notice title="Overpaid amount">{moneyLabel(bill.credit)} remains recorded. Refund or transfer rules are deferred.</Notice> : null}<SectionHeader title="Student payment history" /><Payments payments={bill.paymentHistory} />
        <SectionHeader title="Record College Office payment" /><form className="form-stack" onSubmit={savePayment}><Field label="Received amount (BDT)"><Input type="number" min="0.01" step="0.01" value={paymentAmount} onChange={(event) => setPaymentAmount(event.target.value)} required /></Field><Field label="Office reference (optional)"><Input value={reference} onChange={(event) => setReference(event.target.value)} /></Field><Button type="submit" disabled={saving || bill.model !== 'FIXED'}>Record office payment</Button></form></> : null}
    </Card>
    <SectionHeader title="College Office collection summary" description={`Recorded for ${period}. Office authorization is pending Security implementation.`} /><Card><h2>{moneyLabel(office?.collected)}</h2><Payments payments={office?.payments ?? []} /></Card>
  </div>
}

export function FinalYearCollectionsPage() {
  const { user } = useDemoUser()
  const { period, setPeriod, year, month } = useBillingPeriod()
  const currentMonth = useMemo(() => currentBillingMonth(), [])
  const [amounts, setAmounts] = useState<Record<string, string>>({})
  const [revision, setRevision] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const [savingStudent, setSavingStudent] = useState<string | null>(null)
  const { data: rows, loading, error: loadError } = useMealQuery(
    () => billingApi.collections(user.id, year, month), [user.id, year, month, revision])
  const isPastMonth = year < currentMonth.year || year === currentMonth.year && month < currentMonth.month
  const list = rows ?? []
  const totalBilled = list.reduce((total, row) => total + Number(row.total ?? 0), 0)
  const totalPaid = list.reduce((total, row) => total + Number(row.paid ?? 0), 0)
  const totalRemaining = list.reduce((total, row) => total + Number(row.outstanding ?? 0), 0)

  const finalize = async (row: CollectionRow) => {
    setSavingStudent(row.studentId)
    setActionError(null)
    try {
      await billingApi.finalize(user.id, row.studentId, year, month)
      setRevision((value) => value + 1)
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : 'Could not finalize bill') }
    finally { setSavingStudent(null) }
  }

  const collect = async (row: CollectionRow, amount: string) => {
    setSavingStudent(row.studentId)
    setActionError(null)
    try {
      await billingApi.collect(user.id, row.studentId, year, month, amount, dhakaToday(), '')
      setAmounts((current) => ({ ...current, [row.studentId]: '' }))
      setRevision((value) => value + 1)
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : 'Could not record payment') }
    finally { setSavingStudent(null) }
  }

  const partialPayment = (event: FormEvent, row: CollectionRow) => {
    event.preventDefault()
    const amount = amounts[row.studentId]?.trim()
    if (amount) void collect(row, amount)
  }

  return <div className="page-enter wide-page print-document"><Breadcrumbs items={['Meal Management', 'Final Year collections']} /><PageHeader title="Final Year collections" description="Finalize monthly order bills and record offline payments received by current Meal Managers." actions={<>
    <Button variant="secondary" icon={<Download size={17} />} disabled={!list.length} onClick={() => exportCollectionsCsv(list, year, month)}>Export CSV</Button>
    <Button variant="secondary" icon={<FileDown size={17} />} disabled={!list.length} onClick={() => exportCollectionsPdf(list, year, month)}>Save PDF</Button>
  </>} />
    <Notice title="Offline collection only">Choose a month, then record the amount already received from each student. Current-month bills remain read-only until the month ends.</Notice>
    <div className="print-hidden"><Field label="Billing month"><Input type="month" value={period} onChange={(event) => setPeriod(event.target.value)} /></Field></div>
    {actionError || loadError ? <Notice tone="error" title="Collection action failed">{actionError || loadError}</Notice> : null}
    {loading ? <Skeleton lines={7} /> : <>
      <div className="summary-strip"><span><b>{list.length}</b> Final Year students</span><span><b>{moneyLabel(totalBilled.toFixed(2))}</b> Total bill</span><span><b>{moneyLabel(totalPaid.toFixed(2))}</b> Paid</span><span><b>{moneyLabel(totalRemaining.toFixed(2))}</b> Remaining</span></div>
      <Card className="table-card"><div className="responsive-table"><table className="collection-table"><thead><tr><th>Final Year student</th><th className="numeric">Total bill</th><th className="numeric">Paid</th><th className="numeric">Remaining</th><th>Status</th><th>Record offline payment</th></tr></thead><tbody>
        {list.map((row) => {
          const outstanding = Number(row.outstanding ?? 0)
          const saving = savingStudent === row.studentId
          const paid = row.status === 'FINALIZED' && outstanding === 0
          return <tr key={row.studentId}><td><strong>{row.studentName}</strong><small>{row.studentId}</small></td><td className="numeric">{moneyLabel(row.total)}</td><td className="numeric">{moneyLabel(row.paid)}</td><td className="numeric"><strong>{moneyLabel(row.outstanding)}</strong></td><td><StatusBadge status={row.pricePending ? 'Price Pending' : paid ? 'Paid' : row.status === 'FINALIZED' ? 'Outstanding' : isPastMonth ? 'Not finalized' : 'Current month'} /></td><td>
            {row.pricePending ? <small>Add missing prices before finalizing.</small>
              : row.status !== 'FINALIZED' ? isPastMonth ? <Button variant="secondary" disabled={saving} onClick={() => void finalize(row)}>Finalize bill</Button> : <small>Payment opens after month end.</small>
                : paid ? <strong>Fully paid</strong> : <div className="collection-actions"><Button variant="secondary" disabled={saving} onClick={() => void collect(row, String(row.outstanding))}>Full payment</Button><form onSubmit={(event) => partialPayment(event, row)}><Input aria-label={`Partial payment for ${row.studentName}`} type="number" min="0.01" max={row.outstanding ?? undefined} step="0.01" placeholder="Partial amount" value={amounts[row.studentId] ?? ''} onChange={(event) => setAmounts((current) => ({ ...current, [row.studentId]: event.target.value }))} required /><Button type="submit" disabled={saving}>Record</Button></form></div>}
          </td></tr>
        })}
      </tbody></table></div>{!list.length ? <EmptyState title="No Final Year students found" description="Final Year students will appear here automatically." /> : null}</Card>
    </>}
  </div>
}
