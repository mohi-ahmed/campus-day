import '@fontsource/manrope/400.css'
import '@fontsource/manrope/500.css'
import '@fontsource/manrope/600.css'
import '@fontsource/manrope/700.css'
import '@fontsource/manrope/800.css'
import '@fontsource/noto-sans-bengali/500.css'
import { createRoot } from 'react-dom/client'
import {
  ArrowUpRight,
  Bell,
  BookOpen,
  CalendarDays,
  Check,
  ChevronRight,
  CircleHelp,
  Clock3,
  CloudSun,
  Command,
  Flame,
  Home,
  Languages,
  LayoutGrid,
  Lightbulb,
  Newspaper,
  Search,
  Settings,
  ShoppingBag,
  Sparkles,
  Target,
  Utensils,
} from 'lucide-react'
import './dashboard-preview.css'

const navigation = [
  { label: 'Overview', icon: Home, active: true },
  { label: 'Study planner', icon: BookOpen },
  { label: 'Meals', icon: Utensils },
  { label: 'Current affairs', icon: Newspaper },
  { label: 'Marketplace', icon: ShoppingBag },
  { label: 'Lost & found', icon: CircleHelp },
]

const schedule = [
  { time: '09:00', period: 'AM', title: 'Data Structures', detail: 'Room 402 - Academic Building', icon: BookOpen, tone: 'green' },
  { time: '01:15', period: 'PM', title: 'Lunch at Central Cafeteria', detail: 'Rice, chicken curry, dal and salad', icon: Utensils, tone: 'amber' },
  { time: '04:30', period: 'PM', title: 'BCS study focus', detail: 'Bangladesh affairs - 45 minutes', icon: Target, tone: 'blue' },
]

function CampusDayMark() {
  return <div className="cd-mark" aria-hidden="true"><span>C</span><span>D</span></div>
}

function Sidebar() {
  return <aside className="preview-sidebar">
    <div className="preview-brand"><CampusDayMark /><div><strong>Campus Day</strong><small>Student life, organized</small></div></div>
    <nav aria-label="Preview navigation">
      <p>Your campus</p>
      {navigation.map(({ label, icon: Icon, active }) => <button className={active ? 'is-active' : ''} key={label} type="button"><Icon size={19} strokeWidth={1.8} /><span>{label}</span>{active ? <ChevronRight size={15} /> : null}</button>)}
    </nav>
    <div className="sidebar-foot">
      <button type="button"><Settings size={18} strokeWidth={1.8} /><span>Settings</span></button>
      <div className="support-note"><Lightbulb size={19} /><span><strong>Need a hand?</strong><small>Ask Campus Day</small></span><ArrowUpRight size={16} /></div>
    </div>
  </aside>
}

function Topbar() {
  return <header className="preview-topbar">
    <label className="preview-search"><Search size={18} strokeWidth={1.8} /><input aria-label="Search Campus Day" placeholder="Search Campus Day" /><kbd><Command size={12} /> K</kbd></label>
    <div className="preview-top-actions">
      <button className="text-control" type="button"><Languages size={18} /><span lang="bn">বাংলা</span></button>
      <button className="icon-control notification-control" type="button" aria-label="Notifications"><Bell size={19} /><span>3</span></button>
      <div className="profile-chip"><span>NJ</span><div><strong>Nusrat Jahan</strong><small>3rd Year - CSE</small></div><ChevronRight size={15} /></div>
    </div>
  </header>
}

function DayOverview() {
  return <section className="day-overview">
    <div className="day-title"><p>Saturday, 10 October</p><h1>Good afternoon, Nusrat</h1><span>Your campus day is looking manageable.</span></div>
    <button className="customize-button" type="button"><LayoutGrid size={17} />Customize view</button>
    <div className="glance-panel">
      <div className="glance-heading"><span><Sparkles size={18} /></span><div><strong>Today at a glance</strong><small>Three things deserve your attention</small></div></div>
      <div className="glance-item"><span className="glance-icon"><Clock3 size={17} /></span><div><small>Next class</small><strong>Data Structures at 9:00 AM</strong></div></div>
      <div className="glance-item"><span className="glance-icon"><Utensils size={17} /></span><div><small>Meal update</small><strong>Lunch is confirmed</strong></div></div>
      <div className="glance-item"><span className="glance-icon"><Flame size={17} /></span><div><small>Study streak</small><strong>6 focused days</strong></div></div>
    </div>
  </section>
}

function SchedulePanel() {
  return <section className="surface schedule-panel">
    <header className="section-head"><div><h2>Your day</h2><p>A clear view of what is coming next.</p></div><button type="button">Open calendar <ArrowUpRight size={15} /></button></header>
    <div className="schedule-list">
      {schedule.map(({ time, period, title, detail, icon: Icon, tone }, index) => <article className="schedule-row" key={title}>
        <div className="schedule-time"><strong>{time}</strong><small>{period}</small></div>
        <div className={`schedule-icon schedule-icon--${tone}`}><Icon size={19} strokeWidth={1.8} /></div>
        <div className="schedule-copy"><strong>{title}</strong><span>{detail}</span></div>
        <button type="button" aria-label={`Open ${title}`}>{index === 0 ? <span className="now-label">NEXT</span> : null}<ChevronRight size={17} /></button>
      </article>)}
    </div>
    <button className="add-plan" type="button"><span>+</span>Add something to your day</button>
  </section>
}

function FocusPanel() {
  return <section className="focus-panel">
    <div className="focus-top"><span className="focus-date"><strong>10</strong><small>OCT</small></span><span className="focus-badge">NEXT UP</span></div>
    <div className="focus-copy"><small>09:00 AM - 10:20 AM</small><h2>Data Structures</h2><p>Room 402, Academic Building</p></div>
    <div className="focus-meta"><span><Clock3 size={16} />Starts in 42 minutes</span><span><BookOpen size={16} />Bring lab notebook</span></div>
    <button type="button"><Check size={17} />I am ready</button>
  </section>
}

function StudyPanel() {
  return <section className="surface study-panel">
    <header className="section-head"><div><h2>Study momentum</h2><p>This week is moving in the right direction.</p></div><button type="button">View planner <ChevronRight size={15} /></button></header>
    <div className="study-summary"><div><strong>6.5h</strong><span>Focused this week</span><small><ArrowUpRight size={13} />1.2h from last week</small></div><div className="week-bars" aria-label="Study hours by day">
      {[42, 64, 35, 78, 58, 86, 22].map((height, index) => <div key={index}><span style={{ height: `${height}%` }} className={index === 5 ? 'is-current' : ''} /><small>{['S', 'M', 'T', 'W', 'T', 'F', 'S'][index]}</small></div>)}
    </div></div>
    <div className="study-target"><span><Target size={19} /></span><div><strong>BCS preparation</strong><small>Bangladesh affairs - 12 of 18 topics reviewed</small></div><b>67%</b></div>
  </section>
}

function CampusPulse() {
  return <section className="surface pulse-panel">
    <header className="section-head"><div><h2>Campus pulse</h2><p>Useful updates, without the noise.</p></div><button type="button">See all <ChevronRight size={15} /></button></header>
    <article className="pulse-feature"><div className="pulse-visual"><CloudSun size={28} /><span>29°C</span><small>Clear afternoon</small></div><div><small>CAMPUS UPDATE</small><h3>Library study zone stays open until 10 PM</h3><p>Extended hours begin today for the midterm preparation period.</p><button type="button">Read update <ArrowUpRight size={14} /></button></div></article>
    <div className="pulse-row"><span><ShoppingBag size={17} /></span><div><strong>New marketplace picks</strong><small>8 verified listings near you</small></div><ChevronRight size={16} /></div>
  </section>
}

function QuickActions() {
  return <section className="quick-actions">
    <header><h2>Quick actions</h2><p>Common tasks, one click away.</p></header>
    <div>
      <button type="button"><span><Utensils size={18} /></span><strong>View menu</strong><small>Today and tomorrow</small><ChevronRight size={16} /></button>
      <button type="button"><span><CalendarDays size={18} /></span><strong>Absence notice</strong><small>Help the kitchen plan</small><ChevronRight size={16} /></button>
      <button type="button"><span><Newspaper size={18} /></span><strong>Daily brief</strong><small>7 verified updates</small><ChevronRight size={16} /></button>
      <button type="button"><span><CircleHelp size={18} /></span><strong>Lost something?</strong><small>Search recent reports</small><ChevronRight size={16} /></button>
    </div>
  </section>
}

function DashboardPreview() {
  return <div className="preview-app"><Sidebar /><div className="preview-body"><Topbar /><main><DayOverview /><div className="dashboard-grid"><SchedulePanel /><FocusPanel /><StudyPanel /><CampusPulse /><QuickActions /></div></main></div></div>
}

createRoot(document.getElementById('dashboard-preview-root')!).render(<DashboardPreview />)
