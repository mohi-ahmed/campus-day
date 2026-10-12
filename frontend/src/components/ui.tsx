import { AlertCircle, Check, ChevronRight, CircleAlert, Info, LoaderCircle, Search, X } from 'lucide-react'
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'
import type { Tone } from '../features/meal/types'

export function Button({ variant = 'primary', icon, children, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger'; icon?: ReactNode }) {
  return <button className={`button button--${variant} ${className}`} {...props}>{icon}{children}</button>
}

export function IconButton({ label, children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return <button className="icon-button" aria-label={label} title={label} {...props}>{children}</button>
}

export function Card({ children, className = '', interactive = false }: { children: ReactNode; className?: string; interactive?: boolean }) {
  return <section className={`card ${interactive ? 'card--interactive' : ''} ${className}`}>{children}</section>
}

const toneIcons: Record<Tone, ReactNode> = {
  success: <Check size={13} strokeWidth={2.5} />,
  warning: <CircleAlert size={13} />,
  error: <X size={13} strokeWidth={2.5} />,
  info: <Info size={13} />,
  neutral: <span className="status-dot" />,
  brand: <Check size={13} />,
}

const statusTone: Record<string, Tone> = {
  Active: 'success', Published: 'success', Paid: 'success', Finalized: 'success', Available: 'success', Completed: 'success',
  Pending: 'warning', Unpaid: 'warning', Outstanding: 'error', 'Pending Finalization': 'warning', 'Price Pending': 'warning', 'Low stock': 'warning',
  Locked: 'info', Ordered: 'brand', Draft: 'neutral', Closed: 'neutral', 'Not Eating': 'neutral', Cancelled: 'error', Unavailable: 'error',
}

export function StatusBadge({ status, tone }: { status: string; tone?: Tone }) {
  const resolved = tone ?? statusTone[status] ?? 'neutral'
  return <span className={`status status--${resolved}`}>{toneIcons[resolved]}{status}</span>
}

export function PageHeader({ title, description, eyebrow, actions }: { title: string; description?: string; eyebrow?: string; actions?: ReactNode }) {
  return <header className="page-header">
    <div>{eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}<h1>{title}</h1>{description ? <p>{description}</p> : null}</div>
    {actions ? <div className="page-actions">{actions}</div> : null}
  </header>
}

export function SectionHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="section-header"><div><h2>{title}</h2>{description ? <p>{description}</p> : null}</div>{action}</div>
}

export function Notice({ tone = 'info', title, children }: { tone?: Tone; title: string; children?: ReactNode }) {
  return <div className={`notice notice--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
    {tone === 'error' ? <AlertCircle size={19} /> : <Info size={19} />}
    <div><strong>{title}</strong>{children ? <div>{children}</div> : null}</div>
  </div>
}

export function Skeleton({ lines = 3 }: { lines?: number }) {
  return <div className="skeleton" aria-label="Loading meal information">{Array.from({ length: lines }, (_, index) => <span key={index} />)}</div>
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="empty-state"><div className="empty-state__icon"><Search size={22} /></div><h3>{title}</h3><p>{description}</p>{action}</div>
}

export function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: ReactNode }) {
  return <label className="field"><span className="field__label">{label}</span>{children}{error ? <span className="field__error">{error}</span> : hint ? <span className="field__hint">{hint}</span> : null}</label>
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) { return <input className="input" {...props} /> }
export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) { return <select className="input" {...props} /> }

export function SearchField({ value, onChange, placeholder = 'Search' }: { value: string; onChange: (value: string) => void; placeholder?: string }) {
  return <label className="search-field"><Search size={17} /><span className="sr-only">{placeholder}</span><input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>
}

export function Metric({ label, value, detail, tone = 'neutral' }: { label: string; value: string; detail?: string; tone?: Tone }) {
  return <div className={`metric metric--${tone}`}><span>{label}</span><strong>{value}</strong>{detail ? <small>{detail}</small> : null}</div>
}

export function Breadcrumbs({ items }: { items: string[] }) {
  return <nav className="breadcrumbs" aria-label="Breadcrumb">{items.map((item, index) => <span key={item}>{index ? <ChevronRight size={13} /> : null}{item}</span>)}</nav>
}

export function SubmittingLabel({ submitting, idle }: { submitting: boolean; idle: string }) {
  return submitting ? <><LoaderCircle className="spin" size={17} />Saving…</> : <>{idle}</>
}
