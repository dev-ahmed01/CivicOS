export function label(value?: string | null) {
  if (!value) return 'Not recorded'
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export function formatDate(value?: string | null) {
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function tone(value?: string | null): 'info' | 'success' | 'warning' | 'danger' {
  if (value === 'PASSED' || value === 'ACCEPTED' || value === 'COMPLETED') return 'success'
  if (value === 'FAILED' || value === 'REJECTED' || value === 'CANCELLED') return 'danger'
  if (value === 'CONDITIONAL' || value === 'IN_PROGRESS' || value === 'UNDER_REVIEW') return 'warning'
  return 'info'
}
