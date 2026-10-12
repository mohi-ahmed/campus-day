import { useState, type FormEvent } from 'react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Select, Skeleton } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { dhakaToday } from '../services/menuApi'
import { stockApi, type StockMealType } from '../services/stockApi'

export default function KitchenIssuePage() {
  const { user } = useDemoUser()
  const [date, setDate] = useState(() => dhakaToday())
  const [mealType, setMealType] = useState<StockMealType>('BREAKFAST')
  const [ingredientId, setIngredientId] = useState('')
  const [quantity, setQuantity] = useState('')
  const [note, setNote] = useState('')
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const { data: ingredients, error: stockError } = useMealQuery(() => stockApi.ingredients(), [revision])
  const { data: issues, loading, error } = useMealQuery(() => stockApi.issues(date, page), [date, page, revision])
  const currentIssues = issues?.date === date && issues.page === page ? issues : null
  const chosen = ingredients?.find((item) => item.id === ingredientId)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!chosen) return
    setSaving(true); setFailure(null)
    try {
      await stockApi.issue(user.id, chosen, date, mealType, quantity, note)
      setQuantity(''); setNote(''); setPage(0); setRevision((value) => value + 1)
      setMessage('Kitchen issue recorded and stock reduced.')
    } catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not record the issue.') }
    finally { setSaving(false) }
  }

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Kitchen issue']} />
    <PageHeader title="Kitchen issue" description="Record the ingredient quantity actually issued for a meal." />
    {message ? <Notice tone="success" title={message} /> : null}
    {failure ? <Notice tone="error" title="Issue failed">{failure}</Notice> : null}
    {stockError ? <Notice tone="error" title="Ingredients unavailable">Please try again.</Notice> : null}
    <form className="form-stack" onSubmit={submit}><Card><SectionHeader title="Record ingredient issue" /><div className="form-grid">
      <Field label="Issue date"><Input type="date" value={date} onChange={(event) => { setDate(event.target.value); setPage(0) }} required /></Field>
      <Field label="Meal"><Select value={mealType} onChange={(event) => setMealType(event.target.value as StockMealType)}><option value="BREAKFAST">Breakfast</option><option value="LUNCH">Lunch</option><option value="DINNER">Dinner</option></Select></Field>
      <Field label="Ingredient"><Select value={ingredientId} onChange={(event) => setIngredientId(event.target.value)} required><option value="">Choose ingredient</option>{ingredients?.map((item) => <option key={item.id} value={item.id}>{item.name} · {Number(item.currentStock)} {item.unit} available</option>)}</Select></Field>
      <Field label={`Quantity${chosen ? ` (${chosen.unit})` : ''}`}><Input type="number" min="0.001" step="0.001" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></Field>
      <Field label="Note (optional)"><Input value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} /></Field>
    </div><div className="form-actions"><Button type="submit" disabled={saving || !chosen}>{saving ? 'Saving…' : 'Record issue'}</Button></div></Card></form>
    <Card className="table-card"><SectionHeader title={`Recorded issues · ${date}`} description="Each row is one ingredient issue. Stock changes are tracked in movement history." />
      {error ? <Notice tone="error" title="Issue history unavailable">Please try again.</Notice> : loading || !currentIssues ? <Skeleton lines={6} /> : !currentIssues.content.length ? <EmptyState title="No issues recorded for this date" description="Recorded ingredient issues will appear here." /> : <div className="responsive-table"><table><thead><tr><th>Meal</th><th>Ingredient</th><th className="numeric">Quantity</th><th>Note</th><th>Recorded</th></tr></thead><tbody>{currentIssues.content.map((issue) => <tr key={issue.id}><td>{issue.mealType}</td><td><strong>{issue.ingredientName}</strong></td><td className="numeric">{Number(issue.quantity)} {issue.unit}</td><td>{issue.note ?? '—'}</td><td>{new Intl.DateTimeFormat('en-BD', { timeZone: 'Asia/Dhaka', dateStyle: 'medium', timeStyle: 'short' }).format(new Date(issue.occurredAt))}</td></tr>)}</tbody></table></div>}
      <div className="table-footer"><span>{currentIssues?.totalElements ?? 0} issue lines</span><div><Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><b>{page + 1}</b><Button variant="secondary" disabled={!currentIssues || page + 1 >= currentIssues.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div></div>
    </Card>
  </div>
}
