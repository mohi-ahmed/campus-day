import { useCallback, useEffect, useMemo, useState, type CSSProperties, type FormEvent } from 'react'
import {
  BarChart3,
  Bell,
  Bot,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Circle,
  Clock3,
  Cloud,
  Download,
  Hourglass,
  ListTodo,
  Pencil,
  Plus,
  RefreshCw,
  Sparkles,
  SunMedium,
  TimerReset,
  Trash2,
  Unplug,
  X,
  type LucideIcon,
} from 'lucide-react'
import {
  addDays,
  dateKey,
  dayNames,
  daysFromToday,
  formatTime,
  fromDateKey,
  minuteValue,
  mondayIndex,
  relativeDate,
  startOfDay,
  subjects,
  type FocusSession,
  type RoutineBlock,
  type RoutineKind,
  type StudyTask,
  type SubjectKey,
  type TaskPriority,
  type TaskType,
} from './studyData'
import { studyRoutineApi, type RoutineDay, type StudyRoutineInput, type StudyRoutineRecord } from './studyRoutineApi'
import { studyTaskApi, type StudyTaskInput, type StudyTaskRecord } from './studyTaskApi'
import { studyFocusApi, type FocusSessionOutcome, type FocusSessionRecord } from './studyFocusApi'
import { studyHabitApi, type StudyHabitProgressRecord } from './studyHabitApi'
import { studyInsightsApi, type StudyInsightsRecord } from './studyInsightsApi'
import { studyPreferencesApi, type StudyPreferencesInput, type StudyPreferencesRecord } from './studyPreferencesApi'
import { studySyllabusApi, type SyllabusSubjectProgressRecord, type SyllabusTopicStatus } from './studySyllabusApi'
import { studyCalendarIntegrationApi, type CalendarIntegrationStatus } from './studyCalendarIntegrationApi'
import { StudyTaskbot } from './StudyTaskbot'
import { useDemoUser } from '../../app/DemoUserContext'
import './study.css'

type StudyView = 'today' | 'routine' | 'tasks' | 'focus' | 'assistant' | 'calendar' | 'insights' | 'reminders'
type ModalType = 'task' | 'block' | null
type CalendarMode = 'week' | 'month'
type TimerMode = 'focus' | 'short-break' | 'long-break'
type TaskDraft = Omit<StudyTask, 'id' | 'source' | 'done'>
type RoutineDraft = Omit<RoutineBlock, 'id'>

const today = startOfDay()
const todayKey = dateKey(today)
const routineDays: RoutineDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

const tabs: Array<{ key: Exclude<StudyView, 'reminders'>; label: string; icon: LucideIcon }> = [
  { key: 'today', label: 'Today', icon: SunMedium },
  { key: 'routine', label: 'Routine', icon: CalendarDays },
  { key: 'tasks', label: 'Tasks', icon: ListTodo },
  { key: 'focus', label: 'Focus', icon: TimerReset },
  { key: 'assistant', label: 'Taskbot', icon: Bot },
  { key: 'calendar', label: 'Calendar', icon: CalendarDays },
  { key: 'insights', label: 'Insights', icon: BarChart3 },
]

const taskTypes: TaskType[] = ['Study', 'Assignment', 'Revision', 'Exam', 'Personal']
const syllabusStatuses: SyllabusTopicStatus[] = ['NOT_STARTED', 'LEARNING', 'REVISION', 'COMPLETED']
const syllabusStatusLabels: Record<SyllabusTopicStatus, string> = {
  NOT_STARTED: 'Not started',
  LEARNING: 'Learning',
  REVISION: 'Revision',
  COMPLETED: 'Done',
}

function cx(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(' ')
}

function subjectStyle(subject: SubjectKey) {
  return { '--study-subject': subjects[subject].color } as CSSProperties
}

function subjectColorStyle(color?: string | null) {
  return { '--study-subject': color || subjects.GEN.color } as CSSProperties
}

function subjectLabel(item: { subject: SubjectKey; subjectLabel?: string }) {
  return item.subjectLabel?.trim() || subjects[item.subject].name
}

function taskTypeLabel(task: Pick<StudyTask, 'type' | 'typeLabel'>) {
  return task.typeLabel?.trim() || task.type
}

function routineKindLabel(block: Pick<RoutineBlock, 'kind' | 'kindLabel'>) {
  return block.kindLabel?.trim() || block.kind
}

function resolveSubject(value: string): SubjectKey {
  const normalized = value.trim().toLowerCase()
  const match = Object.entries(subjects).find(([, subject]) => subject.name.toLowerCase() === normalized)
  return (match?.[0] as SubjectKey | undefined) ?? 'GEN'
}

function resolveTaskType(value: string): TaskType {
  const normalized = value.trim().toLowerCase()
  return taskTypes.find((type) => type.toLowerCase() === normalized) ?? 'Study'
}

function resolveRoutineKind(value: string): RoutineKind {
  return value.trim().toLowerCase().includes('class') ? 'Class' : 'Self-study'
}

function routineFromRecord(record: StudyRoutineRecord): RoutineBlock {
  return {
    id: record.id,
    days: record.days.map((day) => routineDays.indexOf(day)).filter((day) => day >= 0),
    start: record.startTime.slice(0, 5),
    end: record.endTime.slice(0, 5),
    subject: resolveSubject(record.subject),
    subjectLabel: record.subject,
    topic: record.topic,
    location: record.location?.trim() || 'TBA',
    kind: resolveRoutineKind(record.blockType),
    kindLabel: record.blockType,
  }
}

function routineInput(block: RoutineDraft): StudyRoutineInput {
  return {
    subject: block.subjectLabel?.trim() || subjects[block.subject].name,
    blockType: block.kindLabel?.trim() || block.kind,
    topic: block.topic.trim(),
    location: block.location.trim() === 'TBA' ? undefined : block.location.trim(),
    days: block.days.map((day) => routineDays[day]).filter((day): day is RoutineDay => Boolean(day)),
    startTime: block.start,
    endTime: block.end,
  }
}

function taskFromRecord(record: StudyTaskRecord): StudyTask {
  const subject = resolveSubject(record.subject)
  const type = resolveTaskType(record.taskType)
  const priority: TaskPriority = record.priority === 'HIGH' ? 'High' : record.priority === 'LOW' ? 'Low' : 'Med'
  return {
    id: record.id,
    source: 'user',
    title: record.title,
    subject,
    subjectLabel: record.subject,
    type,
    typeLabel: record.taskType,
    priority,
    due: record.dueDate,
    time: record.dueTime ?? undefined,
    estimate: record.estimatedMinutes ?? undefined,
    repeat: record.repeat === 'NONE' ? undefined : record.repeat.toLowerCase() as 'daily' | 'weekly',
    note: record.note ?? undefined,
    done: record.status === 'COMPLETED',
  }
}

function focusFromRecord(record: FocusSessionRecord): FocusSession {
  const subject = resolveSubject(record.subject)
  return {
    id: record.id,
    date: record.focusDate,
    subject,
    subjectLabel: record.subject,
    minutes: record.focusedMinutes,
    topic: record.taskTitle,
  }
}

function preferencesInput(preferences: StudyPreferencesRecord): StudyPreferencesInput {
  return {
    focusMinutes: preferences.focusMinutes,
    shortBreakMinutes: preferences.shortBreakMinutes,
    longBreakMinutes: preferences.longBreakMinutes,
    dailyFocusGoalMinutes: preferences.dailyFocusGoalMinutes,
    weeklySessionGoal: preferences.weeklySessionGoal,
    topicCompletionGoal: preferences.topicCompletionGoal,
    taskReminders: preferences.taskReminders,
    routineReminders: preferences.routineReminders,
    examReminders: preferences.examReminders,
    reminderLeadMinutes: preferences.reminderLeadMinutes,
    morningDigest: preferences.morningDigest,
    quietHoursEnabled: preferences.quietHoursEnabled,
    dailyReminderLimit: preferences.dailyReminderLimit,
  }
}

function taskInput(task: TaskDraft): StudyTaskInput {
  return {
    title: task.title.trim(),
    subject: task.subjectLabel?.trim() || subjects[task.subject].name,
    taskType: task.typeLabel?.trim() || task.type,
    priority: task.priority === 'High' ? 'HIGH' : task.priority === 'Low' ? 'LOW' : 'MEDIUM',
    dueDate: task.due,
    dueTime: task.time || undefined,
    estimatedMinutes: task.estimate || undefined,
    repeat: task.repeat ? task.repeat.toUpperCase() as 'DAILY' | 'WEEKLY' : 'NONE',
    note: task.note?.trim() || undefined,
  }
}

function taskDraft(task: StudyTask): TaskDraft {
  return {
    title: task.title,
    subject: task.subject,
    subjectLabel: task.subjectLabel,
    type: task.type,
    typeLabel: task.typeLabel,
    priority: task.priority,
    due: task.due,
    time: task.time,
    estimate: task.estimate,
    repeat: task.repeat,
    note: task.note,
  }
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Study request failed'
}

function greeting() {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

function formatStudyDuration(minutes: number) {
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder ? `${hours}h ${remainder}m` : `${hours}h`
}

function weekDates(anchor = today) {
  const monday = addDays(anchor, -mondayIndex(anchor))
  return dayNames.map((_, index) => addDays(monday, index))
}

function itemsForDate(date: string, routine: RoutineBlock[], tasks: StudyTask[]) {
  const day = mondayIndex(fromDateKey(date))
  const blocks = routine.filter((block) => block.days.includes(day)).map((block) => ({ kind: 'block' as const, block }))
  const dueTasks = tasks.filter((task) => task.due === date).map((task) => ({ kind: 'task' as const, task }))
  return [...blocks, ...dueTasks].sort((first, second) => {
    const firstTime = first.kind === 'block' ? first.block.start : first.task.time ?? '23:59'
    const secondTime = second.kind === 'block' ? second.block.start : second.task.time ?? '23:59'
    return firstTime.localeCompare(secondTime)
  })
}

function TimelineStrip({ blocks, showNow = false }: { blocks: RoutineBlock[]; showNow?: boolean }) {
  const startMinute = 7 * 60
  const range = 17 * 60
  const currentMinute = new Date().getHours() * 60 + new Date().getMinutes()
  return <div className="study-timeline" aria-label="Daily schedule overview">
    {blocks.map((block) => {
      const left = Math.max(0, ((minuteValue(block.start) - startMinute) / range) * 100)
      const width = Math.min(100 - left, ((minuteValue(block.end) - minuteValue(block.start)) / range) * 100)
      return <span
        key={block.id}
        className={cx('study-timeline__block', block.kind === 'Class' && 'is-class')}
        style={{ ...subjectStyle(block.subject), left: `${left}%`, width: `${Math.max(width, 2)}%` }}
        title={`${block.topic}, ${formatTime(block.start)} to ${formatTime(block.end)}`}
      />
    })}
    {showNow ? <span className="study-timeline__now" style={{ left: `${Math.min(100, Math.max(0, ((currentMinute - startMinute) / range) * 100))}%` }} /> : null}
  </div>
}

function ProgressMeter({ label, value, goal, unit = '', controls, controlStep = 1, onChange }: { label: string; value: number; goal: number; unit?: string; controls?: boolean; controlStep?: number; onChange?: (amount: number) => void }) {
  const percentage = Math.min(100, goal ? (value / goal) * 100 : 0)
  return <div className="study-meter">
    <div className="study-meter__label"><span>{label}</span><span className="study-meter__value"><strong>{value}</strong> of {goal}{unit ? ` ${unit}` : ''}{controls ? <span className="study-stepper"><button type="button" onClick={() => onChange?.(-1)} aria-label={`Decrease ${label} goal by ${controlStep}${unit ? ` ${unit}` : ''}`} title={`Decrease goal by ${controlStep}${unit ? ` ${unit}` : ''}`}>−{controlStep}</button><button type="button" onClick={() => onChange?.(1)} aria-label={`Increase ${label} goal by ${controlStep}${unit ? ` ${unit}` : ''}`} title={`Increase goal by ${controlStep}${unit ? ` ${unit}` : ''}`}>+{controlStep}</button></span> : null}</span></div>
    <div className="study-meter__track"><span style={{ width: `${percentage}%` }} /></div>
  </div>
}

function StudyItemRow({ item, date = todayKey, onToggle, onStart }: {
  item: ReturnType<typeof itemsForDate>[number]
  date?: string
  onToggle: (id: string) => void
  onStart: (block: RoutineBlock) => void
}) {
  if (item.kind === 'block') {
    const { block } = item
    const now = new Date()
    const currentMinutes = now.getHours() * 60 + now.getMinutes()
    const isToday = date === todayKey
    const isPast = isToday && minuteValue(block.end) < currentMinutes
    const isLive = isToday && minuteValue(block.start) <= currentMinutes && currentMinutes < minuteValue(block.end)
    return <div className={cx('study-row', isPast && 'is-muted', isLive && 'is-live')} style={subjectStyle(block.subject)}>
      <div className="study-row__time"><strong>{formatTime(block.start)}</strong><span>{formatTime(block.end)}</span></div>
      <span className={cx('study-row__bar', block.kind === 'Class' && 'is-class')} />
      <div className="study-row__content"><strong>{block.topic}</strong><span>{subjectLabel(block)} <i /> {routineKindLabel(block)} <i /> {block.location}</span></div>
      {isLive ? <span className="study-now">Now</span> : block.kind === 'Self-study' && !isPast ? <button className="study-outline-button" type="button" onClick={() => onStart(block)}>Start</button> : <span />}
    </div>
  }
  const { task } = item
  return <div className={cx('study-row', task.done && 'is-muted')} style={subjectStyle(task.subject)}>
    <div className="study-row__time"><strong>{formatTime(task.time)}</strong><span>{task.estimate ? `${task.estimate} min` : ''}</span></div>
    <span className="study-row__bar is-task" />
    <div className="study-row__content"><strong>{task.title}</strong><span>{taskTypeLabel(task)} <i /> {subjectLabel(task)}</span></div>
    <button className={cx('study-check', task.done && 'is-checked')} type="button" onClick={() => onToggle(task.id)} aria-label={task.done ? `Mark ${task.title} incomplete` : `Mark ${task.title} complete`}>{task.done ? <Check size={15} /> : null}</button>
  </div>
}

function StudyModal({ type, selectedDay, task, block, onClose, onSaveTask, onSaveBlock }: {
  type: Exclude<ModalType, null>
  selectedDay: number
  task?: StudyTask
  block?: RoutineBlock
  onClose: () => void
  onSaveTask: (task: TaskDraft) => void
  onSaveBlock: (block: RoutineDraft) => void
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    if (type === 'task') {
      const subject = String(data.get('subject')).trim()
      const taskType = String(data.get('type')).trim()
      onSaveTask({
        title: String(data.get('title')).trim(),
        subject: resolveSubject(subject),
        subjectLabel: subject,
        type: resolveTaskType(taskType),
        typeLabel: taskType,
        priority: String(data.get('priority')) as TaskPriority,
        due: String(data.get('due')),
        time: String(data.get('time')) || undefined,
        estimate: Number(data.get('estimate')) || undefined,
        repeat: String(data.get('repeat')) === 'none'
          ? undefined
          : String(data.get('repeat')) as 'daily' | 'weekly',
        note: String(data.get('note')) || undefined,
      })
    } else {
      const days = data.getAll('days').map(Number)
      const subject = String(data.get('subject')).trim()
      const routineType = String(data.get('kind')).trim()
      onSaveBlock({
        subject: resolveSubject(subject),
        subjectLabel: subject,
        kind: resolveRoutineKind(routineType),
        kindLabel: routineType,
        topic: String(data.get('topic')).trim(),
        location: String(data.get('location')) || 'TBA',
        start: String(data.get('start')),
        end: String(data.get('end')),
        days: days.length ? days : [selectedDay],
      })
    }
  }
  return <div className="study-modal" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}>
    <form className="study-modal__panel" onSubmit={submit}>
      <div className="study-modal__header"><div><h2>{type === 'task' ? task ? 'Edit task' : 'New task' : block ? 'Edit routine block' : 'New routine block'}</h2><p>{type === 'task' ? task ? 'Update the task details and deadline.' : 'Add a deadline, revision, or personal task.' : block ? 'Update this recurring study block.' : 'Schedule a class or self-study block.'}</p></div><button className="study-icon-button" type="button" onClick={onClose} aria-label="Close dialog"><X size={20} /></button></div>
      {type === 'task' ? <>
        <label className="study-field"><span>Title</span><input name="title" required autoFocus defaultValue={task?.title} placeholder="What needs doing?" /></label>
        <div className="study-form-grid">
          <label className="study-field"><span>Subject</span><input name="subject" required defaultValue={task ? subjectLabel(task) : ''} placeholder="For example: Data Structures" /></label>
          <label className="study-field"><span>Type</span><input name="type" required defaultValue={task ? taskTypeLabel(task) : ''} placeholder="For example: Assignment" /></label>
          <label className="study-field"><span>Priority</span><select name="priority" defaultValue={task?.priority ?? 'Med'}>{(['High', 'Med', 'Low'] as TaskPriority[]).map((item) => <option key={item}>{item}</option>)}</select></label>
          <label className="study-field"><span>Due date</span><input type="date" name="due" defaultValue={task?.due ?? todayKey} required /></label>
          <label className="study-field"><span>Time</span><input type="time" name="time" defaultValue={task?.time} /></label>
          <label className="study-field"><span>Estimate (minutes)</span><input type="number" name="estimate" min="1" max="1440" step="5" defaultValue={task?.estimate} /></label>
          <label className="study-field"><span>Repeat</span><select name="repeat" defaultValue={task?.repeat ?? 'none'}><option value="none">Does not repeat</option><option value="daily">Daily</option><option value="weekly">Weekly</option></select></label>
        </div>
        <label className="study-field"><span>Note</span><input name="note" defaultValue={task?.note} placeholder="Optional note or link" /></label>
      </> : <>
        <div className="study-form-grid study-form-grid--two">
          <label className="study-field"><span>Subject</span><input name="subject" required autoFocus defaultValue={block ? subjectLabel(block) : ''} placeholder="For example: Advanced Java" /></label>
          <label className="study-field"><span>Type</span><input name="kind" required defaultValue={block ? routineKindLabel(block) : ''} placeholder="For example: Lab class" /></label>
        </div>
        <label className="study-field"><span>Topic</span><input name="topic" required defaultValue={block?.topic} placeholder="For example: Collections framework" /></label>
        <label className="study-field"><span>Location</span><input name="location" defaultValue={block?.location === 'TBA' ? '' : block?.location} placeholder="Library" /></label>
        <div className="study-form-grid study-form-grid--two">
          <label className="study-field"><span>Start</span><input type="time" name="start" defaultValue={block?.start ?? '18:00'} required /></label>
          <label className="study-field"><span>End</span><input type="time" name="end" defaultValue={block?.end ?? '19:30'} required /></label>
        </div>
        <fieldset className="study-day-picker"><legend>Repeat on</legend>{dayNames.map((day, index) => <label key={day}><input type="checkbox" name="days" value={index} defaultChecked={block ? block.days.includes(index) : index === selectedDay} /><span>{day}</span></label>)}</fieldset>
      </>}
      <div className="study-modal__actions"><button className="study-outline-button" type="button" onClick={onClose}>Cancel</button><button className="study-primary-button" type="submit">{type === 'task' && task || type === 'block' && block ? 'Update' : 'Save'}</button></div>
    </form>
  </div>
}

export default function StudyPage() {
  const { user } = useDemoUser()
  const [view, setView] = useState<StudyView>('today')
  const [routine, setRoutine] = useState<RoutineBlock[]>([])
  const [tasks, setTasks] = useState<StudyTask[]>([])
  const [sessions, setSessions] = useState<FocusSession[]>([])
  const [selectedDay, setSelectedDay] = useState(mondayIndex(today))
  const [taskFilter, setTaskFilter] = useState('All')
  const [modal, setModal] = useState<ModalType>(null)
  const [editingTask, setEditingTask] = useState<StudyTask>()
  const [editingBlock, setEditingBlock] = useState<RoutineBlock>()
  const [toast, setToast] = useState('')
  const [focusTaskId, setFocusTaskId] = useState<string>()
  const [focusSubject, setFocusSubject] = useState<SubjectKey>('GEN')
  const [focusMinutes, setFocusMinutes] = useState(25)
  const [timerMode, setTimerMode] = useState<TimerMode>('focus')
  const [secondsLeft, setSecondsLeft] = useState(25 * 60)
  const [timerRunning, setTimerRunning] = useState(false)
  const [calendarMode, setCalendarMode] = useState<CalendarMode>('week')
  const [calendarOffset, setCalendarOffset] = useState(0)
  const [selectedDate, setSelectedDate] = useState(todayKey)
  const [insights, setInsights] = useState<StudyInsightsRecord>()
  const [habitProgress, setHabitProgress] = useState<StudyHabitProgressRecord[]>([])
  const [syllabusProgress, setSyllabusProgress] = useState<SyllabusSubjectProgressRecord[]>([])
  const [studyPreferences, setStudyPreferences] = useState<StudyPreferencesRecord>()
  const [calendarIntegration, setCalendarIntegration] = useState<CalendarIntegrationStatus>()
  const [calendarIntegrationBusy, setCalendarIntegrationBusy] = useState(false)

  const refreshCalendarIntegration = useCallback(async () => {
    const status = await studyCalendarIntegrationApi.status(user.id)
    setCalendarIntegration(status)
  }, [user.id])

  const refreshInsights = useCallback(async () => {
    const from = dateKey(addDays(today, -6))
    const focusFrom = dateKey(addDays(today, -365))
    const [summary, habitRecords, syllabusRecords, preferenceRecord, focusRecords] = await Promise.all([
      studyInsightsApi.summary(user.id, from, todayKey),
      studyHabitApi.progress(user.id, from, todayKey),
      studySyllabusApi.progress(user.id),
      studyPreferencesApi.get(user.id),
      studyFocusApi.list(user.id, focusFrom, todayKey),
    ])
    setInsights(summary)
    setHabitProgress(habitRecords)
    setSyllabusProgress(syllabusRecords)
    setStudyPreferences(preferenceRecord)
    setSessions(focusRecords.map(focusFromRecord).reverse())
  }, [user.id])

  const recordFocusSession = useCallback(async (
    focusedMinutes: number,
    outcome: FocusSessionOutcome,
  ) => {
    if (!focusTaskId) return
    const endedAt = new Date()
    const startedAt = new Date(endedAt.getTime() - focusedMinutes * 60_000)
    try {
      await studyFocusApi.record(user.id, {
        taskId: focusTaskId,
        focusDate: todayKey,
        startedAt: startedAt.toISOString(),
        endedAt: endedAt.toISOString(),
        plannedMinutes: focusMinutes,
        focusedMinutes,
        outcome,
      })
      await refreshInsights()
    } catch (error) {
      setToast(errorMessage(error))
    }
  }, [focusMinutes, focusTaskId, refreshInsights, user.id])

  useEffect(() => {
    let active = true
    setTasks([])
    setFocusTaskId(undefined)
    studyTaskApi.list(user.id)
      .then((records) => {
        if (active) setTasks(records.map(taskFromRecord))
      })
      .catch((error) => {
        if (active) setToast(errorMessage(error))
      })
    return () => { active = false }
  }, [user.id])

  useEffect(() => {
    let active = true
    refreshInsights().catch((error) => {
      if (active) setToast(errorMessage(error))
    })
    return () => { active = false }
  }, [refreshInsights])

  useEffect(() => {
    if (view !== 'insights') return
    refreshInsights().catch((error) => setToast(errorMessage(error)))
  }, [refreshInsights, view])

  useEffect(() => {
    if (view !== 'reminders') return
    let active = true
    studyPreferencesApi.get(user.id)
      .then((result) => {
        if (active) setStudyPreferences(result)
      })
      .catch((error) => {
        if (active) setToast(errorMessage(error))
      })
    return () => { active = false }
  }, [user.id, view])

  useEffect(() => {
    if (view !== 'reminders') return
    let active = true
    studyCalendarIntegrationApi.status(user.id)
      .then((result) => {
        if (active) setCalendarIntegration(result)
      })
      .catch((error) => {
        if (active) setToast(errorMessage(error))
      })
    return () => { active = false }
  }, [user.id, view])

  useEffect(() => {
    const handleCalendarMessage = (event: MessageEvent) => {
      const type = typeof event.data === 'object' && event.data !== null
        ? (event.data as { type?: string }).type
        : undefined
      if (type === 'campus-day-calendar-connected') {
        void refreshCalendarIntegration()
        setToast('Google Calendar connected')
      }
      if (type === 'campus-day-calendar-error') {
        setToast('Google Calendar was not connected')
      }
    }
    window.addEventListener('message', handleCalendarMessage)
    return () => window.removeEventListener('message', handleCalendarMessage)
  }, [refreshCalendarIntegration])

  useEffect(() => {
    let active = true
    setRoutine([])
    studyRoutineApi.list(user.id)
      .then((records) => {
        if (active) setRoutine(records.map(routineFromRecord))
      })
      .catch((error) => {
        if (active) setToast(errorMessage(error))
      })
    return () => { active = false }
  }, [user.id])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(''), 2200)
    return () => window.clearTimeout(timer)
  }, [toast])

  useEffect(() => {
    if (!timerRunning) return
    const timer = window.setInterval(() => {
      setSecondsLeft((current) => {
        if (current <= 1) {
          setTimerRunning(false)
          if (timerMode === 'focus') {
            void recordFocusSession(focusMinutes, 'COMPLETED')
            setToast('Focus session complete')
          } else {
            setToast(timerMode === 'short-break' ? 'Short break complete' : 'Long break complete')
          }
          return 0
        }
        return current - 1
      })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [timerRunning, timerMode, focusMinutes, recordFocusSession])

  const week = useMemo(() => weekDates(today), [])
  const weekKeys = useMemo(() => week.map(dateKey), [week])
  const todayItems = useMemo(() => itemsForDate(todayKey, routine, tasks), [routine, tasks])
  const todayBlocks = routine.filter((block) => block.days.includes(mondayIndex(today)))
  const todayFocusedMinutes = insights?.goals.focusedMinutesToday
    ?? sessions.filter((session) => session.date === todayKey).reduce((total, session) => total + session.minutes, 0)
  const weeklySessions = insights?.goals.sessionsThisWeek
    ?? sessions.filter((session) => weekKeys.includes(session.date)).length
  const completedTopics = insights?.goals.completedTopics ?? 0
  const goals = {
    day: insights?.goals.dailyFocusGoalMinutes ?? 0,
    sessions: insights?.goals.weeklySessionGoal ?? 0,
    topics: insights?.goals.topicCompletionGoal ?? 0,
  }

  const showToast = (message: string) => setToast(message)
  const toggleTask = async (id: string) => {
    const task = tasks.find((item) => item.id === id)
    if (!task) return
    try {
      const record = task.done
        ? await studyTaskApi.reopen(user.id, id)
        : await studyTaskApi.complete(user.id, id)
      setTasks((items) => items.map((item) => item.id === id ? taskFromRecord(record) : item))
    } catch (error) {
      showToast(errorMessage(error))
    }
  }
  const openFocus = () => {
    setTimerMode('focus')
    setSecondsLeft(focusMinutes * 60)
    setTimerRunning(false)
    setView('focus')
  }

  const persistTask = async (draft: TaskDraft, taskId?: string) => {
    try {
      const record = taskId
        ? await studyTaskApi.update(user.id, taskId, taskInput(draft))
        : await studyTaskApi.create(user.id, taskInput(draft))
      const saved = taskFromRecord(record)
      setTasks((items) => taskId
        ? items.map((item) => item.id === taskId ? saved : item)
        : [...items, saved])
      if (!taskId) {
        setFocusTaskId(saved.id)
        setFocusSubject(saved.subject)
      }
      setEditingTask(undefined)
      setModal(null)
      showToast(taskId ? `Updated: ${saved.title}` : `Added: ${saved.title}`)
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const deleteTask = async (task: StudyTask) => {
    if (!window.confirm(`Delete “${task.title}”? This cannot be undone.`)) return
    try {
      await studyTaskApi.remove(user.id, task.id)
      setTasks((items) => items.filter((item) => item.id !== task.id))
      if (focusTaskId === task.id) setFocusTaskId(undefined)
      showToast(`Deleted: ${task.title}`)
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const openNewTask = () => {
    setEditingTask(undefined)
    setEditingBlock(undefined)
    setModal('task')
  }

  const openEditTask = (task: StudyTask) => {
    setEditingTask(task)
    setEditingBlock(undefined)
    setModal('task')
  }

  const closeModal = () => {
    setEditingTask(undefined)
    setEditingBlock(undefined)
    setModal(null)
  }

  const rescheduleTask = (id: string, days: number) => {
    const task = tasks.find((item) => item.id === id)
    if (!task) return
    void persistTask({ ...taskDraft(task), due: dateKey(addDays(today, days)) }, id)
  }
  const persistBlock = async (draft: RoutineDraft, blockId?: string) => {
    try {
      const record = blockId
        ? await studyRoutineApi.update(user.id, blockId, routineInput(draft))
        : await studyRoutineApi.create(user.id, routineInput(draft))
      const saved = routineFromRecord(record)
      setRoutine((items) => blockId
        ? items.map((item) => item.id === blockId ? saved : item)
        : [...items, saved])
      setEditingBlock(undefined)
      setModal(null)
      showToast(blockId ? `Updated: ${saved.topic}` : `Added: ${saved.topic}`)
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const deleteBlock = async (block: RoutineBlock) => {
    if (!window.confirm(`Delete “${block.topic}”? This cannot be undone.`)) return
    try {
      await studyRoutineApi.remove(user.id, block.id)
      setRoutine((items) => items.filter((item) => item.id !== block.id))
      showToast(`Deleted: ${block.topic}`)
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const openNewBlock = () => {
    setEditingTask(undefined)
    setEditingBlock(undefined)
    setModal('block')
  }

  const openEditBlock = (block: RoutineBlock) => {
    setEditingTask(undefined)
    setEditingBlock(block)
    setModal('block')
  }

  const setTimerLength = (minutes: number) => {
    setTimerMode('focus')
    setFocusMinutes(minutes)
    setSecondsLeft(minutes * 60)
    setTimerRunning(false)
  }

  const setTimerModeAndReset = (mode: TimerMode) => {
    const minutes = mode === 'short-break' ? 5 : mode === 'long-break' ? 15 : focusMinutes
    setTimerMode(mode)
    setSecondsLeft(minutes * 60)
    setTimerRunning(false)
  }

  const selectFocusTask = (id: string) => {
    const task = tasks.find((item) => item.id === id && item.source === 'user')
    if (!task) return
    setFocusTaskId(id)
    setFocusSubject(task.subject)
    setTimerModeAndReset('focus')
  }

  const stopAndLog = () => {
    const activeMinutes = timerMode === 'short-break' ? 5 : timerMode === 'long-break' ? 15 : focusMinutes
    const elapsed = Math.floor((activeMinutes * 60 - secondsLeft) / 60)
    setTimerRunning(false)
    if (timerMode === 'focus' && elapsed > 0) {
      void recordFocusSession(elapsed, elapsed === focusMinutes ? 'COMPLETED' : 'STOPPED')
      showToast(`Logged ${elapsed} min`)
    }
    setSecondsLeft(activeMinutes * 60)
  }

  const updateGoal = async (key: 'day' | 'sessions' | 'topics', amount: number) => {
    if (!studyPreferences) return
    const next = preferencesInput(studyPreferences)
    if (key === 'day') {
      next.dailyFocusGoalMinutes = Math.min(1440, Math.max(1, next.dailyFocusGoalMinutes + amount))
    } else if (key === 'sessions') {
      next.weeklySessionGoal = Math.min(100, Math.max(1, next.weeklySessionGoal + amount))
    } else {
      next.topicCompletionGoal = Math.min(1000, Math.max(1, next.topicCompletionGoal + amount))
    }
    try {
      setStudyPreferences(await studyPreferencesApi.update(user.id, next))
      await refreshInsights()
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const updateReminderPreferences = async (changes: Partial<StudyPreferencesInput>) => {
    if (!studyPreferences) return
    const previous = studyPreferences
    const next = {
      ...preferencesInput(studyPreferences),
      ...changes,
    }
    setStudyPreferences({ ...studyPreferences, ...changes })
    try {
      const updated = await studyPreferencesApi.update(user.id, next)
      setStudyPreferences(updated)
      showToast('Reminder settings saved')
    } catch (error) {
      setStudyPreferences(previous)
      showToast(errorMessage(error))
    }
  }

  const toggleHabitCompletion = async (habitId: string, completedOn: string, completed: boolean) => {
    try {
      if (completed) {
        await studyHabitApi.uncomplete(user.id, habitId, completedOn)
      } else {
        await studyHabitApi.complete(user.id, habitId, completedOn)
      }
      await refreshInsights()
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const cycleSyllabusTopic = async (
    subjectId: string,
    topicId: string,
    status: SyllabusTopicStatus,
  ) => {
    const nextStatus = syllabusStatuses[(syllabusStatuses.indexOf(status) + 1) % syllabusStatuses.length]
    try {
      await studySyllabusApi.updateStatus(user.id, subjectId, topicId, nextStatus)
      await refreshInsights()
    } catch (error) {
      showToast(errorMessage(error))
    }
  }

  const connectGoogleCalendar = async () => {
    setCalendarIntegrationBusy(true)
    try {
      const { authorizationUrl } = await studyCalendarIntegrationApi.googleAuthorizationUrl(user.id)
      const popup = window.open(
        authorizationUrl,
        'campus-day-google-calendar',
        'popup=yes,width=560,height=720,resizable=yes,scrollbars=yes',
      )
      if (!popup) throw new Error('Allow pop-ups to connect Google Calendar')
      popup.focus()
    } catch (error) {
      showToast(errorMessage(error))
    } finally {
      setCalendarIntegrationBusy(false)
    }
  }

  const syncGoogleCalendar = async () => {
    setCalendarIntegrationBusy(true)
    try {
      const result = await studyCalendarIntegrationApi.syncGoogle(
        user.id,
        todayKey,
        dateKey(addDays(today, 365)),
      )
      await refreshCalendarIntegration()
      showToast(`Calendar synced: ${result.created} added, ${result.updated} updated`)
    } catch (error) {
      showToast(errorMessage(error))
    } finally {
      setCalendarIntegrationBusy(false)
    }
  }

  const disconnectGoogleCalendar = async () => {
    const confirmed = window.confirm(
      'Disconnect Google Calendar and remove Campus Day events from it?',
    )
    if (!confirmed) return
    setCalendarIntegrationBusy(true)
    try {
      await studyCalendarIntegrationApi.disconnectGoogle(user.id)
      await refreshCalendarIntegration()
      showToast('Google Calendar disconnected')
    } catch (error) {
      showToast(errorMessage(error))
    } finally {
      setCalendarIntegrationBusy(false)
    }
  }

  const downloadAppleCalendar = async () => {
    setCalendarIntegrationBusy(true)
    try {
      await studyCalendarIntegrationApi.downloadAppleCalendar(
        user.id,
        todayKey,
        dateKey(addDays(today, 365)),
      )
      showToast('Apple Calendar file downloaded')
    } catch (error) {
      showToast(errorMessage(error))
    } finally {
      setCalendarIntegrationBusy(false)
    }
  }

  const handleTaskbotTaskConfirmed = (record: StudyTaskRecord) => {
    const savedTask = taskFromRecord(record)
    setTasks((items) => [...items.filter((item) => item.id !== savedTask.id), savedTask])
    setFocusTaskId(savedTask.id)
    setFocusSubject(savedTask.subject)
    showToast(`Added: ${savedTask.title}`)
  }

  const renderView = () => {
    if (view === 'routine') return <RoutineView routine={routine} selectedDay={selectedDay} onSelectDay={setSelectedDay} onAdd={openNewBlock} onEdit={openEditBlock} onDelete={deleteBlock} />
    if (view === 'tasks') return <TasksView tasks={tasks} filter={taskFilter} onFilter={setTaskFilter} onToggle={toggleTask} onAdd={openNewTask} onEdit={openEditTask} onDelete={deleteTask} />
    if (view === 'focus') return <FocusView tasks={tasks.filter((task) => task.source === 'user' && !task.done)} sessions={sessions} selectedTaskId={focusTaskId} mode={timerMode} subject={focusSubject} minutes={focusMinutes} secondsLeft={secondsLeft} running={timerRunning} onSelectTask={selectFocusTask} onCreateTask={openNewTask} onMode={setTimerModeAndReset} onMinutes={setTimerLength} onRun={() => setTimerRunning((current) => !current)} onStop={stopAndLog} />
    if (view === 'calendar') return <CalendarView mode={calendarMode} offset={calendarOffset} selectedDate={selectedDate} routine={routine} tasks={tasks} onMode={(mode) => { setCalendarMode(mode); setCalendarOffset(0) }} onOffset={setCalendarOffset} onSelectDate={setSelectedDate} onToggle={toggleTask} onStart={openFocus} />
    if (view === 'insights') return <InsightsView insights={insights} habits={habitProgress} syllabus={syllabusProgress} onGoal={(key, amount) => { void updateGoal(key, amount) }} onHabit={(habitId, completedOn, completed) => { void toggleHabitCompletion(habitId, completedOn, completed) }} onSyllabus={(subjectId, topicId, status) => { void cycleSyllabusTopic(subjectId, topicId, status) }} />
    if (view === 'reminders') return <RemindersView preferences={studyPreferences} integration={calendarIntegration} busy={calendarIntegrationBusy} onChange={(changes) => { void updateReminderPreferences(changes) }} onConnectGoogle={() => { void connectGoogleCalendar() }} onSyncGoogle={() => { void syncGoogleCalendar() }} onDisconnectGoogle={() => { void disconnectGoogleCalendar() }} onDownloadApple={() => { void downloadAppleCalendar() }} />
    return <TodayView tasks={tasks} items={todayItems} blocks={todayBlocks} focusedMinutes={todayFocusedMinutes} weeklySessions={weeklySessions} completedTopics={completedTopics} goals={goals} onToggle={toggleTask} onStart={openFocus} onOpenTasks={() => setView('tasks')} onReschedule={rescheduleTask} />
  }

  return <div className="study-page">
    <div className="study-toolbar">
      <nav className="study-tabs" aria-label="Study and time management">
        {tabs.map(({ key, label, icon: Icon }) => <button key={key} type="button" className={view === key ? 'is-active' : ''} onClick={() => setView(key)}><Icon size={18} /><span>{label}</span></button>)}
      </nav>
      <button className={cx('study-icon-button', view === 'reminders' && 'is-active')} type="button" onClick={() => setView('reminders')} aria-label="Reminder settings"><Bell size={19} /></button>
    </div>
    <div className="study-view page-enter" hidden={view !== 'assistant'}>
      <StudyTaskbot studentId={user.id} onTaskConfirmed={handleTaskbotTaskConfirmed} onOpenTasks={() => setView('tasks')} />
    </div>
    {view !== 'assistant' ? <div className="study-view page-enter" key={view}>{renderView()}</div> : null}
    {modal ? <StudyModal type={modal} selectedDay={selectedDay} task={modal === 'task' ? editingTask : undefined} block={modal === 'block' ? editingBlock : undefined} onClose={closeModal} onSaveTask={(draft) => { void persistTask(draft, editingTask?.id) }} onSaveBlock={(draft) => { void persistBlock(draft, editingBlock?.id) }} /> : null}
    <div className={cx('study-toast', toast && 'is-visible')} role="status">{toast}</div>
  </div>
}

function TodayView({ tasks, items, blocks, focusedMinutes, weeklySessions, completedTopics, goals, onToggle, onStart, onOpenTasks, onReschedule }: {
  tasks: StudyTask[]
  items: ReturnType<typeof itemsForDate>
  blocks: RoutineBlock[]
  focusedMinutes: number
  weeklySessions: number
  completedTopics: number
  goals: { day: number; sessions: number; topics: number }
  onToggle: (id: string) => void
  onStart: (block: RoutineBlock) => void
  onOpenTasks: () => void
  onReschedule: (id: string, days: number) => void
}) {
  const completed = items.filter((item) => item.kind === 'task' ? item.task.done : minuteValue(item.block.end) < new Date().getHours() * 60 + new Date().getMinutes()).length
  const nextBlock = blocks.find((block) => block.kind === 'Self-study' && minuteValue(block.end) > new Date().getHours() * 60 + new Date().getMinutes())
  const missed = tasks.filter((task) => !task.done && daysFromToday(task.due, today) < 0)
  const countdown = tasks.filter((task) => !task.done && ['Exam', 'Assignment'].includes(task.type) && daysFromToday(task.due, today) >= 0).sort((a, b) => a.due.localeCompare(b.due)).slice(0, 4)
  const featuredTask = countdown[0] ?? tasks.find((task) => !task.done)
  const upcoming = countdown[0]
  const taskProgress = [
    { label: 'Completed', value: tasks.filter((task) => task.done).length, icon: Check, tone: 'mint' },
    { label: 'In progress', value: tasks.filter((task) => !task.done && daysFromToday(task.due, today) >= 0).length, icon: Hourglass, tone: 'sky' },
    { label: 'Overdue', value: missed.length, icon: Bell, tone: 'sand' },
    { label: 'Scheduled', value: tasks.filter((task) => !task.done && Boolean(task.time)).length, icon: Clock3, tone: 'rose' },
  ]
  return <>
    <header className="study-day-header">
      <div className="study-day-header__intro"><span>{greeting()}</span><h1>Study Day</h1></div>
      <time dateTime={todayKey}>{today.toLocaleDateString('en-BD', { weekday: 'short', day: 'numeric', month: 'short' })}</time>
    </header>
    <StudyWeekStrip />
    <div className="study-day-layout">
      <main className="study-day-main">
        {nextBlock ? <section className="study-feature-stack" style={subjectStyle(nextBlock.subject)}>
          <div className="study-feature-stack__back study-feature-stack__back--one" aria-hidden="true" />
          <div className="study-feature-stack__back study-feature-stack__back--two" aria-hidden="true" />
          <article className="study-feature-card">
            <div className="study-feature-card__top"><span className="study-priority-badge"><i />Up next</span><strong>{formatTime(nextBlock.start)}</strong></div>
            <div><h2>{nextBlock.topic}</h2><p>{subjectLabel(nextBlock)} <i /> {nextBlock.location} <i /> {formatTime(nextBlock.start)} to {formatTime(nextBlock.end)}</p></div>
            <button type="button" onClick={() => onStart(nextBlock)}>Start session <ChevronRight size={16} /></button>
          </article>
        </section> : featuredTask ? <section className="study-feature-stack" style={subjectStyle(featuredTask.subject)}>
          <div className="study-feature-stack__back study-feature-stack__back--one" aria-hidden="true" />
          <div className="study-feature-stack__back study-feature-stack__back--two" aria-hidden="true" />
          <article className="study-feature-card">
            <div className="study-feature-card__top"><span className="study-priority-badge"><i />{featuredTask.priority} priority</span><strong>{relativeDate(featuredTask.due, today)}</strong></div>
            <div><h2>{featuredTask.title}</h2><p>{taskTypeLabel(featuredTask)} <i /> {subjectLabel(featuredTask)}{featuredTask.estimate ? <><i />{featuredTask.estimate} min</> : null}</p></div>
            <button type="button" onClick={onOpenTasks}>View task <ChevronRight size={16} /></button>
          </article>
        </section> : <StudyEmpty title="Your study day is open" description="Add a task or routine block to create your first focus card." />}
        {missed.length ? <section className="study-missed"><p><strong>{missed.length} missed.</strong> Nothing was deleted. Pick a new time.</p>{missed.map((task) => <div key={task.id}><span>{task.title}</span><button type="button" onClick={() => onReschedule(task.id, 1)}>Tomorrow</button><button type="button" onClick={() => onReschedule(task.id, Math.max(1, 4 - mondayIndex(today)))}>Friday</button></div>)}</section> : null}
        <section className="study-agenda-card"><div className="study-section-title"><h2>Today's agenda</h2><span>{items.length} items</span></div><div className="study-list">{items.length ? items.map((item) => <StudyItemRow key={`${item.kind}-${item.kind === 'block' ? item.block.id : item.task.id}`} item={item} onToggle={onToggle} onStart={onStart} />) : <StudyEmpty title="Nothing scheduled" description="Add a task or a routine block to plan today." />}</div></section>
      </main>
      <aside className="study-day-aside">
        <section className="study-progress-card"><div className="study-section-title"><h2>Tasks Progress</h2><span>{completed}/{items.length}</span></div><div className="study-progress-grid">{taskProgress.map(({ label, value, icon: Icon, tone }, index) => <div className={`tone-${tone}`} style={{ '--tile-order': index } as CSSProperties} key={label}><span><Icon size={15} />{label}</span><strong>{value}</strong></div>)}</div></section>
        <section className="study-upcoming"><div className="study-section-title"><h2>Upcoming Events</h2><span>{countdown.length} this week</span></div>{upcoming ? <article style={subjectStyle(upcoming.subject)}><div className="study-upcoming__top"><span className="study-priority-badge"><i />{upcoming.priority} priority</span><Sparkles size={17} /></div><h3>{upcoming.title}</h3><p>{upcoming.note || `${taskTypeLabel(upcoming)} for ${subjectLabel(upcoming)}.`}</p><div className="study-upcoming__meta"><span><CalendarDays size={14} />{relativeDate(upcoming.due, today)}</span><span><Clock3 size={14} />{formatTime(upcoming.time)}</span></div></article> : <StudyEmpty title="Clear horizon" description="There are no upcoming exams or assignments." />}</section>
        <section className="study-goals-card"><div className="study-section-title"><h2>Weekly goals</h2><span>{weeklySessions}/{goals.sessions} sessions</span></div><ProgressMeter label="Study time today" value={focusedMinutes} goal={goals.day} unit="min" /><ProgressMeter label="Sessions this week" value={weeklySessions} goal={goals.sessions} /><ProgressMeter label="Topics completed" value={completedTopics} goal={goals.topics} /></section>
      </aside>
    </div>
  </>
}

function StudyWeekStrip() {
  const dates = weekDates(today)
  return <section className="study-week-strip" aria-label="This week">
    <div className="study-week-strip__days">{dates.map((date, index) => {
      const isToday = dateKey(date) === todayKey
      return <span className={isToday ? 'is-today' : ''} key={dateKey(date)}><small>{dayNames[index]}</small><strong>{date.getDate()}</strong></span>
    })}</div>
  </section>
}

function RoutineView({ routine, selectedDay, onSelectDay, onAdd, onEdit, onDelete }: {
  routine: RoutineBlock[]
  selectedDay: number
  onSelectDay: (day: number) => void
  onAdd: () => void
  onEdit: (block: RoutineBlock) => void
  onDelete: (block: RoutineBlock) => void
}) {
  const blocks = routine.filter((block) => block.days.includes(selectedDay)).sort((a, b) => a.start.localeCompare(b.start))
  return <>
    <header className="study-view-header"><h1>Weekly routine</h1><p>Solid blocks are self-study. Striped blocks are classes.</p></header>
    <div className="study-routine-grid">
      <div className="study-week-list">{dayNames.map((day, index) => <button type="button" key={day} className={cx(index === selectedDay && 'is-selected', index === mondayIndex(today) && 'is-today')} onClick={() => onSelectDay(index)}><strong>{day}</strong><TimelineStrip blocks={routine.filter((block) => block.days.includes(index))} /></button>)}</div>
      <div><div className="study-section-title"><h2>{dayNames[selectedDay]}</h2><button className="study-primary-button study-primary-button--small" type="button" onClick={onAdd}><Plus size={15} />Add block</button></div>
        <div className="study-list">{blocks.length ? blocks.map((block) => <div className="study-row" key={block.id} style={subjectStyle(block.subject)}><div className="study-row__time"><strong>{formatTime(block.start)}</strong><span>{formatTime(block.end)}</span></div><span className={cx('study-row__bar', block.kind === 'Class' && 'is-class')} /><div className="study-row__content"><strong>{block.topic}</strong><span>{subjectLabel(block)} <i /> {routineKindLabel(block)} <i /> {block.location} <i /> {block.days.length > 1 ? `Repeats ${block.days.length}x` : 'Weekly'}</span></div><div className="study-row-actions"><button className="study-icon-button study-icon-button--small" type="button" onClick={() => onEdit(block)} aria-label={`Edit ${block.topic}`}><Pencil size={15} /></button><button className="study-icon-button study-icon-button--small is-danger" type="button" onClick={() => onDelete(block)} aria-label={`Delete ${block.topic}`}><Trash2 size={15} /></button></div></div>) : <StudyEmpty title="Free day" description="Add a class or study block when you are ready." />}</div>
      </div>
    </div>
  </>
}

function TasksView({ tasks, filter, onFilter, onToggle, onAdd, onEdit, onDelete }: {
  tasks: StudyTask[]
  filter: string
  onFilter: (filter: string) => void
  onToggle: (id: string) => void
  onAdd: () => void
  onEdit: (task: StudyTask) => void
  onDelete: (task: StudyTask) => void
}) {
  const availableTypes = Array.from(new Set(tasks.map(taskTypeLabel)))
  const filtered = tasks.filter((task) => filter === 'All' || taskTypeLabel(task) === filter)
  const open = filtered.filter((task) => !task.done)
  const groups = [
    { title: 'Missed', tasks: open.filter((task) => daysFromToday(task.due, today) < 0) },
    { title: 'Today', tasks: open.filter((task) => daysFromToday(task.due, today) === 0) },
    { title: 'Upcoming', tasks: open.filter((task) => daysFromToday(task.due, today) > 0).sort((a, b) => a.due.localeCompare(b.due)) },
    { title: 'Done', tasks: filtered.filter((task) => task.done) },
  ]
  return <>
    <header className="study-view-header"><h1>Task planner</h1><p>{tasks.filter((task) => !task.done).length} open</p></header>
    <div className="study-filter-row">{['All', ...availableTypes].map((item) => <button type="button" className={filter === item ? 'is-active' : ''} key={item} onClick={() => onFilter(item)}>{item}</button>)}<button className="study-primary-button study-primary-button--small" type="button" onClick={onAdd}><Plus size={15} />New task</button></div>
    {groups.map((group) => group.tasks.length ? <section className="study-task-group" key={group.title}><div className="study-section-title"><h2>{group.title}</h2><span>{group.tasks.length}</span></div>{group.tasks.map((task) => <TaskPlannerRow key={task.id} task={task} onToggle={onToggle} onEdit={onEdit} onDelete={onDelete} />)}</section> : null)}
    {!filtered.length ? <StudyEmpty title="No matching tasks" description="Change the filter or add a new task." /> : null}
  </>
}

function TaskPlannerRow({ task, onToggle, onEdit, onDelete }: {
  task: StudyTask
  onToggle: (id: string) => void
  onEdit: (task: StudyTask) => void
  onDelete: (task: StudyTask) => void
}) {
  return <div className={cx('study-planner-row', task.done && 'is-muted')} style={subjectStyle(task.subject)}>
    <span className="study-planner-row__bar" />
    <div><strong>{task.title}</strong><p>{taskTypeLabel(task)} <i /> {subjectLabel(task)} <i /> <span className={daysFromToday(task.due, today) < 0 && !task.done ? 'is-late' : ''}>{relativeDate(task.due, today)}{task.time ? ` ${formatTime(task.time)}` : ''}</span>{task.estimate ? <><i /> {task.estimate} min</> : null}</p>{task.note ? <small>{task.note}</small> : null}</div>
    <span className={cx('study-priority', task.priority === 'High' && 'is-high')}>{task.priority}</span>
    <div className="study-task-actions">
      <button className="study-icon-button study-icon-button--small" type="button" onClick={() => onEdit(task)} aria-label={`Edit ${task.title}`}><Pencil size={15} /></button>
      <button className="study-icon-button study-icon-button--small is-danger" type="button" onClick={() => onDelete(task)} aria-label={`Delete ${task.title}`}><Trash2 size={15} /></button>
      <button className={cx('study-check', task.done && 'is-checked')} type="button" onClick={() => onToggle(task.id)} aria-label={task.done ? `Mark ${task.title} incomplete` : `Mark ${task.title} complete`}>{task.done ? <Check size={15} /> : null}</button>
    </div>
  </div>
}

function FocusView({ tasks, sessions, selectedTaskId, mode, subject, minutes, secondsLeft, running, onSelectTask, onCreateTask, onMode, onMinutes, onRun, onStop }: { tasks: StudyTask[]; sessions: FocusSession[]; selectedTaskId?: string; mode: TimerMode; subject: SubjectKey; minutes: number; secondsLeft: number; running: boolean; onSelectTask: (id: string) => void; onCreateTask: () => void; onMode: (mode: TimerMode) => void; onMinutes: (minutes: number) => void; onRun: () => void; onStop: () => void }) {
  const selectedTask = tasks.find((task) => task.id === selectedTaskId)
  const activeMinutes = mode === 'short-break' ? 5 : mode === 'long-break' ? 15 : minutes
  const totalSeconds = activeMinutes * 60
  const progress = Math.max(0, Math.min(1, secondsLeft / totalSeconds))
  const clock = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`
  const timerLabel = mode === 'short-break' ? 'Short break' : mode === 'long-break' ? 'Long break' : selectedTask ? subjectLabel(selectedTask) : 'Choose a task'
  const canStart = mode !== 'focus' || Boolean(selectedTask)
  return <div className="study-focus">
    <header className="study-view-header"><h1>Focus mode</h1><p>Choose a task, focus, then take a real break.</p></header>
    <div className="study-focus__modes" aria-label="Timer mode">
      <button type="button" className={mode === 'focus' ? 'is-active' : ''} onClick={() => onMode('focus')}><strong>Focus</strong><small>{minutes} min</small></button>
      <button type="button" className={mode === 'short-break' ? 'is-active' : ''} onClick={() => onMode('short-break')}><strong>Short break</strong><small>5 min</small></button>
      <button type="button" className={mode === 'long-break' ? 'is-active' : ''} onClick={() => onMode('long-break')}><strong>Long break</strong><small>15 min</small></button>
    </div>
    <div className="study-focus__ring" style={{ '--study-progress': `${progress * 360}deg`, ...subjectStyle(mode === 'focus' ? subject : 'GEN') } as CSSProperties}><div><strong>{clock}</strong><span>{timerLabel}</span></div></div>
    <div className="study-focus__actions"><button className="study-primary-button" type="button" onClick={onRun} disabled={!canStart}>{running ? 'Pause' : secondsLeft < totalSeconds ? 'Resume' : 'Start'}</button>{secondsLeft < totalSeconds ? <button className="study-outline-button" type="button" onClick={onStop}>{mode === 'focus' ? 'Stop and log' : 'Reset break'}</button> : null}</div>
    {mode === 'focus' ? <>
      <div className="study-filter-row study-filter-row--center"><button type="button" className={minutes === 25 ? 'is-active' : ''} onClick={() => onMinutes(25)}>25 min</button><button type="button" className={minutes === 50 ? 'is-active' : ''} onClick={() => onMinutes(50)}>50 min</button><label>Custom <input type="number" min="5" max="180" value={![25, 50].includes(minutes) ? minutes : ''} onChange={(event) => onMinutes(Math.min(180, Math.max(5, Number(event.target.value) || 25)))} aria-label="Custom focus minutes" /></label></div>
      <section className="study-focus-task">
        <div className="study-section-title"><div><h2>Focus task</h2><span>Only tasks you create appear here.</span></div><button className="study-primary-button study-primary-button--small" type="button" onClick={onCreateTask}><Plus size={15} />Create task</button></div>
        {tasks.length ? <>
          <label className="study-field"><span>Task</span><select value={selectedTaskId ?? ''} onChange={(event) => onSelectTask(event.target.value)}><option value="" disabled>Select a task</option>{tasks.map((task) => <option key={task.id} value={task.id}>{task.title}</option>)}</select></label>
          {selectedTask ? <p className="study-focus-task__meta">{taskTypeLabel(selectedTask)} <i /> {subjectLabel(selectedTask)}{selectedTask.due ? <><i /> {relativeDate(selectedTask.due, today)}</> : null}</p> : null}
        </> : <StudyEmpty title="No task selected" description="Create a task first, then start your focus timer." />}
      </section>
    </> : <div className="study-break-note"><strong>{mode === 'short-break' ? 'Take a quick reset' : 'Step away and recharge'}</strong><p>{mode === 'short-break' ? 'Stretch, breathe, or get some water for five minutes.' : 'Use fifteen minutes for a proper rest before your next task.'}</p></div>}
    <div className="study-section-title"><h2>Focus history</h2><span>{sessions.filter((session) => session.date === todayKey).reduce((total, session) => total + session.minutes, 0)} min today</span></div>
    <div className="study-list">{sessions.length ? [...sessions].reverse().slice(0, 6).map((session) => <div className="study-row study-row--history" key={session.id} style={subjectStyle(session.subject)}><div className="study-row__time"><strong>{relativeDate(session.date, today)}</strong></div><span className="study-row__bar" /><div className="study-row__content"><strong>{session.topic}</strong><span>{subjectLabel(session)}</span></div><span className="study-priority">{session.minutes} min</span></div>) : <StudyEmpty title="No focus sessions yet" description="Finish or stop a focus timer to build your history." />}</div>
  </div>
}

function CalendarView({ mode, offset, selectedDate, routine, tasks, onMode, onOffset, onSelectDate, onToggle, onStart }: { mode: CalendarMode; offset: number; selectedDate: string; routine: RoutineBlock[]; tasks: StudyTask[]; onMode: (mode: CalendarMode) => void; onOffset: (offset: number) => void; onSelectDate: (date: string) => void; onToggle: (id: string) => void; onStart: (block: RoutineBlock) => void }) {
  const anchor = mode === 'week' ? addDays(today, offset * 7) : new Date(today.getFullYear(), today.getMonth() + offset, 1)
  const dates = mode === 'week' ? weekDates(anchor) : Array.from({ length: new Date(anchor.getFullYear(), anchor.getMonth() + 1, 0).getDate() }, (_, index) => new Date(anchor.getFullYear(), anchor.getMonth(), index + 1))
  const label = mode === 'week' ? `${dates[0].toLocaleDateString('en-BD', { day: 'numeric', month: 'short' })} - ${dates[6].toLocaleDateString('en-BD', { day: 'numeric', month: 'short' })}` : anchor.toLocaleDateString('en-BD', { month: 'long', year: 'numeric' })
  const selectedItems = itemsForDate(selectedDate, routine, tasks)
  return <>
    <header className="study-view-header"><h1>Calendar</h1></header>
    <div className="study-calendar-controls"><div className="study-filter-row"><button type="button" className={mode === 'week' ? 'is-active' : ''} onClick={() => onMode('week')}>Week</button><button type="button" className={mode === 'month' ? 'is-active' : ''} onClick={() => onMode('month')}>Month</button></div><div><button className="study-icon-button study-icon-button--small" type="button" onClick={() => onOffset(offset - 1)} aria-label="Previous period"><ChevronLeft size={16} /></button><strong>{label}</strong><button className="study-icon-button study-icon-button--small" type="button" onClick={() => onOffset(offset + 1)} aria-label="Next period"><ChevronRight size={16} /></button></div></div>
    <div className={cx('study-calendar-grid', mode === 'month' && 'is-month')}>
      {mode === 'month' ? dayNames.map((day) => <span className="study-calendar-grid__weekday" key={day}>{day}</span>) : null}
      {mode === 'month' ? Array.from({ length: mondayIndex(dates[0]) }, (_, index) => <span key={`blank-${index}`} />) : null}
      {dates.map((date) => {
        const key = dateKey(date)
        const items = itemsForDate(key, routine, tasks)
        return <button type="button" key={key} className={cx(key === selectedDate && 'is-selected', key === todayKey && 'is-today')} onClick={() => onSelectDate(key)}><small>{dayNames[mondayIndex(date)]}</small><strong>{date.getDate()}</strong><span>{items.slice(0, 4).map((item) => { const subject = item.kind === 'block' ? item.block.subject : item.task.subject; return <i key={`${item.kind}-${item.kind === 'block' ? item.block.id : item.task.id}`} style={subjectStyle(subject)} /> })}</span></button>
      })}
    </div>
    <div className="study-section-title"><h2>{fromDateKey(selectedDate).toLocaleDateString('en-BD', { weekday: 'long', day: 'numeric', month: 'long' })}</h2><span>Diamond marks an exam</span></div>
    <div className="study-list">{selectedItems.length ? selectedItems.map((item) => <StudyItemRow key={`${item.kind}-${item.kind === 'block' ? item.block.id : item.task.id}`} item={item} date={selectedDate} onToggle={onToggle} onStart={onStart} />) : <StudyEmpty title="Free day" description="No classes, study blocks, or tasks are scheduled." />}</div>
  </>
}

function InsightsView({ insights, habits, syllabus, onGoal, onHabit, onSyllabus }: {
  insights?: StudyInsightsRecord
  habits: StudyHabitProgressRecord[]
  syllabus: SyllabusSubjectProgressRecord[]
  onGoal: (key: 'day' | 'sessions' | 'topics', amount: number) => void
  onHabit: (habitId: string, completedOn: string, completed: boolean) => void
  onSyllabus: (subjectId: string, topicId: string, status: SyllabusTopicStatus) => void
}) {
  if (!insights) {
    return <><header className="study-view-header"><h1>Insights</h1><p>Your study data will appear here.</p></header><StudyEmpty title="Loading insights" description="Fetching your current study progress." /></>
  }

  const trend = insights.dailyTrend
  const maxDailyMinutes = Math.max(1, ...trend.flatMap((day) => [day.plannedMinutes, day.focusedMinutes]))
  const focusedSubjects = insights.subjects.filter((subject) => subject.focusedMinutes > 0)
  const maxSubjectMinutes = Math.max(1, ...focusedSubjects.map((subject) => subject.focusedMinutes))
  const habitDates = trend.map((day) => day.date)
  const hasFocusActivity = insights.focus.totalSessions > 0
  const activeDayLabel = `${insights.focus.activeDays} active ${insights.focus.activeDays === 1 ? 'day' : 'days'}`

  return <>
    <header className="study-view-header"><h1>Insights</h1><p>{hasFocusActivity ? `${activeDayLabel} in the last ${insights.period.totalDays} days. Keep building your study rhythm.` : `No focus sessions logged in the last ${insights.period.totalDays} days. Start a Focus session to build your insights.`}</p></header>
    <div className="study-insight-summary" aria-label="Study summary">
      <article><span>Focused time</span><strong>{formatStudyDuration(insights.focus.totalMinutes)}</strong><small>Last {insights.period.totalDays} days</small></article>
      <article><span>Focus sessions</span><strong>{insights.focus.totalSessions}</strong><small>{insights.focus.completedSessions} completed</small></article>
      <article><span>Tasks completed</span><strong>{insights.tasks.completedTasks}/{insights.tasks.totalTasks}</strong><small>{Math.round(insights.tasks.completionPercentage)}% completion</small></article>
      <article><span>Current streak</span><strong>{insights.focus.currentStreakDays} {insights.focus.currentStreakDays === 1 ? 'day' : 'days'}</strong><small>Longest: {insights.focus.longestStreakDays} {insights.focus.longestStreakDays === 1 ? 'day' : 'days'}</small></article>
    </div>
    <div className="study-insights-grid">
      <div>
        <div className="study-section-title"><h2>Planned vs focused</h2><span>Last {insights.period.totalDays} days</span></div>
        <div className="study-chart-legend" aria-label="Chart legend"><span><i className="is-planned" />Planned self-study</span><span><i className="is-focused" />Focused time</span></div>
        <div className="study-chart">{trend.map((day) => {
          const date = fromDateKey(day.date)
          const readableDate = date.toLocaleDateString('en-BD', { weekday: 'long', day: 'numeric', month: 'short' })
          return <div key={day.date} role="img" aria-label={`${readableDate}: ${day.plannedMinutes} planned minutes and ${day.focusedMinutes} focused minutes`} title={`${readableDate} — Planned ${day.plannedMinutes} min, focused ${day.focusedMinutes} min`}><span><i style={{ height: `${(day.plannedMinutes / maxDailyMinutes) * 100}%` }} /><i style={{ height: `${(day.focusedMinutes / maxDailyMinutes) * 100}%` }} /></span><small><strong>{date.toLocaleDateString('en-BD', { weekday: 'short' })}</strong><span>P {day.plannedMinutes} · F {day.focusedMinutes}</span></small></div>
        })}</div>
        <div className="study-section-title"><h2>Focused time by subject</h2></div>
        {focusedSubjects.length ? <div className="study-subject-bars">{focusedSubjects.map((subject) => <div key={subject.subject} style={subjectColorStyle(subject.color)}><span><strong>{subject.subject}</strong><small>{formatStudyDuration(subject.focusedMinutes)}</small></span><i><b style={{ width: `${(subject.focusedMinutes / maxSubjectMinutes) * 100}%` }} /></i></div>)}</div> : <StudyEmpty title="No focused subject yet" description="Complete or stop a Focus session linked to a task to see subject-level time here." />}
        <div className="study-section-title"><h2>Habits</h2></div>
        {habits.length ? <><p className="study-section-copy">Select a scheduled day to mark the habit complete.</p><div className="study-habits">
          <div><span /><div>{habitDates.map((date) => <small key={date}>{fromDateKey(date).toLocaleDateString('en-BD', { weekday: 'short' })[0]}</small>)}</div></div>
          {habits.map((habit) => <div key={habit.id}><span>{habit.name}</span><div>{habitDates.map((date) => {
            const scheduled = habit.repeatDays.includes(routineDays[mondayIndex(fromDateKey(date))])
            const completed = habit.completedDates.includes(date)
            return <button type="button" key={date} className={completed ? 'is-checked' : ''} disabled={!scheduled} onClick={() => onHabit(habit.id, date, completed)} aria-label={`${habit.name}, ${date}, ${completed ? 'complete' : 'incomplete'}`}>{completed ? <Check size={14} /> : null}</button>
          })}</div></div>)}
        </div></> : <StudyEmpty title="No active habits" description="Your active study habits will appear here once they are available." />}
      </div>
      <div>
        <div className="study-section-title"><h2>Study goals</h2></div>
        <p className="study-section-copy">Adjust your targets below. Changes are saved automatically.</p>
        <ProgressMeter label="Study time today" value={insights.goals.focusedMinutesToday} goal={insights.goals.dailyFocusGoalMinutes} unit="min" controls controlStep={15} onChange={(amount) => onGoal('day', amount * 15)} />
        <ProgressMeter label="Sessions this week" value={insights.goals.sessionsThisWeek} goal={insights.goals.weeklySessionGoal} controls onChange={(amount) => onGoal('sessions', amount)} />
        <ProgressMeter label="Topics completed" value={insights.goals.completedTopics} goal={insights.goals.topicCompletionGoal} controls onChange={(amount) => onGoal('topics', amount)} />
        <div className="study-section-title"><h2>Syllabus</h2></div>
        {syllabus.length ? <><p className="study-section-copy">Select a topic to move it to the next progress stage.</p><div className="study-syllabus">{syllabus.map(({ subject, topics, completedTopics, totalTopics }) => <section key={subject.id} style={subjectColorStyle(subject.color)}><div><strong>{subject.name}</strong><span>{completedTopics} of {totalTopics} complete</span></div><i><b style={{ width: `${totalTopics ? (completedTopics / totalTopics) * 100 : 0}%` }} /></i><div className="study-syllabus__topics">{topics.map((topic) => {
          const statusIndex = syllabusStatuses.indexOf(topic.status)
          return <button type="button" key={topic.id} className={`status-${statusIndex}`} onClick={() => onSyllabus(subject.id, topic.id, topic.status)}>{topic.title} <span>{syllabusStatusLabels[topic.status]}</span></button>
        })}</div></section>)}</div></> : <StudyEmpty title="No syllabus progress" description="Your syllabus subjects and topics will appear here once they are available." />}
      </div>
    </div>
  </>
}

function RemindersView({ preferences, integration, busy, onChange, onConnectGoogle, onSyncGoogle, onDisconnectGoogle, onDownloadApple }: {
  preferences?: StudyPreferencesRecord
  integration?: CalendarIntegrationStatus
  busy: boolean
  onChange: (changes: Partial<StudyPreferencesInput>) => void
  onConnectGoogle: () => void
  onSyncGoogle: () => void
  onDisconnectGoogle: () => void
  onDownloadApple: () => void
}) {
  const later = [
    ['Study partner session', 'Invite friends to a group revision block.'],
    ['Smart rescheduling', 'Suggest free slots for unfinished work.'],
    ['Smart study plan', 'Build a plan from exam dates, topics, and free time.'],
  ]
  if (!preferences) return <StudyEmpty title="Reminder settings unavailable" description="Check the backend connection and student record." />
  return <div className="study-reminders">
    <header className="study-view-header"><h1>Reminders</h1><p>Few and useful. Extra alerts are bundled into one digest.</p></header>
    <ReminderToggle label="Task reminders" description="Alert at the time you set. Tasks without a time stay quiet." checked={preferences.taskReminders} onChange={() => onChange({ taskReminders: !preferences.taskReminders })} />
    <ReminderToggle label="Study block reminders" description="Alert before self-study blocks, not classes." checked={preferences.routineReminders} onChange={() => onChange({ routineReminders: !preferences.routineReminders })} />
    <div className="study-setting-row"><span><strong>Lead time</strong><small>Minutes before a study block</small></span><select value={preferences.reminderLeadMinutes} onChange={(event) => onChange({ reminderLeadMinutes: Number(event.target.value) })}>{[5, 10, 15, 30].map((value) => <option key={value}>{value}</option>)}</select></div>
    <ReminderToggle label="Exam and deadline" description="Alert one day before and again on the morning." checked={preferences.examReminders} onChange={() => onChange({ examReminders: !preferences.examReminders })} />
    <ReminderToggle label="Morning digest" description="Send one 7:30 AM summary instead of many alerts." checked={preferences.morningDigest} onChange={() => onChange({ morningDigest: !preferences.morningDigest })} />
    <ReminderToggle label="Quiet hours" description="No alerts from 11 PM to 6 AM." checked={preferences.quietHoursEnabled} onChange={() => onChange({ quietHoursEnabled: !preferences.quietHoursEnabled })} />
    <div className="study-setting-row"><span><strong>Daily limit</strong><small>Maximum individual reminders per day</small></span><select value={preferences.dailyReminderLimit} onChange={(event) => onChange({ dailyReminderLimit: Number(event.target.value) })}>{[3, 5, 8].map((value) => <option key={value}>{value}</option>)}</select></div>
    <CalendarIntegrationPanel integration={integration} busy={busy} onConnectGoogle={onConnectGoogle} onSyncGoogle={onSyncGoogle} onDisconnectGoogle={onDisconnectGoogle} onDownloadApple={onDownloadApple} />
    <div className="study-section-title"><h2>Coming later</h2></div>
    {later.map(([title, description]) => <div className="study-setting-row is-muted" key={title}><span><strong>{title}</strong><small>{description}</small></span><span className="study-priority">Later</span></div>)}
  </div>
}

function CalendarIntegrationPanel({ integration, busy, onConnectGoogle, onSyncGoogle, onDisconnectGoogle, onDownloadApple }: {
  integration?: CalendarIntegrationStatus
  busy: boolean
  onConnectGoogle: () => void
  onSyncGoogle: () => void
  onDisconnectGoogle: () => void
  onDownloadApple: () => void
}) {
  const lastSynced = integration?.googleLastSyncedAt
    ? new Date(integration.googleLastSyncedAt).toLocaleString('en-BD', { dateStyle: 'medium', timeStyle: 'short' })
    : null
  return <section className="study-calendar-integration">
    <div className="study-section-title">
      <div><span className="study-calendar-integration__icon"><Cloud size={18} /></span><span><h2>Calendar integration</h2><small>Keep upcoming study work in your personal calendar.</small></span></div>
      {integration?.googleConnected ? <span className="study-connection-status is-connected">Connected</span> : null}
    </div>
    <div className="study-calendar-provider">
      <div><strong>Google Calendar</strong><small>{integration?.googleConnected ? (lastSynced ? `Last synced ${lastSynced}` : 'Connected and ready to sync') : integration?.googleConfigured === false ? 'Google OAuth credentials need to be configured' : 'Connect once, then sync the next 12 months'}</small></div>
      <div className="study-calendar-provider__actions">
        {integration?.googleConnected ? <>
          <button className="study-outline-button" type="button" disabled={busy} onClick={onSyncGoogle}><RefreshCw size={15} />Sync now</button>
          <button className="study-icon-button study-icon-button--small is-danger" type="button" disabled={busy} onClick={onDisconnectGoogle} aria-label="Disconnect Google Calendar"><Unplug size={15} /></button>
        </> : <button className="study-primary-button study-primary-button--small" type="button" disabled={busy || !integration?.googleConfigured} onClick={onConnectGoogle}>{integration?.googleConfigured === false ? 'Setup required' : 'Connect Google'}</button>}
      </div>
    </div>
    <div className="study-calendar-provider">
      <div><strong>Apple Calendar</strong><small>Download an ICS file containing the next 12 months.</small></div>
      <button className="study-outline-button" type="button" disabled={busy || integration?.appleExportAvailable === false} onClick={onDownloadApple}><Download size={15} />Download .ics</button>
    </div>
    <p className="study-calendar-integration__note">Tasks, routine blocks, and syllabus targets are included. Focus history and habits stay private in Campus Day.</p>
  </section>
}

function ReminderToggle({ label, description, checked, onChange }: { label: string; description: string; checked: boolean; onChange: () => void }) {
  return <label className="study-setting-row"><span><strong>{label}</strong><small>{description}</small></span><input type="checkbox" role="switch" checked={checked} onChange={onChange} /></label>
}

function StudyEmpty({ title, description }: { title: string; description: string }) {
  return <div className="study-empty"><Circle size={20} /><div><strong>{title}</strong><p>{description}</p></div></div>
}
