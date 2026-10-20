export function inr(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return '—'
  const amount = typeof value === 'string' ? Number(value) : value
  if (Number.isNaN(amount)) return '—'
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

export function dateLabel(value?: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
}

export function titleCase(value?: string | null) {
  if (!value) return '—'
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (char) => char.toUpperCase())
}
