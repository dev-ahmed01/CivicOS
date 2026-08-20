import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import type { ResolvedWorkspace } from '../../config/workspaces'
import { AdminAgenciesPage } from './AdminAgenciesPage'
import { AdminAuditPage } from './AdminAuditPage'
import { AdminConfigurationPage } from './AdminConfigurationPage'
import { AdminRolesPage } from './AdminRolesPage'
import { AdminSignIn } from './AdminSignIn'
import { AdminUsersPage } from './AdminUsersPage'
import { loadAdminIdentity } from './session'
import type { CurrentUser } from './types'
import './admin.css'

export function AdminWorkspace({ accessToken, route, onAuthenticated }: { accessToken: string | null; route: ResolvedWorkspace; onAuthenticated: (token: string) => void }) {
  const [user, setUser] = useState<CurrentUser | null>(null); const [error, setError] = useState<string | null>(null)
  useEffect(() => { if (!accessToken || user) return; let active = true; loadAdminIdentity(accessToken).then((identity) => { if (active) setUser(identity) }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Admin access could not be verified.') }); return () => { active = false } }, [accessToken, user])
  if (!accessToken) return <AdminSignIn onAuthenticated={(token, identity) => { setUser(identity); onAuthenticated(token) }} />
  if (error) return <ErrorState title="Admin access could not be verified" description={error} />
  if (!user) return <LoadingState label="Verifying administrator access" />
  if (route.requestedPath === '/app/admin/roles') return user.permissions.includes('POLICY_VIEW') ? <AdminRolesPage accessToken={accessToken} /> : <PermissionError />
  if (route.requestedPath === '/app/admin/agencies') return user.permissions.includes('POLICY_VIEW') ? <AdminAgenciesPage accessToken={accessToken} canManage={user.permissions.includes('POLICY_MANAGE')} /> : <PermissionError />
  if (route.requestedPath === '/app/admin/configuration') return user.permissions.includes('POLICY_VIEW') ? <AdminConfigurationPage accessToken={accessToken} /> : <PermissionError />
  if (route.requestedPath === '/app/admin/audit') return user.permissions.includes('AUDIT_VIEW') ? <AdminAuditPage accessToken={accessToken} /> : <PermissionError />
  return user.permissions.includes('USER_VIEW') ? <AdminUsersPage accessToken={accessToken} permissions={user.permissions} /> : <PermissionError />
}

function PermissionError() { return <ErrorState title="Administrative permission required" description="This administrator account does not hold the permission required for this governance screen." /> }
