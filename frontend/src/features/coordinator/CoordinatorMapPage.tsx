import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listConflicts, listInterventions } from './api'
import { formatDate, label, statusTone } from './formatters'
import { SpatialContextMap } from './SpatialContextMap'
import type { Conflict, Intervention } from './types'

type MapData = { interventions: Intervention[]; conflicts: Conflict[] }

const operationalStatuses = [
  'COORDINATION_REQUIRED', 'COORDINATION_COMPLETE', 'APPROVAL_PENDING', 'APPROVED',
  'SCHEDULED', 'IN_PROGRESS', 'RESTORATION_PENDING', 'EVIDENCE_PENDING', 'VERIFICATION_PENDING',
]

export function CoordinatorMapPage({ accessToken, filter }: { accessToken: string; filter?: 'verification' | 'upcoming' }) {
  const [data, setData] = useState<MapData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const statuses = filter === 'verification' ? ['VERIFICATION_PENDING']
      : filter === 'upcoming' ? ['APPROVED', 'SCHEDULED'] : operationalStatuses
    Promise.all([
      listInterventions(accessToken, {
        statuses,
        plannedFrom: filter === 'upcoming' ? new Date().toISOString() : undefined,
        size: 100,
      }),
      listConflicts(accessToken, { status: 'OPEN', size: 100 }),
    ]).then(([interventions, conflicts]) => {
      if (active) setData({ interventions: interventions.data, conflicts: conflicts.data })
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'The coordinator map could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, filter])

  if (error) return <ErrorState title="Coordinator map could not be loaded" description={error} />
  if (!data) return <LoadingState label="Loading spatial intervention context" />

  return (
    <>
      <header className="coordinator-page-heading"><div><p className="eyebrow">Spatial awareness with list parity</p><h1>{filter === 'verification' ? 'Verification backlog map' : filter === 'upcoming' ? 'Upcoming road work map' : 'Coordinator map'}</h1><p>The geometry view and structured list use the same authoritative intervention records.</p></div><StatusIndicator label={`${data.conflicts.length} open conflicts`} tone={data.conflicts.length ? 'warning' : 'success'} /></header>
      {data.interventions.length > 0 ? <SpatialContextMap interventions={data.interventions} /> : <EmptyState title="No mapped interventions" description="No intervention matches this operational map filter." />}
      <section className="panel map-records-panel" aria-labelledby="map-records-title">
        <div className="panel-header"><h2 id="map-records-title">Equivalent operational list</h2><p>Every mapped record remains available without relying on colour or pointer interaction.</p></div>
        {data.interventions.length > 0 ? <div className="coordinator-table-wrap"><table className="coordinator-table">
          <thead><tr><th>Intervention</th><th>Agency</th><th>Road</th><th>Status</th><th>Planned start</th><th>Planned end</th></tr></thead>
          <tbody>{data.interventions.map((item) => <tr key={item.id}>
            <td><strong>{item.interventionNumber}</strong><small>{label(item.type)}</small></td><td>{item.agencyName}</td><td>{item.roadSegmentName}</td>
            <td><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /></td><td>{formatDate(item.plannedStart)}</td><td>{formatDate(item.plannedEnd)}</td>
          </tr>)}</tbody>
        </table></div> : null}
      </section>
    </>
  )
}
