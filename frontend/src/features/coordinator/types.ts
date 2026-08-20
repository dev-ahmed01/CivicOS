export type CurrentUser = {
  id: string
  agencyId: string | null
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
  priority: string
  version: number
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

export type Approval = {
  id: string
  interventionId: string
  interventionNumber: string
  status: string
  createdAt: string
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

export type AiRecommendation = {
  recommendationId: string
  runId: string
  conflictId: string
  recommendation: Record<string, unknown>
  confidence?: number | null
  status: string
  reviewedBy?: string | null
  reviewedAt?: string | null
  reviewReason?: string | null
}

export type AiGenerationResult = {
  execution: {
    runId?: string | null
    status: string
    humanReviewRequired: boolean
    errorMessage?: string | null
  }
  recommendation?: AiRecommendation | null
}

export type CoordinationDecision = {
  decisionId: string
  conflictId: string
  coordinatorId: string
  decisionType: string
  decisionText: string
  acceptedRecommendationId?: string | null
  createdAt: string
}
