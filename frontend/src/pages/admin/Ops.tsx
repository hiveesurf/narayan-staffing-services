import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Badge, Button, EmptyState, Field, PageHeader, SelectInput, TableShell, TextArea, TextInput, statusTone } from '../../components/desk-ui'
import { api, errorMessage } from '../../lib/api'
import { dateLabel, inr, titleCase } from '../../lib/format'

const PAGE_SIZE = 5

type CompanyRow = {
  id: string
  companyCode: string
  companyName: string
  legalName?: string
  industry?: string
  companyType?: string
  website?: string
  email?: string
  phone?: string
  address?: string
  city?: string
  state?: string
  country?: string
  pincode?: string
  gstNumber?: string
  pan?: string
  contactPerson?: string
  contactPersonDesignation?: string
  contactPersonPhone?: string
  contactPersonEmail?: string
  billingAddress?: string
  status: string
  notes?: string
  logoPath?: string
  coverPaths?: string[]
  createdAt?: string
}

function addressLine(company: CompanyRow) {
  const line = [company.address, company.city, company.state, company.pincode].filter(Boolean).join(', ')
  return line || '—'
}

async function uploadImage(id: string, kind: 'logo' | 'covers', file: File) {
  const body = new FormData()
  body.append('file', file)
  await api.post(`/api/companies/${id}/${kind}`, body)
}

function companyBody(base: CompanyRow | null, fields: { companyName: string; address: string; city: string; state: string; pincode: string }) {
  return {
    companyName: fields.companyName.trim(),
    legalName: base?.legalName ?? null,
    industry: base?.industry ?? null,
    companyType: base?.companyType ?? null,
    website: base?.website ?? null,
    email: base?.email || null,
    phone: base?.phone ?? null,
    address: fields.address || null,
    city: fields.city || null,
    state: fields.state || null,
    country: base?.country || 'India',
    pincode: fields.pincode || null,
    gstNumber: base?.gstNumber ?? null,
    pan: base?.pan ?? null,
    contactPerson: base?.contactPerson ?? null,
    contactPersonDesignation: base?.contactPersonDesignation ?? null,
    contactPersonPhone: base?.contactPersonPhone ?? null,
    contactPersonEmail: base?.contactPersonEmail || null,
    billingAddress: base?.billingAddress ?? null,
    status: base?.status ?? 'PROSPECT',
    notes: base?.notes ?? null,
  }
}

function ImagePick({ files, current = [], multiple, onPick }: { files: File[]; current?: string[]; multiple?: boolean; onPick: (files: File[]) => void }) {
  const [urls, setUrls] = useState<string[]>([])
  const signature = files.map((file) => `${file.name}:${file.size}:${file.lastModified}`).join('|')
  useEffect(() => {
    const next = files.map((file) => URL.createObjectURL(file))
    setUrls(next)
    return () => next.forEach((url) => URL.revokeObjectURL(url))
  }, [signature])
  const previews = urls.length > 0 ? urls : current
  return (
    <span className="flex h-11 items-center gap-2 border border-ink bg-card pl-1 pr-1">
      <span className="flex shrink-0 items-center gap-1">
        {previews.length === 0 && <span className="size-9 bg-paper-2" />}
        {previews.slice(0, 3).map((url) => <img key={url} src={url} alt="" className="size-9 object-cover" />)}
      </span>
      <span className="min-w-0 flex-1 truncate text-sm text-ink-soft">{files.length ? files.map((file) => file.name).join(', ') : 'PNG, JPG or WebP'}</span>
      <span className="inline-flex h-9 shrink-0 cursor-pointer items-center bg-saffron px-3 font-display text-sm uppercase text-ink">
        {previews.length ? (multiple ? 'Add' : 'Replace') : 'Choose'}
        <input className="sr-only" type="file" accept="image/png,image/jpeg,image/webp" multiple={multiple} onChange={(event) => {
          onPick(Array.from(event.target.files ?? []).slice(0, multiple ? 4 : 1))
          event.target.value = ''
        }} />
      </span>
    </span>
  )
}

const blankCompany = { name: '', address: '', city: '', state: 'Maharashtra', pincode: '' }

export function CompaniesPage() {
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<CompanyRow | null>(null)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [city, setCity] = useState('')
  const [state, setState] = useState('Maharashtra')
  const [pincode, setPincode] = useState('')
  const [logo, setLogo] = useState<File | null>(null)
  const [covers, setCovers] = useState<File[]>([])
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const client = useQueryClient()
  const query = useQuery({
    queryKey: ['companies', page],
    queryFn: async () => (await api.get('/api/companies', { params: { page, size: PAGE_SIZE, sort: 'createdAt', direction: 'desc' } })).data as {
      content: CompanyRow[]
      page: number
      size: number
      totalElements: number
      totalPages: number
    },
  })
  const rows = query.data?.content ?? []
  const total = query.data?.totalElements ?? 0
  const totalPages = query.data?.totalPages ?? 0

  function resetForm() {
    setEditing(null)
    setName(blankCompany.name)
    setAddress(blankCompany.address)
    setCity(blankCompany.city)
    setState(blankCompany.state)
    setPincode(blankCompany.pincode)
    setLogo(null)
    setCovers([])
    setError('')
  }

  function openCreate() {
    if (open && !editing) {
      setOpen(false)
      resetForm()
      return
    }
    resetForm()
    setOpen(true)
  }

  function openEdit(company: CompanyRow) {
    setEditing(company)
    setName(company.companyName)
    setAddress(company.address ?? '')
    setCity(company.city ?? '')
    setState(company.state ?? 'Maharashtra')
    setPincode(company.pincode ?? '')
    setLogo(null)
    setCovers([])
    setError('')
    setOpen(true)
  }

  async function refresh() {
    await client.invalidateQueries({ queryKey: ['companies'] })
  }

  async function setStatus(id: string, status: 'ACTIVE' | 'INACTIVE') {
    setBusy(id)
    setError('')
    try {
      await api.patch(`/api/companies/${id}/status`, { status })
      await refresh()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy('')
    }
  }

  async function removeCover(id: string, path: string) {
    setBusy(id)
    setError('')
    try {
      await api.delete(`/api/companies/${id}/covers`, { params: { path } })
      setEditing((current) => current && current.id === id
        ? { ...current, coverPaths: (current.coverPaths ?? []).filter((item) => item !== path) }
        : current)
      await refresh()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy('')
    }
  }

  return (
    <div>
      <PageHeader title="Companies" actions={<Button onClick={openCreate}>{open && !editing ? 'Close' : 'Add company'}</Button>} />
      {open && (
        <form className="mb-4 border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          setError('')
          setBusy('save')
          const current = editing
          const fields = { companyName: name, address, city, state, pincode }
          try {
            const saved = current
              ? await api.put(`/api/companies/${current.id}`, companyBody(current, fields))
              : await api.post('/api/companies', companyBody(null, fields))
            const id = saved.data.id as string
            if (logo) await uploadImage(id, 'logo', logo)
            for (const file of covers) await uploadImage(id, 'covers', file)
            setOpen(false)
            resetForm()
            if (!current) setPage(0)
            await refresh()
          } catch (err) {
            setError(errorMessage(err))
          } finally {
            setBusy('')
          }
        }}>
          <p className="font-display text-2xl">{editing ? 'Edit company' : 'New company'}</p>
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Name" required><TextInput value={name} onChange={(e) => setName(e.target.value)} required /></Field>
            <Field label="City"><TextInput value={city} onChange={(e) => setCity(e.target.value)} /></Field>
            <Field label="State"><TextInput value={state} onChange={(e) => setState(e.target.value)} /></Field>
            <Field label="Pincode"><TextInput value={pincode} onChange={(e) => setPincode(e.target.value)} /></Field>
            <div className="sm:col-span-2 lg:col-span-4">
              <Field label="Address"><TextInput value={address} onChange={(e) => setAddress(e.target.value)} /></Field>
            </div>
            <div className="sm:col-span-2">
              <Field label="Logo">
                <ImagePick files={logo ? [logo] : []} current={editing?.logoPath && !logo ? [editing.logoPath] : []} onPick={(picked) => setLogo(picked[0] ?? null)} />
              </Field>
            </div>
            <div className="sm:col-span-2">
              <Field label="Cover photos">
                <ImagePick files={covers} multiple current={covers.length ? [] : (editing?.coverPaths ?? []).slice(0, 1)} onPick={setCovers} />
              </Field>
              {(editing?.coverPaths ?? []).length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {editing?.coverPaths?.map((path) => (
                    <span key={path} className="flex h-11 items-center gap-2 border border-line bg-paper pl-1 pr-1">
                      <img src={path} alt="" className="h-9 w-14 object-cover" />
                      <button className="h-9 px-2 font-display text-sm uppercase disabled:opacity-50" type="button" disabled={busy === editing.id} onClick={() => removeCover(editing.id, path)}>Remove</button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="mt-4 flex items-center justify-end gap-2 border-t border-line pt-4">
            {error && <p className="mr-auto text-sm text-danger">{error}</p>}
            <Button type="button" variant="ghost" onClick={() => { setOpen(false); resetForm() }}>Cancel</Button>
            <Button type="submit" disabled={busy === 'save'}>{editing ? 'Save' : 'Create'}</Button>
          </div>
        </form>
      )}
      {error && !open && <p className="mb-3 text-sm text-danger">{error}</p>}
      {query.isLoading && !query.data && <div className="skeleton h-48" />}
      {!query.isLoading && rows.length === 0 && <EmptyState title="No companies yet." body="Add a company with its address, logo, and cover photos." />}
      {rows.length > 0 && (
        <TableShell>
          <table className="w-full min-w-[880px] text-left text-sm">
            <thead className="bg-paper-2">
              <tr>{['Photos', 'Company', 'Address', 'Added', 'Status', 'Action'].map((heading) => <th key={heading} className={`px-3 py-3 font-semibold ${heading === 'Action' ? 'text-right' : ''}`}>{heading}</th>)}</tr>
            </thead>
            <tbody>
              {rows.map((company) => (
                <tr key={company.id} className="border-t border-line">
                  <td className="px-3 py-3 align-middle">
                    <div className="flex items-center gap-2">
                      {company.logoPath
                        ? <img src={company.logoPath} alt="" className="size-10 object-cover" />
                        : <span className="flex size-10 items-center justify-center bg-paper-2 font-display text-lg">{company.companyName.slice(0, 1)}</span>}
                      {(company.coverPaths ?? []).slice(0, 1).map((path) => <img key={path} src={path} alt="" className="h-10 w-16 object-cover" />)}
                    </div>
                  </td>
                  <td className="px-3 py-3 align-middle">
                    <Link className="font-semibold" to={`/admin/companies/${company.id}`}>{company.companyName}</Link>
                    <p className="text-xs text-ink-soft">{company.companyCode}</p>
                  </td>
                  <td className="max-w-xs px-3 py-3 align-middle text-pretty">{addressLine(company)}</td>
                  <td className="whitespace-nowrap px-3 py-3 align-middle tabular-nums">{dateLabel(company.createdAt)}</td>
                  <td className="px-3 py-3 align-middle"><Badge tone={statusTone(company.status)}>{titleCase(company.status)}</Badge></td>
                  <td className="px-3 py-3 align-middle">
                    <div className="flex items-center justify-end gap-2">
                      <button className="h-8 border border-ink px-2 font-display text-sm uppercase" type="button" onClick={() => openEdit(company)}>Edit</button>
                      <button className={`h-8 px-2 font-display text-sm uppercase disabled:opacity-50 ${company.status === 'ACTIVE' ? 'bg-saffron text-ink' : 'border border-line text-ink'}`} type="button" aria-pressed={company.status === 'ACTIVE'} disabled={busy === company.id} onClick={() => setStatus(company.id, 'ACTIVE')}>Active</button>
                      <button className={`h-8 px-2 font-display text-sm uppercase disabled:opacity-50 ${company.status === 'INACTIVE' ? 'bg-ink text-paper' : 'border border-line text-ink'}`} type="button" aria-pressed={company.status === 'INACTIVE'} disabled={busy === company.id} onClick={() => setStatus(company.id, 'INACTIVE')}>Inactive</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableShell>
      )}
      <div className="mt-3 flex items-center justify-between text-sm">
        <p className="tabular-nums text-ink-soft">{total === 0 ? '0 companies' : `${page * PAGE_SIZE + 1}–${Math.min(total, (page + 1) * PAGE_SIZE)} of ${total}`}</p>
        <div className="flex gap-2">
          <button className="h-9 border border-ink px-3 disabled:opacity-40" type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button>
          <span className="flex h-9 items-center tabular-nums">{totalPages === 0 ? '1 / 1' : `${page + 1} / ${totalPages}`}</span>
          <button className="h-9 border border-ink px-3 disabled:opacity-40" type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button>
        </div>
      </div>
    </div>
  )
}

type StaffRow = {
  id: string
  employeeCode: string
  fullName: string
  designation?: string
  department?: string
  joiningDate?: string
  workLocation?: string
  status: string
  photoPath?: string
}

type CompanyJob = {
  id: string
  title: string
  jobCode: string
  location?: string
  department?: string
  status: string
  vacancies: number
  salaryMin?: number
  salaryMax?: number
}

const WORKING = new Set(['ACTIVE', 'ON_LEAVE'])
const OPEN_JOB = new Set(['PUBLISHED', 'PAUSED', 'DRAFT'])

function companyAbout(company: CompanyRow) {
  if (company.notes?.trim()) return company.notes.trim()
  const place = [company.city, company.state].filter(Boolean).join(', ')
  return [company.industry, company.companyType, place].filter(Boolean).join(' · ')
}

function companyFacts(company: CompanyRow) {
  return [
    ['Address', addressLine(company)],
    ['Phone', company.phone],
    ['Email', company.email],
    ['Website', company.website],
    ['Industry', company.industry],
    ['Type', company.companyType],
    ['Legal name', company.legalName],
    ['GST', company.gstNumber],
    ['PAN', company.pan],
    ['Contact', [company.contactPerson, company.contactPersonDesignation].filter(Boolean).join(', ')],
    ['Contact phone', company.contactPersonPhone],
    ['Contact email', company.contactPersonEmail],
    ['Billing address', company.billingAddress],
  ].filter((item): item is [string, string] => Boolean(item[1] && item[1] !== '—'))
}

export function CompanyDetailPage() {
  const { id } = useParams()
  const [staffView, setStaffView] = useState<'working' | 'previous'>('working')
  const [jobView, setJobView] = useState<'open' | 'closed'>('open')
  const query = useQuery({ queryKey: ['company', id], queryFn: async () => (await api.get(`/api/companies/${id}/profile`)).data })
  const people = useQuery({
    queryKey: ['company-employees', id],
    enabled: Boolean(id),
    queryFn: async () => (await api.get('/api/employees', { params: { companyId: id, size: 100, sort: 'fullName', direction: 'asc' } })).data as { content: StaffRow[] },
  })
  const jobs = useQuery({
    queryKey: ['company-jobs', id],
    enabled: Boolean(id),
    queryFn: async () => (await api.get('/api/jobs', { params: { companyId: id, size: 100, sort: 'title', direction: 'asc' } })).data as { content: CompanyJob[] },
  })
  const profile = query.data
  if (query.isLoading && !profile) return <div className="skeleton h-64" />
  if (!profile) return <p className="text-sm text-danger">{errorMessage(query.error)}</p>
  const company = profile.company as CompanyRow
  const cover = company.coverPaths?.[0]
  const staff = people.data?.content ?? []
  const openings = jobs.data?.content ?? []
  const working = staff.filter((person) => WORKING.has(person.status))
  const previous = staff.filter((person) => !WORKING.has(person.status))
  const openJobs = openings.filter((job) => OPEN_JOB.has(job.status))
  const closedJobs = openings.filter((job) => !OPEN_JOB.has(job.status))
  const shownStaff = staffView === 'working' ? working : previous
  const shownJobs = jobView === 'open' ? openJobs : closedJobs
  const about = companyAbout(company)
  return (
    <div>
      <section className="border border-line bg-card">
        <div className="h-44 bg-paper-2 sm:h-56">
          {cover && <img src={cover} alt="" className="size-full object-cover" />}
        </div>
        <div className="px-4 pb-5 sm:px-6">
          <div className="-mt-10 flex items-end gap-4">
            {company.logoPath
              ? <img src={company.logoPath} alt="" className="size-20 border-4 border-card object-cover" />
              : <span className="flex size-20 items-center justify-center border-4 border-card bg-paper-2 font-display text-3xl text-pine">{company.companyName.slice(0, 1)}</span>}
            <Badge tone={statusTone(company.status)}>{titleCase(company.status)}</Badge>
          </div>
          <h1 className="mt-3 text-balance text-4xl">{company.companyName}</h1>
          <p className="mt-1 text-sm text-ink-soft">{company.companyCode} · Added {dateLabel(company.createdAt)}</p>
          {about && <p className="mt-3 max-w-3xl text-pretty text-ink">{about}</p>}
          <dl className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {companyFacts(company).map(([label, value]) => (
              <div key={label}>
                <dt className="text-sm text-ink-soft">{label}</dt>
                <dd className="text-pretty">{value}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>
      <div className="mt-4 grid gap-3 sm:grid-cols-4">
        <Stat label="Open jobs" value={profile.openJobs} />
        <Stat label="Working now" value={people.data ? working.length : profile.activeEmployees} />
        <Stat label="Placements" value={profile.placements} />
        <div className="border border-line bg-card p-4"><p className="text-sm text-ink-soft">Payroll net</p><p className="font-display text-3xl tabular-nums">{inr(profile.payrollNet)}</p></div>
      </div>

      <section className="mt-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <h2 className="text-balance text-2xl">Employees</h2>
          <FilterChoice
            label="Employee tenure"
            value={staffView}
            onChange={setStaffView}
            options={[
              { id: 'working', label: 'Working', count: working.length },
              { id: 'previous', label: 'Previously working', count: previous.length },
            ]}
          />
        </div>
        {people.isLoading && !people.data && <div className="skeleton mt-3 h-32" />}
        {people.isError && <p className="mt-3 text-sm text-danger">{errorMessage(people.error)}</p>}
        {people.data && shownStaff.length === 0 && (
          <div className="mt-3">
            <EmptyState
              title={staffView === 'working' ? 'No one is working here now.' : 'No previous employees.'}
              body={staffView === 'working' ? 'People placed at this company appear here while they are active or on leave.' : 'People who have resigned, been terminated, or marked inactive appear here.'}
            />
          </div>
        )}
        {shownStaff.length > 0 && (
          <div className="mt-3">
            <TableShell>
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="bg-paper-2">
                  <tr>{['Employee', 'Role', 'Joined', 'Status'].map((heading) => <th key={heading} className="px-3 py-3 font-semibold">{heading}</th>)}</tr>
                </thead>
                <tbody>
                  {shownStaff.map((person) => (
                    <tr key={person.id} className="border-t border-line">
                      <td className="px-3 py-3">
                        <Link className="flex items-center gap-3" to={`/admin/employees/${person.id}`}>
                          {person.photoPath
                            ? <img src={person.photoPath} alt="" className="size-10 object-cover" />
                            : <span className="flex size-10 items-center justify-center bg-paper-2 font-display text-lg text-pine">{person.fullName.slice(0, 1)}</span>}
                          <span>
                            <span className="block font-semibold">{person.fullName}</span>
                            <span className="block text-xs text-ink-soft">{person.employeeCode}</span>
                          </span>
                        </Link>
                      </td>
                      <td className="px-3 py-3 text-pretty">{[person.designation, person.department, person.workLocation].filter(Boolean).join(' · ') || '—'}</td>
                      <td className="px-3 py-3 tabular-nums">{person.joiningDate ? dateLabel(person.joiningDate) : '—'}</td>
                      <td className="px-3 py-3"><Badge tone={statusTone(person.status)}>{titleCase(person.status)}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableShell>
          </div>
        )}
      </section>

      <section className="mt-8">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <h2 className="text-balance text-2xl">Jobs</h2>
          <FilterChoice
            label="Job availability"
            value={jobView}
            onChange={setJobView}
            options={[
              { id: 'open', label: 'Open', count: openJobs.length },
              { id: 'closed', label: 'Closed', count: closedJobs.length },
            ]}
          />
        </div>
        {jobs.isLoading && !jobs.data && <div className="skeleton mt-3 h-32" />}
        {jobs.isError && <p className="mt-3 text-sm text-danger">{errorMessage(jobs.error)}</p>}
        {jobs.data && shownJobs.length === 0 && (
          <div className="mt-3">
            <EmptyState
              title={jobView === 'open' ? 'No open jobs.' : 'No closed jobs.'}
              body={jobView === 'open' ? 'Published, paused, and draft jobs for this company appear here.' : 'Jobs marked closed or archived appear here.'}
            />
          </div>
        )}
        {shownJobs.length > 0 && (
          <div className="mt-3">
            <TableShell>
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="bg-paper-2">
                  <tr>{['Job', 'Location', 'Pay', 'Vacancies', 'Status'].map((heading) => <th key={heading} className="px-3 py-3 font-semibold">{heading}</th>)}</tr>
                </thead>
                <tbody>
                  {shownJobs.map((job) => (
                    <tr key={job.id} className="border-t border-line">
                      <td className="px-3 py-3">
                        <Link className="font-semibold" to={`/admin/jobs/${job.id}`}>{job.title}</Link>
                        <p className="text-xs text-ink-soft">{job.jobCode}{job.department ? ` · ${job.department}` : ''}</p>
                      </td>
                      <td className="px-3 py-3">{job.location || '—'}</td>
                      <td className="px-3 py-3 tabular-nums">{job.salaryMin || job.salaryMax ? `${inr(job.salaryMin)}–${inr(job.salaryMax)}` : '—'}</td>
                      <td className="px-3 py-3 tabular-nums">{job.vacancies}</td>
                      <td className="px-3 py-3"><Badge tone={statusTone(job.status)}>{titleCase(job.status)}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableShell>
          </div>
        )}
      </section>
    </div>
  )
}

function FilterChoice<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: { id: T; label: string; count: number }[]; onChange: (id: T) => void }) {
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label={label}>
      {options.map((option) => {
        const selected = option.id === value
        return (
          <button
            key={option.id}
            type="button"
            aria-pressed={selected}
            className={`h-10 px-3 font-display text-lg uppercase focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-pine ${selected ? 'bg-pine text-white' : 'border border-line bg-card text-pine'}`}
            onClick={() => onChange(option.id)}
          >
            {option.label} <span className="tabular-nums">{option.count}</span>
          </button>
        )
      })}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return <div className="border border-line bg-card p-4"><p className="text-sm text-ink-soft">{label}</p><p className="font-display text-3xl">{value}</p></div>
}

export function CandidatesPage() {
  const [q, setQ] = useState('')
  const query = useQuery({ queryKey: ['candidates', q], queryFn: async () => (await api.get('/api/candidates', { params: { q, size: 50 } })).data })
  return (
    <div>
      <PageHeader title="Candidates" />
      <TextInput className="mb-4 max-w-sm" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} />
      {query.data?.content?.length === 0 && <EmptyState title="No candidates yet." body="Applications from the careers page appear here." />}
      <div className="grid gap-3">
        {query.data?.content?.map((person: { id: string; fullName: string; email: string; city?: string; totalExperience?: number }) => (
          <Link key={person.id} to={`/admin/candidates/${person.id}`} className="border border-line bg-card p-4">
            <p className="font-semibold">{person.fullName}</p>
            <p className="text-sm text-ink-soft">{person.email} · {person.city || '—'} · {person.totalExperience ?? 0} yrs</p>
          </Link>
        ))}
      </div>
    </div>
  )
}

export function CandidateDetailPage() {
  const { id } = useParams()
  const query = useQuery({ queryKey: ['candidate', id], queryFn: async () => (await api.get(`/api/candidates/${id}`)).data })
  const client = useQueryClient()
  const person = query.data
  if (!person) return <div className="skeleton h-48 rounded-lg" />
  return (
    <div>
      <PageHeader title={person.fullName} lede={`${person.email} · ${person.mobile}`} actions={<Button onClick={async () => { const res = await api.post(`/api/employees/from-candidate/${id}`); window.location.href = `/admin/employees/${res.data.id}` }}>Create employee profile</Button>} />
      <div className="grid gap-4 border border-line bg-card p-4 text-sm sm:grid-cols-2">
        <p>City: {person.city || '—'}</p>
        <p>Experience: {person.totalExperience ?? 0} years</p>
        <p>Aadhaar: {person.aadhaar || '—'}</p>
        <p>PAN: {person.pan || '—'}</p>
      </div>
      <h2 className="mt-6 text-2xl">Education</h2>
      <ul className="text-sm">{person.education.map((row: { id: string; degree: string; institution?: string }) => <li key={row.id}>{row.degree} · {row.institution}</li>)}</ul>
      <Button className="mt-4" variant="ghost" onClick={() => client.invalidateQueries({ queryKey: ['candidate', id] })}>Refresh</Button>
    </div>
  )
}

export function PlacementsPage() {
  const query = useQuery({ queryKey: ['placements'], queryFn: async () => (await api.get('/api/placements', { params: { size: 50 } })).data })
  const [form, setForm] = useState({ candidateId: '', companyId: '', designation: '', agreedSalary: '', placementStatus: 'OFFERED' })
  const [error, setError] = useState('')
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Placements" lede="A placement links a candidate to a company without overwriting earlier assignments." />
      <form className="mb-4 grid gap-3 border border-line bg-card p-4 md:grid-cols-3" onSubmit={async (event) => {
        event.preventDefault()
        try {
          await api.post('/api/placements', { ...form, agreedSalary: form.agreedSalary ? Number(form.agreedSalary) : null })
          client.invalidateQueries({ queryKey: ['placements'] })
        } catch (err) { setError(errorMessage(err)) }
      }}>
        <Field label="Candidate ID" required><TextInput value={form.candidateId} onChange={(e) => setForm({ ...form, candidateId: e.target.value })} required /></Field>
        <Field label="Company ID" required><TextInput value={form.companyId} onChange={(e) => setForm({ ...form, companyId: e.target.value })} required /></Field>
        <Field label="Designation"><TextInput value={form.designation} onChange={(e) => setForm({ ...form, designation: e.target.value })} /></Field>
        <Field label="Agreed salary"><TextInput value={form.agreedSalary} onChange={(e) => setForm({ ...form, agreedSalary: e.target.value })} /></Field>
        <Field label="Status">
          <SelectInput value={form.placementStatus} onChange={(e) => setForm({ ...form, placementStatus: e.target.value })}>
            {['OFFERED', 'ACCEPTED', 'JOINING_PENDING', 'JOINED', 'ACTIVE'].map((status) => <option key={status}>{status}</option>)}
          </SelectInput>
        </Field>
        <Button className="self-end" type="submit">Create placement</Button>
        {error && <p className="text-sm text-danger md:col-span-3">{error}</p>}
      </form>
      <TableShell>
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-paper-2"><tr>{['Candidate', 'Company', 'Designation', 'Joining', 'Status'].map((h) => <th key={h} className="px-3 py-3">{h}</th>)}</tr></thead>
          <tbody>
            {query.data?.content?.map((row: { id: string; candidateName: string; companyName: string; designation?: string; joiningDate?: string; placementStatus: string }) => (
              <tr key={row.id} className="border-t border-line"><td className="px-3 py-3">{row.candidateName}</td><td className="px-3 py-3">{row.companyName}</td><td className="px-3 py-3">{row.designation}</td><td className="px-3 py-3">{dateLabel(row.joiningDate)}</td><td className="px-3 py-3"><Badge tone={statusTone(row.placementStatus)}>{titleCase(row.placementStatus)}</Badge></td></tr>
            ))}
          </tbody>
        </table>
      </TableShell>
    </div>
  )
}

const employmentTypes = ['CONTRACT', 'FULL_TIME', 'PART_TIME', 'TEMPORARY', 'INTERN']

type EmployeeForm = {
  fullName: string
  mobile: string
  email: string
  address: string
  city: string
  companyId: string
  designation: string
  department: string
  joiningDate: string
  employmentType: string
  workLocation: string
  reportingManager: string
  notes: string
}

const emptyEmployee: EmployeeForm = {
  fullName: '', mobile: '', email: '', address: '', city: '', companyId: '', designation: '', department: '',
  joiningDate: '', employmentType: 'CONTRACT', workLocation: '', reportingManager: '', notes: '',
}

export function EmployeesPage() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(emptyEmployee)
  const [photo, setPhoto] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const companies = useQuery({ queryKey: ['company-options'], queryFn: async () => (await api.get('/api/companies', { params: { size: 100 } })).data })
  const query = useQuery({ queryKey: ['employees', q], queryFn: async () => (await api.get('/api/employees', { params: { q, size: 50 } })).data })
  const rows = query.data?.content ?? []
  function set<K extends keyof EmployeeForm>(key: K, value: EmployeeForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }
  return (
    <div>
      <PageHeader title="Employees" actions={
        <>
          <Button onClick={() => { setOpen((value) => !value); setError('') }}>{open ? 'Close' : 'Add employee'}</Button>
          <Button variant="ghost" type="button" onClick={async () => {
            const res = await api.get('/api/reports/export', { params: { type: 'employees', format: 'csv' }, responseType: 'blob' })
            const url = URL.createObjectURL(res.data)
            const link = document.createElement('a')
            link.href = url
            link.download = 'employees.csv'
            link.click()
          }}>Export CSV</Button>
        </>
      } />
      {open && (
        <form className="mb-4 border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          setBusy(true)
          setError('')
          try {
            const saved = await api.post('/api/employees', { ...form, companyId: form.companyId, joiningDate: form.joiningDate })
            if (photo) {
              const body = new FormData()
              body.append('file', photo)
              await api.post(`/api/employees/${saved.data.id}/photo`, body)
            }
            setOpen(false)
            setForm(emptyEmployee)
            setPhoto(null)
            navigate(`/admin/employees/${saved.data.id}`)
          } catch (err) {
            setError(errorMessage(err))
          } finally {
            setBusy(false)
          }
        }}>
          <p className="font-display text-2xl">New employee</p>
          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Name" required><TextInput value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required /></Field>
            <Field label="Mobile" required><TextInput value={form.mobile} onChange={(e) => set('mobile', e.target.value)} required /></Field>
            <Field label="Email" required><TextInput type="email" value={form.email} onChange={(e) => set('email', e.target.value)} required /></Field>
            <Field label="Company" required>
              <SelectInput value={form.companyId} onChange={(e) => set('companyId', e.target.value)} required>
                <option value="">Select company</option>
                {companies.data?.content?.map((company: { id: string; companyName: string }) => <option key={company.id} value={company.id}>{company.companyName}</option>)}
              </SelectInput>
            </Field>
            <Field label="Designation"><TextInput value={form.designation} onChange={(e) => set('designation', e.target.value)} /></Field>
            <Field label="Department"><TextInput value={form.department} onChange={(e) => set('department', e.target.value)} /></Field>
            <Field label="Working since" required><TextInput type="date" value={form.joiningDate} onChange={(e) => set('joiningDate', e.target.value)} required /></Field>
            <Field label="Employment">
              <SelectInput value={form.employmentType} onChange={(e) => set('employmentType', e.target.value)}>
                {employmentTypes.map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}
              </SelectInput>
            </Field>
            <div className="sm:col-span-2 lg:col-span-4">
              <Field label="Address"><TextInput value={form.address} onChange={(e) => set('address', e.target.value)} /></Field>
            </div>
            <div className="sm:col-span-2">
              <Field label="Profile photo">
                <ImagePick files={photo ? [photo] : []} onPick={(picked) => setPhoto(picked[0] ?? null)} />
              </Field>
            </div>
            <Field label="City"><TextInput value={form.city} onChange={(e) => set('city', e.target.value)} /></Field>
            <Field label="Work location"><TextInput value={form.workLocation} onChange={(e) => set('workLocation', e.target.value)} /></Field>
          </div>
          <div className="mt-4 flex items-center justify-end gap-2 border-t border-line pt-4">
            {error && <p className="mr-auto text-sm text-danger">{error}</p>}
            <Button type="button" variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={busy}>Create</Button>
          </div>
        </form>
      )}
      <TextInput className="mb-4 max-w-sm" placeholder="Search" value={q} onChange={(e) => setQ(e.target.value)} />
      {query.isLoading && !query.data && <div className="skeleton h-48" />}
      {!query.isLoading && rows.length === 0 && <EmptyState title="No employees yet." body="Add an employee and assign the company they joined." />}
      {rows.length > 0 && (
        <TableShell>
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead className="bg-paper-2"><tr>{['Employee', 'Company', 'Since', 'Designation', 'Salary', 'Status'].map((heading) => <th key={heading} className="px-3 py-3 font-semibold">{heading}</th>)}</tr></thead>
            <tbody>
              {rows.map((row: { id: string; employeeCode: string; fullName: string; photoPath?: string; designation?: string; companyName?: string; joiningDate?: string; currentNet?: number; status: string }) => (
                <tr key={row.id} className="cursor-pointer border-t border-line hover:bg-paper-2" onClick={() => navigate(`/admin/employees/${row.id}`)}>
                  <td className="px-3 py-3 align-middle">
                    <div className="flex items-center gap-3">
                      {row.photoPath
                        ? <img src={row.photoPath} alt="" className="size-10 object-cover" />
                        : <span className="flex size-10 items-center justify-center bg-paper-2 font-display text-lg">{row.fullName.slice(0, 1)}</span>}
                      <span>
                        <span className="block font-semibold">{row.fullName}</span>
                        <span className="text-xs text-ink-soft">{row.employeeCode}</span>
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-3 align-middle">{row.companyName || '—'}</td>
                  <td className="whitespace-nowrap px-3 py-3 align-middle tabular-nums">{dateLabel(row.joiningDate)}</td>
                  <td className="px-3 py-3 align-middle">{row.designation || '—'}</td>
                  <td className="whitespace-nowrap px-3 py-3 align-middle tabular-nums">{inr(row.currentNet)}</td>
                  <td className="px-3 py-3 align-middle"><Badge tone={statusTone(row.status)}>{titleCase(row.status)}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </TableShell>
      )}
    </div>
  )
}

function Fact({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-sm text-ink-soft">{label}</p>
      <p className="mt-1">{value || '—'}</p>
    </div>
  )
}

type ComponentDef = {
  id: string
  name: string
  code: string
  componentType: 'EARNING' | 'DEDUCTION'
  calculationType: string
  defaultValue: number
  displayOrder: number
  active: boolean
}

type StructureLine = {
  code: string
  name: string
  type: 'EARNING' | 'DEDUCTION'
  calculationType: string
  value: number
  amount: number
}

type SalaryStructure = {
  id: string
  effectiveDate: string
  active: boolean
  gross: number
  deductions: number
  net: number
  lines: StructureLine[]
}

type DraftLine = { calculationType: string; value: string }

const calculations = [
  ['FIXED', 'Fixed amount'],
  ['PERCENT_OF_BASIC', '% of basic'],
  ['PERCENT_OF_GROSS', '% of gross'],
] as const

function rateLabel(calculationType: string, value: number) {
  const amount = Number(value)
  if (calculationType === 'PERCENT_OF_BASIC') return `${amount}% of basic`
  if (calculationType === 'PERCENT_OF_GROSS') return `${amount}% of gross`
  return inr(amount)
}

function roundMoney(value: number) {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

function lineAmount(calculationType: string, value: number, basic: number, gross: number) {
  if (calculationType === 'PERCENT_OF_BASIC') return roundMoney(basic * value / 100)
  if (calculationType === 'PERCENT_OF_GROSS') return roundMoney(gross * value / 100)
  return roundMoney(value)
}

function previewAmounts(rows: { code: string; type: string; calculationType: string; value: number; displayOrder: number }[]) {
  const basicRow = rows.find((row) => row.code === 'BASIC')
  const basic = !basicRow || basicRow.calculationType === 'PERCENT_OF_BASIC' || basicRow.calculationType === 'PERCENT_OF_GROSS'
    ? 0
    : roundMoney(basicRow.value)
  const earnings = rows.filter((row) => row.type === 'EARNING').sort((a, b) => a.displayOrder - b.displayOrder)
  const deductions = rows.filter((row) => row.type === 'DEDUCTION').sort((a, b) => a.displayOrder - b.displayOrder)
  const amounts = new Map<string, number>()
  let grossBase = 0
  const percentGrossEarnings = []
  for (const row of earnings) {
    if (row.calculationType === 'PERCENT_OF_GROSS' && row.code !== 'BASIC') {
      percentGrossEarnings.push(row)
      continue
    }
    const amount = row.code === 'BASIC' ? basic : lineAmount(row.calculationType, row.value, basic, 0)
    amounts.set(row.code, amount)
    grossBase += amount
  }
  let gross = grossBase
  for (const row of percentGrossEarnings) {
    const amount = roundMoney(grossBase * row.value / 100)
    amounts.set(row.code, amount)
    gross += amount
  }
  let deductionTotal = 0
  const percentGrossDeductions = []
  for (const row of deductions) {
    if (row.calculationType === 'PERCENT_OF_GROSS') {
      percentGrossDeductions.push(row)
      continue
    }
    const amount = lineAmount(row.calculationType, row.value, basic, gross)
    amounts.set(row.code, amount)
    deductionTotal += amount
  }
  for (const row of percentGrossDeductions) {
    const amount = roundMoney(gross * row.value / 100)
    amounts.set(row.code, amount)
    deductionTotal += amount
  }
  return {
    gross: roundMoney(gross),
    deductions: roundMoney(deductionTotal),
    net: roundMoney(gross - deductionTotal),
    amounts,
  }
}

function structureDraft(defs: ComponentDef[], structure?: SalaryStructure) {
  const saved = new Map((structure?.lines ?? []).map((line) => [line.code, line]))
  const draft: Record<string, DraftLine> = {}
  for (const item of defs.filter((item) => item.active)) {
    const line = saved.get(item.code)
    draft[item.code] = {
      calculationType: line?.calculationType ?? item.calculationType,
      value: String(line?.value ?? item.defaultValue ?? 0),
    }
  }
  return draft
}

function SalaryStructurePanel({ defs, structure, editing, draft, onDraft, onSave, busy, error }: {
  defs: ComponentDef[]
  structure?: SalaryStructure
  editing: boolean
  draft: Record<string, DraftLine>
  onDraft: (code: string, patch: Partial<DraftLine>) => void
  onSave: () => void
  busy: boolean
  error: string
}) {
  const rows = defs.filter((item) => item.active).sort((a, b) => a.displayOrder - b.displayOrder).map((item) => {
    const saved = structure?.lines.find((line) => line.code === item.code)
    const edited = draft[item.code]
    return {
      ...item,
      calculationType: editing ? (edited?.calculationType ?? saved?.calculationType ?? item.calculationType) : (saved?.calculationType ?? item.calculationType),
      value: editing ? Number(edited?.value || 0) : Number(saved?.value ?? item.defaultValue ?? 0),
      savedAmount: saved?.amount,
    }
  })
  const preview = previewAmounts(rows.map((row) => ({ ...row, type: row.componentType })))
  const earnings = rows.filter((row) => row.componentType === 'EARNING')
  const deductions = rows.filter((row) => row.componentType === 'DEDUCTION')
  function amountFor(row: { code: string }) {
    if (editing || !structure) return preview.amounts.get(row.code) ?? 0
    return rows.find((item) => item.code === row.code)?.savedAmount ?? 0
  }
  const gross = editing || !structure ? preview.gross : structure.gross
  const deductionTotal = editing || !structure ? preview.deductions : structure.deductions
  const net = editing || !structure ? preview.net : structure.net
  const [open, setOpen] = useState(false)
  const wasEditing = useRef(false)
  useEffect(() => {
    if (editing && !wasEditing.current) setOpen(true)
    wasEditing.current = editing
  }, [editing])
  if (defs.length === 0) return <div className="skeleton mt-6 h-40" />
  return (
    <section className="mt-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-2xl">Salary structure</h2>
          <p className="mt-1 text-sm text-ink-soft">{open
            ? (structure ? `Effective ${dateLabel(structure.effectiveDate)}. Earnings, PF, ESI and the other deductions for this employee.` : 'Basic, HRA, PF, ESI, professional tax and the other payroll components. Amounts use the standard rates until this structure is saved.')
            : `Gross ${inr(gross)} · Deductions ${inr(deductionTotal)} · Net ${inr(net)}`}</p>
        </div>
        <Button type="button" variant="ghost" aria-expanded={open} onClick={() => setOpen((current) => !current)}>{open ? 'Minimize' : 'Show'}</Button>
      </div>
      {open && <div className="mt-3">
        <TableShell>
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="bg-paper-2">
              <tr>
                {['Component', 'Calculation', 'Amount'].map((heading) => <th key={heading} className={`px-3 py-3 font-semibold ${heading === 'Amount' ? 'text-right' : ''}`}>{heading}</th>)}
              </tr>
            </thead>
            <tbody>
              <StructureGroup title="Earnings" rows={earnings} editing={editing} amountFor={amountFor} onDraft={onDraft} draft={draft} />
              <tr className="border-t border-line">
                <td className="px-3 py-3 font-semibold" colSpan={2}>Gross</td>
                <td className="px-3 py-3 text-right font-semibold tabular-nums">{inr(gross)}</td>
              </tr>
              <StructureGroup title="Deductions" rows={deductions} editing={editing} amountFor={amountFor} onDraft={onDraft} draft={draft} />
              <tr className="border-t border-line">
                <td className="px-3 py-3 font-semibold" colSpan={2}>Total deductions</td>
                <td className="px-3 py-3 text-right font-semibold tabular-nums">{inr(deductionTotal)}</td>
              </tr>
              <tr className="border-t border-line bg-paper-2">
                <td className="px-3 py-3 font-semibold" colSpan={2}>Net pay</td>
                <td className="px-3 py-3 text-right font-semibold tabular-nums">{inr(net)}</td>
              </tr>
            </tbody>
          </table>
        </TableShell>
      </div>}
      {editing && open && (
        <div className="mt-4 flex items-center justify-end gap-2">
          {error && <p className="mr-auto text-sm text-danger">{error}</p>}
          <Button type="button" disabled={busy} onClick={onSave}>Save structure</Button>
        </div>
      )}
    </section>
  )
}

function StructureGroup({ title, rows, editing, amountFor, onDraft, draft }: {
  title: string
  rows: { code: string; name: string; componentType: string; calculationType: string; value: number; defaultValue: number }[]
  editing: boolean
  amountFor: (row: { code: string }) => number
  onDraft: (code: string, patch: Partial<DraftLine>) => void
  draft: Record<string, DraftLine>
}) {
  return (
    <>
      <tr className="border-t border-line bg-paper-2">
        <td className="px-3 py-2 font-display text-lg uppercase" colSpan={3}>{title}</td>
      </tr>
      {rows.map((row) => (
        <tr key={row.code} className="border-t border-line">
          <td className="px-3 py-3 align-middle font-semibold">{row.name}</td>
          <td className="px-3 py-3 align-middle">
            {editing ? (
              <div className="grid gap-2 sm:grid-cols-[12rem_8rem]">
                <SelectInput value={draft[row.code]?.calculationType ?? row.calculationType} onChange={(event) => onDraft(row.code, { calculationType: event.target.value })}>
                  {calculations.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </SelectInput>
                <TextInput inputMode="decimal" value={draft[row.code]?.value ?? String(row.value)} onChange={(event) => onDraft(row.code, { value: event.target.value })} />
              </div>
            ) : (
              <span className="text-ink-soft">{rateLabel(row.calculationType, row.value)}</span>
            )}
          </td>
          <td className="px-3 py-3 text-right align-middle tabular-nums">{inr(amountFor(row))}</td>
        </tr>
      ))}
    </>
  )
}

export function EmployeeDetailPage() {
  const { id } = useParams()
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['employee', id], queryFn: async () => (await api.get(`/api/employees/${id}`)).data })
  const slips = useQuery({ queryKey: ['slips', id], queryFn: async () => (await api.get(`/api/employees/${id}/salary-slips`)).data as { recordId: string; slipNumber?: string; month: number; year: number; companyName: string; gross: number; net: number }[] })
  const structures = useQuery({ queryKey: ['structures', id], queryFn: async () => (await api.get(`/api/employees/${id}/salary-structures`)).data as SalaryStructure[] })
  const components = useQuery({ queryKey: ['components'], queryFn: async () => (await api.get('/api/salary-components')).data as ComponentDef[] })
  const companies = useQuery({ queryKey: ['company-options'], queryFn: async () => (await api.get('/api/companies', { params: { size: 100 } })).data })
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<EmployeeForm>(emptyEmployee)
  const [draft, setDraft] = useState<Record<string, DraftLine>>({})
  const [error, setError] = useState('')
  const [structureError, setStructureError] = useState('')
  const [busy, setBusy] = useState(false)
  const [cvBusy, setCvBusy] = useState(false)
  const [cvError, setCvError] = useState('')
  const profile = query.data
  const employee = profile?.employee
  const person = profile?.candidate
  function startEdit() {
    if (!employee || !person) return
    setForm({
      fullName: employee.fullName ?? '',
      mobile: person.mobile ?? '',
      email: person.email ?? '',
      address: person.address ?? '',
      city: person.city ?? '',
      companyId: employee.companyId ?? '',
      designation: employee.designation ?? '',
      department: employee.department ?? '',
      joiningDate: employee.joiningDate ?? '',
      employmentType: employee.employmentType ?? 'CONTRACT',
      workLocation: employee.workLocation ?? '',
      reportingManager: employee.reportingManager ?? '',
      notes: employee.notes ?? '',
    })
    setDraft(structureDraft(components.data ?? [], (structures.data ?? []).find((row) => row.active)))
    setError('')
    setEditing(true)
  }
  function set<K extends keyof EmployeeForm>(key: K, value: EmployeeForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }
  if (!employee || !person) return <div className="skeleton h-48" />
  return (
    <div>
      <PageHeader title={employee.fullName} lede={`${employee.employeeCode} · ${titleCase(employee.status)} · Since ${dateLabel(employee.joiningDate)}`} actions={
        <>
          {editing
            ? <Button type="button" variant="ghost" onClick={() => { setEditing(false); setError('') }}>Cancel</Button>
            : <Button type="button" onClick={startEdit}>Edit</Button>}
          <Link className="inline-flex h-11 items-center border border-ink px-5 font-display text-lg uppercase" to="/admin/employees">All employees</Link>
        </>
      } />
      {editing ? (
        <form className="border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          setBusy(true)
          setError('')
          try {
            await api.put(`/api/employees/${id}`, { ...form, companyId: form.companyId || null, joiningDate: form.joiningDate || null })
            await client.invalidateQueries({ queryKey: ['employee', id] })
            await client.invalidateQueries({ queryKey: ['employees'] })
            setEditing(false)
          } catch (err) {
            setError(errorMessage(err))
          } finally {
            setBusy(false)
          }
        }}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="sm:col-span-2 lg:col-span-4">
              <Field label="Profile photo">
                <ImagePick files={[]} current={employee.photoPath ? [employee.photoPath] : []} onPick={async (picked) => {
                  const file = picked[0]
                  if (!file) return
                  const body = new FormData()
                  body.append('file', file)
                  await api.post(`/api/employees/${id}/photo`, body)
                  await client.invalidateQueries({ queryKey: ['employee', id] })
                }} />
              </Field>
            </div>
            <Field label="Name" required><TextInput value={form.fullName} onChange={(e) => set('fullName', e.target.value)} required /></Field>
            <Field label="Mobile" required><TextInput value={form.mobile} onChange={(e) => set('mobile', e.target.value)} required /></Field>
            <Field label="Email" required><TextInput type="email" value={form.email} onChange={(e) => set('email', e.target.value)} required /></Field>
            <Field label="Company" required>
              <SelectInput value={form.companyId} onChange={(e) => set('companyId', e.target.value)} required>
                <option value="">Select company</option>
                {companies.data?.content?.map((company: { id: string; companyName: string }) => <option key={company.id} value={company.id}>{company.companyName}</option>)}
              </SelectInput>
            </Field>
            <Field label="Designation"><TextInput value={form.designation} onChange={(e) => set('designation', e.target.value)} /></Field>
            <Field label="Department"><TextInput value={form.department} onChange={(e) => set('department', e.target.value)} /></Field>
            <Field label="Working since" required><TextInput type="date" value={form.joiningDate} onChange={(e) => set('joiningDate', e.target.value)} required /></Field>
            <Field label="Employment">
              <SelectInput value={form.employmentType} onChange={(e) => set('employmentType', e.target.value)}>
                {employmentTypes.map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}
              </SelectInput>
            </Field>
            <Field label="Work location"><TextInput value={form.workLocation} onChange={(e) => set('workLocation', e.target.value)} /></Field>
            <Field label="Reporting manager"><TextInput value={form.reportingManager} onChange={(e) => set('reportingManager', e.target.value)} /></Field>
            <Field label="City"><TextInput value={form.city} onChange={(e) => set('city', e.target.value)} /></Field>
            <div className="sm:col-span-2 lg:col-span-4">
              <Field label="Address"><TextInput value={form.address} onChange={(e) => set('address', e.target.value)} /></Field>
            </div>
            <div className="sm:col-span-2 lg:col-span-4">
              <Field label="Notes"><TextArea value={form.notes} onChange={(e) => set('notes', e.target.value)} /></Field>
            </div>
          </div>
          <div className="mt-4 flex items-center justify-end gap-2 border-t border-line pt-4">
            {error && <p className="mr-auto text-sm text-danger">{error}</p>}
            <Button type="submit" disabled={busy}>Save</Button>
          </div>
        </form>
      ) : (
        <section className="border border-line bg-card p-4">
          <div className="grid items-start gap-4 sm:grid-cols-[4rem_1fr]">
            {employee.photoPath
              ? <img src={employee.photoPath} alt="" className="size-16 object-cover" />
              : <span className="flex size-16 items-center justify-center bg-paper-2 font-display text-3xl">{employee.fullName.slice(0, 1)}</span>}
            <div className="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-4">
            <Fact label="Mobile" value={person.mobile} />
            <Fact label="Email" value={person.email} />
            <Fact label="Company" value={employee.companyName} />
            <Fact label="Working since" value={dateLabel(employee.joiningDate)} />
            <Fact label="Designation" value={employee.designation} />
            <Fact label="Department" value={employee.department} />
            <Fact label="Employment" value={titleCase(employee.employmentType)} />
            <Fact label="Salary" value={inr(employee.currentNet)} />
            <Fact label="Qualification" value={person.highestQualification} />
            <Fact label="Experience" value={person.totalExperience == null ? null : `${person.totalExperience} yrs`} />
            <Fact label="Work location" value={employee.workLocation} />
            <Fact label="Reporting manager" value={employee.reportingManager} />
            <Fact label="City" value={person.city} />
            <Fact label="Status" value={titleCase(employee.status)} />
            <div className="col-span-2 sm:col-span-4"><Fact label="Address" value={[person.address, person.city].filter(Boolean).join(', ')} /></div>
            {employee.notes && <div className="col-span-2 sm:col-span-4"><Fact label="Notes" value={employee.notes} /></div>}
            </div>
          </div>
        </section>
      )}
      <section className="mt-4 border border-line bg-card p-4">
        <h2 className="text-balance text-2xl">Hiring</h2>
        <div className="mt-4 grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-4">
          <Fact label="Applied for" value={profile.hire?.jobTitle} />
          <Fact label="Application" value={profile.hire?.applicationNumber} />
          <Fact label="Application status" value={titleCase(profile.hire?.applicationStatus)} />
          <Fact label="Expected salary" value={inr(person.expectedSalary)} />
          <Fact label="Earlier salary" value={inr(person.currentSalary)} />
          <Fact label="Earlier role" value={[person.currentJobTitle, person.currentCompany].filter(Boolean).join(', ')} />
          <Fact label="Notice period" value={person.noticePeriod} />
          <Fact label="Specialization" value={person.specialization} />
          <div className="col-span-2 sm:col-span-4"><Fact label="Skills" value={person.skills?.length ? person.skills.join(', ') : null} /></div>
          {(person.emergencyContactName || person.emergencyContactPhone) && (
            <div className="col-span-2 sm:col-span-4"><Fact label="Emergency contact" value={[person.emergencyContactName, person.emergencyContactPhone].filter(Boolean).join(' · ')} /></div>
          )}
        </div>
        <div className="mt-5 border-t border-line pt-4">
          <p className="text-sm font-semibold">CV</p>
          {profile.hire?.resumeDocumentId ? (
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <p className="text-pretty">{profile.hire.resumeFileName || 'Resume'}</p>
              <button className="h-10 border border-pine px-3 font-display text-lg uppercase text-pine" type="button" onClick={() => openResume(profile.hire.resumeDocumentId)}>Open CV</button>
            </div>
          ) : (
            <p className="mt-2 text-pretty text-sm text-ink-soft">No CV was attached when this person applied.</p>
          )}
          <label className="mt-3 block text-sm font-semibold">
            {profile.hire?.resumeDocumentId ? 'Replace CV' : 'Upload CV'}
            <input
              className="mt-1.5 block w-full text-sm file:mr-3 file:h-11 file:border-0 file:bg-pine file:px-4 file:font-display file:text-lg file:uppercase file:text-white"
              type="file"
              accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              disabled={cvBusy}
              onChange={async (event) => {
                const file = event.target.files?.[0]
                event.target.value = ''
                if (!file) return
                setCvError('')
                setCvBusy(true)
                const body = new FormData()
                body.append('file', file)
                try {
                  await api.post(`/api/employees/${id}/resume`, body)
                  await client.invalidateQueries({ queryKey: ['employee', id] })
                } catch (err) {
                  setCvError(errorMessage(err))
                } finally {
                  setCvBusy(false)
                }
              }}
            />
          </label>
          <p className="mt-2 text-sm text-ink-soft">PDF or Word, up to 10 MB. A file uploaded here is kept for this employee.</p>
          {cvError && <p className="mt-2 text-sm text-danger">{cvError}</p>}
        </div>
      </section>
      <SalaryStructurePanel
        defs={components.data ?? []}
        structure={(structures.data ?? []).find((row) => row.active)}
        editing={editing}
        draft={draft}
        onDraft={(code, patch) => setDraft((current) => ({ ...current, [code]: { ...current[code], ...patch } }))}
        onSave={async () => {
          setStructureError('')
          setBusy(true)
          try {
            await api.post(`/api/employees/${id}/salary-structures`, {
              effectiveDate: new Date().toISOString().slice(0, 10),
              reason: 'Structure set from the employee profile',
              components: (components.data ?? []).filter((item) => item.active).map((item) => ({
                componentId: item.id,
                calculationType: draft[item.code]?.calculationType ?? item.calculationType,
                value: Number(draft[item.code]?.value || 0),
              })),
            })
            await client.invalidateQueries({ queryKey: ['structures', id] })
            await client.invalidateQueries({ queryKey: ['employee', id] })
            await client.invalidateQueries({ queryKey: ['employees'] })
          } catch (err) {
            setStructureError(errorMessage(err))
          } finally {
            setBusy(false)
          }
        }}
        busy={busy}
        error={structureError}
      />
      <section className="mt-6">
        <h2 className="text-2xl">Salary slips</h2>
        {(slips.data ?? []).length === 0 && <div className="mt-3"><EmptyState title="No salary slips yet." body="Slips appear here after payroll is generated for this employee’s company." /></div>}
        {(slips.data ?? []).length > 0 && (
          <div className="mt-3">
            <TableShell>
              <table className="w-full text-left text-sm">
                <thead className="bg-paper-2"><tr>{['Period', 'Company', 'Gross', 'Net', ''].map((heading) => <th key={heading} className={`px-3 py-3 font-semibold ${heading === '' ? 'text-right' : ''}`}>{heading}</th>)}</tr></thead>
                <tbody>
                  {slips.data?.map((slip) => (
                    <tr key={slip.recordId} className="border-t border-line">
                      <td className="px-3 py-3 align-middle tabular-nums">{String(slip.month).padStart(2, '0')}/{slip.year}</td>
                      <td className="px-3 py-3 align-middle">{slip.companyName}</td>
                      <td className="px-3 py-3 align-middle tabular-nums">{inr(slip.gross)}</td>
                      <td className="px-3 py-3 align-middle tabular-nums">{inr(slip.net)}</td>
                      <td className="px-3 py-3 text-right align-middle"><Link className="inline-flex h-8 items-center border border-pine px-2 font-display text-sm uppercase text-pine" to={`/admin/payroll/slips/${slip.recordId}`}>Open slip</Link></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableShell>
          </div>
        )}
      </section>
    </div>
  )
}

export function PayrollPage() {
  const companies = useQuery({ queryKey: ['company-options'], queryFn: async () => (await api.get('/api/companies', { params: { size: 100 } })).data })
  const runs = useQuery({ queryKey: ['runs'], queryFn: async () => (await api.get('/api/payroll/runs')).data })
  const [companyId, setCompanyId] = useState('')
  const [month, setMonth] = useState(String(new Date().getMonth() + 1))
  const [year, setYear] = useState(String(new Date().getFullYear()))
  const [error, setError] = useState('')
  const [uploading, setUploading] = useState(false)
  const client = useQueryClient()
  const periodReady = Boolean(companyId && month && year)
  const attendance = useQuery({
    queryKey: ['attendance', companyId, month, year],
    enabled: periodReady,
    queryFn: async () => (await api.get('/api/payroll/attendance', { params: { companyId, month, year } })).data as AttendanceSheet,
  })
  const sheet = attendance.data
  return (
    <div>
      <PageHeader title="Payroll runs" actions={<Button variant="ghost" onClick={downloadAttendanceTemplate}>Attendance template</Button>} />
      <form className="mb-4 border border-line bg-card p-4" onSubmit={async (event) => {
        event.preventDefault()
        setError('')
        try {
          const res = await api.post('/api/payroll/runs', null, { params: { companyId, month, year } })
          window.location.href = `/admin/payroll/${res.data.id}`
        } catch (err) { setError(errorMessage(err)) }
      }}>
        <div className="grid gap-3 md:grid-cols-4">
          <Field label="Company" required>
            <SelectInput value={companyId} onChange={(e) => setCompanyId(e.target.value)} required>
              <option value="">Select</option>
              {companies.data?.content?.map((company: { id: string; companyName: string }) => <option key={company.id} value={company.id}>{company.companyName}</option>)}
            </SelectInput>
          </Field>
          <Field label="Month"><TextInput value={month} onChange={(e) => setMonth(e.target.value)} /></Field>
          <Field label="Year"><TextInput value={year} onChange={(e) => setYear(e.target.value)} /></Field>
          <Button className="self-end" type="submit">Generate</Button>
        </div>
        <div className="mt-4 border-t border-line pt-4">
          <Field label="Attendance sheet">
            <input
              className="block w-full text-sm file:mr-3 file:h-11 file:border-0 file:bg-pine file:px-4 file:font-display file:text-lg file:uppercase file:text-white"
              type="file"
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              disabled={!periodReady || uploading}
              onChange={async (event) => {
                const file = event.target.files?.[0]
                event.target.value = ''
                if (!file || !periodReady) return
                setError('')
                setUploading(true)
                const body = new FormData()
                body.append('file', file)
                body.append('companyId', companyId)
                body.append('month', month)
                body.append('year', year)
                try {
                  await api.post('/api/payroll/attendance', body)
                  await client.invalidateQueries({ queryKey: ['attendance', companyId, month, year] })
                } catch (err) {
                  setError(errorMessage(err))
                } finally {
                  setUploading(false)
                }
              }}
            />
          </Field>
          <p className="mt-2 text-pretty text-sm text-ink-soft">
            {sheet?.uploaded
              ? 'Generate uses the paid days below. Present and weekly off are paid. Leave and absent are not.'
              : 'Upload the company’s Excel of who came which day. Without a sheet, generate pays the full month.'}
          </p>
        </div>
        {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      </form>
      {attendance.isLoading && periodReady && <div className="skeleton mb-4 h-32" />}
      {sheet?.uploaded && sheet.employees.length === 0 && <div className="mb-4"><EmptyState title="No days in this sheet." body="Upload a workbook with one row per employee per date." /></div>}
      {sheet?.employees?.map((person) => (
        <section key={person.employeeId} className="mb-4">
          <div className="mb-2 flex flex-wrap items-end justify-between gap-2">
            <h2 className="text-balance text-2xl">{person.employeeName}</h2>
            <p className="text-sm text-ink-soft">{person.employeeCode} · Paid {person.paidDays} · Present {person.present} · Weekly off {person.weeklyOff} · Leave {person.leave} · Absent {person.absent}</p>
          </div>
          <TableShell>
            <table className="w-full min-w-[420px] text-left text-sm">
              <thead className="bg-paper-2"><tr>{['Date', 'Status'].map((heading) => <th key={heading} className="px-3 py-3 font-semibold">{heading}</th>)}</tr></thead>
              <tbody>
                {person.days.map((day) => (
                  <tr key={day.date} className="border-t border-line">
                    <td className="px-3 py-3 tabular-nums">{dateLabel(day.date)}</td>
                    <td className="px-3 py-3"><Badge tone={day.status === 'PRESENT' || day.status === 'WEEKLY_OFF' ? 'good' : day.status === 'LEAVE' ? 'warn' : 'bad'}>{titleCase(day.status)}</Badge></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableShell>
        </section>
      ))}
      <div className="grid gap-3">
        {runs.data?.content?.map((run: { id: string; companyName: string; month: number; year: number; status: string; totalNet: number }) => (
          <Link key={run.id} to={`/admin/payroll/${run.id}`} className="flex items-center justify-between border border-line bg-card p-4" onClick={() => client.invalidateQueries({ queryKey: ['runs'] })}>
            <span>{run.companyName} · {run.month}/{run.year}</span>
            <span className="flex items-center gap-3"><Badge tone={statusTone(run.status)}>{titleCase(run.status)}</Badge> {inr(run.totalNet)}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}

type AttendanceSheet = {
  uploaded: boolean
  fileName?: string
  employees: {
    employeeId: string
    employeeCode: string
    employeeName: string
    present: number
    absent: number
    leave: number
    weeklyOff: number
    paidDays: number
    days: { date: string; status: string }[]
  }[]
}

export function SalarySlipPage() {
  const { recordId } = useParams()
  const [url, setUrl] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    let objectUrl = ''
    let cancelled = false
    ;(async () => {
      try {
        const slip = await api.post(`/api/payroll/records/${recordId}/slip`)
        const pdf = await api.get(`/api/salary-slips/${slip.data.id}/pdf`, { responseType: 'blob' })
        objectUrl = URL.createObjectURL(pdf.data)
        if (!cancelled) setUrl(objectUrl)
      } catch (err) {
        if (!cancelled) setError(errorMessage(err))
      }
    })()
    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [recordId])
  return (
    <div>
      <PageHeader title="Salary slip" actions={<Button variant="ghost" onClick={() => window.history.back()}>Back</Button>} />
      {error && <p className="text-sm text-danger">{error}</p>}
      {!url && !error && <div className="skeleton h-96" />}
      {url && <iframe title="Salary slip" src={url} className="h-[75dvh] w-full border border-line bg-card" />}
    </div>
  )
}

async function openResume(documentId: string) {
  const file = await api.get(`/api/documents/${documentId}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(file.data)
  window.open(url, '_blank', 'noopener')
}

async function downloadAttendanceTemplate() {
  const file = await api.get('/api/payroll/attendance/template', { responseType: 'blob' })
  const url = URL.createObjectURL(file.data)
  const link = document.createElement('a')
  link.href = url
  link.download = 'attendance-template.xlsx'
  link.click()
  URL.revokeObjectURL(url)
}

export function PayrollDetailPage() {
  const { id } = useParams()
  const query = useQuery({ queryKey: ['run', id], queryFn: async () => (await api.get(`/api/payroll/runs/${id}`)).data })
  const client = useQueryClient()
  const run = query.data
  if (!run) return <div className="skeleton h-48 rounded-lg" />
  return (
    <div>
      <PageHeader title={`${run.companyName} · ${run.month}/${run.year}`} lede={`Gross ${inr(run.totalGross)} · Deductions ${inr(run.totalDeductions)} · Net ${inr(run.totalNet)}`} actions={
        <>
          <Badge tone={statusTone(run.status)}>{titleCase(run.status)}</Badge>
          <Button variant="ghost" onClick={async () => { await api.post(`/api/payroll/runs/${id}/generate`); client.invalidateQueries({ queryKey: ['run', id] }) }}>Regenerate</Button>
          <Button variant="secondary" onClick={async () => { await api.post(`/api/payroll/runs/${id}/approve`); client.invalidateQueries({ queryKey: ['run', id] }) }}>Approve</Button>
          <Button onClick={async () => { if (confirm('Lock this payroll? Later corrections are stored as adjustments.')) { await api.post(`/api/payroll/runs/${id}/lock`); client.invalidateQueries({ queryKey: ['run', id] }) } }}>Lock</Button>
        </>
      } />
      {run.warnings?.map((warning: string) => <p key={warning} className="mb-2 text-sm text-warning">{warning}</p>)}
      <TableShell>
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="bg-paper-2"><tr>{['Employee', 'Designation', 'Days', 'Basic', 'Gross', 'Deductions', 'Net', ''].map((h) => <th key={h} className="px-3 py-3">{h}</th>)}</tr></thead>
          <tbody>
            {run.records.map((row: { id: string; employeeName: string; employeeCode: string; designation?: string; payableDays: number; workingDays: number; basic: number; gross: number; deductions: number; net: number }) => (
              <tr key={row.id} className="border-t border-line">
                <td className="px-3 py-3">{row.employeeName}<div className="text-xs text-ink-soft">{row.employeeCode}</div></td>
                <td className="px-3 py-3">{row.designation}</td>
                <td className="px-3 py-3">{row.payableDays}/{row.workingDays}</td>
                <td className="px-3 py-3">{inr(row.basic)}</td>
                <td className="px-3 py-3">{inr(row.gross)}</td>
                <td className="px-3 py-3">{inr(row.deductions)}</td>
                <td className="px-3 py-3 font-semibold">{inr(row.net)}</td>
                <td className="px-3 py-3"><Link className="text-pine" to={`/admin/payroll/slips/${row.id}`}>Slip PDF</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableShell>
      {run.records.length === 0 && <div className="mt-4"><EmptyState title="No payable employees" body="Place active employees at this company and give them a salary structure, then regenerate." /></div>}
    </div>
  )
}
