import { useEffect, useState } from 'react'
import { Bell, Check, Clock3, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useDemoUser } from '../app/DemoUserContext'
import {
  studyReminderApi,
  type StudyReminderInbox,
  type StudyReminderRecord,
} from '../features/study/studyReminderApi'
import { IconButton } from './ui'

const EMPTY_INBOX: StudyReminderInbox = {
  unreadCount: 0,
  scheduledCount: 0,
  reminders: [],
}

const REMINDER_DATE_TIME = new Intl.DateTimeFormat('en-BD', {
  timeZone: 'Asia/Dhaka',
  day: 'numeric',
  month: 'short',
  hour: 'numeric',
  minute: '2-digit',
})

function reminderTime(value: string) {
  return REMINDER_DATE_TIME.format(new Date(value))
}

export function StudyNotificationCenter() {
  const { user } = useDemoUser()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [inbox, setInbox] = useState<StudyReminderInbox>(EMPTY_INBOX)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setInbox(EMPTY_INBOX)
    setLoading(true)
    const refresh = async () => {
      try {
        const result = await studyReminderApi.inbox(user.id)
        if (active) {
          setInbox(result)
          setError('')
          setLoading(false)
        }
      } catch (requestError) {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : 'Reminders are unavailable.')
          setLoading(false)
        }
      }
    }
    void refresh()
    const timer = window.setInterval(() => { void refresh() }, 30_000)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [user.id])

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [open])

  const openReminder = async (reminder: StudyReminderRecord) => {
    if (reminder.status === 'DELIVERED') {
      try {
        const updated = await studyReminderApi.markRead(user.id, reminder.id)
        setInbox((current) => ({
          ...current,
          unreadCount: Math.max(0, current.unreadCount - 1),
          reminders: current.reminders.map((item) => item.id === updated.id ? updated : item),
        }))
      } catch (requestError) {
        setError(requestError instanceof Error ? requestError.message : 'Could not mark reminder as read.')
        return
      }
    }
    setOpen(false)
    if (reminder.actionPath) navigate(reminder.actionPath)
  }

  const removeReminder = async (reminder: StudyReminderRecord) => {
    try {
      if (reminder.status === 'SCHEDULED') {
        await studyReminderApi.cancel(user.id, reminder.id)
      } else {
        await studyReminderApi.dismiss(user.id, reminder.id)
      }
      setInbox((current) => ({
        unreadCount: reminder.status === 'DELIVERED'
          ? Math.max(0, current.unreadCount - 1)
          : current.unreadCount,
        scheduledCount: reminder.status === 'SCHEDULED'
          ? Math.max(0, current.scheduledCount - 1)
          : current.scheduledCount,
        reminders: current.reminders.filter((item) => item.id !== reminder.id),
      }))
      setError('')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not update reminder.')
    }
  }

  return <div className="notification-center">
    <IconButton
      label={inbox.unreadCount ? `${inbox.unreadCount} unread notifications` : 'Notifications'}
      aria-expanded={open}
      aria-haspopup="dialog"
      onClick={() => setOpen((current) => !current)}
    >
      <Bell size={19} />
      {inbox.unreadCount ? <span className="notification-count">{Math.min(99, inbox.unreadCount)}</span> : null}
    </IconButton>
    {open ? <section className="notification-panel" role="dialog" aria-label="Study reminders">
      <header>
        <div><strong>Reminders</strong><span>{inbox.unreadCount} unread · {inbox.scheduledCount} upcoming</span></div>
        <button type="button" onClick={() => setOpen(false)} aria-label="Close reminders"><X size={17} /></button>
      </header>
      {error ? <p className="notification-error" role="alert">{error}</p> : null}
      <div className="notification-list">
        {loading ? <div className="notification-empty"><Clock3 size={20} /><strong>Loading reminders</strong></div> : inbox.reminders.length ? inbox.reminders.map((reminder) => <article
          className={`notification-item is-${reminder.status.toLowerCase()}`}
          key={reminder.id}
        >
          <button className="notification-item__content" type="button" onClick={() => { void openReminder(reminder) }}>
            <span className="notification-item__icon">{reminder.status === 'SCHEDULED' ? <Clock3 size={16} /> : <Check size={16} />}</span>
            <span><strong>{reminder.title}</strong><small>{reminder.message}</small><time>{reminderTime(reminder.scheduledAt)}</time></span>
          </button>
          <button className="notification-item__remove" type="button" onClick={() => { void removeReminder(reminder) }} aria-label={reminder.status === 'SCHEDULED' ? `Cancel ${reminder.title}` : `Dismiss ${reminder.title}`}><X size={15} /></button>
        </article>) : <div className="notification-empty"><Bell size={20} /><strong>You're all caught up</strong><span>New study reminders will appear here.</span></div>}
      </div>
    </section> : null}
  </div>
}
