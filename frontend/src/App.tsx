import { useState } from 'react'
import './App.css'
import { OperationalShell } from './components/layout/OperationalShell'
import { WorkspacePage } from './components/workflow/WorkspacePage'
import { resolveWorkspace } from './config/workspaces'
import { CitizenWorkspace } from './features/citizen/CitizenWorkspace'
import { readCitizenAccessToken } from './features/citizen/session'

type AppProps = {
  pathname?: string
  accessToken?: string | null
}

function App({ pathname = window.location.pathname, accessToken }: AppProps) {
  const route = resolveWorkspace(pathname)
  const [citizenToken, setCitizenToken] = useState<string | null>(() =>
    accessToken === undefined ? readCitizenAccessToken() : accessToken,
  )

  return (
    <OperationalShell route={route}>
      {route.workspace.role === 'citizen' ? (
        <CitizenWorkspace
          accessToken={citizenToken}
          route={route}
          onAuthenticated={setCitizenToken}
        />
      ) : (
        <WorkspacePage route={route} />
      )}
    </OperationalShell>
  )
}

export default App
