import { useEffect, useState, type FormEvent } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import {
  decideApproval,
  getIntervention,
  listApprovals,
  listConflicts,
  listDependencies,
  listEvidence,
  listSlas,
} from './api'
import { formatDate, label, slaLabel } from './formatters'
import type { Approval, Conflict, CurrentUser, Dependency, Evidence, Intervention, Sla } from './types'

type ApprovalContext = {
  intervention: Intervention
  conflicts: Conflict[]
  dependencies: Dependency[]
  evidence: Evidence[]
  slas: Sla[]
}

export function AgencyApprovalsPage({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [approvals, setApprovals] = useState<Approval[] | null>(null)
  const [selected, setSelected] = useState<Approval | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    listApprovals(accessToken, 'PENDING', 100)
      .then((response) => {
        if (!active) return
        setApprovals(response.data)
        setSelected((current) => response.data.find(({ id }) => id === current?.id) ?? response.data[0] ?? null)
      })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'The approval queue could not be loaded.')
      })
    return () => { active = false }
  }, [accessToken, refreshKey])

  return (
    <>
      <header className="agency-page-heading"><div><p className="eyebrow">Authoritative decisions</p><h1>Approvals</h1><p>Review scope, dependencies, conflict risk, evidence, SLA, and consequences before deciding.</p></div></header>
      {error ? <ErrorState title="Approval queue could not be loaded" description={error} /> : null}
      {!error && approvals === null ? <LoadingState label="Loading pending approvals" /> : null}
      {!error && approvals?.length === 0 ? <EmptyState title="No approvals pending" description="New requests within your agency scope will appear here." /> : null}
      {approvals && approvals.length > 0 ? (
        <div className="approval-workspace">
          <section className="panel approval-queue" aria-labelledby="approval-queue-title">
            <div className="panel-header"><h2 id="approval-queue-title">Pending queue</h2><p>{approvals.length} requests awaiting a decision.</p></div>
            <ul>{approvals.map((approval) => <li key={approval.id}><button type="button" aria-pressed={selected?.id === approval.id} onClick={() => setSelected(approval)}><strong>{approval.interventionNumber}</strong><span>Requested {formatDate(approval.createdAt)}</span><StatusIndicator label="Pending" tone="warning" /></button></li>)}</ul>
          </section>
          {selected ? <ApprovalDecisionPanel key={selected.id} accessToken={accessToken} approval={selected} user={user} onDecided={() => setRefreshKey((value) => value + 1)} /> : null}
        </div>
      ) : null}
    </>
  )
}

function ApprovalDecisionPanel(props: { accessToken: string; approval: Approval; user: CurrentUser; onDecided: () => void }) {
  const [context, setContext] = useState<ApprovalContext | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    getIntervention(props.accessToken, props.approval.interventionId)
      .then(async (intervention) => {
        const can = (permission: string) => props.user.permissions.includes(permission)
        const [conflicts, dependencies, evidence, slas] = await Promise.all([
          can('CONFLICT_VIEW') ? listConflicts(props.accessToken, undefined, intervention.roadSegmentId, 100) : Promise.resolve(null),
          listDependencies(props.accessToken, intervention.id),
          can('EVIDENCE_VIEW') ? listEvidence(props.accessToken, [intervention.id]) : Promise.resolve(null),
          can('SLA_VIEW') ? listSlas(props.accessToken, [intervention.id]) : Promise.resolve(null),
        ])
        if (active) setContext({
          intervention,
          conflicts: conflicts?.data.filter((item) => item.interventionIds.includes(intervention.id)) ?? [],
          dependencies,
          evidence: evidence?.data ?? [],
          slas: slas?.data ?? [],
        })
      })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'Approval context could not be loaded.')
      })
    return () => { active = false }
  }, [props.accessToken, props.approval.interventionId, props.user.permissions])

  if (error) return <ErrorState title="Approval context could not be loaded" description={error} />
  if (!context) return <LoadingState label="Loading approval context" />

  const activeSla = context.slas.find(({ status }) => !['COMPLETED'].includes(status))
  return (
    <section className="panel approval-decision" aria-labelledby="approval-decision-title">
      <div className="panel-header"><p className="eyebrow">What am I approving?</p><h2 id="approval-decision-title">{context.intervention.interventionNumber} · {label(context.intervention.type)}</h2><p>{context.intervention.description}</p></div>
      <dl className="approval-context-grid">
        <div><dt>Road</dt><dd>{context.intervention.roadSegmentName}</dd></div>
        <div><dt>Planned window</dt><dd>{formatDate(context.intervention.plannedStart)}<small>to {formatDate(context.intervention.plannedEnd)}</small></dd></div>
        <div><dt>Risk/conflicts</dt><dd>{context.conflicts.length} visible<small>{context.conflicts.filter(({ severity }) => severity === 'HIGH' || severity === 'CRITICAL').length} high severity</small></dd></div>
        <div><dt>Dependencies</dt><dd>{context.dependencies.length}<small>{context.dependencies.filter(({ required }) => required).length} required</small></dd></div>
        <div><dt>Evidence</dt><dd>{context.evidence.length} items<small>{context.evidence.filter(({ status }) => status === 'ACCEPTED').length} accepted</small></dd></div>
        <div><dt>SLA</dt><dd>{slaLabel(activeSla)}<small>{activeSla ? label(activeSla.status) : 'No active SLA'}</small></dd></div>
      </dl>
      <div className="approval-consequence"><strong>What happens after approval?</strong><p>An authoritative approval advances the intervention to APPROVED. It does not start work; scheduling remains a separate lifecycle action.</p></div>
      {context.intervention.createdBy === props.user.id ? (
        <div className="agency-alert" role="status"><strong>Separation of duties applies.</strong> The officer who created this intervention cannot approve it.</div>
      ) : (
        <ApprovalForm {...props} />
      )}
    </section>
  )
}

function ApprovalForm(props: { accessToken: string; approval: Approval; user: CurrentUser; onDecided: () => void }) {
  const [decision, setDecision] = useState('APPROVE')
  const [reason, setReason] = useState('')
  const [condition, setCondition] = useState('')
  const [owner, setOwner] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const options = [
    ['APPROVE', 'Approve', 'APPROVAL_APPROVE'],
    ['APPROVE_WITH_CONDITIONS', 'Approve with conditions', 'APPROVAL_CONDITIONAL'],
    ['REJECT', 'Reject', 'APPROVAL_REJECT'],
    ['RETURN', 'Return for correction', 'APPROVAL_RETURN'],
  ].filter(([, , permission]) => props.user.permissions.includes(permission))
  const selectedDecision = options.some(([value]) => value === decision) ? decision : options[0]?.[0] ?? ''

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!reason.trim()) { setError('Record the reason for this authoritative decision.'); return }
    if (selectedDecision === 'APPROVE_WITH_CONDITIONS' && (!condition.trim() || !owner.trim() || !dueDate)) {
      setError('Conditional approval requires a condition, owner, and due date.'); return
    }
    setSubmitting(true); setError(null)
    try {
      const conditions = selectedDecision === 'APPROVE_WITH_CONDITIONS'
        ? [{ condition: condition.trim(), owner: owner.trim(), dueDate, status: 'PENDING' }]
        : []
      await decideApproval(props.accessToken, props.approval, selectedDecision, reason.trim(), conditions)
      props.onDecided()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'The approval decision was not saved.')
    } finally {
      setSubmitting(false)
    }
  }

  if (options.length === 0) return <div className="verification-boundary"><p>You can view this request but do not hold an approval-decision permission.</p></div>

  return (
    <form className="approval-form" onSubmit={submit}>
      {error ? <div className="agency-alert" role="alert">{error}</div> : null}
      <label>Decision<select value={selectedDecision} onChange={(event) => setDecision(event.target.value)}>{options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></label>
      <label>Decision reason<textarea rows={4} value={reason} onChange={(event) => setReason(event.target.value)} /></label>
      {selectedDecision === 'APPROVE_WITH_CONDITIONS' ? <div className="condition-grid"><label>Condition<input value={condition} onChange={(event) => setCondition(event.target.value)} /></label><label>Owner<input value={owner} onChange={(event) => setOwner(event.target.value)} /></label><label>Due date<input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} /></label></div> : null}
      <Button type="submit" disabled={submitting}>{submitting ? 'Recording decision...' : options.find(([value]) => value === selectedDecision)?.[1]}</Button>
    </form>
  )
}
