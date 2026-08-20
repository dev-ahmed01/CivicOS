import { apiMultipartRequest, apiRequest } from '../../lib/api/client'
import type {
  Approval,
  AuditEvent,
  CivicCase,
  Conflict,
  Dependency,
  Evidence,
  Intervention,
  PagedResponse,
  Sla,
} from './types'

type InterventionFilters = {
  statuses?: string[]
  priority?: string
  page?: number
  size?: number
}

export function listInterventions(accessToken: string, filters: InterventionFilters = {}) {
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 20),
    sort: 'plannedStart,asc',
  })
  filters.statuses?.forEach((status) => params.append('status', status))
  if (filters.priority) params.set('priority', filters.priority)
  return apiRequest<PagedResponse<Intervention>>(`/api/v1/interventions?${params}`, { accessToken })
}

export function getIntervention(accessToken: string, interventionId: string) {
  return apiRequest<Intervention>(`/api/v1/interventions/${interventionId}`, { accessToken })
}

export function transitionIntervention(
  accessToken: string,
  intervention: Intervention,
  action: string,
  reason: string,
) {
  return apiRequest(`/api/v1/interventions/${intervention.id}/actions/${action}`, {
    method: 'POST',
    accessToken,
    idempotencyKey: crypto.randomUUID(),
    body: { expectedVersion: intervention.version, reason },
  })
}

export function listApprovals(accessToken: string, status?: string, size = 20) {
  const params = new URLSearchParams({ page: '0', size: String(size), sort: 'createdAt,desc' })
  if (status) params.set('status', status)
  return apiRequest<PagedResponse<Approval>>(`/api/v1/approval-requests?${params}`, { accessToken })
}

export function listInterventionApprovals(accessToken: string, interventionId: string) {
  return apiRequest<Approval[]>(`/api/v1/interventions/${interventionId}/approval-requests`, { accessToken })
}

export function requestApproval(accessToken: string, interventionId: string, reason: string) {
  return apiRequest(`/api/v1/interventions/${interventionId}/approval-requests`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(), body: { reason },
  })
}

export function decideApproval(
  accessToken: string,
  approval: Approval,
  decision: string,
  reason: string,
  conditions: Array<Record<string, unknown>>,
) {
  return apiRequest(`/api/v1/approval-requests/${approval.id}/decision`, {
    method: 'POST',
    accessToken,
    idempotencyKey: crypto.randomUUID(),
    body: { decision, reason, conditions, expectedVersion: approval.version },
  })
}

export function listEvidence(accessToken: string, targetIds?: string[], status?: string, size = 100) {
  const params = new URLSearchParams({ targetType: 'INTERVENTION', page: '0', size: String(size), sort: 'createdAt,desc' })
  targetIds?.forEach((targetId) => params.append('targetId', targetId))
  if (status) params.set('status', status)
  return apiRequest<PagedResponse<Evidence>>(`/api/v1/evidence?${params}`, { accessToken })
}

export function uploadInterventionEvidence(
  accessToken: string,
  interventionId: string,
  type: string,
  file: File,
) {
  const formData = new FormData()
  formData.append('metadata', new Blob([JSON.stringify({
    targetType: 'INTERVENTION', targetId: interventionId, type,
    capturedAt: new Date().toISOString(), metadata: { source: 'AGENCY_WORKSPACE' },
  })], { type: 'application/json' }))
  formData.append('file', file)
  return apiMultipartRequest('/api/v1/evidence', formData, accessToken, crypto.randomUUID())
}

export function submitEvidence(accessToken: string, evidence: Evidence, reason: string) {
  return apiRequest(`/api/v1/evidence/${evidence.id}/submit`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
    body: { expectedVersion: evidence.version, reason },
  })
}

export function listConflicts(accessToken: string, status?: string, roadSegmentId?: string, size = 20) {
  const params = new URLSearchParams({ page: '0', size: String(size), sort: 'detectedAt,desc' })
  if (status) params.set('status', status)
  if (roadSegmentId) params.set('roadSegmentId', roadSegmentId)
  return apiRequest<PagedResponse<Conflict>>(`/api/v1/conflicts?${params}`, { accessToken })
}

export function listSlas(accessToken: string, targetIds?: string[], statuses?: string[], size = 100) {
  const params = new URLSearchParams({ targetType: 'INTERVENTION', page: '0', size: String(size), sort: 'deadline,asc' })
  targetIds?.forEach((targetId) => params.append('targetId', targetId))
  statuses?.forEach((status) => params.append('status', status))
  return apiRequest<PagedResponse<Sla>>(`/api/v1/slas?${params}`, { accessToken })
}

export function getCase(accessToken: string, caseId: string) {
  return apiRequest<CivicCase>(`/api/v1/cases/${caseId}`, { accessToken })
}

export function listDependencies(accessToken: string, interventionId: string) {
  return apiRequest<Dependency[]>(`/api/v1/interventions/${interventionId}/dependencies`, { accessToken })
}

export function listAudit(accessToken: string, interventionId: string) {
  return apiRequest<PagedResponse<AuditEvent>>(
    `/api/v1/audit-events/entities/INTERVENTION/${interventionId}?page=0&size=100&sort=occurredAt,asc`,
    { accessToken },
  )
}
