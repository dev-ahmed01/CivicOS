import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listApprovals, listConflicts, listInterventions, listSlas } from './api'
import { CoordinationWorkspace } from './CoordinationWorkspace'
import { formatDate, label, statusTone } from './formatters'
import type { Conflict, CurrentUser } from './types'

type CommandCenterData = {
  breachedSlas: number
  atRiskSlas: number
  highConflicts: number
  pendingDecisions: number
  pendingApprovals: number
  verificationBacklog: number
  upcomingWork: number
  activeConflicts: Conflict[]
}

const severityRank: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1 }

export function CoordinatorCommandCenter({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [data, setData] = useState<CommandCenterData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const can = (permission: string) => user.permissions.includes(permission)
    Promise.all([
      can('SLA_VIEW') ? listSlas(accessToken, ['BREACHED'], 1) : Promise.resolve(null),
      can('SLA_VIEW') ? listSlas(accessToken, ['AT_RISK'], 1) : Promise.resolve(null),
      listConflicts(accessToken, { status: 'OPEN', size: 50 }),
      listConflicts(accessToken, { status: 'UNDER_REVIEW', size: 50 }),
      can('APPROVAL_VIEW') ? listApprovals(accessToken, 'PENDING', 1) : Promise.resolve(null),
      can('INTERVENTION_VIEW') ? listInterventions(accessToken, { statuses: ['VERIFICATION_PENDING'], size: 1 }) : Promise.resolve(null),
      can('INTERVENTION_VIEW') ? listInterventions(accessToken, {
        statuses: ['APPROVED', 'SCHEDULED'], plannedFrom: new Date().toISOString(), size: 1,
      }) : Promise.resolve(null),
    ]).then(([breached, atRisk, open, underReview, approvals, verification, upcoming]) => {
      if (!active) return
      const activeConflicts = [...open.data, ...underReview.data]
        .toSorted((first, second) => (severityRank[second.severity] ?? 0) - (severityRank[first.severity] ?? 0))
      setData({
        breachedSlas: breached?.pagination.totalElements ?? 0,
        atRiskSlas: atRisk?.pagination.totalElements ?? 0,
        highConflicts: activeConflicts.filter(({ severity }) => severity === 'HIGH').length,
        pendingDecisions: activeConflicts.length,
        pendingApprovals: approvals?.pagination.totalElements ?? 0,
        verificationBacklog: verification?.pagination.totalElements ?? 0,
        upcomingWork: upcoming?.pagination.totalElements ?? 0,
        activeConflicts,
      })
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'The command center could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, user.permissions])

  if (error) return <ErrorState title="Command center could not be loaded" description={error} />
  if (!data) return <LoadingState label="Loading citywide coordination priorities" />

  const metrics = [
    { label: 'SLA breaches', value: data.breachedSlas, detail: `${data.atRiskSlas} additional at risk`, href: '/app/coordinator/sla/breached', urgency: 'danger' },
    { label: 'High-severity conflicts', value: data.highConflicts, detail: 'Deterministic risks', href: '/app/coordinator/conflicts/high', urgency: 'danger' },
    { label: 'Pending coordination decisions', value: data.pendingDecisions, detail: 'Open or under review', href: '/app/coordinator/conflicts/active', urgency: 'warning' },
    { label: 'Pending approvals', value: data.pendingApprovals, detail: 'Awaiting authorized decision', href: '#pending-approvals', urgency: 'info' },
    { label: 'Verification backlog', value: data.verificationBacklog, detail: 'Official checks pending', href: '/app/coordinator/map/verification', urgency: 'warning' },
    { label: 'Upcoming road work', value: data.upcomingWork, detail: 'Approved or scheduled', href: '/app/coordinator/map/upcoming', urgency: 'info' },
  ] as const

  return (
    <>
      <header className="coordinator-page-heading">
        <div><p className="eyebrow">Citywide operational priority</p><h1>Coordination Command Center</h1><p>Move from unrelated agency projects to one governed physical-road decision.</p></div>
        <StatusIndicator label={`Coordinator · ${user.fullName}`} tone="success" />
      </header>
      <nav className="command-metrics" aria-label="Filtered operational queues">
        {metrics.map((metric) => (
          <a key={metric.label} href={metric.href} data-urgency={metric.urgency}>
            <span>{metric.label}</span><strong>{metric.value}</strong><small>{metric.detail}</small>
          </a>
        ))}
      </nav>
      <section className="priority-queue panel" aria-labelledby="priority-conflicts-title">
        <div className="panel-header"><h2 id="priority-conflicts-title">Active conflict queue</h2><p>Highest deterministic severity appears first.</p></div>
        {data.activeConflicts.length > 0 ? (
          <div className="coordinator-table-wrap"><table className="coordinator-table">
            <thead><tr><th>Conflict</th><th>Severity</th><th>Type</th><th>Status</th><th>Detected</th><th>Affected work</th><th>Action</th></tr></thead>
            <tbody>{data.activeConflicts.slice(0, 8).map((conflict) => (
              <tr key={conflict.id}>
                <td><strong>{conflict.conflictNumber}</strong></td>
                <td><StatusIndicator label={label(conflict.severity)} tone={statusTone(conflict.severity)} /></td>
                <td>{label(conflict.type)}</td><td>{label(conflict.status)}</td><td>{formatDate(conflict.detectedAt)}</td>
                <td>{conflict.interventionIds.length}</td><td><a href={`/app/coordinator/coordination/${conflict.id}`}>Coordinate</a></td>
              </tr>
            ))}</tbody>
          </table></div>
        ) : <EmptyState title="No active conflicts" description="All currently detected intervention conflicts are final." />}
      </section>
      {data.activeConflicts[0] && user.permissions.includes('COORDINATION_VIEW') && user.permissions.includes('INTERVENTION_VIEW') ? (
        <section className="featured-coordination" aria-labelledby="featured-coordination-title">
          <div className="section-intro"><p className="eyebrow">Most urgent coordination problem</p><h2 id="featured-coordination-title">From detection to accountable decision</h2></div>
          <CoordinationWorkspace accessToken={accessToken} user={user} conflictId={data.activeConflicts[0].id} />
        </section>
      ) : null}
      <section id="pending-approvals" className="command-boundary-note">
        <strong>Approval boundary</strong>
        <p>{data.pendingApprovals} request{data.pendingApprovals === 1 ? '' : 's'} pending. Coordination decisions do not approve agency work; approval remains a separately authorized workflow.</p>
      </section>
    </>
  )
}
