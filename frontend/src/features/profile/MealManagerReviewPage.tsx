import { useEffect, useState, type ReactNode } from 'react'
import { CheckCircle2, Clock3, ShieldCheck, UserCog, XCircle } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../public/PublicLayout'
import {
  mealManagerApplicationApi,
  type MealManagerApplication,
  type MealManagerDecision,
} from './mealManagerApplicationApi'
import './mealManagerReview.css'

export default function MealManagerReviewPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim() ?? ''
  const requestedDecision = searchParams.get('decision') === 'REJECT' ? 'REJECT' : 'APPROVE'
  const [application, setApplication] = useState<MealManagerApplication | null>(null)
  const [loading, setLoading] = useState(true)
  const [deciding, setDeciding] = useState<MealManagerDecision | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) {
      setError('This email link is incomplete.')
      setLoading(false)
      return
    }
    void mealManagerApplicationApi.review(token)
      .then(setApplication)
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : 'This application could not be loaded.'))
      .finally(() => setLoading(false))
  }, [token])

  const decide = async (decision: MealManagerDecision) => {
    setDeciding(decision)
    setError('')
    try {
      setApplication(await mealManagerApplicationApi.decide(token, decision))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The decision could not be saved.')
    } finally {
      setDeciding(null)
    }
  }

  const period = application?.periodStartDate && application.periodEndDate
    ? `${formatDate(application.periodStartDate)} to ${formatDate(application.periodEndDate)}`
    : null
  const expired = application?.status === 'EXPIRED'
  const decided = application?.status === 'APPROVED' || application?.status === 'REJECTED'

  return <AuthLayout>
    <section className="manager-review__aside">
      <span className="manager-review__symbol"><ShieldCheck size={24} /></span>
      <p className="manager-review__label">Secure admin review</p>
      <h1>One request.<br /><span>One clear decision.</span></h1>
      <p>Approve only when the student should receive access to the Meal Management workspace.</p>
    </section>
    <section className="manager-review__panel">
      <div className="manager-review__card">
        {loading ? <div className="manager-review__loading" aria-label="Loading application"><span /><span /><span /></div> : error && !application ? <ReviewMessage icon={<XCircle />} title="Link unavailable" text={error} /> : application ? <>
          <div className="manager-review__heading">
            <span><UserCog size={21} /></span>
            <div><small>Meal Manager application</small><h2>{application.studentName}</h2></div>
          </div>
          <dl className="manager-review__details">
            <div><dt>Student ID</dt><dd>{application.studentId}</dd></div>
            <div><dt>Email</dt><dd>{application.studentEmail}</dd></div>
            {period ? <div><dt>Management period</dt><dd>{period}</dd></div> : null}
          </dl>
          {application.status === 'PENDING' ? <>
            <div className="manager-review__deadline"><Clock3 size={17} /><span>Decision link expires {formatDateTime(application.expiresAt)}</span></div>
            {error ? <p className="cd-form-alert">{error}</p> : null}
            <div className="manager-review__actions">
              <button className={requestedDecision === 'APPROVE' ? 'is-primary' : ''} type="button" disabled={deciding !== null} onClick={() => void decide('APPROVE')}><CheckCircle2 size={17} />{deciding === 'APPROVE' ? 'Approving…' : 'Approve access'}</button>
              <button className={requestedDecision === 'REJECT' ? 'is-danger' : ''} type="button" disabled={deciding !== null} onClick={() => void decide('REJECT')}><XCircle size={17} />{deciding === 'REJECT' ? 'Rejecting…' : 'Reject request'}</button>
            </div>
            <p className="manager-review__note">Your choice is final for this application.</p>
          </> : <ReviewMessage
            icon={application.status === 'APPROVED' ? <CheckCircle2 /> : <XCircle />}
            title={application.status === 'APPROVED' ? 'Access approved' : expired ? 'Link expired' : 'Request rejected'}
            text={application.status === 'APPROVED' ? 'The student now has access to the Meal Management workspace.' : expired ? 'No decision was made within 72 hours, so the request was automatically rejected.' : 'The request was rejected and no access was granted.'}
            success={application.status === 'APPROVED'}
          />}
          {decided || expired ? <Link className="manager-review__home" to="/">Return to Campus Day</Link> : null}
        </> : null}
      </div>
    </section>
  </AuthLayout>
}

function ReviewMessage({ icon, title, text, success = false }: { icon: ReactNode; title: string; text: string; success?: boolean }) {
  return <div className={`manager-review__message${success ? ' is-success' : ''}`}>
    <span>{icon}</span><h2>{title}</h2><p>{text}</p>
  </div>
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-BD', { dateStyle: 'medium', timeZone: 'Asia/Dhaka' }).format(new Date(`${value}T00:00:00+06:00`))
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-BD', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Dhaka' }).format(new Date(value))
}
