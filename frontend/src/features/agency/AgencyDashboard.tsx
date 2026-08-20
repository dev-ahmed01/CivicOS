import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listApprovals, listConflicts, listInterventions, listSlas } from './api'
import { formatDate, label, statusTone } from './formatters'
import type { CurrentUser, Intervention } from './types'
import { actionableStatuses, primaryAction } from './workflow'

type DashboardData = {
  assigned: number
  pendingActions: number
  approvals: number | null
  slaAtRisk: number | null
  conflicts: number | null
  evidencePending: number | null
  verificationPending: number
  queue: Intervention[]
}

export function AgencyDashboard({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const can = (permission: string) => user.permissions.includes(permission)
    const statuses = actionableStatuses(user.permissions)
    Promise.all([
      listInterventions(accessToken, { page: 0, size: 6 }),
      statuses.length > 0 ? listInterventions(accessToken, { statuses, page: 0, size: 6 }) : Promise.resolve(emptyPage<Intervention>()),
      can('APPROVAL_VIEW') ? listApprovals(accessToken, 'PENDING', 1) : Promise.resolve(null),
      can('SLA_VIEW') ? listSlas(accessToken, undefined, ['AT_RISK', 'BREACHED'], 1) : Promise.resolve(null),
      can('CONFLICT_VIEW') ? listConflicts(accessToken, 'OPEN', undefined, 1) : Promise.resolve(null),
      listInterventions(accessToken, { statuses: ['EVIDENCE_PENDING'], page: 0, size: 1 }),
      listInterventions(accessToken, { statuses: ['VERIFICATION_PENDING'], page: 0, size: 1 }),
    ]).then(([assigned, actions, approvals, slas, conflicts, evidence, verification]) => {
      if (!active) return
      setData({
        assigned: assigned.pagination.totalElements,
        pendingActions: actions.pagination.totalElements,
        approvals: approvals?.pagination.totalElements ?? null,
        slaAtRisk: slas?.pagination.totalElements ?? null,
        conflicts: conflicts?.pagination.totalElements ?? null,
        evidencePending: evidence.pagination.totalElements,
        verificationPending: verification.pagination.totalElements,
        queue: actions.data,
      })
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'The agency dashboard could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, user.permissions])

  if (error) return <ErrorState title="Agency dashboard could not be loaded" description={error} />
  if (!data) return <LoadingState label="Loading agency priorities" />

  const metrics = [
    ['Assigned interventions', data.assigned], ['Pending actions', data.pendingActions],
    ['Approvals pending', data.approvals], ['SLA at risk', data.slaAtRisk],
    ['Active conflicts', data.conflicts], ['Evidence pending', data.evidencePending],
    ['Verification pending', data.verificationPending],
  ] as const

  return (
    <>
      <header className="agency-page-heading">
        <div><p className="eyebrow">Agency command view</p><h1>Agency overview for {user.fullName}</h1><p>Actions requiring attention are prioritized ahead of analytics.</p></div>
        <StatusIndicator label="Agency scoped" tone="success" />
      </header>
      <dl className="agency-metrics" aria-label="Agency operational metrics">
        {metrics.map(([name, value]) => <div key={name}><dt>{name}</dt><dd>{value ?? 'Restricted'}</dd></div>)}
      </dl>
      <section className="panel" aria-labelledby="agency-priority-title">
        <div className="panel-header"><h2 id="agency-priority-title">Priority work queue</h2><p>The first six interventions with a permitted agency action.</p></div>
        <div className="agency-table-wrap">
          <table className="agency-table">
            <thead><tr><th>Case</th><th>Road</th><th>Intervention</th><th>Priority</th><th>Status</th><th>Planned start</th><th>Next action</th></tr></thead>
            <tbody>{data.queue.map((item) => (
              <tr key={item.id}>
                <td>{item.caseNumber}</td><td>{item.roadSegmentName}</td>
                <td><a href={`/app/agency/interventions/${item.id}`}>{item.interventionNumber}</a><small>{label(item.type)}</small></td>
                <td>{label(item.priority)}</td><td><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /></td>
                <td>{formatDate(item.plannedStart)}</td><td>{primaryAction(item.status, user.permissions)?.label ?? 'Open details'}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </section>
    </>
  )
}

function emptyPage<T>() {
  return { data: [] as T[], pagination: { page: 0, size: 0, totalElements: 0, totalPages: 0 }, meta: {} }
}
