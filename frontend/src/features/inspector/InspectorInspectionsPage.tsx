import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listInspections } from './api'
import { formatDate, label, tone } from './formatters'
import type { Inspection } from './types'

export function InspectorInspectionsPage({ accessToken }: { accessToken: string }) {
  const [inspections, setInspections] = useState<Inspection[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { let active = true; listInspections(accessToken).then((response) => { if (active) setInspections(response.data) }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Assigned inspections could not be loaded.') }); return () => { active = false } }, [accessToken])

  return <>
    <header className="inspector-page-heading"><div><p className="eyebrow">Assigned field work</p><h1>My inspections</h1><p>Open a task, confirm its stored location and expected work, capture proof, then record the official inspection result.</p></div></header>
    {error ? <ErrorState title="Assigned inspections could not be loaded" description={error} /> : null}
    {!error && !inspections ? <LoadingState label="Loading assigned inspections" /> : null}
    {inspections ? <section className="panel"><div className="panel-header"><h2>Inspection queue</h2><p>{inspections.length} tasks assigned to this inspector account.</p></div>
      {inspections.length === 0 ? <EmptyState title="No assigned inspections" description="New field-verification tasks will appear here when assigned by the backend." /> : <div className="inspector-card-list">{inspections.map((inspection) => <InspectionCard key={inspection.id} inspection={inspection} />)}</div>}
    </section> : null}
  </>
}

function InspectionCard({ inspection }: { inspection: Inspection }) {
  const next = inspection.status === 'SCHEDULED' ? 'Confirm location and start' : inspection.status === 'IN_PROGRESS' ? 'Capture evidence and submit result' : 'Review completed record'
  return <article className="inspection-card"><div><p className="eyebrow">{inspection.interventionNumber} · {inspection.agencyName}</p><h3><a href={`/app/inspector/inspections/${inspection.id}`}>{inspection.roadSegmentName}</a></h3><p>{inspection.interventionDescription}</p><small>Planned {formatDate(inspection.plannedStart)} — {formatDate(inspection.plannedEnd)}</small></div><div className="inspection-card-state"><StatusIndicator label={label(inspection.status)} tone={tone(inspection.status)} /><strong>Next: {next}</strong></div></article>
}
