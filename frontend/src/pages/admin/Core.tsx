import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ClientMarquee } from '../../components/motion'
import { Badge, Button, EmptyState, Field, PageHeader, SelectInput, TableShell, TextArea, TextInput, statusTone } from '../../components/desk-ui'
import { api, errorMessage } from '../../lib/api'
import { dateLabel, inr, titleCase } from '../../lib/format'
import { useAuth } from '../../lib/auth'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  return (
    <main className="grid min-h-dvh lg:grid-cols-2">
      <section className="relative hidden min-h-dvh overflow-hidden lg:block">
        <img src="/media/site-work.jpg" alt="" className="absolute inset-0 h-full w-full object-cover" />
        <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(160deg,#ffffff_0%,rgba(255,255,255,0.72)_42%,rgba(92,181,115,0.55)_100%)]" />
        <div className="relative flex h-full flex-col justify-between p-12">
          <span />
          <div>
            <h1 className="max-w-md text-balance text-5xl">The staffing desk.</h1>
            <p className="mt-4 max-w-sm text-pretty">Jobs, applications, placements and payroll.</p>
          </div>
          <p className="text-sm text-ink-soft">Staff only</p>
        </div>
      </section>
      <section className="flex items-center justify-center bg-paper p-6">
        <form className="w-full max-w-sm" onSubmit={async (event) => {
          event.preventDefault()
          setLoading(true)
          setError('')
          try {
            await login(email, password)
            navigate('/admin/dashboard')
          } catch (err) {
            setError(errorMessage(err, 'Email or password is incorrect.'))
          } finally {
            setLoading(false)
          }
        }}>
          <img src="/media/logo.png" alt="Narayan Staffing Services" className="mb-8 h-12 w-auto" />
          <h2 className="text-4xl">Sign in</h2>
          <div className="mt-6 grid gap-4">
            <Field label="Email" required><TextInput type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></Field>
            <Field label="Password" required><TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></Field>
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" variant="secondary" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</Button>
            <Link to="/admin/forgot-password" className="text-sm text-ink-soft hover:text-ink">Forgot password</Link>
          </div>
        </form>
      </section>
    </main>
  )
}

export function ForgotPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  return (
    <main className="mx-auto flex min-h-dvh max-w-md items-center px-4">
      <form className="grid w-full gap-4 border border-line bg-card p-6 shadow-sm" onSubmit={async (event) => {
        event.preventDefault()
        const res = await api.post('/api/auth/forgot-password', { email })
        setMessage(res.data.message)
      }}>
        <h1 className="text-balance text-4xl">Reset password</h1>
        <Field label="Email" required><TextInput type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></Field>
        <Button type="submit">Send reset link</Button>
        {message && <p className="text-sm text-pine">{message}</p>}
      </form>
    </main>
  )
}

export function ResetPage() {
  const token = new URLSearchParams(window.location.search).get('token') || ''
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  return (
    <main className="mx-auto flex min-h-dvh max-w-md items-center px-4">
      <form className="grid w-full gap-4 border border-line bg-card p-6 shadow-sm" onSubmit={async (event) => {
        event.preventDefault()
        setError('')
        try {
          const res = await api.post('/api/auth/reset-password', { token, password })
          setMessage(res.data.message)
        } catch (err) {
          setError(errorMessage(err))
        }
      }}>
        <h1 className="text-balance text-4xl">Choose a new password</h1>
        <Field label="New password" required><TextInput type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></Field>
        {error && <p className="text-sm text-danger">{error}</p>}
        {message && <p className="text-sm text-pine">{message}</p>}
        <Button type="submit">Update password</Button>
      </form>
    </main>
  )
}

export function DashboardPage() {
  const { can } = useAuth()
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['dashboard'], queryFn: async () => (await api.get('/api/dashboard')).data })
  const clients = useQuery({
    queryKey: ['dashboard-clients'],
    enabled: can('CMS_READ'),
    queryFn: async () => (await api.get<{ id: string; name: string; logoPath?: string }[]>('/api/clients')).data,
  })
  if (isLoading) return <div className="skeleton h-64 rounded-lg" />
  if (isError || !data) return <EmptyState title="Dashboard unavailable" body="The figures could not be loaded." action={<Button onClick={() => refetch()}>Retry</Button>} />
  const cards = [
    ['Jobs', data.openJobs], ['New', data.newApplications], ['Shortlist', data.shortlisted],
    ['Interviews', data.interviews], ['Selected', data.selected], ['Joined', data.joined],
    ['Staff', data.employees], ['Companies', data.companies],
  ]
  return (
    <div>
      <PageHeader title="Dashboard" />
      {clients.data && clients.data.length > 0 && (
        <section className="mb-4 overflow-hidden border border-line bg-paper">
          <ClientMarquee compact clients={clients.data.filter((client) => client.name)} />
        </section>
      )}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(([label, value]) => (
          <div key={String(label)} className="border border-line bg-card px-4 py-3">
            <p className="nums font-display text-4xl">{value}</p>
            <p className="text-sm text-ink-soft">{label}</p>
          </div>
        ))}
      </div>
      <div className="mt-4 grid gap-3 lg:grid-cols-2">
        <section className="border border-line bg-card p-4">
          <h2 className="text-xl">Months</h2>
          <div className="mt-3 space-y-2">
            {data.applicationsByMonth.map((row: { month: string; count: number }) => (
              <div key={row.month} className="grid grid-cols-[4.5rem_1fr_2rem] items-center gap-2 text-sm">
                <span>{row.month}</span>
                <div className="h-2 bg-paper-2"><div className="h-2 bg-saffron" style={{ width: `${Math.min(100, row.count * 20)}%` }} /></div>
                <span className="nums text-right">{row.count}</span>
              </div>
            ))}
          </div>
        </section>
        <section className="border border-line bg-card p-4">
          <h2 className="text-xl">Funnel</h2>
          <div className="mt-3 space-y-2">
            {data.funnel.map((row: { stage: string; count: number; percent: number }) => (
              <div key={row.stage} className="grid grid-cols-[7rem_1fr_2rem] items-center gap-2 text-sm">
                <span>{row.stage}</span>
                <div className="h-2 bg-paper-2"><div className="h-2 bg-saffron" style={{ width: `${row.percent}%` }} /></div>
                <span className="nums text-right">{row.count}</span>
              </div>
            ))}
          </div>
        </section>
        <section className="border border-line bg-card p-4">
          <h2 className="text-xl">Categories</h2>
          {data.applicationsByCategory.length === 0 && <p className="mt-3 text-sm text-ink-soft">None</p>}
          <div className="mt-3 space-y-2">
            {data.applicationsByCategory.map((row: { category: string; count: number }) => (
              <p key={row.category} className="flex justify-between text-sm"><span>{row.category}</span><span className="nums">{row.count}</span></p>
            ))}
          </div>
        </section>
        <section className="border border-line bg-card p-4">
          <h2 className="text-xl">Payroll</h2>
          <p className="nums mt-2 font-display text-4xl">{inr(data.payroll.net)}</p>
          <p className="mt-1 text-sm text-ink-soft">{inr(data.payroll.gross)} gross · {inr(data.payroll.deductions)} off</p>
          <div className="mt-3 space-y-1">
            {data.employeesByCompany.map((row: { company: string; count: number }) => (
              <p key={row.company} className="flex justify-between text-sm"><span>{row.company}</span><span className="nums">{row.count}</span></p>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}

type JobRow = { id: string; jobCode: string; title: string; companyName: string; location: string; status: string }

export function JobsPage() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [notice, setNotice] = useState('')
  const query = useQuery({ queryKey: ['admin-jobs', q], queryFn: async () => (await api.get('/api/jobs', { params: { q, size: 50 } })).data })
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Jobs" actions={<Link to="/admin/jobs/new" className="inline-flex h-11 items-center bg-pine px-5 font-display text-lg text-white hover:bg-saffron hover:text-ink">New job</Link>} />
      <TextInput placeholder="Search jobs" value={q} onChange={(e) => setQ(e.target.value)} className="mb-4 max-w-sm" />
      {notice && <p className="mb-3 text-sm text-danger">{notice}</p>}
      <TableShell>
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead className="bg-paper-2 text-xs uppercase text-ink-soft"><tr>{['Code', 'Title', 'Company', 'Location', 'Status', ''].map((h) => <th key={h} className="px-3 py-3 font-semibold">{h}</th>)}</tr></thead>
          <tbody>
            {query.data?.content?.map((job: JobRow) => (
              <tr key={job.id} className="cursor-pointer border-t border-line hover:bg-paper" onClick={() => navigate(`/admin/jobs/${job.id}`)}>
                <td className="px-3 py-3">{job.jobCode}</td>
                <td className="px-3 py-3 font-semibold">{job.title}</td>
                <td className="px-3 py-3">{job.companyName}</td>
                <td className="px-3 py-3">{job.location}</td>
                <td className="px-3 py-3"><Badge tone={statusTone(job.status)}>{titleCase(job.status)}</Badge></td>
                <td className="px-3 py-3 text-right" onClick={(event) => event.stopPropagation()}>
                  <button className="mr-3 font-semibold text-pine" type="button" onClick={() => {
                    if (job.status === 'PUBLISHED') {
                      setNotice(`Unpublish ${job.title} before editing it.`)
                      return
                    }
                    navigate(`/admin/jobs/${job.id}/edit`)
                  }}>Edit</button>
                  {job.status === 'PUBLISHED'
                    ? <button className="text-ink-soft" type="button" onClick={async () => { await api.post(`/api/jobs/${job.id}/unpublish`); setNotice(''); client.invalidateQueries({ queryKey: ['admin-jobs'] }) }}>Unpublish</button>
                    : <button className="text-ink-soft" type="button" onClick={async () => { await api.post(`/api/jobs/${job.id}/publish`); client.invalidateQueries({ queryKey: ['admin-jobs'] }) }}>Publish</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableShell>
      {!query.isLoading && query.data?.content?.length === 0 && <div className="mt-4"><EmptyState title="No jobs posted yet." body="Create a job and publish it to the careers page." /></div>}
    </div>
  )
}

type Applicant = {
  id: string
  applicationNumber: string
  candidateName: string
  email?: string
  mobile?: string
  location?: string
  experience?: number
  expectedSalary?: number
  qualification?: string
  status: string
  appliedAt: string
  resumeDocumentId?: string
}

export function JobApplicantsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const jobQuery = useQuery({ queryKey: ['job', id], queryFn: async () => (await api.get(`/api/jobs/${id}`)).data })
  const applicants = useQuery({ queryKey: ['job-applicants', id], queryFn: async () => (await api.get('/api/applications', { params: { jobId: id, size: 100 } })).data })
  const [error, setError] = useState('')
  const job = jobQuery.data
  const rows: Applicant[] = applicants.data?.content ?? []
  if (!job) return <div className="skeleton h-48" />
  async function decide(applicationId: string, status: 'SHORTLISTED' | 'REJECTED') {
    setError('')
    try {
      if (status === 'SHORTLISTED') await api.post(`/api/applications/${applicationId}/shortlist`)
      else await api.patch(`/api/applications/${applicationId}/status`, { status: 'REJECTED', note: 'Rejected from the job' })
      await client.invalidateQueries({ queryKey: ['job-applicants', id] })
    } catch (err) {
      setError(errorMessage(err))
    }
  }
  return (
    <div>
      <PageHeader title={job.title} lede={`${job.jobCode} · ${job.companyName} · ${job.location}`} actions={
        <>
          <Badge tone={statusTone(job.status)}>{titleCase(job.status)}</Badge>
          {job.status === 'PUBLISHED'
            ? <Button type="button" variant="ghost" onClick={async () => { await api.post(`/api/jobs/${id}/unpublish`); client.invalidateQueries({ queryKey: ['job', id] }); client.invalidateQueries({ queryKey: ['admin-jobs'] }) }}>Unpublish</Button>
            : <Button type="button" variant="ghost" onClick={async () => { await api.post(`/api/jobs/${id}/publish`); client.invalidateQueries({ queryKey: ['job', id] }); client.invalidateQueries({ queryKey: ['admin-jobs'] }) }}>Publish</Button>}
          <Button type="button" onClick={() => {
            if (job.status === 'PUBLISHED') {
              setError('Unpublish this job before editing it.')
              return
            }
            navigate(`/admin/jobs/${id}/edit`)
          }}>Edit</Button>
        </>
      } />
      {error && <p className="mb-3 text-sm text-danger">{error}</p>}
      <h2 className="text-2xl">Applicants</h2>
      {applicants.isLoading && !applicants.data && <div className="skeleton mt-3 h-40" />}
      {!applicants.isLoading && rows.length === 0 && <div className="mt-3"><EmptyState title="No one has applied yet." body="Applications for this job appear here with the candidate’s details and CV." /></div>}
      {rows.length > 0 && (
        <div className="mt-3">
          <TableShell>
            <table className="w-full min-w-[980px] text-left text-sm">
              <thead className="bg-paper-2"><tr>{['Candidate', 'Contact', 'Experience', 'Expected', 'Applied', 'Status', ''].map((heading) => <th key={heading} className="px-3 py-3 font-semibold">{heading}</th>)}</tr></thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id} className="border-t border-line">
                    <td className="px-3 py-3 align-middle">
                      <span className="block font-semibold">{row.candidateName}</span>
                      <span className="text-xs text-ink-soft">{row.applicationNumber}{row.qualification ? ` · ${row.qualification}` : ''}</span>
                    </td>
                    <td className="px-3 py-3 align-middle">{row.mobile || '—'}<div className="text-xs text-ink-soft">{row.email || row.location || '—'}</div></td>
                    <td className="px-3 py-3 align-middle tabular-nums">{row.experience ?? 0} yrs</td>
                    <td className="px-3 py-3 align-middle tabular-nums">{inr(row.expectedSalary)}</td>
                    <td className="px-3 py-3 align-middle">{dateLabel(row.appliedAt)}</td>
                    <td className="px-3 py-3 align-middle"><Badge tone={statusTone(row.status)}>{titleCase(row.status)}</Badge></td>
                    <td className="px-3 py-3 text-right align-middle">
                      <div className="flex flex-wrap justify-end gap-2">
                        <button className="h-8 border border-pine px-2 text-pine disabled:opacity-40" type="button" disabled={!row.resumeDocumentId} onClick={() => row.resumeDocumentId && openCv(row.resumeDocumentId)}>CV</button>
                        <button className="h-8 bg-pine px-2 text-white disabled:opacity-40" type="button" disabled={row.status === 'SHORTLISTED' || row.status === 'REJECTED'} onClick={() => decide(row.id, 'SHORTLISTED')}>Shortlist</button>
                        <button className="h-8 border border-line px-2 text-danger disabled:opacity-40" type="button" disabled={row.status === 'REJECTED'} onClick={() => decide(row.id, 'REJECTED')}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableShell>
        </div>
      )}
    </div>
  )
}

async function openCv(documentId: string) {
  const file = await api.get(`/api/documents/${documentId}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(file.data)
  window.open(url, '_blank', 'noopener')
}

export function JobFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const existing = useQuery({ queryKey: ['job', id], enabled: !!id && id !== 'new', queryFn: async () => (await api.get(`/api/jobs/${id}`)).data })
  const companies = useQuery({ queryKey: ['company-options'], queryFn: async () => (await api.get('/api/companies', { params: { size: 100 } })).data })
  const categories = useQuery({ queryKey: ['admin-categories'], queryFn: async () => (await api.get('/api/categories')).data })
  const job = existing.data
  const [form, setForm] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  if (id && id !== 'new' && existing.isLoading && !job) return <div className="skeleton h-48" />
  if (id && id !== 'new' && job?.status === 'PUBLISHED') {
    return (
      <div>
        <PageHeader title={job.title} lede="This job is published. Unpublish it before editing." actions={
          <Button type="button" onClick={async () => { await api.post(`/api/jobs/${id}/unpublish`); existing.refetch() }}>Unpublish</Button>
        } />
        <Link className="text-sm font-semibold text-pine" to={`/admin/jobs/${id}`}>Back to applicants</Link>
      </div>
    )
  }
  const value = (key: string, fallback = '') => form[key] ?? (job?.[key] ?? fallback)
  async function save(event: FormEvent) {
    event.preventDefault()
    const payload = {
      title: value('title'), categoryId: value('categoryId') || null, companyId: value('companyId'), location: value('location'),
      department: value('department'), employmentType: value('employmentType', 'CONTRACT'), experienceMin: num(value('experienceMin')),
      experienceMax: num(value('experienceMax')), education: value('education'), salaryMin: num(value('salaryMin')), salaryMax: num(value('salaryMax')),
      salaryType: value('salaryType', 'MONTHLY'), vacancies: Number(value('vacancies', '1')), jobDescription: value('jobDescription'),
      responsibilities: value('responsibilities'), requiredSkills: value('requiredSkills'), preferredSkills: value('preferredSkills'),
      benefits: value('benefits'), deadline: value('deadline') || null, featured: value('featured') === 'true',
    }
    try {
      const res = id && id !== 'new' ? await api.put(`/api/jobs/${id}`, payload) : await api.post('/api/jobs', payload)
      navigate(`/admin/jobs/${res.data.id}`)
    } catch (err) {
      setError(errorMessage(err))
    }
  }
  return (
    <form className="mx-auto max-w-3xl" onSubmit={save}>
      <PageHeader title={id && id !== 'new' ? 'Edit job' : 'New job'} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Title" required><TextInput value={value('title')} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
        <Field label="Company" required>
          <SelectInput value={value('companyId')} onChange={(e) => setForm({ ...form, companyId: e.target.value })} required>
            <option value="">Select</option>
            {companies.data?.content?.map((company: { id: string; companyName: string }) => <option key={company.id} value={company.id}>{company.companyName}</option>)}
          </SelectInput>
        </Field>
        <Field label="Category">
          <SelectInput value={value('categoryId')} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
            <option value="">None</option>
            {categories.data?.map((category: { id: string; name: string }) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </SelectInput>
        </Field>
        <Field label="Location" required><TextInput value={value('location')} onChange={(e) => setForm({ ...form, location: e.target.value })} required /></Field>
        <Field label="Department"><TextInput value={value('department')} onChange={(e) => setForm({ ...form, department: e.target.value })} /></Field>
        <Field label="Employment type">
          <SelectInput value={value('employmentType', 'CONTRACT')} onChange={(e) => setForm({ ...form, employmentType: e.target.value })}>
            {['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN'].map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}
          </SelectInput>
        </Field>
        <Field label="Experience min"><TextInput value={value('experienceMin')} onChange={(e) => setForm({ ...form, experienceMin: e.target.value })} /></Field>
        <Field label="Experience max"><TextInput value={value('experienceMax')} onChange={(e) => setForm({ ...form, experienceMax: e.target.value })} /></Field>
        <Field label="Salary min"><TextInput value={value('salaryMin')} onChange={(e) => setForm({ ...form, salaryMin: e.target.value })} /></Field>
        <Field label="Salary max"><TextInput value={value('salaryMax')} onChange={(e) => setForm({ ...form, salaryMax: e.target.value })} /></Field>
        <Field label="Vacancies" required><TextInput value={value('vacancies', '1')} onChange={(e) => setForm({ ...form, vacancies: e.target.value })} /></Field>
        <Field label="Deadline"><TextInput type="date" value={value('deadline')} onChange={(e) => setForm({ ...form, deadline: e.target.value })} /></Field>
      </div>
      <div className="mt-4 grid gap-4">
        <Field label="Description"><TextArea value={value('jobDescription')} onChange={(e) => setForm({ ...form, jobDescription: e.target.value })} /></Field>
        <Field label="Responsibilities"><TextArea value={value('responsibilities')} onChange={(e) => setForm({ ...form, responsibilities: e.target.value })} /></Field>
        <Field label="Required skills"><TextArea value={value('requiredSkills')} onChange={(e) => setForm({ ...form, requiredSkills: e.target.value })} /></Field>
        <Field label="Benefits"><TextArea value={value('benefits')} onChange={(e) => setForm({ ...form, benefits: e.target.value })} /></Field>
      </div>
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      <Button className="mt-4" type="submit">Save job</Button>
    </form>
  )
}

function num(value: string) {
  return value === '' ? null : Number(value)
}

export function ApplicationsPage() {
  const board = useQuery({ queryKey: ['board'], queryFn: async () => (await api.get('/api/applications/board')).data })
  const client = useQueryClient()
  const columns = ['APPLIED', 'SCREENING', 'SHORTLISTED', 'INTERVIEW', 'SELECTED', 'JOINED']
  const target: Record<string, string> = { APPLIED: 'APPLIED', SCREENING: 'SCREENING', SHORTLISTED: 'SHORTLISTED', INTERVIEW: 'INTERVIEW_SCHEDULED', SELECTED: 'SELECTED', JOINED: 'JOINED' }
  return (
    <div>
      <PageHeader title="Applications" lede="Drag a card to move it along the pipeline." />
      {board.isLoading && <div className="skeleton h-64 rounded-lg" />}
      <div className="grid gap-3 xl:grid-cols-6">
        {columns.map((column) => (
          <section key={column} className="bg-paper-2 p-2" onDragOver={(event) => event.preventDefault()} onDrop={async (event) => {
            const id = event.dataTransfer.getData('text/plain')
            await api.patch(`/api/applications/${id}/status`, { status: target[column], note: 'Moved on the board' }).catch(() => undefined)
            client.invalidateQueries({ queryKey: ['board'] })
          }}>
            <h2 className="px-2 py-2 text-sm font-semibold">{titleCase(column)}</h2>
            <div className="space-y-2">
              {(board.data?.[column] ?? []).map((card: { id: string; candidateName: string; jobTitle: string; location?: string; experience?: number; expectedSalary?: number; appliedAt: string; status: string }) => (
                <article key={card.id} draggable onDragStart={(event) => event.dataTransfer.setData('text/plain', card.id)} className="cursor-grab border border-line bg-card p-3 text-sm">
                  <Link to={`/admin/applications/${card.id}`} className="font-semibold">{card.candidateName}</Link>
                  <p className="text-ink-soft">{card.jobTitle}</p>
                  <p className="mt-1 text-xs text-ink-soft">{card.location} · {card.experience ?? 0} yrs · {inr(card.expectedSalary)}</p>
                  <p className="text-xs text-ink-soft">{dateLabel(card.appliedAt)}</p>
                  <Badge tone={statusTone(card.status)}>{titleCase(card.status)}</Badge>
                </article>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}

export function ApplicationDetailPage() {
  const { id } = useParams()
  const query = useQuery({ queryKey: ['application', id], queryFn: async () => (await api.get(`/api/applications/${id}`)).data })
  const client = useQueryClient()
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [interview, setInterview] = useState({ date: '', time: '', mode: 'IN_PERSON', location: '' })
  const app = query.data
  if (!app) return <div className="skeleton h-64 rounded-lg" />
  async function act(path: string, body?: unknown) {
    setError('')
    try {
      await api.post(path, body)
      client.invalidateQueries({ queryKey: ['application', id] })
    } catch (err) {
      setError(errorMessage(err))
    }
  }
  return (
    <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
      <div>
        <PageHeader title={app.candidate.fullName} lede={`${app.applicationNumber} · ${app.job.title} · ${app.job.companyName}`} actions={<Badge tone={statusTone(app.status)}>{titleCase(app.status)}</Badge>} />
        <div className="flex flex-wrap gap-2">
          <Button type="button" onClick={() => act(`/api/applications/${id}/shortlist`)}>Shortlist</Button>
          <Button type="button" variant="ghost" onClick={() => { if (confirm('Reject this application?')) api.patch(`/api/applications/${id}/status`, { status: 'REJECTED', note: 'Rejected' }).then(() => client.invalidateQueries({ queryKey: ['application', id] })).catch((err) => setError(errorMessage(err))) }}>Reject</Button>
          <Button type="button" variant="secondary" onClick={() => api.patch(`/api/applications/${id}/status`, { status: 'SELECTED', note: 'Selected' }).then(() => client.invalidateQueries({ queryKey: ['application', id] })).catch((err) => setError(errorMessage(err)))}>Select</Button>
          <Link to={`/admin/candidates/${app.candidate.id}`} className="inline-flex h-11 items-center text-sm font-semibold text-copper">Open profile</Link>
        </div>
        {error && <p className="mt-3 text-sm text-danger">{error}</p>}
        <section className="mt-6 border border-line bg-card p-4 text-sm">
          <p>{app.candidate.email} · {app.candidate.mobile}</p>
          <p>{app.candidate.city} · {app.candidate.totalExperience ?? 0} years · expected {inr(app.candidate.expectedSalary)}</p>
          <p className="mt-2">Skills: {(app.candidate.skills || []).join(', ') || '—'}</p>
        </section>
        <h2 className="mt-6 text-2xl">History</h2>
        <ul className="mt-2 space-y-2 text-sm">{app.history.map((item: { id: string; newStatus: string; changedAt: string; note?: string }) => <li key={item.id}>{dateLabel(item.changedAt)} · {titleCase(item.newStatus)} {item.note}</li>)}</ul>
      </div>
      <div className="space-y-4">
        <form className="border border-line bg-card p-4" onSubmit={async (event) => { event.preventDefault(); await api.post(`/api/applications/${id}/notes`, { note }); setNote(''); client.invalidateQueries({ queryKey: ['application', id] }) }}>
          <Field label="Internal note"><TextArea value={note} onChange={(e) => setNote(e.target.value)} /></Field>
          <Button className="mt-3" type="submit">Add note</Button>
          <ul className="mt-3 space-y-2 text-sm">{app.notes.map((item: { id: string; note: string; author: string }) => <li key={item.id}><strong>{item.author}:</strong> {item.note}</li>)}</ul>
        </form>
        <form className="border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          await api.post('/api/interviews', { applicationId: id, date: interview.date, time: interview.time, mode: interview.mode, location: interview.location })
          client.invalidateQueries({ queryKey: ['application', id] })
        }}>
          <h3 className="text-xl">Schedule interview</h3>
          <div className="mt-3 grid gap-3">
            <TextInput type="date" value={interview.date} onChange={(e) => setInterview({ ...interview, date: e.target.value })} required />
            <TextInput type="time" value={interview.time} onChange={(e) => setInterview({ ...interview, time: e.target.value })} required />
            <SelectInput value={interview.mode} onChange={(e) => setInterview({ ...interview, mode: e.target.value })}>
              <option value="IN_PERSON">In person</option>
              <option value="VIDEO">Video</option>
              <option value="PHONE">Phone</option>
            </SelectInput>
            <TextInput placeholder="Location or link" value={interview.location} onChange={(e) => setInterview({ ...interview, location: e.target.value })} />
            <Button type="submit">Schedule</Button>
          </div>
        </form>
      </div>
    </div>
  )
}
