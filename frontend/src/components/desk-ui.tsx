import { useEffect, useRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'
import { clsx } from 'clsx'

export function Button({ className, variant = 'primary', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger' }) {
  const styles = {
    primary: 'bg-pine text-white hover:bg-saffron hover:text-ink',
    secondary: 'bg-saffron text-ink hover:bg-pine hover:text-white',
    ghost: 'border border-pine bg-transparent text-pine hover:bg-pine hover:text-white',
    danger: 'bg-danger text-white hover:bg-ink',
  }[variant]
  return <button className={clsx('inline-flex h-11 items-center justify-center gap-2 rounded-none px-5 font-display text-lg uppercase disabled:opacity-60', styles, className)} {...props} />
}

export function Field({ label, error, children, required }: { label: string; error?: string; children: ReactNode; required?: boolean }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-semibold text-ink">{label}{required && <span className="text-pine"> *</span>}</span>
      {children}
      {error && <span className="block text-sm text-danger">{error}</span>}
    </label>
  )
}

const control = 'h-11 w-full rounded-none border border-line bg-card px-3 text-sm text-ink outline-none focus:border-pine focus:bg-paper'

export function TextInput({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={clsx(control, className)} {...props} />
}

export function TextArea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={clsx('min-h-28 w-full rounded-none border border-line bg-card px-3 py-2 text-sm outline-none focus:border-pine focus:bg-paper', className)} {...props} />
}

export function SelectInput({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={clsx(control, className)} {...props} />
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'good' | 'warn' | 'bad' | 'info' }) {
  const tones = {
    neutral: 'bg-paper-2 text-ink-soft',
    good: 'bg-pine/10 text-pine',
    warn: 'bg-warning/10 text-warning',
    bad: 'bg-danger/10 text-danger',
    info: 'bg-navy/10 text-navy',
    saffron: 'bg-saffron/25 text-saffron-deep',
  }[tone]
  return <span className={clsx('inline-flex items-center px-2 py-0.5 font-display text-sm uppercase', tones)}>{children}</span>
}

export function statusTone(status?: string) {
  if (!status) return 'neutral' as const
  if (['PUBLISHED', 'ACTIVE', 'JOINED', 'SELECTED', 'APPROVED', 'LOCKED', 'RESOLVED'].includes(status)) return 'good' as const
  if (['DRAFT', 'ON_HOLD', 'PROSPECT', 'PROCESSING', 'IN_PROGRESS', 'OFFERED'].includes(status)) return 'warn' as const
  if (['REJECTED', 'CLOSED', 'TERMINATED', 'CANCELLED', 'ARCHIVED', 'INACTIVE'].includes(status)) return 'bad' as const
  return 'info' as const
}

export function EmptyState({ title, body, action }: { title: string; body: string; action?: ReactNode }) {
  return (
    <div className="border border-dashed border-line bg-card px-6 py-14 text-center">
      <h3 className="text-balance text-xl">{title}</h3>
      <p className="mx-auto mt-2 max-w-md text-pretty text-ink-soft">{body}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function PageHeader({ title, lede, actions }: { title: string; lede?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-balance text-3xl text-ink">{title}</h1>
        {lede && <p className="mt-1 max-w-2xl text-pretty text-ink-soft">{lede}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function PageBand({ kicker, title, lede }: { kicker?: string; title: string; lede?: string }) {
  return (
    <section className="border-b border-line bg-paper text-ink">
      <div className="container-page py-10">
        {kicker && <p className="font-display text-xl text-pine">{kicker}</p>}
        <h1 className="mt-1 max-w-4xl text-5xl text-ink sm:text-7xl">{title}</h1>
        {lede && <p className="mt-4 max-w-2xl text-pretty text-lg text-ink-soft">{lede}</p>}
      </div>
    </section>
  )
}

export function Modal({ open, title, children, onClose }: { open: boolean; title: string; children: ReactNode; onClose: () => void }) {
  const panel = useRef<HTMLDivElement>(null)
  const previous = useRef<HTMLElement | null>(null)
  useEffect(() => {
    if (!open) return
    previous.current = document.activeElement as HTMLElement | null
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
      if (event.key !== 'Tab' || !panel.current) return
      const items = panel.current.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
      if (items.length === 0) return
      const first = items[0]
      const last = items[items.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    panel.current?.querySelector<HTMLElement>('button')?.focus()
    return () => {
      document.removeEventListener('keydown', onKey)
      previous.current?.focus()
    }
  }, [open, onClose])
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink/50 p-4 sm:items-center" role="presentation">
      <div ref={panel} className="w-full max-w-lg border border-line bg-card p-5 shadow-xl" role="dialog" aria-modal="true" aria-label={title}>
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-balance text-2xl">{title}</h2>
          <button className="text-sm text-ink-soft" onClick={onClose} type="button">Close</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function TableShell({ children }: { children: ReactNode }) {
  return <div className="overflow-x-auto border border-line bg-card">{children}</div>
}
