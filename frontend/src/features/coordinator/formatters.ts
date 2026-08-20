export function label(value: string) {
  return value.toLowerCase().split('_').map((part) => part[0]?.toUpperCase() + part.slice(1)).join(' ')
}

export function formatDate(value?: string | null) {
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function statusTone(status: string): 'info' | 'success' | 'warning' | 'danger' {
  if (['RESOLVED', 'APPROVED', 'ACCEPTED', 'COMPLETED', 'VERIFIED'].includes(status)) return 'success'
  if (['DISMISSED', 'REJECTED', 'BREACHED', 'CANCELLED'].includes(status)) return 'danger'
  if (['HIGH', 'AT_RISK', 'UNDER_REVIEW', 'COORDINATION_REQUIRED'].includes(status)) return 'warning'
  return 'info'
}

export function stringValue(value: unknown, fallback = 'Not supplied by the advisory response.') {
  return typeof value === 'string' && value.trim() ? value : fallback
}

export function stringList(value: unknown) {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    : []
}
