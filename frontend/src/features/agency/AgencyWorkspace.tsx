import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import type { ResolvedWorkspace } from '../../config/workspaces'
import { AgencyApprovalsPage } from './AgencyApprovalsPage'
import { AgencyDashboard } from './AgencyDashboard'
import { AgencyEvidencePage } from './AgencyEvidencePage'
import { AgencyInterventionDetail } from './AgencyInterventionDetail'
import { AgencyInterventionsPage } from './AgencyInterventionsPage'
import { AgencySignIn } from './AgencySignIn'
import { loadAgencyIdentity } from './session'
import type { CurrentUser } from './types'
import './agency.css'

type AgencyWorkspaceProps = {
  accessToken: string | null
  route: ResolvedWorkspace
  onAuthenticated: (accessToken: string) => void
}

export function AgencyWorkspace({ accessToken, route, onAuthenticated }: AgencyWorkspaceProps) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [identityError, setIdentityError] = useState<string | null>(null)

  useEffect(() => {
    if (!accessToken || user) return
    let active = true
    loadAgencyIdentity(accessToken)
      .then((identity) => { if (active) setUser(identity) })
      .catch((failure: unknown) => {
        if (active) setIdentityError(failure instanceof Error ? failure.message : 'Agency access could not be verified.')
      })
    return () => { active = false }
  }, [accessToken, user])

  if (!accessToken) {
    return <AgencySignIn onAuthenticated={(token, identity) => { setUser(identity); onAuthenticated(token) }} />
  }
  if (identityError) {
    return <ErrorState title="Agency access could not be verified" description={identityError} />
  }
  if (!user) {
    return <LoadingState label="Verifying agency access" />
  }

  if (route.requestedPath === '/app/agency/dashboard') {
    return <AgencyDashboard accessToken={accessToken} user={user} />
  }
  if (route.requestedPath === '/app/agency/approvals') {
    if (!user.permissions.includes('APPROVAL_VIEW')) {
      return <ErrorState title="Approval permission required" description="Your account cannot view agency approval requests." />
    }
    return <AgencyApprovalsPage accessToken={accessToken} user={user} />
  }
  if (route.requestedPath === '/app/agency/evidence') {
    if (!user.permissions.includes('EVIDENCE_VIEW')) {
      return <ErrorState title="Evidence permission required" description="Your account cannot view agency evidence records." />
    }
    return <AgencyEvidencePage accessToken={accessToken} user={user} />
  }
  if (route.requestedPath.startsWith('/app/agency/interventions/')) {
    return (
      <AgencyInterventionDetail
        accessToken={accessToken}
        interventionId={route.requestedPath.slice('/app/agency/interventions/'.length)}
        user={user}
      />
    )
  }
  return <AgencyInterventionsPage accessToken={accessToken} user={user} />
}
