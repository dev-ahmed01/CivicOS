import { apiRequest } from '../../lib/api/client'
import type { Agency, AuditEvent, Configuration, Page, Permission, Role, User } from './types'
export const listUsers = (token: string) => apiRequest<Page<User>>('/api/v1/users?page=0&size=100&sort=createdAt,desc', { accessToken: token })
export const listAgencies = (token: string) => apiRequest<Page<Agency>>('/api/v1/admin/agencies?page=0&size=100&sort=name,asc', { accessToken: token })
export const listRoles = (token: string) => apiRequest<Role[]>('/api/v1/admin/roles', { accessToken: token })
export const listPermissions = (token: string) => apiRequest<Permission[]>('/api/v1/admin/permissions', { accessToken: token })
export const getConfiguration = (token: string) => apiRequest<Configuration>('/api/v1/admin/configuration', { accessToken: token })
export const listAudit = (token: string) => apiRequest<Page<AuditEvent>>('/api/v1/audit-events?page=0&size=100&sort=occurredAt,desc', { accessToken: token })
export function inviteUser(token: string, body: Record<string, unknown>) { return apiRequest<User>('/api/v1/admin/users', { method: 'POST', accessToken: token, idempotencyKey: crypto.randomUUID(), body }) }
export function updateUser(token: string, userId: string, body: Record<string, unknown>) { return apiRequest<User>(`/api/v1/admin/users/${userId}`, { method: 'PATCH', accessToken: token, idempotencyKey: crypto.randomUUID(), body }) }
export function createAgency(token: string, body: Record<string, unknown>) { return apiRequest<Agency>('/api/v1/admin/agencies', { method: 'POST', accessToken: token, idempotencyKey: crypto.randomUUID(), body }) }
export function updateAgency(token: string, agency: Agency, active: boolean) { return apiRequest<Agency>(`/api/v1/admin/agencies/${agency.id}`, { method: 'PATCH', accessToken: token, idempotencyKey: crypto.randomUUID(), body: { code: agency.code, name: agency.name, type: agency.type, jurisdiction: agency.jurisdiction, contactEmail: agency.contactEmail, contactPhone: agency.contactPhone, active, reason: active ? 'Reactivate governed agency record.' : 'Deactivate governed agency record.' } }) }
