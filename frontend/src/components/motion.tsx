import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'

export function Reveal({ children, className = '' }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const [state, setState] = useState<'shown' | 'wait'>('shown')
  useLayoutEffect(() => {
    const node = ref.current
    if (!node) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const observer = new IntersectionObserver(([entry]) => {
      setState(entry.isIntersecting ? 'shown' : 'wait')
    }, { threshold: 0.12, rootMargin: '0px 0px -32px 0px' })
    observer.observe(node)
    return () => observer.disconnect()
  }, [])
  return <div ref={ref} className={`reveal ${state === 'wait' ? 'reveal-wait' : 'reveal-in'} ${className}`}>{children}</div>
}

const heroSlides = [
  { src: '/media/hero-workforce.jpg', alt: 'Skilled associates on a manufacturing floor' },
  { src: '/media/team-desk.jpg', alt: 'The staffing desk reviewing a requirement' },
  { src: '/media/site-work.jpg', alt: 'A project site supplied by the company' },
]

export function HeroSlide() {
  const ref = useRef<HTMLDivElement>(null)
  const [index, setIndex] = useState(0)
  const [running, setRunning] = useState(false)
  const [motionOk, setMotionOk] = useState(true)

  useEffect(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    setMotionOk(!reduce)
    const node = ref.current
    if (!node || reduce) return
    const observer = new IntersectionObserver(([entry]) => setRunning(entry.isIntersecting), { threshold: 0.25 })
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    if (!running) return
    const timer = window.setInterval(() => setIndex((value) => (value + 1) % heroSlides.length), 4500)
    return () => window.clearInterval(timer)
  }, [running])

  return (
    <div ref={ref} className="relative min-h-80 overflow-hidden lg:min-h-[32rem]" aria-roledescription="carousel" aria-label="Site photographs">
      {heroSlides.map((slide, slideIndex) => (
        <img
          key={slide.src}
          src={slide.src}
          alt={slide.alt}
          aria-hidden={slideIndex === index ? undefined : true}
          className={`absolute inset-0 h-full w-full object-cover ${motionOk ? 'transition-transform duration-200 ease-out' : ''}`}
          style={{ transform: `translateX(${(slideIndex - index) * 100}%)` }}
        />
      ))}
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(100deg,#ffffff_0%,rgba(255,255,255,0.2)_34%,rgba(92,181,115,0.28)_100%)]" />
      <div className="absolute bottom-4 left-4 z-10 flex gap-2">
        {heroSlides.map((slide, slideIndex) => (
          <button
            key={slide.src}
            type="button"
            aria-label={`Show photo ${slideIndex + 1} of ${heroSlides.length}`}
            aria-current={slideIndex === index ? 'true' : undefined}
            className={`size-3 border border-ink ${slideIndex === index ? 'bg-saffron' : 'bg-paper'}`}
            onClick={() => setIndex(slideIndex)}
          />
        ))}
      </div>
    </div>
  )
}

type ClientChip = { id: string; name: string; logoPath?: string }

export function ClientMarquee({ clients, tone = 'light', compact = false }: { clients: ClientChip[]; tone?: 'dark' | 'light'; compact?: boolean }) {
  const ref = useRef<HTMLDivElement>(null)
  const [running, setRunning] = useState(true)
  useEffect(() => {
    const node = ref.current
    if (!node) return
    const observer = new IntersectionObserver(([entry]) => setRunning(entry.isIntersecting), { threshold: 0 })
    observer.observe(node)
    return () => observer.disconnect()
  }, [])
  if (clients.length === 0) return null
  const loop = [...clients, ...clients]
  const chip = tone === 'dark'
    ? 'text-paper hover:text-saffron'
    : 'text-ink hover:bg-saffron'
  return (
    <div ref={ref} className={`marquee ${running ? '' : 'marquee-paused'}`} aria-label="Clients">
      <div className="marquee-track py-3">
        {loop.map((client, index) => (
          <div key={`${client.id}-${index}`} aria-hidden={index >= clients.length ? true : undefined} className={`flex items-center gap-3 border border-line bg-card px-3 font-display uppercase ${compact ? 'h-10 text-lg' : 'h-20 text-2xl'} ${chip}`}>
            {client.logoPath && <img src={client.logoPath} alt="" className={`${compact ? 'h-8 w-12' : 'h-14 w-24'} object-cover`} />}
            <span>{client.name}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
