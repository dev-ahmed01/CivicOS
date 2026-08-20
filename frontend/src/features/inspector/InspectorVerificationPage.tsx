import { useEffect, useState, type FormEvent } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import { listEvidence, listInspections, recordVerification } from './api'
import { formatDate, label, tone } from './formatters'
import type { CurrentUser, Evidence, Inspection } from './types'

export function InspectorVerificationPage({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [data, setData] = useState<{ inspections: Inspection[]; evidence: Evidence[] } | null>(null)
  const [error, setError] = useState<string | null>(null); const [refreshKey, setRefreshKey] = useState(0)
  useEffect(() => { let active = true; Promise.all([listInspections(accessToken), listEvidence(accessToken)]).then(([tasks, evidence]) => { if (active) setData({ inspections: tasks.data.filter((item) => item.status === 'COMPLETED' && item.interventionStatus === 'VERIFICATION_PENDING'), evidence: evidence.data }) }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Verification backlog could not be loaded.') }); return () => { active = false } }, [accessToken, refreshKey])
  return <><header className="inspector-page-heading"><div><p className="eyebrow">Authoritative human decision</p><h1>Verification</h1><p>Compare expected work with accepted evidence and the completed inspection. AI observations, when available, are advisory and cannot invoke verification.</p></div></header>
    <aside className="verification-boundary-note"><strong>Decision boundary</strong><span>Only your explicit action is submitted; CivicOS enforces assignment, compatible results, required accepted evidence, version, and separation of duties.</span></aside>
    {error ? <ErrorState title="Verification backlog could not be loaded" description={error} /> : null}{!error && !data ? <LoadingState label="Loading verification backlog" /> : null}
    {data ? data.inspections.length === 0 ? <EmptyState title="No verification decisions pending" description="Completed assigned inspections awaiting an official decision will appear here." /> : <div className="verification-list">{data.inspections.map((inspection) => <VerificationCard key={inspection.id} accessToken={accessToken} inspection={inspection} evidence={data.evidence.filter(({ targetId }) => targetId === inspection.interventionId)} canDecide={inspection.result === 'PASSED' ? user.permissions.includes('VERIFICATION_PASS') : user.permissions.includes('VERIFICATION_FAIL')} onChanged={() => setRefreshKey((value) => value + 1)} />)}</div> : null}</>
}

function VerificationCard({ accessToken, inspection, evidence, canDecide, onChanged }: { accessToken: string; inspection: Inspection; evidence: Evidence[]; canDecide: boolean; onChanged: () => void }) {
  const [reason, setReason] = useState(inspection.notes ?? ''); const [submitting, setSubmitting] = useState(false); const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  const accepted = evidence.filter(({ status }) => status === 'ACCEPTED')
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!inspection.result) return; if (inspection.result !== 'PASSED' && !reason.trim()) { setMessage({ text: 'A reason is required for a non-passing verification.', error: true }); return } setSubmitting(true); setMessage(null); try { await recordVerification(accessToken, inspection, inspection.result, reason); setMessage({ text: 'Official verification recorded and the backend applied the lifecycle transition.', error: false }); onChanged() } catch (failure) { setMessage({ text: failure instanceof Error ? failure.message : 'Verification was not saved. The inspection and evidence remain unchanged.', error: true }) } finally { setSubmitting(false) } }
  return <article className="panel verification-card"><div className="panel-header"><div><p className="eyebrow">{inspection.interventionNumber} · {inspection.agencyName}</p><h2>{inspection.roadSegmentName}</h2></div><StatusIndicator label={label(inspection.result)} tone={tone(inspection.result)} /></div>
    <div className="expected-observed"><section><h3>Expected state</h3><p>{inspection.interventionDescription}</p><small>Planned through {formatDate(inspection.plannedEnd)}</small></section><section><h3>Observed evidence</h3><p>{accepted.length} accepted of {evidence.length} visible records.</p><p>Inspection: {label(inspection.result)} · {inspection.notes ?? 'No notes'}</p></section></div>
    <aside className="ai-advisory"><strong>AI observations</strong><span>No advisory AI observation was requested for this decision. Official findings above remain human-authored.</span></aside>
    {canDecide ? <form className="inspector-form-stack" onSubmit={submit}><label>Decision reason<textarea rows={3} value={reason} onChange={(event) => setReason(event.target.value)} required={inspection.result !== 'PASSED'} /></label>{message ? <div className={message.error ? 'inspector-alert' : 'inspector-success'} role={message.error ? 'alert' : 'status'}>{message.text}</div> : null}<Button type="submit" disabled={submitting}>{submitting ? 'Recording verification...' : inspection.result === 'PASSED' ? 'Verify complete' : inspection.result === 'FAILED' ? 'Reject completion' : 'Request corrective work'}</Button></form> : <p className="inspector-alert">Verification permission is required to submit this decision.</p>}
  </article>
}
