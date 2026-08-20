import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listConflicts } from './api'
import { formatDate, label, statusTone } from './formatters'
import type { Conflict } from './types'

export function CoordinatorConflictsPage({ accessToken, filter }: { accessToken: string; filter?: 'high' | 'active' }) {
  const [conflicts, setConflicts] = useState<Conflict[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const requests = filter === 'active' || filter === 'high'
      ? Promise.all([
        listConflicts(accessToken, { status: 'OPEN', severity: filter === 'high' ? 'HIGH' : undefined, size: 100 }),
        listConflicts(accessToken, { status: 'UNDER_REVIEW', severity: filter === 'high' ? 'HIGH' : undefined, size: 100 }),
      ]).then(([open, review]) => [...open.data, ...review.data])
      : listConflicts(accessToken, { size: 100 }).then(({ data }) => data)
    requests.then((items) => { if (active) setConflicts(items) }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'Conflicts could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, filter])

  if (error) return <ErrorState title="Conflicts could not be loaded" description={error} />
  if (!conflicts) return <LoadingState label="Loading deterministic conflict records" />

  return (
    <>
      <header className="coordinator-page-heading"><div><p className="eyebrow">Deterministic conflict engine</p><h1>{filter === 'high' ? 'High-severity conflicts' : filter === 'active' ? 'Pending coordination decisions' : 'Conflicts'}</h1><p>AI can explain or recommend, but it does not create the underlying conflict facts.</p></div><StatusIndicator label={`${conflicts.length} records`} tone="info" /></header>
      <section className="panel">
        {conflicts.length > 0 ? <div className="coordinator-table-wrap"><table className="coordinator-table">
          <thead><tr><th>Conflict</th><th>Severity</th><th>Reason</th><th>Status</th><th>Detected</th><th>Interventions</th><th>Action</th></tr></thead>
          <tbody>{conflicts.map((conflict) => <tr key={conflict.id}>
            <td><strong>{conflict.conflictNumber}</strong><small>{label(conflict.type)}</small></td>
            <td><StatusIndicator label={label(conflict.severity)} tone={statusTone(conflict.severity)} /></td>
            <td className="conflict-reason">{conflict.explanation}</td><td>{label(conflict.status)}</td><td>{formatDate(conflict.detectedAt)}</td>
            <td>{conflict.interventionIds.length}</td><td><a href={`/app/coordinator/coordination/${conflict.id}`}>Open command view</a></td>
          </tr>)}</tbody>
        </table></div> : <EmptyState title="No matching conflicts" description="No deterministic conflict record matches this operational filter." />}
      </section>
    </>
  )
}
