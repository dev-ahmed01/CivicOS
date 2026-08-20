import type { Sla } from './types'

export function label(value: string) {
  return value.toLowerCase().split('_').map((part) => part[0]?.toUpperCase() + part.slice(1)).join(' ')
}

export function formatDate(value?: string | null) {
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function statusTone(status: string): 'info' | 'success' | 'warning' | 'danger' {
  if (['VERIFIED', 'CLOSED', 'APPROVED', 'ACCEPTED', 'COMPLETED', 'SATISFIED'].includes(status)) return 'success'
  if (['REJECTED', 'CANCELLED', 'BREACHED', 'DISPUTED'].includes(status)) return 'danger'
  if (['AT_RISK', 'ON_HOLD', 'RETURNED', 'BLOCKED', 'COORDINATION_REQUIRED'].includes(status)) return 'warning'
  return 'info'
}

export function slaLabel(sla?: Sla) {
  if (!sla) return 'No active SLA'
  if (sla.status === 'PAUSED') return 'Paused'
  if (sla.status === 'COMPLETED') return 'Completed'
  const milliseconds = new Date(sla.deadline).getTime() - Date.now()
  if (milliseconds <= 0) return 'Breached'
  const hours = Math.floor(milliseconds / 3_600_000)
  const minutes = Math.floor((milliseconds % 3_600_000) / 60_000)
  return `${hours}h ${minutes}m remaining`
}
