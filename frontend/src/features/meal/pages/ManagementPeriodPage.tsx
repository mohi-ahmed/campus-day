import { useState } from 'react'
import { Breadcrumbs, Button, Card, EmptyState, Notice, PageHeader, Skeleton, StatusBadge } from '../../../components/ui'
import { useDemoUser } from '../../../app/DemoUserContext'
import { part12Api } from '../services/part12Api'
import { formatDate } from '../../../lib/format'
import { useMealQuery } from '../hooks/useMealQuery'
import { managementPeriodService } from '../services/managementPeriodService'
import type { ManagementPeriod } from '../types'

function periodLabel(period: ManagementPeriod) {
  return new Intl.DateTimeFormat('en-BD', { month: 'long', year: 'numeric', timeZone: 'Asia/Dhaka' })
    .format(new Date(Date.UTC(period.year, period.month - 1, 1)))
}

function team(period: ManagementPeriod | null) {
  return period ? period.managers.map((manager) => manager.name).join(' · ') : 'None assigned'
}

export default function ManagementPeriodPage() {
  const { user } = useDemoUser()
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [closing, setClosing] = useState(false)
  const [closeError, setCloseError] = useState<string | null>(null)
  const context = useMealQuery(() => managementPeriodService.context(), [revision])
  const close = async () => { if (!context.data?.current) return; setClosing(true); setCloseError(null); try { await part12Api.close(context.data.current, user.id); setRevision((value) => value + 1) } catch (cause) { setCloseError(cause instanceof Error ? cause.message : 'Could not close period') } finally { setClosing(false) } }

  return <div className="page-enter wide-page"><Breadcrumbs items={['Meal Management', 'Management period']} />
    <PageHeader title="Management period" description="Current team and management history for meal operations." />
    <Notice title="Two assigned managers share access.">Both remain Fourth Year students with equal Meal Manager responsibility during their active period.</Notice>
    <Notice title="Closing and handover are separate records">Review the monthly report and stock handover before closing. Closing uses the existing Management Period status.</Notice>
    {closeError ? <Notice tone="error" title="Close failed">{closeError}</Notice> : null}
    {context.data?.current ? <Button type="button" onClick={close} disabled={closing}>Close current period</Button> : null}
    {context.loading ? <Skeleton lines={3} /> : context.error ? <Notice tone="error" title="We couldn't load the management context.">Please try again.</Notice> : <div className="metric-grid metric-grid--compact">
      <Card><span>Current period</span><h2>{context.data?.current ? periodLabel(context.data.current) : 'No active period'}</h2><small>{team(context.data?.current ?? null)}</small></Card>
      <Card><span>Previous team</span><h2>{context.data?.previous ? periodLabel(context.data.previous) : 'None'}</h2><small>{team(context.data?.previous ?? null)}</small></Card>
      <Card><span>Next team</span><h2>{context.data?.next ? periodLabel(context.data.next) : 'Not assigned'}</h2><small>{team(context.data?.next ?? null)}</small></Card>
    </div>}
    <HistoryList key={`${page}-${revision}`} page={page} onPageChange={setPage} />
  </div>
}

function HistoryList({ page, onPageChange }: { page: number; onPageChange: (page: number) => void }) {
  const history = useMealQuery(() => managementPeriodService.history(page), [page])
  return history.loading ? <Skeleton lines={8} /> : history.error || !history.data ? <Notice tone="error" title="We couldn't load management history.">Please try again.</Notice> : history.data.content.length === 0 ?
      <EmptyState title="No management periods yet" description="Assigned management periods will appear here." /> :
      <Card className="table-card"><div className="responsive-table"><table><thead><tr><th>Period</th><th>Managers</th><th>Dates</th><th>Status</th></tr></thead><tbody>
        {history.data.content.map((period) => <tr key={period.id}><td><strong>{periodLabel(period)}</strong></td><td>{period.managers.map((manager) => <span key={manager.id}>{manager.name} ({manager.studentId})<br /></span>)}</td><td>{formatDate(period.startDate)} – {formatDate(period.endDate)}</td><td><StatusBadge status={period.status === 'ACTIVE' ? 'Active' : 'Closed'} /></td></tr>)}
      </tbody></table></div><div className="table-footer"><span>Showing {history.data.content.length} of {history.data.totalElements} periods</span><div>
        <button aria-label="Previous page" disabled={page === 0} onClick={() => onPageChange(page - 1)}>Previous</button><b>{page + 1}</b><button aria-label="Next page" disabled={page + 1 >= history.data.totalPages} onClick={() => onPageChange(page + 1)}>Next</button>
      </div></div></Card>
}
