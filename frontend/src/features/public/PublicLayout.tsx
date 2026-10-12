import { useEffect, type ReactNode } from 'react'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import './public.css'

type CampusDayBrandProps = {
  to?: string
  className?: string
  ariaLabel?: string
}

export function CampusDayBrand({ to = '/', className, ariaLabel = 'Campus Day home' }: CampusDayBrandProps) {
  return <Link className={['cd-brand', className].filter(Boolean).join(' ')} to={to} aria-label={ariaLabel}>
    <span className="cd-brand-mark"><RefreshCw size={21} strokeWidth={2.2} /></span>
    <span>campus day<i>.</i></span>
  </Link>
}

export function AuthLayout({ children }: { children: ReactNode }) {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' })
  }, [])

  return <div className="cd-auth-page">
    <header className="cd-auth-header cd-page-width">
      <CampusDayBrand />
      <Link className="cd-back-link" to="/"><ArrowLeft size={17} />Back to campus</Link>
    </header>
    <main className="cd-auth-layout cd-page-width">{children}</main>
  </div>
}
