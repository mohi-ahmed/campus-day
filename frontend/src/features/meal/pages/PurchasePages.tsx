import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useDemoUser } from '../../../app/DemoUserContext'
import { BackLink } from '../../../components/AppShell'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Metric, Notice, PageHeader, SearchField, SectionHeader, Select, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { dhakaToday } from '../services/menuApi'
import { purchaseApi, type PurchaseInput, type Supplier } from '../services/purchaseApi'
import { stockApi } from '../services/stockApi'

const taka = (value: number) => `BDT ${new Intl.NumberFormat('en-BD', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}`
const errorText = (cause: unknown) => cause instanceof Error ? cause.message : 'The request could not be completed.'
const blankLine = () => ({ ingredientId: '', quantity: '', unitCost: '' })

export function PurchasesPage() {
  const { user } = useDemoUser()
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [requestId, setRequestId] = useState(() => crypto.randomUUID())
  const [supplierId, setSupplierId] = useState('')
  const [date, setDate] = useState(() => dhakaToday())
  const [mode, setMode] = useState<'CASH' | 'CREDIT'>('CASH')
  const [initialPaid, setInitialPaid] = useState('0')
  const [note, setNote] = useState('')
  const [lines, setLines] = useState([blankLine()])
  const [filterSupplier, setFilterSupplier] = useState('')
  const [filterMode, setFilterMode] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [saving, setSaving] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const { data, loading, error } = useMealQuery(() => purchaseApi.purchases(page, filterSupplier, filterMode, from, to), [page, filterSupplier, filterMode, from, to, revision])
  const { data: suppliers } = useMealQuery(() => purchaseApi.allSuppliers(), [revision])
  const { data: ingredients } = useMealQuery(() => stockApi.ingredients(), [])
  const chosen = lines.map((line) => ingredients?.find((item) => item.id === line.ingredientId))
  const estimated = lines.reduce((sum, line) => sum + Number(line.quantity || 0) * Number(line.unitCost || 0), 0)

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setFailure(null)
    try {
      const input: PurchaseInput = {
        requestId, supplierId, purchaseDate: date, paymentMode: mode,
        initialPaid: mode === 'CASH' ? null : initialPaid, note: note || null,
        lines: lines.map((line, index) => ({ ingredientId: line.ingredientId,
          unit: chosen[index]!.unit, quantity: line.quantity, unitCost: line.unitCost })),
      }
      await purchaseApi.createPurchase(user.id, input)
      setShowForm(false); setRequestId(crypto.randomUUID()); setLines([blankLine()]); setNote(''); setInitialPaid('0')
      setPage(0); setRevision((value) => value + 1)
      setMessage('Purchase recorded. Ingredient stock and supplier due were updated.')
    } catch (cause) { setFailure(errorText(cause)) }
    finally { setSaving(false) }
  }

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Purchases']} />
    <PageHeader title="Purchases" description="Record cash or credit purchases and their stock inflow." actions={<Button onClick={() => setShowForm(!showForm)}>Add purchase</Button>} />
    {message ? <Notice tone="success" title={message} /> : null}{failure ? <Notice tone="error" title="Purchase failed">{failure} Retry this form to safely resume the same request.</Notice> : null}
    {showForm ? <form className="form-stack" onSubmit={submit}><Card><SectionHeader title="New purchase" description="All lines are validated before stock is credited." /><div className="form-grid">
      <Field label="Supplier"><Select value={supplierId} onChange={(event) => setSupplierId(event.target.value)} required><option value="">Choose supplier</option>{suppliers?.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}</Select></Field>
      <Field label="Purchase date"><Input type="date" value={date} onChange={(event) => setDate(event.target.value)} required /></Field>
      <Field label="Payment mode"><Select value={mode} onChange={(event) => setMode(event.target.value as 'CASH' | 'CREDIT')}><option value="CASH">Cash (fully paid)</option><option value="CREDIT">Credit</option></Select></Field>
      {mode === 'CREDIT' ? <Field label="Paid at purchase (BDT)"><Input type="number" min="0" max={estimated} step="0.01" value={initialPaid} onChange={(event) => setInitialPaid(event.target.value)} required /></Field> : null}
      <Field label="Note (optional)"><Input value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} /></Field>
    </div></Card><Card><SectionHeader title="Ingredient lines" action={<Button type="button" variant="secondary" onClick={() => setLines([...lines, blankLine()])}>Add line</Button>} />
      {lines.map((line, index) => <div className="form-grid" key={index}>
        <Field label={`Ingredient ${index + 1}`}><Select value={line.ingredientId} onChange={(event) => setLines(lines.map((item, at) => at === index ? { ...item, ingredientId: event.target.value } : item))} required><option value="">Choose ingredient</option>{ingredients?.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.unit}</option>)}</Select></Field>
        <Field label={`Quantity${chosen[index] ? ` (${chosen[index].unit})` : ''}`}><Input type="number" min="0.001" step="0.001" value={line.quantity} onChange={(event) => setLines(lines.map((item, at) => at === index ? { ...item, quantity: event.target.value } : item))} required /></Field>
        <Field label="Unit cost (BDT)"><Input type="number" min="0" step="0.01" value={line.unitCost} onChange={(event) => setLines(lines.map((item, at) => at === index ? { ...item, unitCost: event.target.value } : item))} required /></Field>
        <div><small>Line estimate: {taka(Number(line.quantity || 0) * Number(line.unitCost || 0))}</small>{lines.length > 1 ? <Button type="button" variant="secondary" onClick={() => setLines(lines.filter((_, at) => at !== index))}>Remove</Button> : null}</div>
      </div>)}<Metric label="Estimated total" value={taka(estimated)} detail="Backend recalculates the authoritative total" /></Card>
      <div className="form-actions"><Button type="submit" disabled={saving || !suppliers?.length || !ingredients?.length || chosen.some((item) => !item)}>{saving ? 'Saving…' : 'Record purchase'}</Button><Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button></div>
    </form> : null}
    <div className="toolbar"><Select value={filterSupplier} onChange={(event) => { setFilterSupplier(event.target.value); setPage(0) }}><option value="">All suppliers</option>{suppliers?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select><Select value={filterMode} onChange={(event) => { setFilterMode(event.target.value); setPage(0) }}><option value="">Cash and credit</option><option value="CASH">Cash</option><option value="CREDIT">Credit</option></Select><Input aria-label="From date" type="date" value={from} onChange={(event) => { setFrom(event.target.value); setPage(0) }} /><Input aria-label="To date" type="date" value={to} onChange={(event) => { setTo(event.target.value); setPage(0) }} /></div>
    {error ? <Notice tone="error" title="Purchases unavailable" /> : loading || !data ? <Skeleton lines={7} /> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Purchase</th><th>Supplier</th><th>Date</th><th>Payment</th><th className="numeric">Total</th><th>Status</th></tr></thead><tbody>{data.purchases.map((purchase) => <tr key={purchase.id}><td><Link to={`/meal/manage/purchases/${purchase.id}`}>{purchase.id.slice(-8)}</Link></td><td>{purchase.supplierName}</td><td>{purchase.purchaseDate}</td><td>{purchase.paymentMode}</td><td className="numeric">{taka(purchase.total)}</td><td><StatusBadge status={purchase.status === 'PROCESSING' ? 'Processing' : purchase.initialDue > 0 ? 'Credit' : 'Paid'} /></td></tr>)}</tbody></table></div>
      {!data.purchases.length ? <EmptyState title="No purchases yet" description="Record a purchase to add ingredient stock." /> : null}
      <div className="table-footer"><span>{data.totalElements} purchases</span><div><Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><b>{page + 1}</b><Button variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div></div></Card>}
  </div>
}

export function PurchaseDetailPage() {
  const { purchaseId = '' } = useParams()
  const { user } = useDemoUser()
  const [revision, setRevision] = useState(0)
  const [failure, setFailure] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const { data, loading, error } = useMealQuery(() => purchaseApi.purchase(purchaseId), [purchaseId, revision])
  const resume = async () => {
    setSaving(true); setFailure(null)
    try { await purchaseApi.resumePurchase(user.id, purchaseId); setRevision((value) => value + 1) }
    catch (cause) { setFailure(errorText(cause)) }
    finally { setSaving(false) }
  }
  return <div className="page-enter form-width"><BackLink to="/meal/manage/purchases" label="Purchases" /><Breadcrumbs items={['Meal Management', 'Purchases', 'Purchase details']} />
    {failure ? <Notice tone="error" title="Could not resume purchase">{failure}</Notice> : null}
    {error ? <Notice tone="error" title="Purchase unavailable" /> : loading || !data ? <Skeleton lines={7} /> : <><PageHeader title={`Purchase ${data.id.slice(-8)}`} description={`${data.paymentMode} purchase · ${data.purchaseDate}`} actions={<StatusBadge status={data.status} />} />
      {data.status === 'PROCESSING' ? <Notice tone="warning" title="Purchase processing was interrupted">Stock entries and supplier due can be safely resumed without double credit. <Button disabled={saving} onClick={resume}>{saving ? 'Resuming…' : 'Resume recording'}</Button></Notice> : null}
      <div className="detail-grid"><Card><SectionHeader title="Purchase summary" /><div className="detail-list"><div><span>Supplier</span><strong>{data.supplierName}</strong></div><div><span>Payment mode</span><strong>{data.paymentMode}</strong></div><div><span>Note</span><strong>{data.note ?? '—'}</strong></div></div></Card>
      <Card><SectionHeader title="At purchase" /><Metric label="Total" value={taka(data.total)} /><Metric label="Paid initially" value={taka(data.initialPaid)} /><Metric label="Initial credit due" value={taka(data.initialDue)} detail="Current supplier due is on Suppliers & dues" /></Card></div>
      <Card className="table-card"><SectionHeader title="Ingredient lines" /><div className="responsive-table"><table><thead><tr><th>Ingredient</th><th className="numeric">Quantity</th><th className="numeric">Unit cost</th><th className="numeric">Line total</th></tr></thead><tbody>{data.lines.map((line) => <tr key={line.ingredientId}><td>{line.ingredientName}</td><td className="numeric">{Number(line.quantity)} {line.unit}</td><td className="numeric">{taka(line.unitCost)}</td><td className="numeric">{taka(line.lineTotal)}</td></tr>)}</tbody></table></div></Card>
      <div className="page-actions"><Link className="text-link" to="/meal/manage/suppliers">View supplier dues and payments</Link></div></>}
  </div>
}

export function SuppliersPage() {
  const { user } = useDemoUser()
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<Supplier | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [contact, setContact] = useState('')
  const [note, setNote] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [paymentPage, setPaymentPage] = useState(0)
  const [paymentId, setPaymentId] = useState(() => crypto.randomUUID())
  const [amount, setAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(() => dhakaToday())
  const [paymentNote, setPaymentNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const { data, loading, error } = useMealQuery(() => purchaseApi.suppliers(page), [page, revision])
  const { data: selected } = useMealQuery(() => selectedId ? purchaseApi.supplier(selectedId) : Promise.resolve(null), [selectedId, revision])
  const { data: payments } = useMealQuery(() => selectedId ? purchaseApi.payments(selectedId, paymentPage) : Promise.resolve(null), [selectedId, paymentPage, revision])
  const rows = data?.suppliers.filter((supplier) => supplier.name.toLowerCase().includes(query.toLowerCase())) ?? []

  const start = (supplier: Supplier | null) => {
    setEditing(supplier); setName(supplier?.name ?? ''); setContact(supplier?.contact ?? ''); setNote(supplier?.note ?? '')
    setShowForm(true); setFailure(null)
  }
  const save = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setFailure(null)
    try {
      await (editing ? purchaseApi.updateSupplier(user.id, editing.id, name, contact, note)
        : purchaseApi.createSupplier(user.id, name, contact, note))
      setShowForm(false); setRevision((value) => value + 1); setMessage(editing ? 'Supplier updated.' : 'Supplier created.')
    } catch (cause) { setFailure(errorText(cause)) } finally { setSaving(false) }
  }
  const pay = async (event: FormEvent) => {
    event.preventDefault(); if (!selectedId) return
    setSaving(true); setFailure(null)
    try {
      await purchaseApi.pay(user.id, selectedId, paymentId, amount, paymentDate, paymentNote)
      setPaymentId(crypto.randomUUID()); setAmount(''); setPaymentNote(''); setPaymentPage(0)
      setRevision((value) => value + 1); setMessage('Supplier payment recorded.')
    } catch (cause) { setFailure(errorText(cause)) } finally { setSaving(false) }
  }

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Suppliers & dues']} />
    <PageHeader title="Suppliers & dues" description="Current due follows the supplier purchase and payment ledger." actions={<Button onClick={() => start(null)}>Add supplier</Button>} />
    {message ? <Notice tone="success" title={message} /> : null}{failure ? <Notice tone="error" title="Action failed">{failure}</Notice> : null}
    {showForm ? <form className="form-stack" onSubmit={save}><Card><SectionHeader title={editing ? 'Edit supplier' : 'New supplier'} /><div className="form-grid"><Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required maxLength={100} /></Field><Field label="Contact (optional)"><Input value={contact} onChange={(event) => setContact(event.target.value)} maxLength={50} /></Field><Field label="Note (optional)"><Input value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} /></Field></div></Card><div className="form-actions"><Button type="submit" disabled={saving}>Save supplier</Button><Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button></div></form> : null}
    <div className="toolbar"><SearchField value={query} onChange={setQuery} placeholder="Search this supplier page" /></div>
    {error ? <Notice tone="error" title="Suppliers unavailable">{error}</Notice> : loading || !data ? <Skeleton lines={7} /> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Supplier</th><th>Contact</th><th className="numeric">Current due</th><th>Actions</th></tr></thead><tbody>{rows.map((supplier) => <tr key={supplier.id}><td><strong>{supplier.name}</strong></td><td>{supplier.contact ?? '—'}</td><td className="numeric">{taka(supplier.outstanding)}</td><td><div className="page-actions"><Button variant="secondary" onClick={() => { setSelectedId(supplier.id); setPaymentPage(0) }}>Dues & payments</Button><Button variant="secondary" onClick={() => start(supplier)}>Edit</Button></div></td></tr>)}</tbody></table></div>{!rows.length ? <EmptyState title="No suppliers found" description="Add a supplier or change the search." /> : null}<div className="table-footer"><span>{data.totalElements} suppliers</span><div><Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><b>{page + 1}</b><Button variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div></div></Card>}
    {selectedId ? <Card className="table-card"><SectionHeader title={`${selected?.name ?? 'Supplier'} · dues & payments`} action={<Button variant="secondary" onClick={() => setSelectedId(null)}>Close</Button>} /><Metric label="Current outstanding" value={taka(selected?.outstanding ?? 0)} detail="From the latest supplier account entry" />
      <form className="form-stack" onSubmit={pay}><div className="form-grid"><Field label="Payment amount (BDT)"><Input type="number" min="0.01" step="0.01" max={selected?.outstanding} value={amount} onChange={(event) => setAmount(event.target.value)} required /></Field><Field label="Payment date"><Input type="date" value={paymentDate} onChange={(event) => setPaymentDate(event.target.value)} required /></Field><Field label="Note (optional)"><Input value={paymentNote} onChange={(event) => setPaymentNote(event.target.value)} maxLength={500} /></Field></div><Button type="submit" disabled={saving || !selected || selected.outstanding <= 0}>Record payment</Button></form>
      <SectionHeader title="Payment history" /><div className="responsive-table"><table><thead><tr><th>Date</th><th className="numeric">Amount</th><th className="numeric">Due after</th><th>Note</th></tr></thead><tbody>{payments?.payments.map((payment) => <tr key={payment.id}><td>{payment.date}</td><td className="numeric">{taka(payment.amount)}</td><td className="numeric">{taka(payment.balanceAfter)}</td><td>{payment.note ?? '—'}</td></tr>)}</tbody></table></div>{payments && !payments.payments.length ? <EmptyState title="No payments recorded" description="Credit purchases appear in the current due above." /> : null}<div className="table-footer"><span>{payments?.totalElements ?? 0} payments</span><div><Button variant="secondary" disabled={paymentPage === 0} onClick={() => setPaymentPage(paymentPage - 1)}>Previous</Button><b>{paymentPage + 1}</b><Button variant="secondary" disabled={!payments || paymentPage + 1 >= payments.totalPages} onClick={() => setPaymentPage(paymentPage + 1)}>Next</Button></div></div>
    </Card> : null}
  </div>
}
