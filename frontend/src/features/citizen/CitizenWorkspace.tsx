import type { ResolvedWorkspace } from '../../config/workspaces'
import { CitizenNotificationsPage } from './CitizenNotificationsPage'
import { CitizenReportDetail } from './CitizenReportDetail'
import { CitizenReportForm } from './CitizenReportForm'
import { CitizenReportsPage } from './CitizenReportsPage'
import { CitizenSignIn } from './CitizenSignIn'
import './citizen.css'

type CitizenWorkspaceProps = {
  accessToken: string | null
  route: ResolvedWorkspace
  onAuthenticated: (accessToken: string) => void
}

export function CitizenWorkspace({ accessToken, route, onAuthenticated }: CitizenWorkspaceProps) {
  if (!accessToken) {
    return <CitizenSignIn onAuthenticated={onAuthenticated} />
  }

  if (route.requestedPath === '/app/citizen/report') {
    return <CitizenReportForm accessToken={accessToken} />
  }
  if (route.requestedPath === '/app/citizen/notifications') {
    return <CitizenNotificationsPage accessToken={accessToken} />
  }
  if (route.requestedPath.startsWith('/app/citizen/reports/')) {
    const observationId = route.requestedPath.slice('/app/citizen/reports/'.length)
    return <CitizenReportDetail accessToken={accessToken} observationId={observationId} />
  }
  return <CitizenReportsPage accessToken={accessToken} />
}
