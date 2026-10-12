import { useState, type FormEvent } from 'react'
import { useDemoUser } from '../../../app/DemoUserContext'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Metric, Notice, PageHeader, SearchField, SectionHeader, Select, Skeleton, StatusBadge } from '../../../components/ui'
import { useMealQuery } from '../hooks/useMealQuery'
import { stockApi, type IngredientStock, type IngredientUnit } from '../services/stockApi'

type StockAction = 'OPENING' | 'ADJUSTMENT_IN' | 'ADJUSTMENT_OUT'
const units: IngredientUnit[] = ['KG', 'LITER', 'PIECE', 'PACKET']
const quantityText = (value: number) => Number(value).toString()

export default function StockPage() {
  const { user } = useDemoUser()
  const [revision, setRevision] = useState(0)
  const { data, loading, error } = useMealQuery(() => stockApi.ingredients(), [revision])
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<IngredientStock | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [unit, setUnit] = useState<IngredientUnit>('KG')
  const [action, setAction] = useState<StockAction | null>(null)
  const [target, setTarget] = useState<IngredientStock | null>(null)
  const [quantity, setQuantity] = useState('')
  const [note, setNote] = useState('')
  const [historyId, setHistoryId] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const { data: history, error: historyError } = useMealQuery(
    () => historyId ? stockApi.movements(historyId, page) : Promise.resolve(null),
    [historyId, page, revision],
  )
  const activeHistory = history?.ingredientId === historyId && history.page === page ? history : null
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const rows = data?.filter((ingredient) => ingredient.name.toLowerCase().includes(query.toLowerCase())) ?? []

  const startIngredient = (ingredient: IngredientStock | null) => {
    setEditing(ingredient); setName(ingredient?.name ?? ''); setUnit(ingredient?.unit ?? 'KG')
    setShowForm(true); setAction(null); setFailure(null)
  }
  const saveIngredient = async (event: FormEvent) => {
    event.preventDefault(); setSaving(true); setFailure(null)
    try {
      await (editing ? stockApi.updateIngredient(user.id, editing.id, name, unit)
        : stockApi.createIngredient(user.id, name, unit))
      setShowForm(false); setRevision((value) => value + 1)
      setMessage(editing ? 'Ingredient updated.' : 'Ingredient created. Set opening stock when available.')
    } catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not save the ingredient.') }
    finally { setSaving(false) }
  }
  const startStock = (ingredient: IngredientStock, selected: StockAction) => {
    setTarget(ingredient); setAction(selected); setQuantity(''); setNote(''); setShowForm(false); setFailure(null)
  }
  const saveStock = async (event: FormEvent) => {
    event.preventDefault()
    if (!target || !action) return
    setSaving(true); setFailure(null)
    try {
      await (action === 'OPENING' ? stockApi.opening(user.id, target, quantity)
        : stockApi.adjustment(user.id, target, action, quantity, note))
      setAction(null); setRevision((value) => value + 1); setMessage('Stock movement recorded.')
    } catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not record stock movement.') }
    finally { setSaving(false) }
  }

  return <div className="page-enter wide-page">
    <Breadcrumbs items={['Meal Management', 'Ingredients & Stock']} />
    <PageHeader title="Ingredients & stock" description="Current balances come from the latest recorded movement for each ingredient." actions={<Button onClick={() => startIngredient(null)}>Add ingredient</Button>} />
    {message ? <Notice tone="success" title={message} /> : null}
    {failure ? <Notice tone="error" title="Stock action failed">{failure}</Notice> : null}
    {showForm ? <form className="form-stack" onSubmit={saveIngredient}><Card><SectionHeader title={editing ? 'Edit ingredient' : 'New ingredient'} /><div className="form-grid">
      <Field label="Ingredient name"><Input value={name} onChange={(event) => setName(event.target.value)} required maxLength={100} /></Field>
      <Field label="Stock unit"><Select value={unit} disabled={!!editing && !editing.openingAllowed} onChange={(event) => setUnit(event.target.value as IngredientUnit)}>{units.map((item) => <option key={item} value={item}>{item}</option>)}</Select></Field>
    </div>{editing && !editing.openingAllowed ? <small>Unit cannot change after stock movements exist.</small> : null}</Card>
      <div className="form-actions"><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save ingredient'}</Button><Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button></div></form> : null}
    {action && target ? <form className="form-stack" onSubmit={saveStock}><Card><SectionHeader title={`${action === 'OPENING' ? 'Opening stock' : action === 'ADJUSTMENT_IN' ? 'Increase adjustment' : 'Decrease adjustment'} · ${target.name}`} description={`Current stock: ${quantityText(target.currentStock)} ${target.unit}`} />
      <Field label={`Quantity (${target.unit})`}><Input type="number" min="0.001" step="0.001" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></Field>
      {action !== 'OPENING' ? <Field label="Reason"><Input value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} required /></Field> : null}</Card>
      <div className="form-actions"><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Record movement'}</Button><Button type="button" variant="secondary" onClick={() => setAction(null)}>Cancel</Button></div></form> : null}
    <div className="metric-grid metric-grid--compact"><Metric label="Ingredients" value={String(data?.length ?? 0)} detail="Configured stock units" /><Metric label="Out of stock" value={String(data?.filter((item) => item.currentStock === 0).length ?? 0)} detail="No available quantity" /></div>
    <div className="toolbar"><SearchField value={query} onChange={setQuery} placeholder="Search ingredients" /></div>
    {loading ? <Skeleton lines={7} /> : error ? <Notice tone="error" title="Stock unavailable">Please try again.</Notice> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th className="sticky-col">Ingredient</th><th>Unit</th><th className="numeric">Current stock</th><th>Status</th><th>Actions</th></tr></thead><tbody>
      {rows.map((item) => <tr key={item.id}><td className="sticky-col"><strong>{item.name}</strong></td><td>{item.unit}</td><td className="numeric"><strong>{quantityText(item.currentStock)}</strong></td><td><StatusBadge status={item.currentStock > 0 ? 'Available' : 'Out of stock'} tone={item.currentStock > 0 ? 'success' : 'error'} /></td><td><div className="page-actions"><Button type="button" variant="secondary" onClick={() => startIngredient(item)}>Edit</Button>{item.openingAllowed ? <Button type="button" variant="secondary" onClick={() => startStock(item, 'OPENING')}>Opening</Button> : null}<Button type="button" variant="secondary" onClick={() => startStock(item, 'ADJUSTMENT_IN')}>Adjust +</Button><Button type="button" variant="secondary" onClick={() => startStock(item, 'ADJUSTMENT_OUT')}>Adjust −</Button><Button type="button" variant="secondary" onClick={() => { setHistoryId(item.id); setPage(0) }}>Movements</Button></div></td></tr>)}
    </tbody></table></div>{!rows.length ? <EmptyState title="No matching ingredients" description="Add an ingredient or change your search." /> : null}</Card>}
    {historyId ? <Card className="table-card"><SectionHeader title={`Movement history · ${data?.find((item) => item.id === historyId)?.name ?? ''}`} action={<Button variant="secondary" onClick={() => setHistoryId(null)}>Close</Button>} />
      {historyError ? <Notice tone="error" title="Movement history unavailable" /> : !activeHistory ? <Skeleton lines={4} /> : <div className="responsive-table"><table><thead><tr><th>Date</th><th>Type</th><th>Meal</th><th className="numeric">Quantity</th><th className="numeric">Balance after</th><th>Reference / note</th></tr></thead><tbody>{activeHistory.content.map((movement) => <tr key={movement.id}><td>{movement.businessDate}</td><td>{movement.type.replaceAll('_', ' ')}</td><td>{movement.mealType ?? '—'}</td><td className="numeric">{quantityText(movement.quantity)} {movement.unit}</td><td className="numeric">{quantityText(movement.balanceAfter)} {movement.unit}</td><td>{movement.sourceReference ?? movement.note ?? '—'}</td></tr>)}</tbody></table></div>}
      <div className="table-footer"><span>{activeHistory?.totalElements ?? 0} movements</span><div><Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><b>{page + 1}</b><Button variant="secondary" disabled={!activeHistory || page + 1 >= activeHistory.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div></div>
    </Card> : null}
  </div>
}
