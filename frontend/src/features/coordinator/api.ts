import { apiRequest } from '../../lib/api/client'
import type {
  AiGenerationResult,
  AiRecommendation,
  Approval,
  Conflict,
  CoordinationDecision,
  Intervention,
  PagedResponse,
  Sla,
} from './types'

export function listConflicts(
  accessToken: string,
  filters: { status?: string; severity?: string; roadSegmentId?: string; page?: number; size?: number } = {},
) {
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 20),
    sort: 'detectedAt,desc',
  })
  if (filters.status) params.set('status', filters.status)
  if (filters.severity) params.set('severity', filters.severity)
  if (filters.roadSegmentId) params.set('roadSegmentId', filters.roadSegmentId)
  return apiRequest<PagedResponse<Conflict>>(`/api/v1/conflicts?${params}`, { accessToken })
}

export function getConflict(accessToken: string, conflictId: string) {
  return apiRequest<Conflict>(`/api/v1/conflicts/${conflictId}`, { accessToken })
}

export function listInterventions(
  accessToken: string,
  filters: { statuses?: string[]; roadSegmentId?: string; plannedFrom?: string; page?: number; size?: number } = {},
) {
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 50),
    sort: 'plannedStart,asc',
  })
  filters.statuses?.forEach((status) => params.append('status', status))
  if (filters.roadSegmentId) params.set('roadSegmentId', filters.roadSegmentId)
  if (filters.plannedFrom) params.set('plannedFrom', filters.plannedFrom)
  return apiRequest<PagedResponse<Intervention>>(`/api/v1/interventions?${params}`, { accessToken })
}

export function getIntervention(accessToken: string, interventionId: string) {
  return apiRequest<Intervention>(`/api/v1/interventions/${interventionId}`, { accessToken })
}

export function listApprovals(accessToken: string, status = 'PENDING', size = 20) {
  const params = new URLSearchParams({ status, page: '0', size: String(size), sort: 'createdAt,desc' })
  return apiRequest<PagedResponse<Approval>>(`/api/v1/approval-requests?${params}`, { accessToken })
}

export function listSlas(accessToken: string, statuses: string[], size = 100) {
  const params = new URLSearchParams({ page: '0', size: String(size), sort: 'deadline,asc' })
  statuses.forEach((status) => params.append('status', status))
  return apiRequest<PagedResponse<Sla>>(`/api/v1/slas?${params}`, { accessToken })
}

export function listRecommendations(accessToken: string, conflictId: string) {
  return apiRequest<AiRecommendation[]>(`/api/v1/conflicts/${conflictId}/recommendations`, { accessToken })
}

export function generateRecommendation(
  accessToken: string,
  conflict: Conflict,
  interventions: Intervention[],
) {
  return apiRequest<AiGenerationResult>('/api/v1/ai/coordination-recommendations', {
    method: 'POST',
    accessToken,
    idempotencyKey: crypto.randomUUID(),
    body: {
      entityType: 'CONFLICT',
      entityId: conflict.id,
      context: {
        affectedInterventions: interventions.map(({ id }) => id),
        affectedAgencies: [...new Set(interventions.map(({ agencyId }) => agencyId))],
      },
    },
  })
}

export function reviewRecommendation(
  accessToken: string,
  recommendationId: string,
  decision: 'ACCEPT' | 'REJECT',
  reason: string,
) {
  return apiRequest<AiRecommendation>(`/api/v1/ai/recommendations/${recommendationId}/review`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(), body: { decision, reason },
  })
}

export function listCoordinationDecisions(accessToken: string, conflictId: string) {
  return apiRequest<CoordinationDecision[]>(
    `/api/v1/conflicts/${conflictId}/coordination-decisions`, { accessToken },
  )
}

export function recordCoordinationDecision(
  accessToken: string,
  conflictId: string,
  decisionType: string,
  decisionText: string,
  acceptedRecommendationId?: string,
) {
  return apiRequest<CoordinationDecision>(`/api/v1/conflicts/${conflictId}/coordination-decisions`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
    body: { decisionType, decisionText, acceptedRecommendationId },
  })
}

export function resolveConflict(
  accessToken: string,
  conflict: Conflict,
  outcome: 'RESOLVED' | 'DISMISSED',
  reason: string,
) {
  return apiRequest<{ conflictId: string; status: string; version: number; resolvedAt: string }>(
    `/api/v1/conflicts/${conflict.id}/resolve`, {
      method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
      body: { outcome, expectedVersion: conflict.version, reason },
    },
  )
}
