import { useState } from 'react'
import './App.css'
import { OperationalShell } from './components/layout/OperationalShell'
import { WorkspacePage } from './components/workflow/WorkspacePage'
import { resolveWorkspace } from './config/workspaces'
import { AgencyWorkspace } from './features/agency/AgencyWorkspace'
import { readAgencyAccessToken } from './features/agency/session'
import { CitizenWorkspace } from './features/citizen/CitizenWorkspace'
import { readCitizenAccessToken } from './features/citizen/session'
import { CoordinatorWorkspace } from './features/coordinator/CoordinatorWorkspace'
import { readCoordinatorAccessToken } from './features/coordinator/session'
import { InspectorWorkspace } from './features/inspector/InspectorWorkspace'
import { readInspectorAccessToken } from './features/inspector/session'

type AppProps = {
  pathname?: string
  accessToken?: string | null
}

function App({ pathname = window.location.pathname, accessToken }: AppProps) {
  const route = resolveWorkspace(pathname)
  const [citizenToken, setCitizenToken] = useState<string | null>(() =>
    accessToken === undefined ? readCitizenAccessToken() : accessToken,
  )
  const [agencyToken, setAgencyToken] = useState<string | null>(() =>
    accessToken === undefined ? readAgencyAccessToken() : accessToken,
  )
  const [coordinatorToken, setCoordinatorToken] = useState<string | null>(() =>
    accessToken === undefined ? readCoordinatorAccessToken() : accessToken,
  )
  const [inspectorToken, setInspectorToken] = useState<string | null>(() =>
    accessToken === undefined ? readInspectorAccessToken() : accessToken,
  )

  return (
    <OperationalShell route={route}>
      {route.workspace.role === 'citizen' ? (
        <CitizenWorkspace
          accessToken={citizenToken}
          route={route}
          onAuthenticated={setCitizenToken}
        />
      ) : route.workspace.role === 'agency' ? (
        <AgencyWorkspace
          accessToken={agencyToken}
          route={route}
          onAuthenticated={setAgencyToken}
        />
      ) : route.workspace.role === 'coordinator' ? (
        <CoordinatorWorkspace
          accessToken={coordinatorToken}
          route={route}
          onAuthenticated={setCoordinatorToken}
        />
      ) : route.workspace.role === 'inspector' ? (
        <InspectorWorkspace
          accessToken={inspectorToken}
          route={route}
          onAuthenticated={setInspectorToken}
        />
      ) : (
        <WorkspacePage route={route} />
      )}
    </OperationalShell>
  )
}

export default App
