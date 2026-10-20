import { useQuery } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ClientMarquee, HeroSlide } from '../../components/motion'
import { Button, EmptyState, Field, PageBand, SelectInput, TextArea, TextInput } from '../../components/desk-ui'
import { api, errorMessage } from '../../lib/api'
import { inr, titleCase } from '../../lib/format'
import { contactSchema } from '../../lib/schemas'

type Section = { title?: string; subtitle?: string; content?: string; ctaLabel?: string; ctaUrl?: string; secondaryCtaLabel?: string; secondaryCtaUrl?: string; imagePath?: string }
type Block = { id: string; title?: string; subtitle?: string; body?: string; metricValue?: string; icon?: string }
type Service = { id: string; title: string; slug: string; shortDescription?: string; detailedDescription?: string; seoTitle?: string; seoDescription?: string }
type Job = {
  id: string; title: string; slug: string; companyName: string; location: string; salaryMin?: number; salaryMax?: number
  experienceMin?: number; experienceMax?: number; employmentType: string; vacancies: number; createdAt?: string; publishDate?: string
  jobDescription?: string; responsibilities?: string; requiredSkills?: string; preferredSkills?: string; benefits?: string
  education?: string; department?: string; deadline?: string; categoryName?: string
}

function useSeo(title: string, description?: string) {
  if (typeof document !== 'undefined') {
    document.title = title
    if (!description) return
    let tag = document.querySelector('meta[name="description"]')
    if (!tag) {
      tag = document.createElement('meta')
      tag.setAttribute('name', 'description')
      document.head.appendChild(tag)
    }
    tag.setAttribute('content', description)
  }
}

const card = 'border border-line border-t-4 border-t-pine bg-card p-5 transition duration-200 hover:bg-saffron'
const action = 'inline-flex h-11 items-center justify-center bg-pine px-5 font-display text-xl text-white hover:bg-saffron hover:text-ink'

export function HomePage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['home'], queryFn: async () => (await api.get('/api/public/home')).data })
  const sections = (data?.sections ?? []) as (Section & { sectionKey?: string })[]
  const hero = sections.find((section) => section.sectionKey === 'HERO') ?? sections.find((section) => section.title)
  const about = sections.find((section) => section.sectionKey === 'ABOUT_SUMMARY') ?? sections.find((section) => section.content)
  const cta = (data?.cta as Section | undefined) ?? sections.find((section) => section.sectionKey === 'CTA')
  const services = (data?.services ?? []) as Service[]
  const why = (data?.why ?? []) as Block[]
  const stats = (data?.stats ?? []) as Block[]
  const industries = (data?.industries ?? []) as { id: string; name: string; description?: string }[]
  const clients = (data?.clients ?? []) as { id: string; name: string; logoPath?: string }[]
  const testimonials = (data?.testimonials ?? []) as { id: string; quote: string; authorName: string; authorRole?: string; companyName?: string }[]
  useSeo('Narayan Staffing Services', hero?.subtitle)
  if (isLoading) return <div className="container-page py-16"><div className="skeleton h-80 rounded-lg" /></div>
  if (isError) return <main className="container-page py-16"><EmptyState title="The homepage could not load." body="The company content did not come back from the server." action={<Button onClick={() => refetch()}>Retry</Button>} /></main>
  return (
    <main>
      <section className="bg-paper text-ink">
        <div className="container-page grid items-center gap-8 py-10 lg:grid-cols-[0.85fr_1.15fr] lg:py-14">
          <div>
            <p className="font-display text-2xl text-pine">Chakan, Pune · PAN India</p>
            <h1 className="mt-2 max-w-4xl text-6xl text-ink sm:text-7xl">{hero?.title || 'Trusted Manpower & Staffing Solutions'}</h1>
            <p className="mt-6 max-w-xl text-pretty text-lg text-ink-soft">{hero?.subtitle}</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={hero?.ctaUrl || '/careers'} className="inline-flex h-12 items-center bg-saffron px-5 font-display text-2xl text-ink hover:bg-ink hover:text-paper">{hero?.ctaLabel || 'View Jobs'}</Link>
              <Link to={hero?.secondaryCtaUrl || '/contact'} className="inline-flex h-12 items-center border border-pine px-5 font-display text-2xl text-pine hover:bg-pine hover:text-white">{hero?.secondaryCtaLabel || 'Contact Us'}</Link>
            </div>
          </div>
          <HeroSlide />
        </div>
        <div className="grid grid-cols-2 border-t border-line bg-pine/10 md:grid-cols-4">
          {stats.map((stat, index) => (
            <div key={stat.id} className={`px-5 py-5 ${index > 0 ? 'border-l border-line' : ''}`}>
              <p className="nums font-display text-5xl text-pine">{stat.metricValue}</p>
              <p className="mt-1 text-sm text-ink-soft">{stat.title}</p>
            </div>
          ))}
        </div>
      </section>
      <section>
        <div className="container-page flex items-end justify-between py-8">
          <h2 className="text-5xl">What the desk takes on</h2>
        </div>
        <ul className="border-y border-line">
          {services.map((service, index) => (
            <li key={service.id} className="border-b border-line transition duration-200 last:border-b-0 hover:bg-saffron">
              <Link to={`/services/${service.slug}`} className="container-page grid gap-x-8 gap-y-1 py-5 md:grid-cols-[4rem_max-content_1fr] md:items-baseline">
                <span className="font-display text-3xl text-pine">{String(index + 1).padStart(2, '0')}</span>
                <h3 className="text-3xl">{service.title}</h3>
                <p className="text-ink-soft">{service.shortDescription}</p>
              </Link>
            </li>
          ))}
        </ul>
      </section>
      <section className="container-page grid gap-10 py-14 lg:grid-cols-[16rem_1fr]">
        <h2 className="text-5xl">Why clients stay</h2>
        <ul className="divide-y divide-ink border-y border-ink">
          {why.map((item) => (
            <li key={item.id} className="py-4">
              <h3 className="text-3xl">{item.title}</h3>
              <p className="mt-1 max-w-2xl text-ink-soft">{item.body}</p>
            </li>
          ))}
        </ul>
      </section>
      <section className="grid lg:grid-cols-2">
        <img src="/media/team-desk.jpg" alt="The staffing desk reviewing a requirement" className="h-full max-h-[520px] w-full object-cover" />
        <div className="flex flex-col justify-center bg-card px-6 py-12 sm:px-12">
          <h2 className="text-5xl">{about?.title}</h2>
          <p className="mt-4 max-w-xl text-ink-soft">{about?.content}</p>
          {about?.ctaLabel && <Link to={about.ctaUrl || '/about'} className="mt-6 inline-flex h-12 w-fit items-center bg-ink px-5 font-display text-2xl text-paper hover:bg-saffron hover:text-ink">{about.ctaLabel}</Link>}
        </div>
      </section>
      <section className="container-page py-12">
        <h2 className="text-5xl">Industries</h2>
        <div className="mt-4 flex flex-wrap gap-2">
          {industries.map((industry) => (
            <span key={industry.id} className="border border-ink px-3 py-1 font-display text-xl uppercase">{industry.name}</span>
          ))}
        </div>
      </section>
      <section className="overflow-hidden border-y border-line bg-paper py-8">
        <div className="container-page mb-2 flex items-end justify-between">
          <h2 className="text-4xl text-ink">Clients</h2>
          <Link to="/clients" className="font-display text-xl text-ink hover:underline">All clients</Link>
        </div>
        <ClientMarquee clients={clients} />
      </section>
      <section className="container-page py-14">
        <h2 className="text-5xl">From the sites</h2>
        <div className="mt-8 grid gap-10 lg:grid-cols-3">
          {testimonials.map((item) => (
            <blockquote key={item.id}>
              <p className="font-display text-7xl leading-none" aria-hidden="true">“</p>
              <p className="text-xl leading-snug">{item.quote}</p>
              <footer className="mt-4 text-sm font-semibold text-ink-soft">{item.authorName}{item.authorRole ? ` · ${item.authorRole}` : ''}{item.companyName ? ` · ${item.companyName}` : ''}</footer>
            </blockquote>
          ))}
        </div>
      </section>
      <section className="grid lg:grid-cols-2">
        <img src="/media/site-work.jpg" alt="A project site supplied by the company" className="h-full max-h-[480px] w-full object-cover" />
        <div className="flex flex-col justify-center bg-saffron px-6 py-12 text-ink sm:px-12">
          <h2 className="max-w-xl text-5xl">{cta?.title || 'Looking for the Right Workforce?'}</h2>
          <p className="mt-3 max-w-xl">{cta?.subtitle}</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to={cta?.ctaUrl || '/contact'} className="inline-flex h-12 items-center bg-ink px-5 font-display text-2xl text-paper hover:bg-paper hover:text-ink">{cta?.ctaLabel || 'Hire Talent'}</Link>
            <Link to={cta?.secondaryCtaUrl || '/careers'} className="inline-flex h-12 items-center border border-ink px-5 font-display text-2xl hover:bg-ink hover:text-paper">{cta?.secondaryCtaLabel || 'Browse Jobs'}</Link>
          </div>
        </div>
      </section>
    </main>
  )
}

export function AboutPage() {
  const { data, isLoading } = useQuery({ queryKey: ['about'], queryFn: async () => (await api.get('/api/public/about')).data })
  const sections = (data?.sections ?? []) as Section[]
  const lead = sections[0]
  useSeo('About | Narayan Staffing Services', lead?.subtitle || lead?.content)
  if (isLoading) return <div className="container-page py-16"><div className="skeleton h-64 rounded-lg" /></div>
  return (
    <>
      <PageBand kicker="The company" title={lead?.title || 'About'} lede={lead?.subtitle} />
      <main className="container-page py-12">
        {sections.map((section, index) => (
          <section key={`${section.title}-${index}`} className="mb-10 max-w-3xl">
            {index === 0 ? null : <h2 className="text-3xl">{section.title}</h2>}
            {index === 0 && section.content && <p className="text-pretty text-lg text-ink-soft">{section.content}</p>}
            {index !== 0 && section.subtitle && <p className="mt-3 text-pretty text-lg text-ink-soft">{section.subtitle}</p>}
            {index !== 0 && section.content && <p className="mt-4 whitespace-pre-line text-pretty text-ink-soft">{section.content}</p>}
          </section>
        ))}
        <div className="grid gap-4 md:grid-cols-2">
          {(data?.values as Block[] | undefined)?.map((item) => (
            <article key={item.id} className={card}><h2 className="text-2xl">{item.title}</h2><p className="mt-2 text-pretty text-ink-soft">{item.body}</p></article>
          ))}
        </div>
        <h2 className="mt-14 text-4xl">How a requirement moves</h2>
        <ol className="mt-6 grid gap-4 md:grid-cols-5">
          {(data?.process as Block[] | undefined)?.map((item) => (
            <li key={item.id} className={`${card} border-t-4 border-t-copper`}><p className="text-xs font-semibold text-copper">{item.subtitle}</p><h3 className="mt-1 text-xl">{item.title}</h3><p className="mt-2 text-pretty text-sm text-ink-soft">{item.body}</p></li>
          ))}
        </ol>
        <h2 className="mt-14 text-4xl">The desks</h2>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {(data?.team as Block[] | undefined)?.map((item) => (
            <article key={item.id} className="border border-line bg-paper p-5 hover:bg-saffron"><h3 className="text-2xl">{item.title}</h3><p className="text-sm text-ink-soft">{item.subtitle}</p><p className="mt-2 text-pretty text-sm text-ink-soft">{item.body}</p></article>
          ))}
        </div>
      </main>
    </>
  )
}

export function ServicesPage() {
  const { data, isLoading } = useQuery({ queryKey: ['services'], queryFn: async () => (await api.get<Service[]>('/api/public/services')).data })
  useSeo('Services | Narayan Staffing Services', 'Recruitment, staffing, payroll, facility management and inspection from Narayan Staffing Services.')
  return (
    <>
      <PageBand kicker="Services" title="Work we are hired for" lede="Recruitment, staffing, payroll, facility management and inspection." />
      <main className="container-page py-12">
        <div className="grid gap-4 md:grid-cols-2">
          {isLoading && <div className="skeleton h-40 rounded-2xl" />}
          {data?.map((service) => (
            <Link key={service.id} to={`/services/${service.slug}`} className={card}>
              <h2 className="text-3xl">{service.title}</h2>
              <p className="mt-3 text-pretty text-ink-soft">{service.shortDescription}</p>
            </Link>
          ))}
        </div>
      </main>
    </>
  )
}

export function ServiceDetailPage() {
  const { slug } = useParams()
  const { data, isLoading, isError } = useQuery({ queryKey: ['service', slug], queryFn: async () => (await api.get<Service>(`/api/public/services/${slug}`)).data })
  useSeo(data?.seoTitle || `${data?.title || 'Service'} | Narayan Staffing Services`, data?.seoDescription)
  if (isLoading) return <div className="container-page py-16"><div className="skeleton h-64 rounded-lg" /></div>
  if (isError || !data) return <main className="container-page py-16"><EmptyState title="Service not found" body="This page is no longer published." /></main>
  return (
    <>
      <PageBand kicker="Service" title={data.title} lede={data.shortDescription} />
      <main className="container-page max-w-3xl py-12">
        <p className="whitespace-pre-line text-pretty">{data.detailedDescription}</p>
        <Link to="/contact" className={`mt-8 ${action}`}>Talk to the desk</Link>
      </main>
    </>
  )
}

export function CareersPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') || ''
  const [draft, setDraft] = useState(q)
  const query = useQuery({
    queryKey: ['jobs', params.toString()],
    queryFn: async () => (await api.get('/api/public/jobs', { params: Object.fromEntries(params.entries()) })).data,
  })
  const categories = useQuery({ queryKey: ['categories'], queryFn: async () => (await api.get('/api/public/categories')).data })
  const locations = useQuery({ queryKey: ['locations'], queryFn: async () => (await api.get<string[]>('/api/public/jobs/locations')).data })
  useSeo('Careers | Narayan Staffing Services', 'Find your next opportunity with Narayan Staffing Services.')
  function update(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    setParams(next)
  }
  const jobs = (query.data?.content ?? []) as Job[]
  const total = (query.data?.totalElements as number | undefined) ?? jobs.length
  return (
    <>
      <PageBand kicker="Careers" title="Find your next opportunity" lede="Open roles from plants, projects and offices the desk is staffing." />
      <main className="container-page py-12">
      <form className="border border-line border-t-4 border-t-saffron bg-paper p-4" onSubmit={(event) => { event.preventDefault(); update('q', draft) }}>
        <div className="flex gap-2">
          <TextInput className="h-12 flex-1" placeholder="Search by job title, skill or keyword" value={draft} onChange={(event) => setDraft(event.target.value)} />
          <Button type="submit">Search</Button>
        </div>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <SelectInput value={params.get('location') || ''} onChange={(event) => update('location', event.target.value)}>
            <option value="">All locations</option>
            {locations.data?.map((location) => <option key={location}>{location}</option>)}
          </SelectInput>
          <SelectInput value={params.get('category') || ''} onChange={(event) => update('category', event.target.value)}>
            <option value="">All categories</option>
            {categories.data?.map((category: { slug: string; name: string }) => <option key={category.slug} value={category.slug}>{category.name}</option>)}
          </SelectInput>
          <SelectInput value={params.get('experienceLevel') || ''} onChange={(event) => update('experienceLevel', event.target.value)}>
            <option value="">Any experience</option>
            <option value="FRESHER">Fresher</option>
            <option value="JUNIOR">1–3 years</option>
            <option value="MID">3–6 years</option>
            <option value="SENIOR">6+ years</option>
          </SelectInput>
          <SelectInput value={params.get('employmentType') || ''} onChange={(event) => update('employmentType', event.target.value)}>
            <option value="">Any type</option>
            {['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN'].map((type) => <option key={type} value={type}>{titleCase(type)}</option>)}
          </SelectInput>
        </div>
      </form>
      <p className="mt-8 font-display text-2xl">{query.isLoading ? 'Loading roles' : `${total} open ${total === 1 ? 'role' : 'roles'}`}</p>
      <div className="mt-4 grid gap-5">
        {query.isLoading && <div className="skeleton h-40" />}
        {!query.isLoading && jobs.length === 0 && <EmptyState title="No jobs posted yet." body="Check back soon, or contact the desk if you have a trade we should know about." />}
        {jobs.map((job) => (
          <article key={job.id} className="border border-line border-l-8 border-l-saffron bg-paper p-5">
            <div className="grid gap-6 md:grid-cols-[1fr_auto] md:items-end">
              <div>
                <p className="font-display text-xl">{job.categoryName || titleCase(job.employmentType)}</p>
                <h2 className="mt-1 text-4xl"><Link to={`/careers/${job.slug}`} className="hover:underline">{job.title}</Link></h2>
                <p className="mt-2 text-ink-soft">{job.companyName} · {job.location}{job.department ? ` · ${job.department}` : ''}</p>
                {excerpt(job.jobDescription) && <p className="mt-3 max-w-2xl text-pretty text-ink-soft">{excerpt(job.jobDescription)}</p>}
                <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
                  <div>
                    <dt className="text-sm text-ink-soft">Type</dt>
                    <dd className="font-display text-2xl">{titleCase(job.employmentType)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm text-ink-soft">Experience</dt>
                    <dd className="nums font-display text-2xl">{job.experienceMin ?? 0}–{job.experienceMax ?? '…'} yrs</dd>
                  </div>
                  <div>
                    <dt className="text-sm text-ink-soft">Pay</dt>
                    <dd className="nums font-display text-2xl">{inr(job.salaryMin)} – {inr(job.salaryMax)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm text-ink-soft">Openings</dt>
                    <dd className="nums font-display text-2xl text-pine">{job.vacancies}</dd>
                  </div>
                </dl>
              </div>
              <Link to={`/careers/${job.slug}`} className="inline-flex h-12 items-center justify-center bg-saffron px-5 font-display text-2xl text-ink hover:bg-ink hover:text-paper">View role</Link>
            </div>
          </article>
        ))}
      </div>
      </main>
    </>
  )
}

function excerpt(text?: string) {
  if (!text) return ''
  const clean = text.replace(/\s+/g, ' ').trim()
  return clean.length > 160 ? `${clean.slice(0, 157)}…` : clean
}

export function JobDetailPage() {
  const { slug } = useParams()
  const { data, isLoading, isError } = useQuery({ queryKey: ['job', slug], queryFn: async () => (await api.get(`/api/public/jobs/${slug}`)).data })
  const job = data?.job as Job | undefined
  useSeo(job ? `${job.title} in ${job.location} | Narayan Staffing Services` : 'Job', job ? `${job.title} with ${job.companyName} in ${job.location}.` : undefined)
  if (isLoading) return <div className="container-page py-16"><div className="skeleton h-80 rounded-lg" /></div>
  if (isError || !job) return <main className="container-page py-16"><EmptyState title="This job is closed" body="It may have been filled or unpublished." action={<Link to="/careers" className="font-semibold text-copper">Back to careers</Link>} /></main>
  async function share() {
    const url = window.location.href
    if (navigator.share) await navigator.share({ title: job?.title, url })
    else await navigator.clipboard.writeText(url)
  }
  return (
    <>
      <PageBand kicker={job.categoryName || 'Careers'} title={job.title} lede={`${job.companyName} · ${job.location}${job.department ? ` · ${job.department}` : ''}`} />
      <main className="container-page py-12">
      <dl className="grid grid-cols-2 border border-line border-t-4 border-t-saffron md:grid-cols-4">
        <div className="px-4 py-4"><dt className="text-sm text-ink-soft">Type</dt><dd className="font-display text-2xl">{titleCase(job.employmentType)}</dd></div>
        <div className="border-l border-line px-4 py-4"><dt className="text-sm text-ink-soft">Pay</dt><dd className="nums font-display text-2xl">{inr(job.salaryMin)} – {inr(job.salaryMax)}</dd></div>
        <div className="border-l border-line px-4 py-4"><dt className="text-sm text-ink-soft">Experience</dt><dd className="nums font-display text-2xl">{job.experienceMin ?? 0}–{job.experienceMax ?? '…'} yrs</dd></div>
        <div className="border-l border-line px-4 py-4"><dt className="text-sm text-ink-soft">Openings</dt><dd className="nums font-display text-2xl text-pine">{job.vacancies}</dd></div>
      </dl>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link to={`/careers/${job.slug}/apply`} className="inline-flex h-12 items-center bg-saffron px-5 font-display text-2xl text-ink hover:bg-ink hover:text-paper">Apply Now</Link>
        <Button variant="ghost" type="button" onClick={share}>Share Job</Button>
      </div>
      <div className="mt-10 grid gap-8 lg:grid-cols-[1.4fr_0.6fr]">
        <div className="space-y-6">
          <Block title="Description" text={job.jobDescription} />
          <Block title="Responsibilities" text={job.responsibilities} />
          <Block title="Required skills" text={job.requiredSkills} />
          <Block title="Preferred skills" text={job.preferredSkills} />
          <Block title="Benefits" text={job.benefits} />
        </div>
        <aside className="h-fit border border-line border-t-4 border-t-saffron bg-paper p-5 text-sm">
          <p><span className="text-ink-soft">Education</span><br />{job.education || '—'}</p>
          <p className="mt-3"><span className="text-ink-soft">Deadline</span><br />{job.deadline || 'Open'}</p>
          {job.department && <p className="mt-3"><span className="text-ink-soft">Department</span><br />{job.department}</p>}
        </aside>
      </div>
      <h2 className="mt-14 text-3xl">Related jobs</h2>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        {(data?.related as Job[] | undefined)?.map((item) => (
          <Link key={item.id} to={`/careers/${item.slug}`} className={card}><h3 className="text-xl">{item.title}</h3><p className="text-sm text-ink-soft">{item.location}</p></Link>
        ))}
      </div>
      </main>
    </>
  )
}

function Block({ title, text }: { title: string; text?: string }) {
  if (!text) return null
  return <section className="border border-line bg-paper p-5"><h2 className="text-2xl">{title}</h2><p className="mt-2 whitespace-pre-line text-pretty text-ink-soft">{text}</p></section>
}

export function GalleryPage() {
  const { data, isLoading } = useQuery({ queryKey: ['gallery'], queryFn: async () => (await api.get('/api/public/gallery')).data })
  const [active, setActive] = useState<string | null>(null)
  useSeo('Gallery | Narayan Staffing Services', 'Photographs from sites and teams staffed by Narayan Staffing Services.')
  return (
    <>
    <PageBand kicker="Gallery" title="From the sites" lede="Photographs the desk publishes from plants, projects and teams." />
    <main className="container-page py-12">
      {isLoading && <div className="skeleton mt-8 h-64 rounded-lg" />}
      {!isLoading && (!data || data.length === 0) && <div className="mt-8"><EmptyState title="Photographs will appear here." body="The desk publishes site and team photographs from the gallery manager." /></div>}
      <div className="mt-8 columns-1 gap-4 sm:columns-2 lg:columns-3">
        {data?.map((item: { id: string; title: string; description?: string; imagePath: string }) => (
          <button key={item.id} className="group mb-4 block w-full break-inside-avoid overflow-hidden border border-ink bg-card text-left transition duration-200 hover:bg-saffron" onClick={() => setActive(item.imagePath)} type="button">
            <img src={item.imagePath} alt={item.title} loading="lazy" className="aspect-[4/3] w-full object-cover transition duration-200 group-hover:scale-[1.03]" />
            <span className="block p-3 font-semibold">{item.title}</span>
          </button>
        ))}
      </div>
      {active && <button className="fixed inset-0 z-50 flex items-center justify-center bg-ink/80 p-6" aria-label="Close photograph" onClick={() => setActive(null)} type="button"><img src={active} alt="" className="max-h-full max-w-full" /></button>}
    </main>
    </>
  )
}

export function ClientsPage() {
  const { data } = useQuery({ queryKey: ['clients'], queryFn: async () => (await api.get('/api/public/clients')).data })
  const clients = (data ?? []) as { id: string; name: string; logoPath?: string }[]
  useSeo('Clients | Narayan Staffing Services', 'Companies staffed by Narayan Staffing Services.')
  return (
    <>
      <PageBand kicker="Clients" title="Companies we staff" lede="Hover a company to pause the strip." />
      <div className="border-b border-line bg-paper pb-8">
        <ClientMarquee clients={clients} />
      </div>
      <div className="container-page mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {clients.map((client) => (
          <article key={client.id} className="border border-ink bg-card">
            {client.logoPath && <img src={client.logoPath} alt="" className="aspect-[16/10] w-full object-cover" />}
            <p className="px-4 py-4 text-center font-display text-2xl uppercase">{client.name}</p>
          </article>
        ))}
      </div>
    </>
  )
}

export function ContactPage() {
  const settings = useQuery({ queryKey: ['layout'], queryFn: async () => (await api.get('/api/public/layout')).data })
  const intro = useQuery({ queryKey: ['contact-intro'], queryFn: async () => (await api.get('/api/public/pages/CONTACT/INTRO')).data })
  const form = useForm({ resolver: zodResolver(contactSchema), defaultValues: { name: '', email: '', phone: '', company: '', subject: '', message: '' } })
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  useSeo('Contact | Narayan Staffing Services', intro.data?.content || 'Contact Narayan Staffing Services in Chakan, Pune.')
  const site = settings.data?.site ?? {}
  return (
    <>
    <PageBand kicker="Contact" title={intro.data?.title || 'Contact'} lede={intro.data?.content} />
    <main className="container-page grid gap-10 py-12 lg:grid-cols-[1fr_0.8fr]">
      <div>
        {done ? <p className="border border-pine bg-card p-4 text-pine">Message received. The desk will reply during working hours.</p> : (
          <form className="grid gap-4" onSubmit={form.handleSubmit(async (values) => {
            setError('')
            try {
              await api.post('/api/public/contact', values)
              setDone(true)
            } catch (err) {
              setError(errorMessage(err))
            }
          })}>
            <Field label="Name" required error={form.formState.errors.name?.message}><TextInput {...form.register('name')} /></Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Email" required error={form.formState.errors.email?.message}><TextInput {...form.register('email')} /></Field>
              <Field label="Phone"><TextInput {...form.register('phone')} /></Field>
            </div>
            <Field label="Company"><TextInput {...form.register('company')} /></Field>
            <Field label="Subject" required error={form.formState.errors.subject?.message}><TextInput {...form.register('subject')} /></Field>
            <Field label="Message" required error={form.formState.errors.message?.message}><TextArea {...form.register('message')} /></Field>
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? 'Sending…' : 'Send message'}</Button>
          </form>
        )}
      </div>
      <aside className="h-fit border border-line bg-paper p-6 text-ink">
        <p className="text-sm text-ink-soft">{site.address}</p>
        <p className="mt-4 font-semibold">{site.phone}</p>
        <p>{site.email}</p>
        <p className="mt-4 text-sm text-ink-soft">{site.hours}</p>
        {site.maps_url && <a className="mt-4 inline-flex text-sm font-semibold text-ink underline" href={site.maps_url} target="_blank" rel="noreferrer">Open map</a>}
      </aside>
    </main>
    </>
  )
}

export function LegalPage({ page }: { page: 'PRIVACY' | 'TERMS' }) {
  const { data } = useQuery({ queryKey: ['legal', page], queryFn: async () => (await api.get(`/api/public/pages/${page}/BODY`)).data })
  useSeo(`${data?.title || 'Policy'} | Narayan Staffing Services`, data?.content?.slice(0, 160))
  return (
    <>
      <PageBand title={data?.title || 'Policy'} />
      <main className="container-page max-w-3xl py-12">
        <p className="whitespace-pre-line text-pretty text-ink-soft">{data?.content}</p>
      </main>
    </>
  )
}

export function CategoriesPage() {
  const { data, isLoading } = useQuery({ queryKey: ['public-categories'], queryFn: async () => (await api.get('/api/public/categories')).data })
  useSeo('Job categories | Narayan Staffing Services', 'Roles grouped so candidates and clients can find the right desk.')
  return (
    <>
      <PageBand kicker="Careers" title="Categories" lede="Roles are grouped so candidates and clients can find the right desk." />
      <main className="container-page py-12">
      {isLoading && <div className="skeleton h-40 rounded-2xl" />}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((category: { id: string; name: string; slug: string; description?: string }) => (
          <Link key={category.id} to={`/careers?category=${category.slug}`} className={card}>
            <h2 className="text-2xl">{category.name}</h2>
            <p className="mt-2 text-pretty text-sm text-ink-soft">{category.description}</p>
          </Link>
        ))}
      </div>
      </main>
    </>
  )
}
