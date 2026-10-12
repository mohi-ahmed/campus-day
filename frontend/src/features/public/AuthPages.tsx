import { useEffect, useRef, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'
import { ArrowLeft, ArrowRight, BookOpen, CircleCheck, Eye, EyeOff, KeyRound, LockKeyhole, Mail, MessageCircle, RefreshCw, Search, ShieldCheck, ShoppingBag, Utensils } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AuthLayout } from './PublicLayout'
import { authApi, type AcademicYear, type StudentGender } from './authApi'
import {
  clearPasswordResetContext,
  readAuthSession,
  readPasswordResetContext,
  saveAuthSession,
  savePasswordResetContext,
} from './authSession'

type AuthFieldProps = {
  id: string
  label: string
  value: string
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
  placeholder: string
  error?: string
  type?: string
  autoComplete?: string
}

const authTiles = [
  { label: 'Meals, sorted.', icon: Utensils, tone: 'peach' },
  { label: 'Find your focus.', icon: BookOpen, tone: 'blue' },
  { label: 'Lost? Found.', icon: Search, tone: 'yellow' },
  { label: 'Better deals.', icon: ShoppingBag, tone: 'green' },
  { label: 'Speak freely.', icon: MessageCircle, tone: 'pink' },
]

function TextField({ id, label, value, onChange, placeholder, error, type = 'text', autoComplete }: AuthFieldProps) {
  return <div className="cd-field-group">
    <label htmlFor={id}>{label}</label>
    <input id={id} name={id} type={type} value={value} onChange={onChange} placeholder={placeholder} autoComplete={autoComplete} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} />
    {error ? <span className="cd-field-error" id={`${id}-error`}>{error}</span> : null}
  </div>
}

function PasswordField({ id, label, value, onChange, placeholder, error, autoComplete = 'current-password' }: AuthFieldProps) {
  const [visible, setVisible] = useState(false)
  return <div className="cd-field-group">
    <label htmlFor={id}>{label}</label>
    <div className="cd-password-wrap">
      <input id={id} name={id} type={visible ? 'text' : 'password'} value={value} onChange={onChange} placeholder={placeholder} autoComplete={autoComplete} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} />
      <button type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible((current) => !current)}>{visible ? <EyeOff size={18} /> : <Eye size={18} />}</button>
    </div>
    {error ? <span className="cd-field-error" id={`${id}-error`}>{error}</span> : null}
  </div>
}

function AuthAside() {
  return <aside className="cd-auth-aside">
    <p className="cd-eyebrow">Less chaos. More campus.</p>
    <h1>A good day starts with <span>your people.</span></h1>
    <p>The meals. The deadlines. The late-night thoughts. One place for everything that makes campus, campus.</p>
    <div className="cd-auth-tiles">{authTiles.map(({ label, icon: Icon, tone }) => <div className={`cd-auth-tile ${tone}`} key={label}><Icon size={19} /><span>{label}</span><ArrowRight size={14} /></div>)}</div>
    <small><ShieldCheck size={15} />Made for students. Built around your day.</small>
  </aside>
}

function AuthPageShell({ children }: { children: ReactNode }) {
  return <AuthLayout><AuthAside /><section className="cd-auth-panel">{children}</section></AuthLayout>
}

function GoogleButton() {
  return <button className="cd-google-button" type="button" onClick={() => window.location.assign(authApi.googleAuthorizationUrl())}><span>G</span>Continue with Google</button>
}

function AuthDivider() {
  return <div className="cd-auth-divider"><span>or continue with email</span></div>
}

function AuthTrust() {
  return <p className="cd-auth-trust"><ShieldCheck size={15} />Your details stay private. Your campus stays connected.</p>
}

export function SignInPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState(() => params.get('oauth') === 'failed'
    ? 'Google sign in could not be completed. Please try again.'
    : '')
  const [busy, setBusy] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!email.includes('@')) next.email = 'Enter a valid email address.'
    if (!password) next.password = 'Enter your password.'
    setErrors(next)
    setApiError('')
    if (Object.keys(next).length > 0) return
    setBusy(true)
    try {
      const tokens = await authApi.login(email, password)
      saveAuthSession(tokens)
      navigate('/meal', { replace: true })
    } catch (error) {
      setApiError(error instanceof Error ? error.message : 'Sign in failed. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  return <AuthPageShell><div className="cd-auth-card">
    <div className="cd-auth-title"><p>Your everyday campus companion</p><h2>Good to see you again.</h2><span>Sign in and pick up where your day left off.</span></div>
    <GoogleButton /><AuthDivider />
    <form onSubmit={submit} noValidate>
      <TextField id="email" label="Email address" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" autoComplete="email" error={errors.email} />
      <PasswordField id="password" label="Password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Enter your password" error={errors.password} />
      <Link className="cd-forgot-link" to="/forgot-password">Forgot password?</Link>
      {apiError ? <p className="cd-form-alert" role="alert">{apiError}</p> : null}
      <button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} {!busy ? <ArrowRight size={16} /> : null}</button>
    </form>
    <p className="cd-auth-switch">New around here? <Link to="/create-account">Create an account</Link></p>
    <AuthTrust />
  </div></AuthPageShell>
}

export function CreateAccountPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', year: '', gender: '', phone: '', email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)
  const update = (key: keyof typeof form) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Enter your full name.'
    if (!form.year) next.year = 'Choose your student year.'
    if (!form.gender) next.gender = 'Choose a gender option.'
    if (!form.phone.trim()) next.phone = 'Enter your phone number.'
    if (!form.email.includes('@')) next.email = 'Enter a valid email address.'
    if (form.password.length < 8) next.password = 'Use at least 8 characters.'
    if (form.confirm !== form.password) next.confirm = 'Passwords do not match.'
    setErrors(next)
    setApiError('')
    if (Object.keys(next).length > 0) return
    setBusy(true)
    try {
      await authApi.register({
        fullName: form.name.trim(),
        academicYear: form.year as AcademicYear,
        gender: form.gender as StudentGender,
        phoneNumber: form.phone.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      navigate(`/verify-email?flow=create&email=${encodeURIComponent(form.email.trim())}`)
    } catch (error) {
      setApiError(error instanceof Error ? error.message : 'Account creation failed. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  return <AuthPageShell><div className="cd-auth-card cd-create-card">
    <div className="cd-auth-title"><p>Your everyday campus companion</p><h2>Your campus day starts here.</h2><span>A few details, and you are part of the campus.</span></div>
    <GoogleButton /><AuthDivider />
    <form onSubmit={submit} noValidate>
      <TextField id="name" label="Full name" value={form.name} onChange={update('name')} placeholder="Your name" autoComplete="name" error={errors.name} />
      <div className="cd-field-row">
        <div className="cd-field-group"><label htmlFor="year">Student year</label><select id="year" value={form.year} onChange={update('year')} aria-invalid={Boolean(errors.year)} aria-describedby={errors.year ? 'year-error' : undefined}><option value="">Select year</option><option value="FIRST_YEAR">Year 1</option><option value="SECOND_YEAR">Year 2</option><option value="THIRD_YEAR">Year 3</option><option value="FOURTH_YEAR">Year 4</option><option value="FINAL_YEAR">Final year</option></select>{errors.year ? <span className="cd-field-error" id="year-error">{errors.year}</span> : null}</div>
        <div className="cd-field-group"><label htmlFor="gender">Gender</label><select id="gender" value={form.gender} onChange={update('gender')} aria-invalid={Boolean(errors.gender)} aria-describedby={errors.gender ? 'gender-error' : undefined}><option value="">Select gender</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option></select>{errors.gender ? <span className="cd-field-error" id="gender-error">{errors.gender}</span> : null}</div>
      </div>
      <TextField id="phone" label="Phone number" type="tel" value={form.phone} onChange={update('phone')} placeholder="+880 1XXXXXXXXX" autoComplete="tel" error={errors.phone} />
      <TextField id="create-email" label="Email address" type="email" value={form.email} onChange={update('email')} placeholder="you@example.com" autoComplete="email" error={errors.email} />
      <PasswordField id="new-password" label="Password" value={form.password} onChange={update('password')} placeholder="At least 8 characters" error={errors.password} autoComplete="new-password" />
      <PasswordField id="confirm-password" label="Confirm password" value={form.confirm} onChange={update('confirm')} placeholder="Enter your password again" error={errors.confirm} autoComplete="new-password" />
      {apiError ? <p className="cd-form-alert" role="alert">{apiError}</p> : null}
      <button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Creating account…' : 'Create account'} {!busy ? <ArrowRight size={16} /> : null}</button>
    </form>
    <p className="cd-auth-switch">Already part of the campus? <Link to="/signin">Sign in</Link></p>
    <AuthTrust />
  </div></AuthPageShell>
}

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!email.includes('@')) { setError('Enter a valid email address.'); return }
    setError('')
    setApiError('')
    setBusy(true)
    try {
      await authApi.forgotPassword(email.trim())
      navigate(`/verify-email?flow=reset&email=${encodeURIComponent(email.trim())}`)
    } catch (requestError) {
      setApiError(requestError instanceof Error ? requestError.message : 'Password recovery could not start.')
    } finally {
      setBusy(false)
    }
  }

  return <AuthPageShell><div className="cd-auth-card cd-focused-card">
    <div className="cd-auth-symbol"><KeyRound size={23} /></div>
    <div className="cd-auth-title"><p>Password recovery</p><h2>Let us get you back in.</h2><span>Enter your Campus Day email to continue to verification.</span></div>
    <form onSubmit={submit} noValidate><TextField id="recovery-email" label="Email address" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" autoComplete="email" error={error} />{apiError ? <p className="cd-form-alert" role="alert">{apiError}</p> : null}<button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Sending code…' : 'Continue to verification'} {!busy ? <ArrowRight size={16} /> : null}</button></form>
    <p className="cd-preview-note">A six-digit reset code will be sent to this email.</p>
    <p className="cd-auth-switch"><Link to="/signin"><ArrowLeft size={14} />Back to sign in</Link></p><AuthTrust />
  </div></AuthPageShell>
}

export function VerifyEmailPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const flow = params.get('flow') === 'reset' ? 'reset' : 'create'
  const email = params.get('email') || 'your email'
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [resending, setResending] = useState(false)
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!/^\d{6}$/.test(code)) { setError('Enter the 6-digit verification code.'); return }
    if (email === 'your email') { setError('Your email is missing. Start the process again.'); return }
    setError('')
    setNotice('')
    if (flow === 'reset') {
      savePasswordResetContext(email, code)
      navigate('/reset-password')
      return
    }
    setBusy(true)
    try {
      const tokens = await authApi.verifyEmail(email, code)
      saveAuthSession(tokens)
      navigate('/meal', { replace: true })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Email verification failed.')
    } finally {
      setBusy(false)
    }
  }

  const resend = async () => {
    if (email === 'your email') { setError('Your email is missing. Start the process again.'); return }
    setError('')
    setNotice('')
    setResending(true)
    try {
      if (flow === 'reset') await authApi.forgotPassword(email)
      else await authApi.resendVerification(email)
      setNotice('A new code was sent. Please check your inbox.')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'A new code could not be sent.')
    } finally {
      setResending(false)
    }
  }

  return <AuthPageShell><div className="cd-auth-card cd-focused-card">
    <div className="cd-auth-symbol"><Mail size={23} /></div>
    <div className="cd-auth-title"><p>Email verification</p><h2>Check your inbox.</h2><span>Enter the 6-digit code for <strong>{email}</strong>.</span></div>
    <form onSubmit={submit} noValidate><div className="cd-field-group cd-otp-group"><label htmlFor="otp">Verification code</label><input id="otp" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => { setCode(event.target.value.replace(/\D/g, '')); setError('') }} placeholder="000000" aria-invalid={Boolean(error)} aria-describedby={error ? 'otp-error' : undefined} />{error ? <span className="cd-field-error" id="otp-error" role="alert">{error}</span> : null}</div><button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Verifying…' : 'Verify email'} {!busy ? <ArrowRight size={16} /> : null}</button></form>
    <button className="cd-resend-button" type="button" onClick={resend} disabled={resending}><RefreshCw size={14} />{resending ? 'Sending…' : 'Resend code'}</button>
    {notice ? <p className="cd-form-notice" role="status">{notice}</p> : null}<AuthTrust />
  </div></AuthPageShell>
}

export function ResetPasswordPage() {
  const [resetContext] = useState(readPasswordResetContext)
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)
  const [complete, setComplete] = useState(false)
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (password.length < 8) next.password = 'Use at least 8 characters.'
    if (confirm !== password) next.confirm = 'Passwords do not match.'
    setErrors(next)
    setApiError('')
    if (Object.keys(next).length > 0) return
    if (!resetContext) { setApiError('Your reset session is missing. Request a new code.'); return }
    setBusy(true)
    try {
      await authApi.resetPassword(resetContext.email, resetContext.otp, password)
      clearPasswordResetContext()
      setComplete(true)
    } catch (requestError) {
      setApiError(requestError instanceof Error ? requestError.message : 'Password reset failed.')
    } finally {
      setBusy(false)
    }
  }

  return <AuthPageShell><div className="cd-auth-card cd-focused-card">
    {complete ? <div className="cd-success-state"><div className="cd-auth-symbol success"><CircleCheck size={25} /></div><div className="cd-auth-title"><p>Password updated</p><h2>You are ready to return.</h2><span>Your new password is active. Sign in to continue.</span></div><Link className="cd-auth-submit" to="/signin">Return to sign in <ArrowRight size={16} /></Link></div> : <><div className="cd-auth-symbol"><LockKeyhole size={23} /></div><div className="cd-auth-title"><p>Set a new password</p><h2>Choose something secure.</h2><span>Use at least 8 characters and avoid passwords you use elsewhere.</span></div><form onSubmit={submit} noValidate><PasswordField id="reset-password" label="New password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" error={errors.password} autoComplete="new-password" /><PasswordField id="reset-confirm" label="Confirm new password" value={confirm} onChange={(event) => setConfirm(event.target.value)} placeholder="Enter it again" error={errors.confirm} autoComplete="new-password" />{apiError ? <p className="cd-form-alert" role="alert">{apiError}</p> : null}<button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Updating password…' : 'Reset password'} {!busy ? <ArrowRight size={16} /> : null}</button></form></>}
    <AuthTrust />
  </div></AuthPageShell>
}

export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [error, setError] = useState('')
  const started = useRef(false)
  const code = params.get('code')

  useEffect(() => {
    if (started.current) return
    started.current = true
    if (!code) { setError('Google did not return a valid sign-in code.'); return }
    void authApi.exchangeOAuthCode(code)
      .then((tokens) => {
        saveAuthSession(tokens)
        navigate(tokens.account.profileComplete ? '/meal' : '/complete-profile', { replace: true })
      })
      .catch((requestError: unknown) => {
        setError(requestError instanceof Error ? requestError.message : 'Google sign in failed.')
      })
  }, [code, navigate])

  return <AuthPageShell><div className="cd-auth-card cd-focused-card">
    <div className="cd-auth-symbol"><ShieldCheck size={23} /></div>
    <div className="cd-auth-title"><p>Google sign in</p><h2>{error ? 'We could not sign you in.' : 'Connecting your campus account…'}</h2><span>{error || 'This will only take a moment.'}</span></div>
    {error ? <Link className="cd-auth-submit" to="/signin">Back to sign in <ArrowRight size={16} /></Link> : <div className="cd-auth-progress" role="status" aria-label="Completing Google sign in" />}
    <AuthTrust />
  </div></AuthPageShell>
}

export function CompleteProfilePage() {
  const navigate = useNavigate()
  const [session] = useState(readAuthSession)
  const [form, setForm] = useState({ name: '', year: '', gender: '', phone: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)
  const update = (key: keyof typeof form) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Enter your full name.'
    if (!form.year) next.year = 'Choose your student year.'
    if (!form.gender) next.gender = 'Choose a gender option.'
    if (!form.phone.trim()) next.phone = 'Enter your phone number.'
    setErrors(next)
    setApiError('')
    if (Object.keys(next).length > 0) return
    setBusy(true)
    try {
      const tokens = await authApi.completeProfile({
        fullName: form.name.trim(),
        academicYear: form.year as AcademicYear,
        gender: form.gender as StudentGender,
        phoneNumber: form.phone.trim(),
      })
      saveAuthSession(tokens)
      navigate('/meal', { replace: true })
    } catch (requestError) {
      setApiError(requestError instanceof Error ? requestError.message : 'Profile could not be completed.')
    } finally {
      setBusy(false)
    }
  }

  if (!session) return <AuthPageShell><div className="cd-auth-card cd-focused-card"><div className="cd-auth-title"><p>Session expired</p><h2>Sign in again.</h2><span>Your Google session is no longer available.</span></div><Link className="cd-auth-submit" to="/signin">Return to sign in <ArrowRight size={16} /></Link></div></AuthPageShell>

  return <AuthPageShell><div className="cd-auth-card cd-create-card">
    <div className="cd-auth-title"><p>One last step</p><h2>Complete your student profile.</h2><span>Signed in as <strong>{session.account.email}</strong>.</span></div>
    <form onSubmit={submit} noValidate>
      <TextField id="profile-name" label="Full name" value={form.name} onChange={update('name')} placeholder="Your name" autoComplete="name" error={errors.name} />
      <div className="cd-field-row">
        <div className="cd-field-group"><label htmlFor="profile-year">Student year</label><select id="profile-year" value={form.year} onChange={update('year')} aria-invalid={Boolean(errors.year)}><option value="">Select year</option><option value="FIRST_YEAR">Year 1</option><option value="SECOND_YEAR">Year 2</option><option value="THIRD_YEAR">Year 3</option><option value="FOURTH_YEAR">Year 4</option><option value="FINAL_YEAR">Final year</option></select>{errors.year ? <span className="cd-field-error">{errors.year}</span> : null}</div>
        <div className="cd-field-group"><label htmlFor="profile-gender">Gender</label><select id="profile-gender" value={form.gender} onChange={update('gender')} aria-invalid={Boolean(errors.gender)}><option value="">Select gender</option><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option></select>{errors.gender ? <span className="cd-field-error">{errors.gender}</span> : null}</div>
      </div>
      <TextField id="profile-phone" label="Phone number" type="tel" value={form.phone} onChange={update('phone')} placeholder="+880 1XXXXXXXXX" autoComplete="tel" error={errors.phone} />
      {apiError ? <p className="cd-form-alert" role="alert">{apiError}</p> : null}
      <button className="cd-auth-submit" type="submit" disabled={busy}>{busy ? 'Saving profile…' : 'Complete profile'} {!busy ? <ArrowRight size={16} /> : null}</button>
    </form>
    <AuthTrust />
  </div></AuthPageShell>
}
