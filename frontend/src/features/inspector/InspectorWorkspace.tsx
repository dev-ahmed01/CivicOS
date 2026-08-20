import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import type { ResolvedWorkspace } from '../../config/workspaces'
import { InspectorEvidencePage } from './InspectorEvidencePage'
import { InspectorInspectionDetail } from './InspectorInspectionDetail'
import { InspectorInspectionsPage } from './InspectorInspectionsPage'
import { InspectorSignIn } from './InspectorSignIn'
import { InspectorVerificationPage } from './InspectorVerificationPage'
import { loadInspectorIdentity } from './session'
import type { CurrentUser } from './types'
import './inspector.css'

export function InspectorWorkspace({ accessToken, route, onAuthenticated }: { accessToken: string | null; route: ResolvedWorkspace; onAuthenticated: (token: string) => void }) {
  const [user, setUser] = useState<CurrentUser | null>(null); const [identityError, setIdentityError] = useState<string | null>(null)
  useEffect(() => { if (!accessToken || user) return; let active = true; loadInspectorIdentity(accessToken).then((identity) => { if (active) setUser(identity) }).catch((failure: unknown) => { if (active) setIdentityError(failure instanceof Error ? failure.message : 'Inspector access could not be verified.') }); return () => { active = false } }, [accessToken, user])
  if (!accessToken) return <InspectorSignIn onAuthenticated={(token, identity) => { setUser(identity); onAuthenticated(token) }} />
  if (identityError) return <ErrorState title="Inspector access could not be verified" description={identityError} />
  if (!user) return <LoadingState label="Verifying inspector access" />
  if (!user.permissions.includes('INSPECTION_VIEW')) return <ErrorState title="Inspection permission required" description="This account cannot view assigned inspections." />
  if (route.requestedPath.startsWith('/app/inspector/inspections/')) return <InspectorInspectionDetail accessToken={accessToken} inspectionId={route.requestedPath.slice('/app/inspector/inspections/'.length)} user={user} />
  if (route.requestedPath === '/app/inspector/evidence') return user.permissions.includes('EVIDENCE_VIEW') ? <InspectorEvidencePage accessToken={accessToken} /> : <ErrorState title="Evidence permission required" description="This account cannot view inspection evidence." />
  if (route.requestedPath === '/app/inspector/verification') return <InspectorVerificationPage accessToken={accessToken} user={user} />
  return <InspectorInspectionsPage accessToken={accessToken} />
}
