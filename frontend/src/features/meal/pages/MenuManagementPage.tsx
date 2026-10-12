import { useMemo, useState, type FormEvent } from 'react'
import { Copy, Pencil, Trash2 } from 'lucide-react'
import { Breadcrumbs, Button, Card, EmptyState, Field, Input, Notice, PageHeader, SectionHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { formatDate } from '../../../lib/format'
import { useMealQuery } from '../hooks/useMealQuery'
import { addDays, dhakaToday, menuApi, type MainMealInput, type MenuInput, type MenuRecord } from '../services/menuApi'

function emptyMenu(date: string): MenuInput {
  const main = (): MainMealInput => ({ description: '', available: true, orderingEnabled: true, fullMealAvailable: true, fullMealDescription: '', items: [] })
  return { mealDate: date, breakfast: { packageDescription: '', available: true, orderingEnabled: true }, lunch: main(), dinner: main() }
}

function MainMealEditor({ title, value, onChange }: { title: string; value: MainMealInput; onChange: (value: MainMealInput) => void }) {
  return <Card><SectionHeader title={title} />
    <div className="form-stack">
      <Field label="Menu description"><Input value={value.description} onChange={(event) => onChange({ ...value, description: event.target.value })} required /></Field>
      <div className="form-grid"><label><input type="checkbox" checked={value.available} onChange={(event) => onChange({ ...value, available: event.target.checked, orderingEnabled: event.target.checked && value.orderingEnabled, fullMealAvailable: event.target.checked && value.fullMealAvailable, items: event.target.checked ? value.items : value.items.map((item) => ({ ...item, available: false })) })} /> Available</label><label><input type="checkbox" checked={value.orderingEnabled} disabled={!value.available} onChange={(event) => onChange({ ...value, orderingEnabled: event.target.checked })} /> Ordering enabled</label><label><input type="checkbox" checked={value.fullMealAvailable} disabled={!value.available} onChange={(event) => onChange({ ...value, fullMealAvailable: event.target.checked })} /> Full meal available</label></div>
      {value.fullMealAvailable ? <Field label="Full meal contents"><Input value={value.fullMealDescription ?? ''} onChange={(event) => onChange({ ...value, fullMealDescription: event.target.value })} required /></Field> : null}
      <SectionHeader title="Individual selectable items" />
      {value.items.map((item, index) => <div className="form-grid" key={item.id ?? index}>
        <Field label={`Item ${index + 1} name`}><Input value={item.name} onChange={(event) => onChange({ ...value, items: value.items.map((current, position) => position === index ? { ...current, name: event.target.value } : current) })} required /></Field>
        <Field label="Bangla name (optional)"><Input value={item.nameBn ?? ''} onChange={(event) => onChange({ ...value, items: value.items.map((current, position) => position === index ? { ...current, nameBn: event.target.value } : current) })} /></Field>
        <Field label="Quantity text (optional)"><Input value={item.quantity ?? ''} onChange={(event) => onChange({ ...value, items: value.items.map((current, position) => position === index ? { ...current, quantity: event.target.value } : current) })} /></Field>
        <label><input type="checkbox" checked={item.available} disabled={!value.available} onChange={(event) => onChange({ ...value, items: value.items.map((current, position) => position === index ? { ...current, available: event.target.checked } : current) })} /> Available</label>
        <Button type="button" variant="secondary" onClick={() => onChange({ ...value, items: value.items.filter((_, position) => position !== index) })}>Remove</Button>
      </div>)}
      <Button type="button" variant="secondary" onClick={() => onChange({ ...value, items: [...value.items, { name: '', available: value.available }] })}>Add item</Button>
    </div>
  </Card>
}

export default function MenuManagementPage() {
  const today = useMemo(() => dhakaToday(), [])
  const [revision, setRevision] = useState(0)
  const { data, loading, error } = useMealQuery(() => menuApi.managed(addDays(today, -7), addDays(today, 83)), [today, revision])
  const [form, setForm] = useState<MenuInput | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingPublished, setEditingPublished] = useState(false)
  const [templateDate, setTemplateDate] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [busyMenuId, setBusyMenuId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)

  const firstAvailableDate = () => {
    const occupied = new Set(data?.map((menu) => menu.mealDate) ?? [])
    for (let offset = 0; offset <= 83; offset += 1) {
      const candidate = addDays(today, offset)
      if (!occupied.has(candidate)) return candidate
    }
    return today
  }
  const startCreate = () => {
    setEditingId(null); setEditingPublished(false); setTemplateDate(null)
    setForm(emptyMenu(firstAvailableDate())); setMessage(null); setFailure(null)
  }
  const startEdit = (menu: MenuRecord) => {
    setEditingId(menu.id)
    setEditingPublished(menu.status === 'PUBLISHED')
    setTemplateDate(null)
    setForm({ mealDate: menu.mealDate, breakfast: menu.breakfast, lunch: menu.lunch, dinner: menu.dinner })
    setMessage(null)
    setFailure(null)
  }
  const startFromPrevious = (menu: MenuRecord) => {
    const copyMeal = (meal: MainMealInput): MainMealInput => ({
      ...meal,
      items: meal.items.map((item) => ({ ...item, id: undefined })),
    })
    setEditingId(null)
    setEditingPublished(false)
    setTemplateDate(menu.mealDate)
    setForm({
      mealDate: firstAvailableDate(),
      breakfast: { ...menu.breakfast },
      lunch: copyMeal(menu.lunch),
      dinner: copyMeal(menu.dinner),
    })
    setMessage(null)
    setFailure(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const save = async (event: FormEvent) => {
    event.preventDefault()
    if (!form) return
    setSaving(true)
    setFailure(null)
    try {
      await (editingId ? menuApi.update(editingId, form) : menuApi.create(form))
      setForm(null)
      setTemplateDate(null)
      setMessage(editingId ? 'Menu updated.' : templateDate ? 'Previous menu copied as a new draft.' : 'Draft menu created.')
      setRevision((value) => value + 1)
    } catch (cause) {
      setFailure(cause instanceof Error ? cause.message : 'Could not save the menu.')
    } finally { setSaving(false) }
  }
  const publish = async (id: string) => {
    setFailure(null); setBusyMenuId(id)
    try { await menuApi.publish(id); setMessage('Menu published.'); setRevision((value) => value + 1) }
    catch (cause) { setFailure(cause instanceof Error ? cause.message : 'Could not publish the menu.') }
    finally { setBusyMenuId(null) }
  }
  const remove = async (menu: MenuRecord) => {
    if (!window.confirm(`Delete the menu for ${formatDate(menu.mealDate)}? This cannot be undone.`)) return
    setFailure(null); setBusyMenuId(menu.id)
    try {
      await menuApi.delete(menu.id)
      if (editingId === menu.id) setForm(null)
      setMessage('Menu deleted.')
      setRevision((value) => value + 1)
    } catch (cause) {
      setFailure(cause instanceof Error ? cause.message : 'Could not delete the menu.')
    } finally { setBusyMenuId(null) }
  }

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Menu management']} /><PageHeader title="Menu management" description="Create and publish daily breakfast, lunch and dinner menus." actions={<Button onClick={startCreate}>Create menu</Button>} />
    {message ? <Notice tone="success" title={message} /> : null}
    {failure ? <Notice tone="error" title="Menu action failed">{failure}</Notice> : null}
    {form ? <form className="form-stack" onSubmit={save}><Card><SectionHeader title={editingId ? 'Edit menu' : templateDate ? 'Use previous menu' : 'New daily menu'} description={templateDate ? `Copied from ${formatDate(templateDate)}. Review the items, choose the new date, then save as a draft.` : editingPublished ? 'Published menu dates stay fixed. Content can be changed until students submit orders.' : undefined} /><Field label="Meal date"><Input type="date" value={form.mealDate} disabled={editingPublished} onChange={(event) => setForm({ ...form, mealDate: event.target.value })} required /></Field></Card>
      <Card><SectionHeader title="Breakfast · complete package" /><div className="form-stack"><Field label="Package contents"><Input value={form.breakfast.packageDescription} onChange={(event) => setForm({ ...form, breakfast: { ...form.breakfast, packageDescription: event.target.value } })} required /></Field><div className="form-grid"><label><input type="checkbox" checked={form.breakfast.available} onChange={(event) => setForm({ ...form, breakfast: { ...form.breakfast, available: event.target.checked, orderingEnabled: event.target.checked && form.breakfast.orderingEnabled } })} /> Available</label><label><input type="checkbox" checked={form.breakfast.orderingEnabled} disabled={!form.breakfast.available} onChange={(event) => setForm({ ...form, breakfast: { ...form.breakfast, orderingEnabled: event.target.checked } })} /> Ordering enabled</label></div></div></Card>
      <MainMealEditor title="Lunch" value={form.lunch} onChange={(lunch) => setForm({ ...form, lunch })} />
      <MainMealEditor title="Dinner" value={form.dinner} onChange={(dinner) => setForm({ ...form, dinner })} />
      <div className="form-actions"><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save menu'}</Button><Button type="button" variant="secondary" onClick={() => setForm(null)}>Cancel</Button></div>
    </form> : null}
    <SectionHeader title="Daily menus" description="Past week and next twelve weeks" />
    {loading ? <Skeleton lines={6} /> : error || !data ? <Notice tone="error" title="Could not load menus">{error ?? 'Please try again.'}</Notice> : data.length === 0 ? <EmptyState title="No menus in this date range" description="Create a daily menu to get started." /> : <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Date</th><th>Breakfast</th><th>Lunch</th><th>Dinner</th><th>Status</th><th>Actions</th></tr></thead><tbody>{data.map((menu) => <tr key={menu.id}><td>{formatDate(menu.mealDate, 'en-BD', { day: 'numeric', month: 'short', year: 'numeric' })}</td><td>{menu.breakfast.packageDescription}</td><td>{menu.lunch.description}</td><td>{menu.dinner.description}</td><td><StatusBadge status={menu.status === 'PUBLISHED' ? 'Published' : 'Draft'} /></td><td><div className="page-actions"><Button variant="secondary" icon={<Copy size={16} />} disabled={busyMenuId === menu.id} onClick={() => startFromPrevious(menu)}>Use as template</Button><Button variant="secondary" icon={<Pencil size={16} />} disabled={busyMenuId === menu.id} onClick={() => startEdit(menu)}>Edit</Button>{menu.status === 'DRAFT' ? <Button disabled={busyMenuId === menu.id} onClick={() => publish(menu.id)}>Publish</Button> : null}<Button variant="danger" icon={<Trash2 size={16} />} disabled={busyMenuId === menu.id} onClick={() => remove(menu)}>Delete</Button></div></td></tr>)}</tbody></table></div></Card>}
  </div>
}
