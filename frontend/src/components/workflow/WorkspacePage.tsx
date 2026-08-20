import type { ResolvedWorkspace } from '../../config/workspaces'
import { EmptyState } from '../feedback/EmptyState'
import { StatusIndicator } from '../status/StatusIndicator'

type WorkspacePageProps = {
  route: ResolvedWorkspace
}

const workflowFacts = [
  ['Current owner', 'Determined by backend assignment'],
  ['Next action', 'Returned by workflow permissions'],
  ['Deadline', 'Calculated from authoritative SLA'],
  ['Transitions', 'Validated and executed by backend'],
] as const

export function WorkspacePage({ route }: WorkspacePageProps) {
  const { workspace } = route
  const title = route.found ? route.route.label : 'Page not available'
  const summary = route.found
    ? route.route.summary
    : `The requested ${workspace.label.toLowerCase()} page is not part of the defined workspace.`

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">{workspace.label} operations</p>
          <h1>{title}</h1>
          <p className="page-summary">{summary}</p>
        </div>
        <StatusIndicator label={route.found ? 'Foundation ready' : 'Route unavailable'} tone={route.found ? 'success' : 'warning'} />
      </div>

      <dl className="workflow-overview" aria-label="Workflow information pattern">
        {workflowFacts.map(([term, detail]) => (
          <div className="workflow-fact" key={term}>
            <dt>{term}</dt>
            <dd>{detail}</dd>
          </div>
        ))}
      </dl>

      <section className="panel" aria-labelledby="workspace-queue-title">
        <div className="panel-header">
          <h2 id="workspace-queue-title">Operational queue</h2>
          <p>Server-side pagination and filters will populate this shared queue surface.</p>
        </div>
        <EmptyState
          title={route.found ? 'Workspace ready for operational data' : 'Use the workspace navigation'}
          description={route.found
            ? 'Feature data is introduced in its documented role phase. CivicOS will not invent records in the browser.'
            : `Choose a defined ${workspace.label.toLowerCase()} page to continue.`}
        />
      </section>
    </>
  )
}
