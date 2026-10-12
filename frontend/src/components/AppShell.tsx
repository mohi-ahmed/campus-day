import { useEffect, useRef, useState, type ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  BarChart3, BookOpenText, CalendarDays, ChevronDown, ChevronLeft, ClipboardCheck, ClipboardList, Clock3, CookingPot,
  CreditCard, FileBarChart, HandCoins, Home, LogOut, Menu, PackageCheck, PanelLeftClose, PanelLeftOpen,
  CircleHelp, MessageCircleHeart, Newspaper, Settings2, ShoppingBasket, Store, Utensils, WalletCards, Warehouse, X,
  UserRound,
} from 'lucide-react'
import { useDemoUser } from '../app/DemoUserContext'
import type { StudentContext } from '../features/meal/types'
import { useLocale } from '../i18n/LocaleContext'
import { IconButton } from './ui'
import { StudyNotificationCenter } from './StudyNotificationCenter'
import { clearAuthSession, readAuthSession } from '../features/public/authSession'
import { CampusDayBrand } from '../features/public/PublicLayout'
import { authApi } from '../features/public/authApi'

type NavItem = { label: string; to: string; icon: typeof Home; end?: boolean }
type NavGroup = { label: string; icon: typeof Home; items: NavItem[] }

const coreItems: NavItem[] = [
  { label: 'Dashboard', to: '/meal', icon: Home, end: true },
  { label: 'Menu', to: '/meal/menu', icon: Utensils },
]

const studyItems: NavItem[] = [
  { label: 'Study & Time', to: '/study', icon: Clock3 },
  { label: 'Current Affairs', to: '/current-affairs', icon: Newspaper },
]

const communityItems: NavItem[] = [
  { label: 'Marketplace', to: '/marketplace', icon: Store },
  { label: 'Lost & Found', to: '/lost-found', icon: CircleHelp },
  { label: 'Autopsy', to: '/autopsy', icon: MessageCircleHeart },
]

const managerItems: NavItem[] = [
  { label: 'Manager Dashboard', to: '/meal/manage', icon: BarChart3, end: true },
  { label: 'Management Period', to: '/meal/manage/period', icon: CalendarDays },
  { label: 'Menu Management', to: '/meal/manage/menu', icon: BookOpenText },
  { label: 'Price Management', to: '/meal/manage/prices', icon: HandCoins },
  { label: 'Order Overview', to: '/meal/manage/orders', icon: ClipboardCheck },
  { label: 'Fraction Sheet', to: '/meal/manage/fraction', icon: ClipboardList },
  { label: 'Cooking Plan', to: '/meal/manage/cooking-plan', icon: CookingPot },
  { label: 'Stock', to: '/meal/manage/stock', icon: Warehouse },
  { label: 'Kitchen Issue', to: '/meal/manage/kitchen-issue', icon: PackageCheck },
  { label: 'Purchases', to: '/meal/manage/purchases', icon: ShoppingBasket },
  { label: 'Suppliers & Dues', to: '/meal/manage/suppliers', icon: Store },
  { label: 'Collections', to: '/meal/manage/collections', icon: WalletCards },
  { label: 'Handover', to: '/meal/manage/handover', icon: Settings2 },
  { label: 'Reports', to: '/meal/manage/reports', icon: FileBarChart },
]

function activeGroupForPath(groups: NavGroup[], pathname: string) {
  const matches = groups.flatMap((group) => group.items
    .filter((item) => item.end ? pathname === item.to : pathname === item.to || pathname.startsWith(`${item.to}/`))
    .map((item) => ({ group: group.label, routeLength: item.to.length })))
    .sort((left, right) => right.routeLength - left.routeLength)
  return matches[0]?.group ?? 'Meal'
}

function NavGroupView({ group, collapsed, expanded, close, toggle }: { group: NavGroup; collapsed: boolean; expanded: boolean; close: () => void; toggle: () => void }) {
  const panelId = `nav-group-${group.label.toLowerCase().replace(/\s+/g, '-')}`
  const GroupIcon = group.icon
  return <div className={`nav-group ${expanded ? 'is-expanded' : ''}`}>
    <button className="nav-group__toggle" type="button" aria-expanded={expanded} aria-controls={panelId} onClick={toggle} title={group.label}>
      <GroupIcon className="nav-group__icon" size={18} /><span>{group.label}</span><ChevronDown className="nav-group__chevron" size={15} />
    </button>
    <div className="nav-group__items" id={panelId}>{group.items.map(({ label, to, icon: Icon, end }) => (
      <NavLink key={to} to={to} end={end} onClick={close} title={collapsed ? label : undefined} className={({ isActive }) => `nav-link ${isActive ? 'is-active' : ''}`}>
        <Icon size={19} /><span>{label}</span>
      </NavLink>
    ))}</div>
  </div>
}

export function AppShell({ children }: { children: ReactNode }) {
  const { user, setContext, authenticated } = useDemoUser()
  const { locale } = useLocale()
  const [collapsed, setCollapsed] = useState(false)
  const [drawer, setDrawer] = useState(false)
  const [accountMenuOpen, setAccountMenuOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const accountMenuRef = useRef<HTMLDivElement>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const topbarDate = new Date()
  const topbarWeekday = new Intl.DateTimeFormat(locale, { timeZone: 'Asia/Dhaka', weekday: 'long' }).format(topbarDate)
  const topbarDay = new Intl.DateTimeFormat(locale, { timeZone: 'Asia/Dhaka', day: 'numeric', month: 'long' }).format(topbarDate)
  const studentItems: NavItem[] = user.context === 'final-year'
    ? [{ label: 'Place Order', to: '/meal/order', icon: ClipboardCheck }, { label: 'My Orders', to: '/meal/orders', icon: ClipboardList }, { label: 'Bills & Payments', to: '/meal/bills', icon: CreditCard }]
    : [{ label: 'Absence Notice', to: '/meal/absence', icon: CalendarDays }]
  const groups: NavGroup[] = [
    { label: 'Meal', icon: Utensils, items: coreItems },
    { label: 'Student meal', icon: ClipboardList, items: studentItems },
    ...(user.isMealManager ? [{ label: 'Meal management', icon: BarChart3, items: managerItems }] : []),
    { label: 'Study', icon: Clock3, items: studyItems },
    { label: 'Community', icon: Store, items: communityItems },
  ]
  const activeGroup = activeGroupForPath(groups, location.pathname)
  const [expandedGroup, setExpandedGroup] = useState<string | null>(activeGroup)

  useEffect(() => {
    setExpandedGroup(activeGroup)
  }, [activeGroup])

  useEffect(() => {
    if (!accountMenuOpen) return
    const closeOnOutsidePress = (event: PointerEvent) => {
      if (event.target instanceof Node && !accountMenuRef.current?.contains(event.target)) setAccountMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setAccountMenuOpen(false)
    }
    document.addEventListener('pointerdown', closeOnOutsidePress)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePress)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [accountMenuOpen])

  const switchContext = (context: StudentContext) => {
    setContext(context)
    if (location.pathname.startsWith('/meal/manage') && context !== 'manager') navigate('/meal')
    if ((location.pathname.includes('/order') || location.pathname.includes('/orders')) && context !== 'final-year') navigate('/meal')
    if (location.pathname.includes('/absence') && context === 'final-year') navigate('/meal')
    if (location.pathname.includes('/bills') && context !== 'final-year') navigate('/meal')
  }

  const openProfile = () => {
    setAccountMenuOpen(false)
    navigate(readAuthSession()?.account.profileComplete === false ? '/complete-profile' : '/profile')
  }

  const logout = async () => {
    if (loggingOut) return
    setLoggingOut(true)
    const session = readAuthSession()
    try {
      if (session?.refreshToken) await authApi.logout(session.refreshToken)
    } catch {
      // Local sign-out must still succeed when the backend cannot be reached.
    } finally {
      clearAuthSession()
      setAccountMenuOpen(false)
      setLoggingOut(false)
      navigate('/signin', { replace: true })
    }
  }

  const bottomItems = user.context === 'final-year'
    ? [coreItems[0], communityItems[0], studyItems[0], studyItems[1], studentItems[1]]
    : [coreItems[0], communityItems[0], studyItems[0], studyItems[1], ...(user.isMealManager ? [{ label: 'Manage', to: '/meal/manage', icon: BarChart3 }] : [coreItems[1]])]

  return <div className={`app-shell ${collapsed ? 'app-shell--collapsed' : ''}`}>
    <aside className={`sidebar ${drawer ? 'is-open' : ''}`} aria-label="Campus Day navigation">
      <div className="sidebar-brand"><CampusDayBrand className="sidebar-logo" to="/meal" ariaLabel="Go to Meal dashboard" /><IconButton label="Close navigation" onClick={() => setDrawer(false)}><X size={20} /></IconButton></div>
      <nav>{groups.map((group) => <NavGroupView key={group.label} group={group} collapsed={collapsed} expanded={expandedGroup === group.label} close={() => setDrawer(false)} toggle={() => setExpandedGroup((current) => current === group.label ? null : group.label)} />)}</nav>
      {authenticated ? <button className="sidebar-logout" type="button" disabled={loggingOut} onClick={() => void logout()}><LogOut size={18} /><span>{loggingOut ? 'Logging out...' : 'Log out'}</span></button> : null}
      <button className="collapse-button" onClick={() => setCollapsed((value) => !value)}>{collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}<span>{collapsed ? 'Expand' : 'Collapse sidebar'}</span></button>
    </aside>
    {drawer ? <button className="drawer-backdrop" aria-label="Close navigation" onClick={() => setDrawer(false)} /> : null}
    <div className="app-body">
      <header className="topbar">
        <IconButton label="Open navigation" onClick={() => setDrawer(true)}><Menu size={21} /></IconButton>
        <div className="topbar-date" aria-label={`${topbarWeekday}, ${topbarDay}`}><CalendarDays size={17} /><span><strong>{topbarWeekday}</strong><small>{topbarDay}</small></span></div>
        <div className="topbar-actions">
          <StudyNotificationCenter />
          {!authenticated ? <label className="demo-context"><span className="sr-only">Demo user context</span><select value={user.context} onChange={(event) => switchContext(event.target.value as StudentContext)}><option value="regular">Years 1-4 Student</option><option value="final-year">Final Year Student</option><option value="manager">Current Meal Manager</option></select></label> : null}
          <div className={`account-menu ${accountMenuOpen ? 'is-open' : ''}`} ref={accountMenuRef}>
            <button className="user-chip" type="button" onClick={() => authenticated ? setAccountMenuOpen((open) => !open) : openProfile()} aria-label={authenticated ? 'Open account menu' : 'Open your profile'} aria-haspopup={authenticated || undefined} aria-expanded={authenticated ? accountMenuOpen : undefined} aria-controls={authenticated ? 'account-menu-panel' : undefined}><span className="user-chip__avatar">{user.profileImageUrl ? <img src={user.profileImageUrl} alt="" /> : user.initials}</span><div><strong>{user.name}</strong><small>{user.isMealManager ? '4th Year · Meal Manager' : `${user.academicYear === 5 ? 'Final' : `${user.academicYear}${user.academicYear === 3 ? 'rd' : 'th'}`} Year Student`}</small></div>{authenticated ? <ChevronDown className="user-chip__chevron" size={16} /> : null}</button>
            {authenticated && accountMenuOpen ? <div className="account-menu__panel" id="account-menu-panel" aria-label="Account actions">
              <button type="button" onClick={openProfile}><UserRound size={17} /><span>View profile</span></button>
              <button className="account-menu__logout" type="button" disabled={loggingOut} onClick={() => void logout()}><LogOut size={17} /><span>{loggingOut ? 'Logging out...' : 'Log out'}</span></button>
            </div> : null}
          </div>
        </div>
      </header>
      <main className="main-content"><div className="content-wrap">{children}</div></main>
      <nav className="bottom-nav" aria-label="Primary mobile navigation">{bottomItems.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} className={({ isActive }) => isActive ? 'is-active' : ''}><Icon size={20} /><span>{label}</span></NavLink>)}</nav>
    </div>
  </div>
}

export function BackLink({ to, label }: { to: string; label: string }) {
  return <NavLink className="back-link" to={to}><ChevronLeft size={16} />{label}</NavLink>
}
