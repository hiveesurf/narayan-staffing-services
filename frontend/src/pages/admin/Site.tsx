import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Badge, Button, EmptyState, Field, PageHeader, SelectInput, TableShell, TextArea, TextInput, statusTone } from '../../components/desk-ui'
import { api, errorMessage } from '../../lib/api'
import { dateLabel, inr, titleCase } from '../../lib/format'

type Row = Record<string, unknown> & { id: string }

const fieldClass = 'h-11 w-full border border-line bg-card px-3 text-sm'

export function InterviewsPage() {
  const query = useQuery({ queryKey: ['interviews'], queryFn: async () => (await api.get('/api/interviews')).data })
  const [feedback, setFeedback] = useState<Record<string, string>>({})
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Interviews" lede="Scheduled conversations and the feedback that follows them." />
      {query.data?.length === 0 && <EmptyState title="No interviews scheduled." body="Open an application and schedule one from there." />}
      <div className="grid gap-3">
        {query.data?.map((item: { id: string; candidateName: string; jobTitle: string; date: string; time: string; mode: string; status: string; location?: string; interviewerName?: string }) => (
          <article key={item.id} className="border border-line bg-card p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-xl">{item.candidateName}</h2>
                <p className="text-sm text-ink-soft">{item.jobTitle} · {dateLabel(item.date)} {item.time} · {titleCase(item.mode)} · {item.location || item.interviewerName || '—'}</p>
              </div>
              <Badge tone={statusTone(item.status)}>{titleCase(item.status)}</Badge>
            </div>
            <form className="mt-3 grid gap-2 sm:grid-cols-2" onSubmit={async (event) => {
              event.preventDefault()
              await api.post(`/api/interviews/${item.id}/feedback`, {
                rating: Number(feedback[`${item.id}-rating`] || 0),
                strengths: feedback[`${item.id}-strengths`],
                weaknesses: feedback[`${item.id}-weaknesses`],
                recommendation: feedback[`${item.id}-recommendation`],
                remarks: feedback[`${item.id}-remarks`],
              })
              client.invalidateQueries({ queryKey: ['interviews'] })
            }}>
              <TextInput placeholder="Rating 1–5" value={feedback[`${item.id}-rating`] || ''} onChange={(e) => setFeedback({ ...feedback, [`${item.id}-rating`]: e.target.value })} />
              <TextInput placeholder="Recommendation" value={feedback[`${item.id}-recommendation`] || ''} onChange={(e) => setFeedback({ ...feedback, [`${item.id}-recommendation`]: e.target.value })} />
              <TextInput placeholder="Strengths" value={feedback[`${item.id}-strengths`] || ''} onChange={(e) => setFeedback({ ...feedback, [`${item.id}-strengths`]: e.target.value })} />
              <TextInput placeholder="Weaknesses" value={feedback[`${item.id}-weaknesses`] || ''} onChange={(e) => setFeedback({ ...feedback, [`${item.id}-weaknesses`]: e.target.value })} />
              <Button type="submit">Save feedback</Button>
            </form>
          </article>
        ))}
      </div>
    </div>
  )
}

export function SalaryComponentsPage() {
  const query = useQuery({ queryKey: ['components'], queryFn: async () => (await api.get('/api/salary-components')).data })
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Salary components" lede="Rates live here. Payroll uses the structure that is active for the month, not a hard-coded percentage." />
      <TableShell>
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-paper-2"><tr>{['Name', 'Type', 'Calculation', 'Value', ''].map((h) => <th key={h} className="px-3 py-3">{h}</th>)}</tr></thead>
          <tbody>
            {query.data?.map((item: { id: string; name: string; code: string; componentType: string; calculationType: string; defaultValue: number; active: boolean }) => (
              <ComponentRow key={item.id} item={item} onSaved={() => client.invalidateQueries({ queryKey: ['components'] })} />
            ))}
          </tbody>
        </table>
      </TableShell>
    </div>
  )
}

function ComponentRow({ item, onSaved }: { item: { id: string; name: string; code: string; componentType: string; calculationType: string; defaultValue: number; active: boolean }; onSaved: () => void }) {
  const [calculationType, setCalculationType] = useState(item.calculationType)
  const [value, setValue] = useState(String(item.defaultValue))
  const [active, setActive] = useState(item.active)
  const [error, setError] = useState('')
  return (
    <tr className="border-t border-line">
      <td className="px-3 py-3">{item.name}<div className="text-xs text-ink-soft">{item.code} · {titleCase(item.componentType)}</div></td>
      <td className="px-3 py-3">{titleCase(item.componentType)}</td>
      <td className="px-3 py-3">
        <select className={fieldClass} value={calculationType} onChange={(e) => setCalculationType(e.target.value)}>
          {['FIXED', 'PERCENT_OF_BASIC', 'PERCENT_OF_GROSS', 'CUSTOM'].map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}
        </select>
      </td>
      <td className="px-3 py-3"><input className={fieldClass} value={value} onChange={(e) => setValue(e.target.value)} /></td>
      <td className="px-3 py-3">
        <label className="mr-3 text-xs"><input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} /> Active</label>
        <button className="text-sm font-semibold text-copper" type="button" onClick={async () => {
          setError('')
          try {
            await api.put(`/api/salary-components/${item.id}`, { calculationType, defaultValue: Number(value), active })
            onSaved()
          } catch (err) { setError(errorMessage(err)) }
        }}>Save</button>
        {error && <p className="text-xs text-danger">{error}</p>}
      </td>
    </tr>
  )
}

type FieldDef = { key: string; label: string; area?: boolean; check?: boolean; number?: boolean }

function ResourcePage({ title, lede, path, fields, blank }: { title: string; lede?: string; path: string; fields: FieldDef[]; blank: Row }) {
  const query = useQuery({ queryKey: [path], queryFn: async () => (await api.get(path)).data })
  const [draft, setDraft] = useState<Row | null>(null)
  const [error, setError] = useState('')
  const client = useQueryClient()
  const rows: Row[] = Array.isArray(query.data) ? query.data : query.data?.content ?? []
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!draft) return
    setError('')
    const body: Record<string, unknown> = { ...draft }
    fields.forEach((field) => {
      if (field.number) body[field.key] = body[field.key] === '' || body[field.key] == null ? 0 : Number(body[field.key])
      if (field.check) body[field.key] = Boolean(body[field.key])
    })
    try {
      if (draft.id) await api.put(`${path}/${draft.id}`, body)
      else {
        delete body.id
        await api.post(path, body)
      }
      setDraft(null)
      client.invalidateQueries({ queryKey: [path] })
    } catch (err) { setError(errorMessage(err)) }
  }
  return (
    <div>
      <PageHeader title={title} lede={lede} actions={<Button onClick={() => setDraft({ ...blank })}>Add</Button>} />
      {draft && (
        <form className="mb-4 grid gap-3 border border-line bg-card p-4" onSubmit={save}>
          {fields.map((field) => (
            <Field key={field.key} label={field.label}>
              {field.area ? <TextArea value={String(draft[field.key] ?? '')} onChange={(e) => setDraft({ ...draft, [field.key]: e.target.value })} />
                : field.check ? <input type="checkbox" checked={Boolean(draft[field.key])} onChange={(e) => setDraft({ ...draft, [field.key]: e.target.checked })} />
                  : <TextInput value={String(draft[field.key] ?? '')} onChange={(e) => setDraft({ ...draft, [field.key]: e.target.value })} />}
            </Field>
          ))}
          {path === '/api/gallery' && <GalleryUpload onUploaded={(url) => setDraft({ ...draft, imagePath: url })} />}
          {error && <p className="text-sm text-danger">{error}</p>}
          <div className="flex gap-2"><Button type="submit">Save</Button><Button type="button" variant="ghost" onClick={() => setDraft(null)}>Cancel</Button></div>
        </form>
      )}
      <div className="grid gap-2">
        {rows.map((row) => (
          <div key={row.id} className="flex flex-wrap items-center justify-between gap-3 border border-line bg-card px-4 py-3">
            <div>
              <p className="font-semibold">{String(row.title || row.name || row.authorName || row.settingKey || 'Item')}</p>
              <p className="text-sm text-ink-soft">{String(row.slug || row.shortDescription || row.quote || row.description || '')}</p>
            </div>
            <div className="flex gap-3 text-sm">
              <button className="font-semibold text-copper" type="button" onClick={() => setDraft(row)}>Edit</button>
              <button className="text-danger" type="button" onClick={async () => {
                if (!confirm('Delete this item?')) return
                await api.delete(`${path}/${row.id}`)
                client.invalidateQueries({ queryKey: [path] })
              }}>Delete</button>
            </div>
          </div>
        ))}
      </div>
      {!query.isLoading && rows.length === 0 && <div className="mt-4"><EmptyState title={`No ${title.toLowerCase()} yet.`} body="Add the first record. It will appear on the public site when it is active." /></div>}
    </div>
  )
}

function GalleryUpload({ onUploaded }: { onUploaded: (url: string) => void }) {
  const [error, setError] = useState('')
  return (
    <Field label="Image">
      <input type="file" accept="image/png,image/jpeg,image/webp" onChange={async (event) => {
        const file = event.target.files?.[0]
        if (!file) return
        const body = new FormData()
        body.append('file', file)
        try {
          const res = await api.post('/api/cms/media', body)
          onUploaded(res.data.url)
        } catch (err) { setError(errorMessage(err)) }
      }} />
      {error && <span className="text-sm text-danger">{error}</span>}
    </Field>
  )
}

export function ServicesAdminPage() {
  return <ResourcePage title="Services" path="/api/services" blank={{ id: '', title: '', slug: '', shortDescription: '', detailedDescription: '', icon: '', active: true, displayOrder: 0 }} fields={[
    { key: 'title', label: 'Title' }, { key: 'slug', label: 'Slug' }, { key: 'shortDescription', label: 'Short description', area: true },
    { key: 'detailedDescription', label: 'Detail', area: true }, { key: 'icon', label: 'Icon' }, { key: 'bannerImage', label: 'Banner image path' },
    { key: 'seoTitle', label: 'SEO title' }, { key: 'seoDescription', label: 'SEO description' },
    { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function CategoriesAdminPage() {
  return <ResourcePage title="Categories" path="/api/categories" blank={{ id: '', name: '', description: '', icon: '', active: true, displayOrder: 0 }} fields={[
    { key: 'name', label: 'Name' }, { key: 'description', label: 'Description', area: true }, { key: 'icon', label: 'Icon' },
    { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function ClientsAdminPage() {
  return <ResourcePage title="Clients" lede="Logos shown on the public site." path="/api/clients" blank={{ id: '', name: '', website: '', logoPath: '', displayOrder: 0, active: true }} fields={[
    { key: 'name', label: 'Name' }, { key: 'website', label: 'Website' }, { key: 'logoPath', label: 'Logo path' },
    { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function GalleryAdminPage() {
  return <ResourcePage title="Gallery" path="/api/gallery" blank={{ id: '', title: '', description: '', category: '', imagePath: '', displayOrder: 0, active: true }} fields={[
    { key: 'title', label: 'Title' }, { key: 'description', label: 'Description', area: true }, { key: 'category', label: 'Category' },
    { key: 'imagePath', label: 'Image path' }, { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function TestimonialsAdminPage() {
  return <ResourcePage title="Testimonials" path="/api/testimonials" blank={{ id: '', authorName: '', authorRole: '', companyName: '', quote: '', displayOrder: 0, active: true }} fields={[
    { key: 'authorName', label: 'Name' }, { key: 'authorRole', label: 'Role' }, { key: 'companyName', label: 'Company' },
    { key: 'quote', label: 'Quote', area: true }, { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function IndustriesAdminPage() {
  return <ResourcePage title="Industries" path="/api/industries" blank={{ id: '', name: '', slug: '', description: '', displayOrder: 0, active: true }} fields={[
    { key: 'name', label: 'Name' }, { key: 'slug', label: 'Slug' }, { key: 'description', label: 'Description', area: true },
    { key: 'displayOrder', label: 'Order', number: true }, { key: 'active', label: 'Active', check: true },
  ]} />
}

export function PagesAdminPage() {
  const sections = useQuery({ queryKey: ['sections'], queryFn: async () => (await api.get('/api/cms/sections')).data })
  const blocks = useQuery({ queryKey: ['blocks'], queryFn: async () => (await api.get('/api/cms/blocks')).data })
  const [draft, setDraft] = useState<Row | null>(null)
  const [error, setError] = useState('')
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Website pages" lede="Each section is stored separately. The public site reads this content; it is not hard-coded in the page." />
      <div className="grid gap-3">
        {sections.data?.map((section: Row) => (
          <button key={section.id} className="border border-line bg-card p-4 text-left" type="button" onClick={() => setDraft(section)}>
            <p className="text-xs uppercase text-ink-soft">{String(section.pageKey)} · {String(section.sectionKey)}</p>
            <p className="font-semibold">{String(section.title || 'Untitled')}</p>
          </button>
        ))}
      </div>
      {draft && (
        <form className="mt-4 grid gap-3 border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          setError('')
          try {
            await api.put('/api/cms/sections', draft)
            setDraft(null)
            client.invalidateQueries({ queryKey: ['sections'] })
          } catch (err) { setError(errorMessage(err)) }
        }}>
          <Field label="Title"><TextInput value={String(draft.title ?? '')} onChange={(e) => setDraft({ ...draft, title: e.target.value })} /></Field>
          <Field label="Subtitle"><TextArea value={String(draft.subtitle ?? '')} onChange={(e) => setDraft({ ...draft, subtitle: e.target.value })} /></Field>
          <Field label="Content"><TextArea value={String(draft.content ?? '')} onChange={(e) => setDraft({ ...draft, content: e.target.value })} /></Field>
          <Field label="Image path"><TextInput value={String(draft.imagePath ?? '')} onChange={(e) => setDraft({ ...draft, imagePath: e.target.value })} /></Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Primary button"><TextInput value={String(draft.ctaLabel ?? '')} onChange={(e) => setDraft({ ...draft, ctaLabel: e.target.value })} /></Field>
            <Field label="Primary link"><TextInput value={String(draft.ctaUrl ?? '')} onChange={(e) => setDraft({ ...draft, ctaUrl: e.target.value })} /></Field>
            <Field label="Secondary button"><TextInput value={String(draft.secondaryCtaLabel ?? '')} onChange={(e) => setDraft({ ...draft, secondaryCtaLabel: e.target.value })} /></Field>
            <Field label="Secondary link"><TextInput value={String(draft.secondaryCtaUrl ?? '')} onChange={(e) => setDraft({ ...draft, secondaryCtaUrl: e.target.value })} /></Field>
          </div>
          <label className="text-sm"><input type="checkbox" checked={Boolean(draft.active)} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} /> Active</label>
          {error && <p className="text-sm text-danger">{error}</p>}
          <div className="flex gap-2"><Button type="submit">Save section</Button><Button type="button" variant="ghost" onClick={() => setDraft(null)}>Close</Button></div>
        </form>
      )}
      <h2 className="mt-8 text-2xl">Blocks</h2>
      <p className="mb-3 text-sm text-ink-soft">Statistics, reasons, values, process steps and navigation labels.</p>
      <BlockEditor rows={blocks.data ?? []} onChange={() => client.invalidateQueries({ queryKey: ['blocks'] })} />
    </div>
  )
}

function BlockEditor({ rows, onChange }: { rows: Row[]; onChange: () => void }) {
  const [draft, setDraft] = useState<Row | null>(null)
  return (
    <div className="grid gap-2">
      <Button onClick={() => setDraft({ id: '', groupKey: 'WHY_CHOOSE_US', title: '', body: '', metricValue: '', displayOrder: rows.length + 1, active: true })}>Add block</Button>
      {draft && (
        <form className="grid gap-3 border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          const body = { ...draft, displayOrder: Number(draft.displayOrder || 0) }
          if (!body.id) delete (body as { id?: string }).id
          await api.put('/api/cms/blocks', body)
          setDraft(null)
          onChange()
        }}>
          <Field label="Group"><SelectInput value={String(draft.groupKey)} onChange={(e) => setDraft({ ...draft, groupKey: e.target.value })}>
            {['NAV', 'STATS', 'WHY_CHOOSE_US', 'VALUES', 'PROCESS', 'TEAM', 'TRUST'].map((group) => <option key={group}>{group}</option>)}
          </SelectInput></Field>
          <Field label="Title"><TextInput value={String(draft.title ?? '')} onChange={(e) => setDraft({ ...draft, title: e.target.value })} /></Field>
          <Field label="Body"><TextArea value={String(draft.body ?? '')} onChange={(e) => setDraft({ ...draft, body: e.target.value })} /></Field>
          <Field label="Metric"><TextInput value={String(draft.metricValue ?? '')} onChange={(e) => setDraft({ ...draft, metricValue: e.target.value })} /></Field>
          <Button type="submit">Save block</Button>
        </form>
      )}
      {rows.map((row) => (
        <div key={row.id} className="flex items-center justify-between border border-line bg-card px-4 py-3 text-sm">
          <span><strong>{String(row.groupKey)}</strong> · {String(row.title || row.metricValue)}</span>
          <button className="text-copper" type="button" onClick={() => setDraft(row)}>Edit</button>
        </div>
      ))}
    </div>
  )
}

export function MessagesPage() {
  const [status, setStatus] = useState('')
  const query = useQuery({ queryKey: ['messages', status], queryFn: async () => (await api.get('/api/contact-messages', { params: { status: status || undefined, size: 50 } })).data })
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Contact messages" />
      <SelectInput className="mb-4 max-w-xs" value={status} onChange={(e) => setStatus(e.target.value)}>
        <option value="">All</option>
        {['NEW', 'IN_PROGRESS', 'RESOLVED', 'ARCHIVED'].map((item) => <option key={item}>{item}</option>)}
      </SelectInput>
      {query.data?.content?.length === 0 && <EmptyState title="No messages." body="Public contact submissions appear here." />}
      <div className="grid gap-3">
        {query.data?.content?.map((message: { id: string; name: string; email: string; subject: string; message: string; status: string; company?: string }) => (
          <article key={message.id} className="border border-line bg-card p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-xl">{message.subject}</h2>
              <Badge tone={statusTone(message.status)}>{titleCase(message.status)}</Badge>
            </div>
            <p className="text-sm text-ink-soft">{message.name} · {message.email} · {message.company}</p>
            <p className="mt-2 text-sm">{message.message}</p>
            <div className="mt-3 flex gap-2">
              {['IN_PROGRESS', 'RESOLVED', 'ARCHIVED'].map((next) => (
                <Button key={next} variant="ghost" type="button" onClick={async () => { await api.patch(`/api/contact-messages/${message.id}`, null, { params: { status: next } }); client.invalidateQueries({ queryKey: ['messages'] }) }}>{titleCase(next)}</Button>
              ))}
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}

export function UsersPage() {
  const users = useQuery({ queryKey: ['users'], queryFn: async () => (await api.get('/api/users', { params: { size: 50 } })).data })
  const roles = useQuery({ queryKey: ['roles'], queryFn: async () => (await api.get('/api/users/roles')).data })
  const [form, setForm] = useState({ email: '', fullName: '', password: '', role: 'HR_MANAGER' })
  const [error, setError] = useState('')
  const client = useQueryClient()
  return (
    <div>
      <PageHeader title="Users" lede="Only a super admin can grant the super admin role." />
      <form className="mb-4 grid gap-3 border border-line bg-card p-4 md:grid-cols-2" onSubmit={async (event) => {
        event.preventDefault()
        setError('')
        try {
          await api.post('/api/users', { email: form.email, fullName: form.fullName, password: form.password, active: true, roles: [form.role] })
          setForm({ email: '', fullName: '', password: '', role: 'HR_MANAGER' })
          client.invalidateQueries({ queryKey: ['users'] })
        } catch (err) { setError(errorMessage(err)) }
      }}>
        <Field label="Name" required><TextInput value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></Field>
        <Field label="Email" required><TextInput type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></Field>
        <Field label="Password" required><TextInput type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required /></Field>
        <Field label="Role"><SelectInput value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>{(roles.data ?? []).map((role: string) => <option key={role}>{role}</option>)}</SelectInput></Field>
        {error && <p className="text-sm text-danger md:col-span-2">{error}</p>}
        <Button type="submit">Create user</Button>
      </form>
      <TableShell>
        <table className="w-full text-left text-sm">
          <thead className="bg-paper-2"><tr>{['Name', 'Email', 'Roles', 'Active'].map((h) => <th key={h} className="px-3 py-3">{h}</th>)}</tr></thead>
          <tbody>
            {users.data?.content?.map((user: { id: string; fullName: string; email: string; roles: string[]; active: boolean }) => (
              <tr key={user.id} className="border-t border-line"><td className="px-3 py-3">{user.fullName}</td><td className="px-3 py-3">{user.email}</td><td className="px-3 py-3">{user.roles.join(', ')}</td><td className="px-3 py-3">{user.active ? 'Yes' : 'No'}</td></tr>
            ))}
          </tbody>
        </table>
      </TableShell>
    </div>
  )
}

export function AuditPage() {
  const [q, setQ] = useState('')
  const query = useQuery({ queryKey: ['audit', q], queryFn: async () => (await api.get('/api/audit-logs', { params: { q, size: 40 } })).data })
  return (
    <div>
      <PageHeader title="Audit log" lede="Sign-ins, job changes, selections, payroll locks and sensitive document access." />
      <TextInput className="mb-4 max-w-sm" placeholder="Search action, user or entity" value={q} onChange={(e) => setQ(e.target.value)} />
      <TableShell>
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="bg-paper-2"><tr>{['When', 'User', 'Action', 'Entity', 'IP'].map((h) => <th key={h} className="px-3 py-3">{h}</th>)}</tr></thead>
          <tbody>
            {query.data?.content?.map((row: { id: string; createdAt: string; userEmail?: string; action: string; entityType?: string; entityId?: string; ipAddress?: string }) => (
              <tr key={row.id} className="border-t border-line">
                <td className="px-3 py-3">{dateLabel(row.createdAt)}</td>
                <td className="px-3 py-3">{row.userEmail || '—'}</td>
                <td className="px-3 py-3">{row.action}</td>
                <td className="px-3 py-3">{row.entityType} {row.entityId}</td>
                <td className="px-3 py-3">{row.ipAddress}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableShell>
    </div>
  )
}

export function ReportsPage() {
  const [tab, setTab] = useState('recruitment')
  const query = useQuery({ queryKey: ['report', tab], queryFn: async () => (await api.get(`/api/reports/${tab}`)).data })
  const rows: Record<string, unknown>[] = query.data ?? []
  const columns = rows[0] ? Object.keys(rows[0]) : []
  return (
    <div>
      <PageHeader title="Reports" actions={<Button variant="ghost" type="button" onClick={() => downloadExport(tab === 'companies' ? 'companies' : tab)}>Export CSV</Button>} />
      <div className="mb-4 flex flex-wrap gap-2">
        {['recruitment', 'companies', 'employees', 'payroll', 'placements'].map((item) => (
          <Button key={item} variant={tab === item ? 'primary' : 'ghost'} type="button" onClick={() => setTab(item)}>{titleCase(item)}</Button>
        ))}
      </div>
      {rows.length === 0 && !query.isLoading && <EmptyState title="Nothing to report yet." body="The figures appear once jobs, placements or payroll exist." />}
      {rows.length > 0 && (
        <TableShell>
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="bg-paper-2"><tr>{columns.map((column) => <th key={column} className="px-3 py-3">{titleCase(column)}</th>)}</tr></thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={index} className="border-t border-line">
                  {columns.map((column) => <td key={column} className="px-3 py-3">{formatCell(column, row[column])}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </TableShell>
      )}
    </div>
  )
}

async function downloadExport(type: string) {
  const res = await api.get('/api/reports/export', { params: { type, format: 'csv' }, responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `${type}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function formatCell(column: string, value: unknown) {
  if (value == null) return '—'
  if (['gross', 'deductions', 'net', 'agreedSalary'].includes(column) && typeof value === 'number') return inr(value)
  if (typeof value === 'string' && value.includes('_')) return titleCase(value)
  return String(value)
}

export function SettingsPage() {
  const settings = useQuery({ queryKey: ['settings'], queryFn: async () => (await api.get('/api/cms/settings')).data as Record<string, string> })
  const requirements = useQuery({ queryKey: ['requirements'], queryFn: async () => (await api.get('/api/settings/document-requirements')).data })
  const [draft, setDraft] = useState<Record<string, string> | null>(null)
  const [password, setPassword] = useState({ currentPassword: '', newPassword: '' })
  const [message, setMessage] = useState('')
  const values = draft ?? settings.data ?? {}
  const client = useQueryClient()
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form className="border border-line bg-card p-4" onSubmit={async (event) => {
        event.preventDefault()
        await api.put('/api/cms/settings', values)
        setMessage('Site details saved.')
        client.invalidateQueries({ queryKey: ['settings'] })
      }}>
        <h2 className="text-2xl">Contact and site</h2>
        <div className="mt-3 grid gap-3">
          {['address', 'phone', 'email', 'hours', 'maps_url', 'facebook', 'linkedin', 'instagram', 'whatsapp'].map((key) => (
            <Field key={key} label={titleCase(key)}><TextInput value={values[key] ?? ''} onChange={(e) => setDraft({ ...values, [key]: e.target.value })} /></Field>
          ))}
          <Button type="submit">Save settings</Button>
          {message && <p className="text-sm text-pine">{message}</p>}
        </div>
      </form>
      <div className="space-y-4">
        <section className="border border-line bg-card p-4">
          <h2 className="text-2xl">Application documents</h2>
          <p className="mt-1 text-sm text-ink-soft">Identity documents stay optional unless you mark them required.</p>
          <ul className="mt-3 space-y-3 text-sm">
            {requirements.data?.map((item: { id: string; code: string; label: string; required: boolean; active: boolean; description?: string; displayOrder: number }) => (
              <li key={item.id} className="flex items-center justify-between gap-3">
                <span>{item.label} <span className="text-ink-soft">{item.code}</span></span>
                <button className="text-copper" type="button" onClick={async () => {
                  await api.put(`/api/settings/document-requirements/${item.id}`, { label: item.label, description: item.description, required: !item.required, active: item.active, displayOrder: item.displayOrder })
                  client.invalidateQueries({ queryKey: ['requirements'] })
                }}>{item.required ? 'Required' : 'Optional'}</button>
              </li>
            ))}
          </ul>
        </section>
        <form className="border border-line bg-card p-4" onSubmit={async (event) => {
          event.preventDefault()
          setMessage('')
          try {
            const res = await api.post('/api/auth/change-password', password)
            setMessage(res.data.message)
          } catch (err) { setMessage(errorMessage(err)) }
        }}>
          <h2 className="text-2xl">Change password</h2>
          <div className="mt-3 grid gap-3">
            <Field label="Current password"><TextInput type="password" value={password.currentPassword} onChange={(e) => setPassword({ ...password, currentPassword: e.target.value })} /></Field>
            <Field label="New password"><TextInput type="password" value={password.newPassword} onChange={(e) => setPassword({ ...password, newPassword: e.target.value })} /></Field>
            <Button type="submit">Update password</Button>
          </div>
        </form>
      </div>
    </div>
  )
}
