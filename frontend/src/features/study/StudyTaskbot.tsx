import { useCallback, useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import {
  AlertCircle,
  Bot,
  CalendarDays,
  Check,
  Clock3,
  Coffee,
  ListTodo,
  RotateCcw,
  SendHorizontal,
  Sparkles,
} from 'lucide-react'
import type { StudyTaskRecord } from './studyTaskApi'
import {
  studyTaskbotApi,
  type TaskbotConversationMessage,
  type TaskbotIntent,
  type TaskbotPlanItem,
  type TaskbotTaskDraft,
  type TaskbotStatusResponse,
} from './studyTaskbotApi'

type UiMessage = TaskbotConversationMessage & {
  id: string
  intent?: TaskbotIntent
}

type FailedRequest = {
  prompt: string
  history: TaskbotConversationMessage[]
}

interface StudyTaskbotProps {
  studentId: string
  onTaskConfirmed: (record: StudyTaskRecord) => void
  onOpenTasks: () => void
}

const quickPrompts = [
  { label: 'Create a task', prompt: 'Help me create a new study task.' },
  { label: 'What is due?', prompt: 'What tasks are due or overdue, and what should I do first?' },
  { label: 'Plan my day', prompt: 'Build a realistic study plan for today from my current tasks and routine.' },
]

const intentLabels: Record<TaskbotIntent, string> = {
  STUDY_STATUS: 'Study status',
  TASK_DRAFT: 'Task draft',
  STUDY_PLAN: 'Study plan',
  RESCHEDULE: 'Reschedule advice',
  GENERAL_STUDY_HELP: 'Study help',
}

const planIcons = {
  STUDY: Sparkles,
  TASK: ListTodo,
  SHORT_BREAK: Coffee,
  LONG_BREAK: Coffee,
}

function messageId() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random()}`
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Taskbot could not complete that request.'
}

function formatDate(value?: string | null) {
  if (!value) return ''
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString('en-BD', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatTime(value?: string | null) {
  if (!value) return ''
  const [hour = '0', minute = '0'] = value.split(':')
  const date = new Date(2000, 0, 1, Number(hour), Number(minute))
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleTimeString('en-BD', { hour: 'numeric', minute: '2-digit' })
}

export function StudyTaskbot({ studentId, onTaskConfirmed, onOpenTasks }: StudyTaskbotProps) {
  const [status, setStatus] = useState<TaskbotStatusResponse>()
  const [statusError, setStatusError] = useState('')
  const [messages, setMessages] = useState<UiMessage[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [failedRequest, setFailedRequest] = useState<FailedRequest>()
  const [taskDraft, setTaskDraft] = useState<TaskbotTaskDraft>()
  const [plan, setPlan] = useState<TaskbotPlanItem[]>([])
  const [confirming, setConfirming] = useState(false)
  const conversationRef = useRef<HTMLDivElement>(null)

  const loadStatus = useCallback(async () => {
    setStatusError('')
    setStatus(undefined)
    try {
      setStatus(await studyTaskbotApi.status())
    } catch (statusFailure) {
      setStatusError(errorMessage(statusFailure))
    }
  }, [])

  useEffect(() => {
    void loadStatus()
  }, [loadStatus])

  useEffect(() => {
    const container = conversationRef.current
    if (!container) return
    container.scrollTo({ top: container.scrollHeight, behavior: busy ? 'smooth' : 'auto' })
  }, [busy, messages, plan, taskDraft])

  const requestReply = async (
    prompt: string,
    history: TaskbotConversationMessage[],
    appendUser: boolean,
  ) => {
    if (busy || !status?.available) return
    setBusy(true)
    setError('')
    setFailedRequest(undefined)
    setTaskDraft(undefined)
    setPlan([])
    if (appendUser) {
      setMessages((current) => [...current, { id: messageId(), role: 'USER', content: prompt }])
    }
    try {
      const response = await studyTaskbotApi.chat(studentId, prompt, history.slice(-10))
      setMessages((current) => [...current, {
        id: messageId(),
        role: 'ASSISTANT',
        content: response.reply,
        intent: response.intent,
      }])
      setTaskDraft(response.taskDraft ?? undefined)
      setPlan(response.plan ?? [])
    } catch (requestFailure) {
      setError(errorMessage(requestFailure))
      setFailedRequest({ prompt, history })
    } finally {
      setBusy(false)
    }
  }

  const sendPrompt = (prompt: string) => {
    const normalized = prompt.trim()
    if (!normalized || busy || !status?.available) return
    const history = messages.map(({ role, content }) => ({ role, content }))
    setInput('')
    void requestReply(normalized, history, true)
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    sendPrompt(input)
  }

  const handleInputKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey) return
    event.preventDefault()
    sendPrompt(input)
  }

  const confirmTask = async () => {
    if (!taskDraft || confirming) return
    setConfirming(true)
    setError('')
    try {
      const record = await studyTaskbotApi.confirmTask(studentId, taskDraft)
      onTaskConfirmed(record)
      setTaskDraft(undefined)
      setMessages((current) => [...current, {
        id: messageId(),
        role: 'ASSISTANT',
        content: `Saved “${record.title}” to your tasks.`,
        intent: 'TASK_DRAFT',
      }])
    } catch (confirmationFailure) {
      setError(errorMessage(confirmationFailure))
    } finally {
      setConfirming(false)
    }
  }

  const providerReady = status?.available === true
  const composerHint = statusError
    ? 'Taskbot backend is unavailable'
    : status && !status.available
      ? 'Gemini API key is not available to the backend'
      : 'Ask about tasks, routine, syllabus, habits, or focus history'

  return <section className="study-taskbot" aria-labelledby="taskbot-title">
    <header className="study-taskbot__header">
      <div className="study-taskbot__identity">
        <span className="study-taskbot__mark" aria-hidden="true"><Bot size={24} /></span>
        <div>
          <span className="study-taskbot__eyebrow">Campus Day assistant</span>
          <h1 id="taskbot-title">Taskbot</h1>
          <p>Plan with your real tasks, routine, syllabus, habits, and focus history.</p>
        </div>
      </div>
      <div className={`study-taskbot__status ${providerReady ? 'is-ready' : statusError || status ? 'is-offline' : 'is-checking'}`}>
        <span aria-hidden="true" />
        <strong>{providerReady ? 'Ready' : statusError ? 'Unavailable' : status ? 'Setup needed' : 'Checking'}</strong>
        {status ? <small>{status.provider} · {status.model}</small> : null}
        {statusError ? <button type="button" onClick={() => { void loadStatus() }}><RotateCcw size={13} />Retry</button> : null}
      </div>
    </header>

    <div className="study-taskbot__prompts" aria-label="Suggested prompts">
      {quickPrompts.map(({ label, prompt }) => <button key={label} type="button" onClick={() => sendPrompt(prompt)} disabled={!providerReady || busy}>
        <Sparkles size={14} />{label}
      </button>)}
      <button type="button" className="study-taskbot__tasks-link" onClick={onOpenTasks}><ListTodo size={14} />Open tasks</button>
    </div>

    <div className="study-taskbot__conversation" ref={conversationRef} aria-live="polite" aria-busy={busy}>
      {messages.length === 0 ? <div className="study-taskbot__empty">
        <span aria-hidden="true"><Sparkles size={21} /></span>
        <div><strong>Start with what you need today</strong><p>I can check your current study data, draft a task for approval, or build a practical plan.</p></div>
      </div> : messages.map((message) => <article className={`study-taskbot__message is-${message.role.toLowerCase()}`} key={message.id}>
        {message.role === 'ASSISTANT' ? <span className="study-taskbot__avatar" aria-hidden="true"><Bot size={16} /></span> : null}
        <div>
          {message.intent ? <small>{intentLabels[message.intent]}</small> : null}
          <p>{message.content}</p>
        </div>
      </article>)}

      {busy ? <div className="study-taskbot__thinking" role="status">
        <span className="study-taskbot__avatar" aria-hidden="true"><Bot size={16} /></span>
        <div><i /><i /><i /><span className="sr-only">Taskbot is thinking</span></div>
      </div> : null}

      {taskDraft ? <TaskDraftPreview draft={taskDraft} confirming={confirming} onConfirm={() => { void confirmTask() }} onDiscard={() => setTaskDraft(undefined)} /> : null}
      {plan.length ? <StudyPlanPreview items={plan} /> : null}

      {error ? <div className="study-taskbot__error" role="alert">
        <AlertCircle size={17} />
        <p>{error}</p>
        {failedRequest ? <button type="button" onClick={() => { void requestReply(failedRequest.prompt, failedRequest.history, false) }} disabled={busy}><RotateCcw size={14} />Retry</button> : null}
      </div> : null}
    </div>

    <form className="study-taskbot__composer" onSubmit={submit}>
      <label className="sr-only" htmlFor="taskbot-input">Ask Taskbot</label>
      <textarea
        id="taskbot-input"
        value={input}
        onChange={(event) => setInput(event.target.value)}
        onKeyDown={handleInputKeyDown}
        placeholder={composerHint}
        maxLength={2000}
        rows={1}
        disabled={!providerReady || busy}
      />
      <button type="submit" aria-label="Send to Taskbot" disabled={!input.trim() || !providerReady || busy}><SendHorizontal size={18} /></button>
      <small>Enter to send · Shift + Enter for a new line</small>
    </form>
  </section>
}

function TaskDraftPreview({ draft, confirming, onConfirm, onDiscard }: {
  draft: TaskbotTaskDraft
  confirming: boolean
  onConfirm: () => void
  onDiscard: () => void
}) {
  const due = [formatDate(draft.dueDate), formatTime(draft.dueTime)].filter(Boolean).join(' at ')
  return <section className="study-taskbot__draft" aria-label="Task draft">
    <div className="study-taskbot__draft-heading">
      <span><ListTodo size={17} /></span>
      <div><small>Review before saving</small><h2>{draft.title}</h2></div>
      <em className={`is-${draft.priority.toLowerCase()}`}>{draft.priority}</em>
    </div>
    <dl>
      <div><dt>Subject</dt><dd>{draft.subject}</dd></div>
      <div><dt>Type</dt><dd>{draft.taskType}</dd></div>
      <div><dt>Due</dt><dd>{due}</dd></div>
      {draft.estimatedMinutes ? <div><dt>Estimate</dt><dd>{draft.estimatedMinutes} min</dd></div> : null}
      {draft.repeat !== 'NONE' ? <div><dt>Repeat</dt><dd>{draft.repeat.toLowerCase()}</dd></div> : null}
    </dl>
    {draft.note ? <p>{draft.note}</p> : null}
    <div className="study-taskbot__draft-actions">
      <button type="button" className="study-outline-button" onClick={onDiscard} disabled={confirming}>Discard</button>
      <button type="button" className="study-primary-button" onClick={onConfirm} disabled={confirming}><Check size={15} />{confirming ? 'Saving' : 'Confirm and save'}</button>
    </div>
  </section>
}

function StudyPlanPreview({ items }: { items: TaskbotPlanItem[] }) {
  return <section className="study-taskbot__plan" aria-label="Suggested study plan">
    <div className="study-taskbot__plan-heading"><CalendarDays size={17} /><div><small>Suggested schedule</small><h2>Your study plan</h2></div></div>
    <ol>
      {items.map((item, index) => {
        const Icon = planIcons[item.type]
        const schedule = [formatDate(item.date), formatTime(item.startTime), item.durationMinutes ? `${item.durationMinutes} min` : ''].filter(Boolean).join(' · ')
        return <li key={`${item.type}-${item.date ?? 'open'}-${item.startTime ?? index}-${item.title}`}>
          <span aria-hidden="true"><Icon size={15} /></span>
          <div><strong>{item.title}</strong><small>{[item.subject, schedule].filter(Boolean).join(' · ')}</small>{item.reason ? <p>{item.reason}</p> : null}</div>
          {item.durationMinutes ? <em><Clock3 size={13} />{item.durationMinutes}m</em> : null}
        </li>
      })}
    </ol>
  </section>
}
