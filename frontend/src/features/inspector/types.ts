export type CurrentUser = {
  id: string
  agencyId?: string | null
  fullName: string
  email: string
  roles: string[]
  permissions: string[]
}

export type PagedResponse<T> = {
  data: T[]
  pagination: { page: number; size: number; totalElements: number; totalPages: number }
  meta: { requestId?: string }
}

export type Inspection = {
  id: string
  interventionId: string
  interventionNumber: string
  interventionDescription: string
  interventionType: string
  interventionStatus: string
  interventionVersion: number
  agencyId: string
  agencyName: string
  roadSegmentId: string
  roadSegmentName: string
  geometryWkt: string
  plannedStart: string
  plannedEnd: string
  inspectorId: string
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  result?: 'PASSED' | 'FAILED' | 'CONDITIONAL' | null
  startedAt?: string | null
  completedAt?: string | null
  notes?: string | null
  version: number
  createdAt: string
}

export type Evidence = {
  id: string
  targetType: string
  targetId: string
  uploadedBy: string
  type: string
  originalFilename?: string | null
  mimeType?: string | null
  fileSizeBytes?: number | null
  checksum?: string | null
  capturedAt?: string | null
  latitude?: number | null
  longitude?: number | null
  metadata: Record<string, unknown>
  status: string
  version: number
  createdAt: string
}
