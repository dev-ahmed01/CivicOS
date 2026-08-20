import './App.css'
import { OperationalShell } from './components/layout/OperationalShell'
import { WorkspacePage } from './components/workflow/WorkspacePage'
import { resolveWorkspace } from './config/workspaces'

type AppProps = {
  pathname?: string
}

function App({ pathname = window.location.pathname }: AppProps) {
  const route = resolveWorkspace(pathname)

  return (
    <OperationalShell route={route}>
      <WorkspacePage route={route} />
    </OperationalShell>
  )
}

export default App
