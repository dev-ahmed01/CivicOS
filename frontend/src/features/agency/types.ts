export type CurrentUser = {
  id: string
  agencyId: string
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

export type Intervention = {
  id: string
  interventionNumber: string
  caseId: string
  caseNumber: string
  agencyId: string
  agencyName: string
  type: string
  description: string
  roadSegmentId: string
  roadSegmentName: string
  geometryWkt: string
  plannedStart: string
  plannedEnd: string
  actualStart?: string | null
  actualEnd?: string | null
  status: string
  heldFromStatus?: string | null
  priority: string
  createdBy: string
  version: number
  createdAt: string
  updatedAt: string
}

export type Approval = {
  id: string
  interventionId: string
  interventionNumber: string
  agencyId: string
  status: string
  decision?: string | null
  actorId?: string | null
  reason?: string | null
  conditions: Array<Record<string, unknown>>
  decidedAt?: string | null
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

export type Conflict = {
  id: string
  conflictNumber: string
  roadSegmentId: string
  type: string
  severity: string
  status: string
  detectedAt: string
  resolvedAt?: string | null
  explanation: string
  interventionIds: string[]
  version: number
}

export type Sla = {
  id: string
  targetType: string
  targetId: string
  slaType: string
  startAt: string
  deadline: string
  status: string
  pausedAt?: string | null
  completedAt?: string | null
  version: number
}

export type Dependency = {
  id: string
  sourceInterventionId: string
  targetInterventionId: string
  type: string
  required: boolean
  status: string
  reason: string
  version: number
  createdAt: string
}

export type AuditEvent = {
  eventId: string
  actorId?: string | null
  action: string
  entityType: string
  entityId: string
  occurredAt: string
  beforeState?: Record<string, unknown> | null
  afterState?: Record<string, unknown> | null
  reason?: string | null
  requestId?: string | null
}

export type CivicCase = {
  id: string
  caseNumber: string
  source: string
  status: string
  priority: string
  roadSegmentId: string
  observationIds: string[]
  interventionIds: string[]
  closedAt?: string | null
  version: number
  createdAt: string
  updatedAt: string
}
