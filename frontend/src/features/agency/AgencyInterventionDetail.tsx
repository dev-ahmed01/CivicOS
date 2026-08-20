import { useEffect, useState, type ReactNode } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import {
  getCase,
  getIntervention,
  listAudit,
  listConflicts,
  listDependencies,
  listEvidence,
  listInterventionApprovals,
  listSlas,
  requestApproval,
  transitionIntervention,
} from './api'
import { formatDate, label, slaLabel, statusTone } from './formatters'
import type {
  Approval,
  AuditEvent,
  CivicCase,
  Conflict,
  CurrentUser,
  Dependency,
  Evidence,
  Intervention,
  Sla,
} from './types'
import { primaryAction } from './workflow'

const tabs = ['Overview', 'Schedule', 'Dependencies', 'Conflicts', 'Approvals', 'Evidence', 'Verification', 'Timeline', 'Audit'] as const
type Tab = typeof tabs[number]

type DetailData = {
  intervention: Intervention
  civicCase: CivicCase | null
  conflicts: Conflict[]
  approvals: Approval[]
  evidence: Evidence[]
  slas: Sla[]
  dependencies: Dependency[]
  audit: AuditEvent[]
}

export function AgencyInterventionDetail(props: { accessToken: string; interventionId: string; user: CurrentUser }) {
  const [data, setData] = useState<DetailData | null>(null)
  const [activeTab, setActiveTab] = useState<Tab>('Overview')
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    getIntervention(props.accessToken, props.interventionId)
      .then(async (intervention) => {
        const can = (permission: string) => props.user.permissions.includes(permission)
        const [civicCase, conflictResponse, approvals, evidenceResponse, slaResponse, dependencies, auditResponse] = await Promise.all([
          can('OBSERVATION_VIEW_RELEVANT') ? getCase(props.accessToken, intervention.caseId) : Promise.resolve(null),
          can('CONFLICT_VIEW') ? listConflicts(props.accessToken, undefined, intervention.roadSegmentId, 100) : Promise.resolve(null),
          can('APPROVAL_VIEW') ? listInterventionApprovals(props.accessToken, intervention.id) : Promise.resolve([]),
          can('EVIDENCE_VIEW') ? listEvidence(props.accessToken, [intervention.id]) : Promise.resolve(null),
          can('SLA_VIEW') ? listSlas(props.accessToken, [intervention.id]) : Promise.resolve(null),
          listDependencies(props.accessToken, intervention.id),
          can('AUDIT_VIEW') ? listAudit(props.accessToken, intervention.id) : Promise.resolve(null),
        ])
        if (!active) return
        setData({
          intervention,
          civicCase,
          conflicts: conflictResponse?.data.filter((conflict) => conflict.interventionIds.includes(intervention.id)) ?? [],
          approvals,
          evidence: evidenceResponse?.data ?? [],
          slas: slaResponse?.data ?? [],
          dependencies,
          audit: auditResponse?.data ?? [],
        })
      })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'The intervention could not be loaded.')
      })
    return () => { active = false }
  }, [props.accessToken, props.interventionId, props.user.permissions, refreshKey])

  if (error) return <ErrorState title="Intervention could not be loaded" description={error} />
  if (!data) return <LoadingState label="Loading intervention details" />

  const currentSla = data.slas.find(({ status }) => !['COMPLETED'].includes(status)) ?? data.slas[0]
  return (
    <>
      <header className="intervention-detail-header">
        <div><p className="eyebrow">{data.intervention.interventionNumber}</p><h1>{label(data.intervention.type)}</h1><p>{data.intervention.roadSegmentName} · {data.intervention.agencyName}</p></div>
        <div className="intervention-header-status"><StatusIndicator label={label(data.intervention.status)} tone={statusTone(data.intervention.status)} /><span><strong>SLA</strong>{slaLabel(currentSla)}</span></div>
      </header>

      <AgencyNextAction
        accessToken={props.accessToken}
        intervention={data.intervention}
        user={props.user}
        onChanged={() => setRefreshKey((value) => value + 1)}
      />

      <div className="agency-tabs" role="tablist" aria-label="Intervention information">
        {tabs.map((tab) => <button key={tab} type="button" role="tab" aria-selected={activeTab === tab} onClick={() => setActiveTab(tab)}>{tab}</button>)}
      </div>
      <section className="panel intervention-tab-panel" role="tabpanel" aria-label={activeTab}>
        <DetailTab activeTab={activeTab} data={data} permissions={props.user.permissions} />
      </section>
    </>
  )
}

function AgencyNextAction(props: { accessToken: string; intervention: Intervention; user: CurrentUser; onChanged: () => void }) {
  const action = primaryAction(props.intervention.status, props.user.permissions)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (!action) {
    return <section className="next-action-panel"><div><p className="eyebrow">Next action</p><h2>No agency action is currently authorized</h2><p>The current lifecycle state or your permissions place the next decision with another authorized role.</p></div></section>
  }

  async function execute() {
    if (!reason.trim()) { setError('Record a reason before executing this authoritative action.'); return }
    setSubmitting(true); setError(null); setMessage(null)
    try {
      if (action?.kind === 'approval-request') await requestApproval(props.accessToken, props.intervention.id, reason.trim())
      else if (action) await transitionIntervention(props.accessToken, props.intervention, action.action, reason.trim())
      setMessage(`${action?.label ?? 'Action'} completed and recorded in the audit trail.`)
      props.onChanged()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'The action was not saved. Review the current state and retry.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="next-action-panel" aria-labelledby="next-action-title">
      <div><p className="eyebrow">Next action</p><h2 id="next-action-title">{action.label}</h2><p>{action.reasonPrompt}</p></div>
      <div className="next-action-controls">
        {error ? <div className="agency-alert" role="alert">{error}</div> : null}
        {message ? <div className="agency-success" role="status">{message}</div> : null}
        <label>Action reason<textarea rows={3} value={reason} onChange={(event) => setReason(event.target.value)} /></label>
        <Button type="button" disabled={submitting} onClick={execute}>{submitting ? 'Recording action...' : action.label}</Button>
      </div>
    </section>
  )
}

function DetailTab({ activeTab, data, permissions }: { activeTab: Tab; data: DetailData; permissions: string[] }) {
  const intervention = data.intervention
  if (activeTab === 'Overview') return (
    <div className="detail-grid"><DetailList values={[
      ['Purpose', intervention.description], ['Case', data.civicCase?.caseNumber ?? intervention.caseNumber],
      ['Road segment', intervention.roadSegmentName], ['Agency', intervention.agencyName],
      ['Priority', label(intervention.priority)], ['Current status', label(intervention.status)],
      ['Planned start', formatDate(intervention.plannedStart)], ['Planned end', formatDate(intervention.plannedEnd)],
    ]} /><div className="agency-map" role="img" aria-label={`Spatial preview for ${intervention.roadSegmentName}`}><span>Road work geometry</span><code>{intervention.geometryWkt}</code></div></div>
  )
  if (activeTab === 'Schedule') return <ScheduleView intervention={intervention} />
  if (activeTab === 'Dependencies') return <Collection title="Dependencies" empty="No dependencies are recorded for this intervention.">{data.dependencies.map((item) => <article key={item.id} className="detail-card"><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /><h3>{label(item.type)}</h3><p>{item.reason}</p><small>{item.sourceInterventionId} → {item.targetInterventionId}{item.required ? ' · Required' : ' · Optional'}</small></article>)}</Collection>
  if (activeTab === 'Conflicts') return permissions.includes('CONFLICT_VIEW') ? <Collection title="Conflicts" empty="No visible conflicts affect this intervention.">{data.conflicts.map((item) => <article key={item.id} className="detail-card"><div className="card-status-row"><StatusIndicator label={label(item.severity)} tone={statusTone(item.severity)} /><span>{item.conflictNumber}</span></div><h3>{label(item.type)}</h3><p>{item.explanation}</p><small>Detected {formatDate(item.detectedAt)} · {label(item.status)}</small></article>)}</Collection> : <RestrictedCopy />
  if (activeTab === 'Approvals') return permissions.includes('APPROVAL_VIEW') ? <Collection title="Approval history" empty="No approval request has been recorded.">{data.approvals.map((item) => <article key={item.id} className="detail-card"><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /><h3>{item.decision ? label(item.decision) : 'Decision pending'}</h3><p>{item.reason ?? 'No decision reason has been recorded.'}</p><small>Requested {formatDate(item.createdAt)} · {item.conditions.length} conditions</small></article>)}</Collection> : <RestrictedCopy />
  if (activeTab === 'Evidence') return permissions.includes('EVIDENCE_VIEW') ? <Collection title="Evidence" empty="No evidence has been uploaded.">{data.evidence.map((item) => <article key={item.id} className="detail-card"><StatusIndicator label={label(item.status)} tone={statusTone(item.status)} /><h3>{label(item.type)}</h3><p>{item.originalFilename ?? 'Stored evidence file'}</p><small>Captured {formatDate(item.capturedAt)} · Uploaded {formatDate(item.createdAt)}</small></article>)}</Collection> : <RestrictedCopy />
  if (activeTab === 'Verification') return <div className="verification-boundary"><p className="eyebrow">Authoritative boundary</p><h2>{intervention.status === 'VERIFICATION_PENDING' ? 'Awaiting independent inspection and verification' : `Current state: ${label(intervention.status)}`}</h2><p>Agency officers can submit accepted evidence, but only an authorized inspector can record the official verification result.</p></div>
  if (activeTab === 'Timeline') return <Timeline events={data.audit} intervention={intervention} />
  return permissions.includes('AUDIT_VIEW') ? <AuditView events={data.audit} /> : <RestrictedCopy />
}

function DetailList({ values }: { values: Array<[string, string]> }) {
  return <dl className="agency-detail-list">{values.map(([term, value]) => <div key={term}><dt>{term}</dt><dd>{value}</dd></div>)}</dl>
}

function ScheduleView({ intervention }: { intervention: Intervention }) {
  return <div className="schedule-view"><div className="schedule-axis"><span>Planned start<br /><strong>{formatDate(intervention.plannedStart)}</strong></span><span>Planned end<br /><strong>{formatDate(intervention.plannedEnd)}</strong></span></div><div className="schedule-track"><span className="schedule-planned">Planned · {intervention.agencyName}</span>{intervention.actualStart ? <span className="schedule-actual">Actual · {formatDate(intervention.actualStart)} to {formatDate(intervention.actualEnd)}</span> : null}</div><p>Who: {intervention.agencyName} · Where: {intervention.roadSegmentName} · When: {formatDate(intervention.plannedStart)} to {formatDate(intervention.plannedEnd)}</p></div>
}

function Collection({ title, empty, children }: { title: string; empty: string; children: ReactNode }) {
  const items = Array.isArray(children) ? children : [children]
  return <div className="detail-collection"><h2>{title}</h2>{items.length === 0 ? <p>{empty}</p> : items}</div>
}

function Timeline({ events, intervention }: { events: AuditEvent[]; intervention: Intervention }) {
  if (events.length === 0) return <div className="verification-boundary"><h2>Timeline unavailable</h2><p>No visible audit events are available. Current status: {label(intervention.status)}.</p></div>
  return <ol className="agency-timeline">{events.map((event) => <li key={event.eventId}><time dateTime={event.occurredAt}>{formatDate(event.occurredAt)}</time><strong>{label(event.action)}</strong><p>{event.reason ?? 'No reason recorded.'}</p></li>)}</ol>
}

function AuditView({ events }: { events: AuditEvent[] }) {
  return <div className="agency-table-wrap"><table className="agency-table"><thead><tr><th>Timestamp</th><th>Actor</th><th>Action</th><th>Before</th><th>After</th><th>Reason</th></tr></thead><tbody>{events.map((event) => <tr key={event.eventId}><td>{formatDate(event.occurredAt)}</td><td>{event.actorId ?? 'System'}</td><td>{label(event.action)}</td><td><code>{JSON.stringify(event.beforeState ?? {})}</code></td><td><code>{JSON.stringify(event.afterState ?? {})}</code></td><td>{event.reason ?? '—'}</td></tr>)}</tbody></table></div>
}

function RestrictedCopy() {
  return <div className="verification-boundary"><h2>Permission required</h2><p>This information is hidden because the signed-in officer does not hold the corresponding view permission.</p></div>
}
