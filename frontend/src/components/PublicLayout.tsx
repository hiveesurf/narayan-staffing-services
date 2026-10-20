import { useQuery } from '@tanstack/react-query'
import { Menu, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { api } from '../lib/api'

type LayoutData = {
  site: Record<string, string>
  nav: { title: string; linkUrl: string }[]
  footer?: { title?: string; content?: string }
  services: { title: string; slug: string }[]
}

export function PublicLayout() {
  const { data } = useQuery({ queryKey: ['layout'], queryFn: async () => (await api.get<LayoutData>('/api/public/layout')).data })
  const [open, setOpen] = useState(false)
  const site = data?.site ?? {}
  const nav = data?.nav ?? []
  return (
    <div className="min-h-dvh bg-paper-2 text-ink">
      <div className="sticky top-0 z-40">
      <div className="h-1.5 bg-saffron" />
      <header className="border-b border-line bg-paper text-ink">
        <div className="container-page flex h-20 items-center justify-between gap-4">
          <Link to="/" className="shrink-0">
            <img src="/media/logo.png" alt="Narayan Staffing Services" className="h-12 w-auto" />
          </Link>
          <nav className="hidden items-center gap-6 font-display text-xl uppercase lg:flex">
            {nav.map((item) => (
              <NavLink key={item.linkUrl} to={item.linkUrl} end={item.linkUrl === '/'} className={({ isActive }) => isActive ? 'border-b-4 border-saffron pb-1 text-ink' : 'border-b-4 border-transparent pb-1 text-ink-soft hover:text-ink'}>{item.title}</NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <Link to="/careers" className="inline-flex h-10 items-center bg-pine px-4 font-display text-xl uppercase text-white hover:bg-saffron hover:text-ink">Open jobs</Link>
            <button className="inline-flex size-10 items-center justify-center text-ink lg:hidden" aria-label="Open menu" onClick={() => setOpen(true)} type="button"><Menu size={20} /></button>
          </div>
        </div>
      </header>
      </div>
      {open && (
        <div className="fixed inset-0 z-50 bg-ink/40 lg:hidden" onClick={() => setOpen(false)}>
          <div className="ml-auto flex h-full w-[min(100%,320px)] flex-col border-l border-line bg-paper p-5 text-ink" onClick={(event) => event.stopPropagation()}>
            <button className="self-end text-ink" aria-label="Close menu" onClick={() => setOpen(false)} type="button"><X /></button>
            <div className="mt-6 flex flex-col gap-4 font-display text-3xl">
              {nav.map((item) => (
                <NavLink key={item.linkUrl} to={item.linkUrl} end={item.linkUrl === '/'} onClick={() => setOpen(false)} className={({ isActive }) => isActive ? 'border-l-4 border-saffron pl-3 text-ink' : 'border-l-4 border-transparent pl-3 text-ink-soft'}>{item.title}</NavLink>
              ))}
              <Link to="/admin/login" onClick={() => setOpen(false)}>Staff login</Link>
            </div>
          </div>
        </div>
      )}
      <Outlet />
      <footer className="mt-16 border-t-4 border-saffron bg-paper-2 text-ink">
        <div className="container-page grid gap-10 py-12 md:grid-cols-4">
          <div className="md:col-span-2">
            <img src="/media/logo.png" alt="Narayan Staffing Services" className="h-14 w-auto" />
            <p className="mt-3 max-w-md text-ink-soft">{data?.footer?.content}</p>
            <p className="mt-4 text-ink-soft">{site.address}</p>
          </div>
          <div>
            <p className="font-display text-xl text-pine">Visit</p>
            <div className="mt-3 flex flex-col gap-2">
              {nav.map((item) => <Link key={item.linkUrl} className="text-ink-soft hover:text-ink" to={item.linkUrl}>{item.title}</Link>)}
              <Link className="text-ink-soft hover:text-ink" to="/privacy-policy">Privacy</Link>
              <Link className="text-ink-soft hover:text-ink" to="/terms">Terms</Link>
            </div>
          </div>
          <div>
            <p className="font-display text-xl text-pine">Services</p>
            <div className="mt-3 flex flex-col gap-2">
              {(data?.services ?? []).map((service) => <Link key={service.slug} className="text-ink-soft hover:text-ink" to={`/services/${service.slug}`}>{service.title}</Link>)}
            </div>
          </div>
        </div>
        <div className="border-t border-line">
          <div className="container-page flex h-12 items-center justify-between text-xs text-ink-soft">
            <span>© {new Date().getFullYear()} Narayan Staffing Services</span>
            <Link to="/admin/login">Staff login</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
