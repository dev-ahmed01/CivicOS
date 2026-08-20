export type CurrentUser = { id: string; agencyId?: string | null; fullName: string; email: string; roles: string[]; permissions: string[] }
export type Page<T> = { data: T[]; pagination: { page: number; size: number; totalElements: number; totalPages: number }; meta: { requestId?: string } }
export type User = { id: string; agencyId?: string | null; agencyName?: string | null; fullName: string; email: string; phone?: string | null; status: string; roles: string[]; createdAt: string; updatedAt: string; lastLoginAt?: string | null }
export type Agency = { id: string; code: string; name: string; type: string; jurisdiction?: string | null; contactEmail?: string | null; contactPhone?: string | null; active: boolean; createdAt: string; updatedAt: string }
export type Role = { id: string; code: string; name: string; description?: string | null; systemRole: boolean; permissions: string[] }
export type Permission = { id: string; code: string; description?: string | null }
export type AuditEvent = { eventId: string; actorId?: string | null; action: string; entityType: string; entityId: string; occurredAt: string; beforeState?: Record<string, unknown> | null; afterState?: Record<string, unknown> | null; reason?: string | null }
export type Configuration = { configurationSource: string; runtimeMutationSupported: boolean; ai: Record<string, unknown>; conflict: Record<string, unknown>; sla: Record<string, unknown>; verification: Record<string, unknown> }
