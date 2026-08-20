import { useEffect, useMemo, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import { listInterventions, listSlas } from './api'
import { formatDate, label, slaLabel, statusTone } from './formatters'
import type { CurrentUser, Intervention, PagedResponse, Sla } from './types'
import { primaryAction } from './workflow'

const statuses = [
  'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'ANALYSIS', 'COORDINATION_REQUIRED',
  'COORDINATION_COMPLETE', 'APPROVAL_PENDING', 'APPROVED', 'SCHEDULED', 'IN_PROGRESS',
  'RESTORATION', 'EVIDENCE_PENDING', 'VERIFICATION_PENDING', 'VERIFIED', 'CLOSED',
  'REJECTED', 'CANCELLED', 'ON_HOLD', 'REOPENED',
]

export function AgencyInterventionsPage({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [response, setResponse] = useState<PagedResponse<Intervention> | null>(null)
  const [slas, setSlas] = useState<Sla[]>([])
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    listInterventions(accessToken, {
      statuses: status ? [status] : undefined,
      priority: priority || undefined,
      page,
      size: 20,
    }).then(async (interventions) => {
      const targetIds = interventions.data.map(({ id }) => id)
      const slaResponse = user.permissions.includes('SLA_VIEW') && targetIds.length > 0
        ? await listSlas(accessToken, targetIds)
        : null
      if (!active) return
      setResponse(interventions)
      setSlas(slaResponse?.data ?? [])
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'The intervention queue could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, page, priority, status, user.permissions])

  const slaByIntervention = useMemo(() => {
    const values = new Map<string, Sla>()
    for (const sla of slas) {
      const current = values.get(sla.targetId)
      if (!current || new Date(sla.deadline) < new Date(current.deadline)) values.set(sla.targetId, sla)
    }
    return values
  }, [slas])

  return (
    <>
      <header className="agency-page-heading"><div><p className="eyebrow">Agency work queue</p><h1>Interventions</h1><p>Server-paginated work assigned to your agency, with lifecycle-safe next actions.</p></div></header>
      <section className="panel" aria-labelledby="agency-interventions-title">
        <div className="agency-filter-bar">
          <div><h2 id="agency-interventions-title">Assigned work</h2><p>{response ? `${response.pagination.totalElements} interventions` : 'Loading count'}</p></div>
          <label>Status<select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0) }}><option value="">All statuses</option>{statuses.map((value) => <option key={value} value={value}>{label(value)}</option>)}</select></label>
          <label>Priority<select value={priority} onChange={(event) => { setPriority(event.target.value); setPage(0) }}><option value="">All priorities</option>{['LOW', 'NORMAL', 'HIGH', 'CRITICAL'].map((value) => <option key={value} value={value}>{label(value)}</option>)}</select></label>
        </div>
        {error ? <ErrorState title="Intervention queue could not be loaded" description={error} /> : null}
        {!error && !response ? <LoadingState label="Loading assigned interventions" /> : null}
        {!error && response?.data.length === 0 ? <EmptyState title="No interventions match" description="Adjust the status or priority filter." /> : null}
        {response && response.data.length > 0 ? (
          <>
            <div className="agency-table-wrap">
              <table className="agency-table">
                <thead><tr><th>Case</th><th>Road</th><th>Intervention</th><th>Priority</th><th>SLA</th><th>Status</th><th>Next action</th><th /></tr></thead>
                <tbody>{response.data.map((item) => {
                  const sla = slaByIntervention.get(item.id)
                  return (
                    <tr key={item.id}>
                      <td>{item.caseNumber}</td><td>{item.roadSegmentName}</td>
                      <td><strong>{label(item.type)}</strong><small>{item.interventionNumber}<br />{formatDate(item.plannedStart)}</small></td>
                      <td>{label(item.priority)}</td>
                      <td><span className="sla-copy" data-state={sla?.status}>{slaLabel(sla)}<small>{sla ? label(sla.status) : 'No SLA record'}</small></span></td>
                      <td><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /></td>
                      <td>{primaryAction(item.status, user.permissions)?.label ?? 'Review details'}</td>
                      <td><a className="table-action" href={`/app/agency/interventions/${item.id}`}>Open</a></td>
                    </tr>
                  )
                })}</tbody>
              </table>
            </div>
            <nav className="agency-pagination" aria-label="Intervention pages">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</Button>
              <span>Page {page + 1} of {Math.max(response.pagination.totalPages, 1)}</span>
              <Button variant="secondary" disabled={page + 1 >= response.pagination.totalPages} onClick={() => setPage((current) => current + 1)}>Next</Button>
            </nav>
          </>
        ) : null}
      </section>
    </>
  )
}
