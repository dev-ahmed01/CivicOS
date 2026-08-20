import { apiMultipartRequest, apiRequest } from '../../lib/api/client'
import type { CitizenObservation, NotificationItem, PagedResponse, RoadCandidate } from './types'

export type CreateObservationInput = {
  category: string
  description: string
  latitude: number
  longitude: number
  roadSegmentId: string
}

export type CitizenValidationInput = {
  decision: 'LOOKS_RESOLVED' | 'STILL_UNRESOLVED'
  reason?: string
}

export function findRoadCandidates(
  accessToken: string,
  latitude: number,
  longitude: number,
) {
  const params = new URLSearchParams({
    latitude: latitude.toString(),
    longitude: longitude.toString(),
  })
  return apiRequest<RoadCandidate[]>(`/api/v1/observations/road-candidates?${params}`, { accessToken })
}

export function createObservation(accessToken: string, input: CreateObservationInput) {
  return apiRequest<CitizenObservation>('/api/v1/observations', {
    method: 'POST',
    accessToken,
    idempotencyKey: crypto.randomUUID(),
    body: input,
  })
}

export function uploadObservationEvidence(
  accessToken: string,
  observation: CitizenObservation,
  file: File,
) {
  const formData = new FormData()
  formData.append('metadata', new Blob([JSON.stringify({
    type: 'DOCUMENT',
    capturedAt: new Date().toISOString(),
    latitude: observation.latitude,
    longitude: observation.longitude,
    metadata: { source: 'CITIZEN_REPORT' },
  })], { type: 'application/json' }))
  formData.append('file', file)
  return apiMultipartRequest(
    `/api/v1/observations/${observation.observationId}/evidence`,
    formData,
    accessToken,
    crypto.randomUUID(),
  )
}

export function listMyObservations(accessToken: string) {
  return apiRequest<PagedResponse<CitizenObservation>>(
    '/api/v1/me/observations?page=0&size=20&sort=submittedAt,desc',
    { accessToken },
  )
}

export function getObservation(accessToken: string, observationId: string) {
  return apiRequest<CitizenObservation>(`/api/v1/observations/${observationId}`, { accessToken })
}

export function validateObservation(
  accessToken: string,
  observationId: string,
  input: CitizenValidationInput,
) {
  return apiRequest(`/api/v1/observations/${observationId}/validation`, {
    method: 'POST',
    accessToken,
    idempotencyKey: crypto.randomUUID(),
    body: input,
  })
}

export function listNotifications(accessToken: string) {
  return apiRequest<PagedResponse<NotificationItem>>(
    '/api/v1/notifications?page=0&size=20&sort=createdAt,desc',
    { accessToken },
  )
}
