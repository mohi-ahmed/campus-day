import { useEffect, useState, type CSSProperties } from 'react'
import {
  ArrowRight, Bell, BookOpen, CalendarDays, Check, ChevronRight, CircleCheck, LockKeyhole,
  MapPinned, Menu, MessageCircle, Search, ShieldCheck, ShoppingBag, Sparkles, Utensils, Users, X,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { CampusDayBrand } from './PublicLayout'

const features = [
  { title: 'Meal Management', copy: 'Plan meals, send an absence notice, check your bill and keep hall dining simple.', action: 'Take a seat', icon: Utensils, tone: 'peach' },
  { title: 'Study & Task', copy: 'Organize classes, study sessions, focus blocks and the deadlines that matter next.', action: 'Find your focus', icon: BookOpen, tone: 'blue' },
  { title: 'Lost & Found', copy: 'Report a missing item, browse recent finds and help belongings make it back home.', action: 'Look around', icon: Search, tone: 'yellow' },
  { title: 'Marketplace', copy: 'Buy, sell or exchange books, devices and useful items inside your campus community.', action: 'Explore the market', icon: ShoppingBag, tone: 'green' },
  { title: 'Autopsy', copy: 'Share thoughts anonymously, ask honest questions and join a moderated campus conversation.', action: 'Join the conversation', icon: MessageCircle, tone: 'pink' },
]

const dayMoments = [
  { time: '8:20 AM', title: 'Know what starts first', copy: 'Classes and your study plan are ready before the day gets busy.', icon: BookOpen },
  { time: '1:10 PM', title: 'Sort lunch in seconds', copy: 'See the menu or send a meal-off notice without opening another chat.', icon: Utensils },
  { time: '4:40 PM', title: 'Catch the important update', copy: 'Notices and current affairs stay readable and easy to act on.', icon: Bell },
  { time: '8:30 PM', title: 'Close the day clearly', copy: 'Review pending tasks, community updates and what comes next.', icon: CircleCheck },
]

function useScrollReveal() {
  useEffect(() => {
    const elements = Array.from(document.querySelectorAll<HTMLElement>('.cd-reveal'))
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      elements.forEach((element) => element.classList.add('is-visible'))
      return undefined
    }

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        observer.unobserve(entry.target)
      })
    }, { threshold: 0.13 })

    elements.forEach((element) => observer.observe(element))
    return () => observer.disconnect()
  }, [])
}

function LandingHeader() {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)

  return <header className="cd-site-header">
    <div className="cd-nav-inner">
      <CampusDayBrand />
      <button className="cd-menu-toggle" type="button" aria-expanded={open} aria-label={open ? 'Close menu' : 'Open menu'} onClick={() => setOpen((value) => !value)}>
        {open ? <X size={21} /> : <Menu size={21} />}
      </button>
      <nav className={`cd-site-nav ${open ? 'is-open' : ''}`} aria-label="Public navigation">
        <a href="#features" onClick={close}>Explore the tools</a>
        <a href="#autopsy" onClick={close}>Meet Autopsy <span>Anonymous</span></a>
        <Link to="/signin" onClick={close}>Sign in</Link>
        <Link className="cd-primary-button compact" to="/create-account" onClick={close}>Create account <ArrowRight size={15} /></Link>
      </nav>
    </div>
  </header>
}

export default function LandingPage() {
  useScrollReveal()

  return <div className="cd-public-site">
    <LandingHeader />
    <main>
      <section className="cd-hero" id="home">
        <div className="cd-hero-chip chip-one"><CalendarDays size={15} />What, where, when?</div>
        <div className="cd-hero-chip chip-two"><BookOpen size={15} />A little more focus.</div>
        <div className="cd-hero-chip chip-three"><Search size={15} />Found your thing?</div>
        <div className="cd-hero-chip chip-four"><LockKeyhole size={15} />Say what matters.</div>

        <div className="cd-page-width cd-hero-content">
          <p className="cd-eyebrow">Your everyday campus companion</p>
          <h1>Campus day<span>.</span></h1>
          <h2>Less chaos. More campus.</h2>
          <p className="cd-hero-copy">From your first class to your last meal, keep university life clear and connected.</p>
          <div className="cd-hero-actions">
            <a className="cd-primary-button" href="#features">Find your campus flow <ArrowRight size={16} /></a>
            <a className="cd-secondary-button" href="#day">Take a peek <ChevronRight size={16} /></a>
          </div>
        </div>

        <div className="cd-hero-quick cd-page-width" aria-label="Campus Day highlights">
          <article><Utensils size={18} /><div><span>Today's meal</span><strong>Lunch at 1:00 PM</strong></div><small>View</small></article>
          <article><BookOpen size={18} /><div><span>A little progress</span><strong>2 study blocks left</strong></div><small>Plan</small></article>
          <article><MessageCircle size={18} /><div><span>Autopsy</span><strong>Campus thoughts, safely</strong></div><small>Open</small></article>
        </div>
      </section>

      <section className="cd-promise-bar" aria-label="Campus Day principles">
        <div className="cd-page-width cd-promise-list">
          <span><ShieldCheck size={15} />Real tools. One campus.</span>
          <span><Users size={15} />Student life, connected.</span>
          <span><Sparkles size={15} />Your space. Your privacy.</span>
          <span><MapPinned size={15} />Local problems, fewer hassles.</span>
        </div>
      </section>

      <section className="cd-feature-section cd-page-width" id="features">
        <div className="cd-section-intro cd-reveal">
          <p className="cd-eyebrow">A place for every part of your day</p>
          <h2>Big campus energy.<br />Small everyday wins.</h2>
          <p>The practical stuff, the social stuff and the parts of student life that should feel simpler.</p>
        </div>
        <div className="cd-feature-grid">
          {features.map(({ title, copy, action, icon: Icon, tone }, index) => <article
            className={`cd-feature-card ${tone} cd-reveal`}
            id={title === 'Autopsy' ? 'autopsy' : undefined}
            style={{ '--delay': `${index * 55}ms` } as CSSProperties}
            key={title}
          >
            <div className="cd-feature-card-top"><span className="cd-feature-icon"><Icon size={21} strokeWidth={1.8} /></span><span>{String(index + 1).padStart(2, '0')}</span></div>
            <div><h3>{title}</h3><p>{copy}</p></div>
            <a href="#day">{action} <ArrowRight size={14} /></a>
          </article>)}
        </div>
      </section>

      <section className="cd-day-section" id="day">
        <div className="cd-page-width cd-day-layout">
          <div className="cd-day-heading cd-reveal">
            <p className="cd-eyebrow">One connected student day</p>
            <h2>The right thing, when you need it.</h2>
            <p>Campus Day follows the real rhythm of university life instead of making every tool compete for attention.</p>
            <Link className="cd-text-link" to="/create-account">Build your campus day <ArrowRight size={16} /></Link>
          </div>
          <div className="cd-moment-list">
            {dayMoments.map(({ time, title, copy, icon: Icon }, index) => <article className="cd-moment-row cd-reveal" style={{ '--delay': `${index * 60}ms` } as CSSProperties} key={time}>
              <span className="cd-moment-time">{time}</span><span className="cd-moment-icon"><Icon size={20} /></span><div><h3>{title}</h3><p>{copy}</p></div>
            </article>)}
          </div>
        </div>
      </section>

      <section className="cd-confidence-section cd-page-width cd-reveal">
        <div><p className="cd-eyebrow">Built around real student problems</p><h2>Useful first. Friendly always.</h2></div>
        <div className="cd-confidence-points">
          <p><Check size={17} />Clear enough to understand at a glance</p>
          <p><Check size={17} />Private spaces with thoughtful boundaries</p>
          <p><Check size={17} />Relevant tools without dashboard overload</p>
        </div>
      </section>

      <section className="cd-final-cta">
        <div className="cd-page-width cd-final-cta-inner cd-reveal">
          <div><p className="cd-eyebrow">More time in class. More time on campus.</p><h2>Your best campus days have a little more connection.</h2><p>Bring studies, meals and everyday campus life into one familiar place.</p></div>
          <Link className="cd-lime-button" to="/create-account">Make it Campus Day <ArrowRight size={16} /></Link>
        </div>
      </section>
    </main>
    <footer className="cd-site-footer cd-page-width"><CampusDayBrand /><p>A little less chaos. A lot more campus.</p><p>© 2026 Campus Day</p></footer>
  </div>
}
