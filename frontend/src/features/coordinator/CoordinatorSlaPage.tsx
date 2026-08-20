import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listSlas } from './api'
import { formatDate, label, statusTone } from './formatters'
import type { Sla } from './types'

export function CoordinatorSlaPage({ accessToken, filter }: { accessToken: string; filter?: 'breached' }) {
  const [slas, setSlas] = useState<Sla[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    listSlas(accessToken, filter === 'breached' ? ['BREACHED'] : ['BREACHED', 'AT_RISK'])
      .then(({ data }) => { if (active) setSlas(data) })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'SLA risks could not be loaded.')
      })
    return () => { active = false }
  }, [accessToken, filter])

  if (error) return <ErrorState title="SLA risks could not be loaded" description={error} />
  if (!slas) return <LoadingState label="Loading service-level risks" />

  return (
    <>
      <header className="coordinator-page-heading"><div><p className="eyebrow">Time-bound accountability</p><h1>{filter === 'breached' ? 'SLA breaches' : 'SLA risk queue'}</h1><p>Breaches appear before at-risk commitments. Official deadlines are never changed by AI.</p></div><StatusIndicator label={`${slas.length} require attention`} tone={slas.some(({ status }) => status === 'BREACHED') ? 'danger' : 'warning'} /></header>
      <section className="panel">
        {slas.length > 0 ? <div className="coordinator-table-wrap"><table className="coordinator-table">
          <thead><tr><th>Status</th><th>Type</th><th>Target</th><th>Started</th><th>Deadline</th><th>Next action</th></tr></thead>
          <tbody>{slas.map((sla) => <tr key={sla.id}>
            <td><StatusIndicator label={label(sla.status)} tone={statusTone(sla.status)} /></td><td>{label(sla.slaType)}</td>
            <td><strong>{sla.targetType}</strong><small>{sla.targetId}</small></td><td>{formatDate(sla.startAt)}</td><td>{formatDate(sla.deadline)}</td>
            <td>Open the target record and coordinate the accountable owner.</td>
          </tr>)}</tbody>
        </table></div> : <EmptyState title="No SLA risks" description="No SLA is currently breached or at risk under this filter." />}
      </section>
    </>
  )
}
