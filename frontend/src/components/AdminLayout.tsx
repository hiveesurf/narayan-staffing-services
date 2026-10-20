import { useQuery } from '@tanstack/react-query'
import { BarChart3, Briefcase, Building2, Calendar, ChevronDown, ChevronRight, ClipboardList, Factory, FileText, Globe, Images, Layers, LayoutDashboard, LogOut, Mail, MapPin, Menu, Quote, ScrollText, Search, Settings, SlidersHorizontal, Tags, UserCog, Users, Wallet } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'

function searchHref(group: string, id: string) {
  if (group === 'pages') return id
  const bases: Record<string, string> = {
    candidates: '/admin/candidates/',
    employees: '/admin/employees/',
    jobs: '/admin/jobs/',
    companies: '/admin/companies/',
    applications: '/admin/applications/',
    payroll: '/admin/payroll/',
    slips: '/admin/payroll/slips/',
    users: '/admin/users',
    interviews: '/admin/interviews',
    placements: '/admin/placements',
  }
  const base = bases[group]
  if (!base) return '/admin/dashboard'
  return base.endsWith('/') ? `${base}${id}` : base
}

const searchLabels: Record<string, string> = {
  pages: 'Pages',
  employees: 'Employees',
  candidates: 'Candidates',
  companies: 'Companies',
  jobs: 'Jobs',
  applications: 'Applications',
  payroll: 'Payroll',
  slips: 'Salary slips',
  placements: 'Placements',
  interviews: 'Interviews',
  users: 'Users',
}

type NavItem = { label: string; to: string; icon: typeof LayoutDashboard; permission: string }

const sections: { title: string; purpose: string; icon: typeof LayoutDashboard; chip: string; mark: string; items: NavItem[] }[] = [
  {
    title: 'Overview',
    purpose: 'The desk and the numbers',
    icon: LayoutDashboard,
    chip: 'bg-pine/15 text-pine',
    mark: 'text-pine',
    items: [
      { label: 'Dashboard', to: '/admin/dashboard', icon: LayoutDashboard, permission: 'DASHBOARD_READ' },
      { label: 'Reports', to: '/admin/reports', icon: BarChart3, permission: 'REPORT_READ' },
    ],
  },
  {
    title: 'Hiring',
    purpose: 'Jobs, applicants, and interviews',
    icon: Briefcase,
    chip: 'bg-lagoon/15 text-lagoon',
    mark: 'text-lagoon',
    items: [
      { label: 'Jobs', to: '/admin/jobs', icon: Briefcase, permission: 'JOB_READ' },
      { label: 'Applications', to: '/admin/applications', icon: ClipboardList, permission: 'APPLICATION_READ' },
      { label: 'Candidates', to: '/admin/candidates', icon: Users, permission: 'CANDIDATE_READ' },
      { label: 'Interviews', to: '/admin/interviews', icon: Calendar, permission: 'INTERVIEW_READ' },
    ],
  },
  {
    title: 'Workforce',
    purpose: 'Companies, placements, and staff',
    icon: Users,
    chip: 'bg-amber/15 text-amber',
    mark: 'text-amber',
    items: [
      { label: 'Companies', to: '/admin/companies', icon: Building2, permission: 'COMPANY_READ' },
      { label: 'Placements', to: '/admin/placements', icon: MapPin, permission: 'PLACEMENT_READ' },
      { label: 'Employees', to: '/admin/employees', icon: Users, permission: 'EMPLOYEE_READ' },
    ],
  },
  {
    title: 'Payroll',
    purpose: 'Monthly pay and salary parts',
    icon: Wallet,
    chip: 'bg-teal/15 text-teal',
    mark: 'text-teal',
    items: [
      { label: 'Payroll', to: '/admin/payroll', icon: Wallet, permission: 'PAYROLL_READ' },
      { label: 'Salary components', to: '/admin/salary-components', icon: SlidersHorizontal, permission: 'PAYROLL_READ' },
    ],
  },
  {
    title: 'Website',
    purpose: 'Public pages, photos, and enquiries',
    icon: Globe,
    chip: 'bg-navy/15 text-navy',
    mark: 'text-navy',
    items: [
      { label: 'Services', to: '/admin/services', icon: Layers, permission: 'CMS_READ' },
      { label: 'Categories', to: '/admin/categories', icon: Tags, permission: 'CMS_READ' },
      { label: 'Clients', to: '/admin/clients', icon: Building2, permission: 'CMS_READ' },
      { label: 'Gallery', to: '/admin/gallery', icon: Images, permission: 'CMS_READ' },
      { label: 'Testimonials', to: '/admin/testimonials', icon: Quote, permission: 'CMS_READ' },
      { label: 'Industries', to: '/admin/industries', icon: Factory, permission: 'CMS_READ' },
      { label: 'Website pages', to: '/admin/pages', icon: FileText, permission: 'CMS_READ' },
      { label: 'Messages', to: '/admin/contact-messages', icon: Mail, permission: 'CMS_READ' },
    ],
  },
  {
    title: 'Office',
    purpose: 'Users, the audit log, and settings',
    icon: Settings,
    chip: 'bg-plum/15 text-plum',
    mark: 'text-plum',
    items: [
      { label: 'Users', to: '/admin/users', icon: UserCog, permission: 'USER_MANAGE' },
      { label: 'Audit log', to: '/admin/audit-logs', icon: ScrollText, permission: 'AUDIT_READ' },
      { label: 'Settings', to: '/admin/settings', icon: Settings, permission: 'SETTINGS_MANAGE' },
    ],
  },
]

export function AdminLayout() {
  const { user, can, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const [q, setQ] = useState('')
  const [picked, setPicked] = useState<{ path: string; title: string | null } | null>(null)
  const term = q.trim()
  const search = useQuery({
    queryKey: ['search', term],
    queryFn: async () => (await api.get('/api/search', { params: { q: term } })).data as Record<string, { id: string; label: string; sub: string }[]>,
    enabled: term.length > 0,
  })
  const groups = Object.entries(search.data ?? {}).filter(([, rows]) => rows.length > 0)
  const visible = sections
    .map((section) => ({ ...section, items: section.items.filter((item) => can(item.permission)) }))
    .filter((section) => section.items.length > 0)
  const activeTitle = visible.find((section) => section.items.some((item) => pathname === item.to || (item.to !== '/admin/dashboard' && pathname.startsWith(`${item.to}/`))))?.title ?? null
  const openTitle = picked?.path === pathname ? picked.title : activeTitle
  return (
    <div className="min-h-dvh bg-paper-2 lg:grid lg:grid-cols-[300px_1fr]">
      <aside className={`${open ? 'fixed inset-y-0 left-0 z-40 flex' : 'hidden'} w-[300px] flex-col border-r border-line bg-card text-ink lg:sticky lg:top-0 lg:flex lg:h-dvh`}>
        <Link to="/admin/dashboard" className="border-b border-line px-4 py-4">
          <img src="/media/logo.png" alt="Narayan Staffing Services" className="h-auto w-full" />
        </Link>
        <nav className="flex-1 overflow-y-auto" aria-label="Admin sections">
          {visible.map((section) => {
            const isOpen = openTitle === section.title
            return (
              <div key={section.title} className="border-b border-line">
                <button
                  type="button"
                  className="flex w-full items-center justify-between gap-3 px-3 py-3 text-left hover:bg-paper-2"
                  aria-expanded={isOpen}
                  onClick={() => setPicked({ path: pathname, title: isOpen ? null : section.title })}
                >
                  <span className="flex min-w-0 items-center gap-3">
                    <span className={`flex size-9 shrink-0 items-center justify-center ${section.chip}`}><section.icon size={18} /></span>
                    <span className="min-w-0">
                      <span className="block font-display text-xl uppercase text-pine">{section.title}</span>
                      {!isOpen && <span className="mt-0.5 block text-xs leading-5 text-ink-soft">{section.purpose}</span>}
                    </span>
                  </span>
                  {isOpen ? <ChevronDown className={`shrink-0 ${section.mark}`} size={16} /> : <ChevronRight className="shrink-0 text-ink-soft" size={16} />}
                </button>
                {isOpen && (
                  <div className="px-2 pb-2">
                    {section.items.map((item) => (
                      <NavLink key={item.to} to={item.to} end={item.to === '/admin/dashboard'} onClick={() => setOpen(false)} className={({ isActive }) => `mb-0.5 flex items-center gap-2 border-l-4 px-3 py-2 text-sm ${isActive ? 'border-saffron bg-saffron/20 font-semibold text-ink' : 'border-transparent text-ink-soft hover:bg-paper-2 hover:text-ink'}`}>
                        <item.icon className={section.mark} size={16} /> {item.label}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </nav>
      </aside>
      {open && <button className="fixed inset-0 z-30 bg-ink/40 lg:hidden" aria-label="Close menu" onClick={() => setOpen(false)} />}
      <div className="min-w-0">
        <header className="flex h-16 items-center gap-3 border-b border-line bg-card px-4 shadow-[inset_0_3px_0_0_var(--color-saffron)]">
          <button className="lg:hidden" aria-label="Open menu" onClick={() => setOpen(true)} type="button"><Menu /></button>
          <div className="relative max-w-xl flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-soft" size={16} />
            <input
              className="h-10 w-full border border-line bg-paper pl-9 pr-3 text-sm outline-none focus:border-pine"
              placeholder="Search employees, companies, jobs, payroll"
              aria-label="Search employees, companies, jobs, payroll"
              aria-expanded={term.length > 0}
              aria-controls="desk-search"
              role="combobox"
              value={q}
              onChange={(event) => setQ(event.target.value)}
            />
            {term.length > 0 && (
              <div id="desk-search" className="absolute z-40 mt-1 max-h-96 w-full overflow-auto border border-line bg-card p-2 text-sm shadow" onMouseDown={(event) => event.preventDefault()}>
                {search.isLoading && !search.data && <p className="px-2 py-2 text-ink-soft">Searching…</p>}
                {search.isError && <p className="px-2 py-2 text-danger">Search could not be completed.</p>}
                {search.data && groups.length === 0 && <p className="px-2 py-2 text-ink-soft" role="status">No results. Try a different name, company, job, or payroll month.</p>}
                {groups.map(([key, rows]) => (
                  <div key={key} className="mb-2">
                    <p className="px-2 text-xs uppercase text-ink-soft">{searchLabels[key] ?? key}</p>
                    {rows.map((row) => (
                      <Link key={`${key}-${row.id}`} to={searchHref(key, row.id)} className="block px-2 py-1.5 hover:bg-paper-2" onClick={() => setQ('')}>
                        {row.label} {row.sub && <span className="text-ink-soft">{row.sub}</span>}
                      </Link>
                    ))}
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="hidden sm:inline">{user?.fullName}</span>
            <button className="inline-flex items-center gap-1 text-pine" type="button" onClick={async () => { await logout(); navigate('/admin/login') }}><LogOut size={16} /> Sign out</button>
          </div>
        </header>
        <main className="p-4 md:p-6"><Outlet /></main>
      </div>
    </div>
  )
}
