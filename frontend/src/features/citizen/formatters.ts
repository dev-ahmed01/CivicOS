export function categoryLabel(category: string) {
  return category.toLowerCase().split('_').map((part) => part[0]?.toUpperCase() + part.slice(1)).join(' ')
}

export function statusLabel(status: string) {
  return status.toLowerCase().split('_').map((part) => part[0]?.toUpperCase() + part.slice(1)).join(' ')
}

export function statusTone(status: string): 'info' | 'success' | 'warning' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CLOSED') return 'warning'
  return 'info'
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
