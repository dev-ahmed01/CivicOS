export type RoadCandidate = {
  roadSegmentId: string
  externalReference: string
  name: string
  classification: string
  surfaceType: string
}

export type CitizenObservation = {
  observationId: string
  caseId: string
  trackingId: string
  category: string
  description: string
  latitude: number
  longitude: number
  detectedRoad: RoadCandidate
  submittedAt: string
  status: string
  caseStatus: string
  publicStatus: PublicReportStatus
  publicMessage: string
  suggestedCategory?: string | null
}

export type PublicReportStatus =
  | 'REPORT_RECEIVED'
  | 'UNDER_REVIEW'
  | 'MATCHED_TO_WORK'
  | 'ACTION_ASSIGNED'
  | 'WORK_IN_PROGRESS'
  | 'VERIFICATION'
  | 'COMPLETED'
  | 'CLOSED'

export type PagedResponse<T> = {
  data: T[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
  meta: { requestId?: string }
}

export type NotificationItem = {
  notificationId: string
  type: string
  title: string
  message: string
  targetType: string
  targetId: string
  readAt?: string | null
  createdAt: string
}
