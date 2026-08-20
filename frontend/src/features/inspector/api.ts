import { apiMultipartRequest, apiRequest } from '../../lib/api/client'
import type { Evidence, Inspection, PagedResponse } from './types'

export function listInspections(accessToken: string) {
  return apiRequest<PagedResponse<Inspection>>('/api/v1/inspections?page=0&size=100&sort=createdAt,desc', { accessToken })
}

export function getInspection(accessToken: string, inspectionId: string) {
  return apiRequest<Inspection>(`/api/v1/inspections/${inspectionId}`, { accessToken })
}

export function startInspection(accessToken: string, inspection: Inspection) {
  return apiRequest(`/api/v1/inspections/${inspection.id}/start`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
    body: { expectedVersion: inspection.version },
  })
}

export function completeInspection(accessToken: string, inspection: Inspection, result: string, notes: string) {
  return apiRequest(`/api/v1/inspections/${inspection.id}/complete`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
    body: { result, notes: notes || null, expectedVersion: inspection.version },
  })
}

export function listEvidence(accessToken: string, targetIds?: string[]) {
  const params = new URLSearchParams({ targetType: 'INTERVENTION', page: '0', size: '100', sort: 'createdAt,desc' })
  targetIds?.forEach((targetId) => params.append('targetId', targetId))
  return apiRequest<PagedResponse<Evidence>>(`/api/v1/evidence?${params}`, { accessToken })
}

export function uploadInspectionEvidence(
  accessToken: string,
  interventionId: string,
  file: File,
  coordinates?: { latitude: number; longitude: number },
) {
  const formData = new FormData()
  formData.append('metadata', new Blob([JSON.stringify({
    targetType: 'INTERVENTION', targetId: interventionId, type: 'INSPECTION',
    capturedAt: new Date().toISOString(),
    latitude: coordinates?.latitude, longitude: coordinates?.longitude,
    metadata: { captureContext: 'INSPECTOR_FIELD_WORKSPACE' },
  })], { type: 'application/json' }))
  formData.append('file', file)
  return apiMultipartRequest('/api/v1/evidence', formData, accessToken, crypto.randomUUID())
}

export function recordVerification(accessToken: string, inspection: Inspection, result: string, reason: string) {
  return apiRequest(`/api/v1/inspections/${inspection.id}/verification`, {
    method: 'POST', accessToken, idempotencyKey: crypto.randomUUID(),
    body: {
      interventionId: inspection.interventionId,
      result,
      reason: reason || null,
      expectedInterventionVersion: inspection.interventionVersion,
    },
  })
}
