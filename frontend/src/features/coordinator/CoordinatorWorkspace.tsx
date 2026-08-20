import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import type { ResolvedWorkspace } from '../../config/workspaces'
import { CoordinationWorkspacePage } from './CoordinationWorkspacePage'
import { CoordinatorCommandCenter } from './CoordinatorCommandCenter'
import { CoordinatorConflictsPage } from './CoordinatorConflictsPage'
import { CoordinatorMapPage } from './CoordinatorMapPage'
import { CoordinatorSignIn } from './CoordinatorSignIn'
import { CoordinatorSlaPage } from './CoordinatorSlaPage'
import { loadCoordinatorIdentity } from './session'
import type { CurrentUser } from './types'
import './coordinator.css'

type CoordinatorWorkspaceProps = {
  accessToken: string | null
  route: ResolvedWorkspace
  onAuthenticated: (accessToken: string) => void
}

export function CoordinatorWorkspace({ accessToken, route, onAuthenticated }: CoordinatorWorkspaceProps) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [identityError, setIdentityError] = useState<string | null>(null)

  useEffect(() => {
    if (!accessToken || user) return
    let active = true
    loadCoordinatorIdentity(accessToken)
      .then((identity) => { if (active) setUser(identity) })
      .catch((failure: unknown) => {
        if (active) setIdentityError(failure instanceof Error ? failure.message : 'Coordinator access could not be verified.')
      })
    return () => { active = false }
  }, [accessToken, user])

  if (!accessToken) {
    return <CoordinatorSignIn onAuthenticated={(token, identity) => { setUser(identity); onAuthenticated(token) }} />
  }
  if (identityError) return <ErrorState title="Coordinator access could not be verified" description={identityError} />
  if (!user) return <LoadingState label="Verifying coordinator access" />
  if (!user.permissions.includes('CONFLICT_VIEW')) {
    return <ErrorState title="Conflict permission required" description="Your coordinator account cannot view deterministic conflict records." />
  }

  const path = route.requestedPath === '/' || route.requestedPath === '/app'
    ? '/app/coordinator/dashboard'
    : route.requestedPath
  if (path === '/app/coordinator/dashboard') return <CoordinatorCommandCenter accessToken={accessToken} user={user} />
  if (path.startsWith('/app/coordinator/conflicts')) {
    const filter = path.endsWith('/high') ? 'high' : path.endsWith('/active') ? 'active' : undefined
    return <CoordinatorConflictsPage accessToken={accessToken} filter={filter} />
  }
  if (path.startsWith('/app/coordinator/map')) {
    if (!user.permissions.includes('INTERVENTION_VIEW')) {
      return <ErrorState title="Intervention permission required" description="Your account cannot view cross-agency intervention geometry." />
    }
    const filter = path.endsWith('/verification') ? 'verification' : path.endsWith('/upcoming') ? 'upcoming' : undefined
    return <CoordinatorMapPage accessToken={accessToken} filter={filter} />
  }
  if (path.startsWith('/app/coordinator/sla')) {
    if (!user.permissions.includes('SLA_VIEW')) {
      return <ErrorState title="SLA permission required" description="Your account cannot view service-level commitments." />
    }
    return <CoordinatorSlaPage accessToken={accessToken} filter={path.endsWith('/breached') ? 'breached' : undefined} />
  }
  if (!user.permissions.includes('COORDINATION_VIEW')) {
    return <ErrorState title="Coordination permission required" description="Your account cannot view coordination decisions." />
  }
  const conflictId = path.startsWith('/app/coordinator/coordination/')
    ? path.slice('/app/coordinator/coordination/'.length)
    : undefined
  return <CoordinationWorkspacePage accessToken={accessToken} user={user} conflictId={conflictId} />
}
