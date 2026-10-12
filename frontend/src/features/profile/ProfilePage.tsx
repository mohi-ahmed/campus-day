import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Camera, Check, CheckCircle2, Clock3, ImagePlus, Mail, Phone, Send, ShieldCheck, Trash2, UserCog, UserRound, XCircle } from 'lucide-react'
import { useDemoUser } from '../../app/DemoUserContext'
import { PageHeader, Skeleton } from '../../components/ui'
import type { AcademicYear, StudentGender } from '../public/authApi'
import { profileApi, type StudentProfile } from './profileApi'
import { mealManagerApplicationApi, type MealManagerApplicationOverview } from './mealManagerApplicationApi'
import './profile.css'

type ProfileForm = {
  fullName: string
  academicYear: AcademicYear
  gender: StudentGender
  phoneNumber: string
}

function formFromProfile(profile: StudentProfile): ProfileForm {
  return {
    fullName: profile.name,
    academicYear: profile.academicYear,
    gender: profile.gender,
    phoneNumber: profile.phoneNumber,
  }
}

export default function ProfilePage() {
  const { refreshProfile } = useDemoUser()
  const fileInput = useRef<HTMLInputElement>(null)
  const [profile, setProfile] = useState<StudentProfile | null>(null)
  const [form, setForm] = useState<ProfileForm | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [managerOverview, setManagerOverview] = useState<MealManagerApplicationOverview | null>(null)
  const [managerLoading, setManagerLoading] = useState(true)
  const [managerApplying, setManagerApplying] = useState(false)
  const [managerError, setManagerError] = useState('')

  useEffect(() => {
    void profileApi.me()
      .then((current) => { setProfile(current); setForm(formFromProfile(current)) })
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : 'Profile could not be loaded.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    void mealManagerApplicationApi.current()
      .then(setManagerOverview)
      .catch((cause: unknown) => setManagerError(cause instanceof Error ? cause.message : 'Meal Manager application is unavailable.'))
      .finally(() => setManagerLoading(false))
  }, [])

  const update = (key: keyof ProfileForm) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm((current) => current ? { ...current, [key]: event.target.value } : current)
  }

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form?.fullName.trim() || !form.phoneNumber.trim()) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const updated = await profileApi.update({ ...form, fullName: form.fullName.trim(), phoneNumber: form.phoneNumber.trim() })
      setProfile(updated)
      setForm(formFromProfile(updated))
      await refreshProfile()
      setNotice('Your profile has been updated.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Profile could not be updated.')
    } finally {
      setSaving(false)
    }
  }

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setUploading(true)
    setError('')
    setNotice('')
    try {
      const updated = await profileApi.uploadImage(file)
      setProfile(updated)
      setForm(formFromProfile(updated))
      await refreshProfile()
      setNotice('Your profile photo has been updated.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Profile photo could not be uploaded.')
    } finally {
      setUploading(false)
    }
  }

  const removeImage = async () => {
    setUploading(true)
    setError('')
    setNotice('')
    try {
      await profileApi.removeImage()
      const updated = await profileApi.me()
      setProfile(updated)
      setForm(formFromProfile(updated))
      await refreshProfile()
      setNotice('Your profile photo has been removed.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Profile photo could not be removed.')
    } finally {
      setUploading(false)
    }
  }

  const applyForMealManager = async () => {
    setManagerApplying(true)
    setManagerError('')
    try {
      const application = await mealManagerApplicationApi.apply()
      setManagerOverview((current) => current ? { ...current, application } : current)
    } catch (cause) {
      setManagerError(cause instanceof Error ? cause.message : 'Your application could not be submitted.')
    } finally {
      setManagerApplying(false)
    }
  }

  if (loading) return <div className="page-enter profile-page"><Skeleton lines={10} /></div>
  if (!profile || !form) return <div className="page-enter profile-page"><PageHeader title="Your profile" description="Manage the details used across Campus Day." /><div className="profile-alert profile-alert--error">{error || 'Profile is unavailable.'}</div></div>

  const initials = profile.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  return <div className="page-enter profile-page">
    <PageHeader title="Your profile" description="Keep your student information and profile photo up to date." />
    <div className="profile-layout">
      <aside className="profile-photo-card">
        <div className="profile-avatar profile-avatar--large">
          {profile.profileImageUrl ? <img src={profile.profileImageUrl} alt={`${profile.name}'s profile`} /> : <span>{initials}</span>}
          <span className="profile-avatar__camera"><Camera size={16} /></span>
        </div>
        <h2>{profile.name}</h2>
        <p>{profile.studentId}</p>
        <input ref={fileInput} className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" onChange={upload} />
        <button className="button button--primary" type="button" disabled={uploading} onClick={() => fileInput.current?.click()}><ImagePlus size={17} />{uploading ? 'Uploading…' : profile.profileImageUrl ? 'Change photo' : 'Add photo'}</button>
        {profile.profileImageUrl ? <button className="profile-remove-photo" type="button" disabled={uploading} onClick={() => void removeImage()}><Trash2 size={15} />Remove photo</button> : null}
        <small>JPEG, PNG or WebP · maximum 5 MB</small>
      </aside>

      <section className="profile-form-card">
        <div className="profile-form-card__heading"><div><span>Personal information</span><h2>Student details</h2></div><ShieldCheck size={22} /></div>
        <form className="form-stack" onSubmit={save}>
          <label className="field"><span className="field__label">Full name</span><span className="profile-input-wrap"><UserRound size={17} /><input className="input" value={form.fullName} maxLength={120} required onChange={update('fullName')} /></span></label>
          <div className="form-grid">
            <label className="field"><span className="field__label">Student year</span><select className="input" value={form.academicYear} onChange={update('academicYear')}><option value="FIRST_YEAR">Year 1</option><option value="SECOND_YEAR">Year 2</option><option value="THIRD_YEAR">Year 3</option><option value="FOURTH_YEAR">Year 4</option><option value="FINAL_YEAR">Final year</option></select></label>
            <label className="field"><span className="field__label">Gender</span><select className="input" value={form.gender} onChange={update('gender')}><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option></select></label>
          </div>
          <label className="field"><span className="field__label">Phone number</span><span className="profile-input-wrap"><Phone size={17} /><input className="input" type="tel" value={form.phoneNumber} required onChange={update('phoneNumber')} /></span></label>
          <label className="field"><span className="field__label">Email address</span><span className="profile-input-wrap is-readonly"><Mail size={17} /><input className="input" value={profile.email} readOnly /></span><span className="field__hint">Your verified sign-in email cannot be changed here.</span></label>
          {error ? <div className="profile-alert profile-alert--error">{error}</div> : null}
          {notice ? <div className="profile-alert profile-alert--success"><Check size={16} />{notice}</div> : null}
          <div className="form-actions"><button className="button button--primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></div>
        </form>
      </section>
    </div>
    <MealManagerAccessPanel
      overview={managerOverview}
      loading={managerLoading}
      applying={managerApplying}
      error={managerError}
      onApply={() => void applyForMealManager()}
    />
  </div>
}

function MealManagerAccessPanel({ overview, loading, applying, error, onApply }: {
  overview: MealManagerApplicationOverview | null
  loading: boolean
  applying: boolean
  error: string
  onApply: () => void
}) {
  const application = overview?.application
  const status = application?.status
  const statusLabel = status === 'PENDING' ? 'Awaiting admin decision'
    : status === 'APPROVED' ? 'Access approved'
      : status === 'REJECTED' ? 'Application rejected'
        : status === 'EXPIRED' ? 'Application expired' : 'Not applied'
  const StatusIcon = status === 'APPROVED' ? CheckCircle2
    : status === 'REJECTED' || status === 'EXPIRED' ? XCircle : Clock3
  const deadline = application?.expiresAt
    ? new Intl.DateTimeFormat('en-BD', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Dhaka' }).format(new Date(application.expiresAt))
    : null

  return <section className="manager-access" aria-labelledby="manager-access-title">
    <div className="manager-access__intro">
      <span className="manager-access__icon"><UserCog size={22} /></span>
      <div>
        <h2 id="manager-access-title">Meal Manager access</h2>
        <p>Fourth Year students can request access to the Meal Management workspace.</p>
      </div>
    </div>
    <div className="manager-access__state">
      {loading ? <><span className="manager-access__skeleton" /><span className="manager-access__skeleton is-short" /></> : error ? <div className="profile-alert profile-alert--error">{error}</div> : overview?.currentAccess ? <>
        <span className="manager-access__status is-approved"><CheckCircle2 size={17} />Meal Manager access active</span>
        <p>You can now open the Meal Management workspace from the navigation.</p>
      </> : application ? <>
        <span className={`manager-access__status is-${status?.toLowerCase()}`}><StatusIcon size={17} />{statusLabel}</span>
        <p>{status === 'PENDING' ? `The admin can decide until ${deadline}. It will expire automatically after that.`
          : status === 'EXPIRED' ? 'No decision was made within 72 hours, so the request was automatically rejected.'
            : status === 'REJECTED' ? 'The admin rejected this request before the decision deadline.'
              : 'Your Meal Manager access is ready.'}</p>
      </> : overview?.eligible ? <>
        <strong>Ready to take responsibility?</strong>
        <p>The admin will receive an email with secure approve and reject options. The link stays valid for 72 hours.</p>
        <button className="button button--primary manager-access__action" type="button" disabled={applying} onClick={onApply}><Send size={16} />{applying ? 'Sending request…' : 'Apply for access'}</button>
      </> : <>
        <strong>Available in Fourth Year</strong>
        <p>Your profile must show Fourth Year before you can apply for Meal Manager access.</p>
      </>}
    </div>
  </section>
}
