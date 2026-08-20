import { useEffect, useState, type FormEvent } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import { completeInspection, getInspection, listEvidence, startInspection } from './api'
import { EvidenceCapture } from './EvidenceCapture'
import { EvidenceGallery } from './EvidenceGallery'
import { formatDate, label, tone } from './formatters'
import { InspectionLocation } from './InspectionLocation'
import type { CurrentUser, Evidence, Inspection } from './types'

const checklist = [
  'Work completed in approved area', 'Road restored', 'Work zone cleared',
  'No visible obstruction', 'Required evidence captured',
] as const

export function InspectorInspectionDetail({ accessToken, inspectionId, user }: { accessToken: string; inspectionId: string; user: CurrentUser }) {
  const [inspection, setInspection] = useState<Inspection | null>(null)
  const [evidence, setEvidence] = useState<Evidence[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    Promise.all([getInspection(accessToken, inspectionId), listEvidence(accessToken)]).then(([task, records]) => {
      if (!active) return
      setInspection(task); setEvidence(records.data.filter(({ targetId }) => targetId === task.interventionId))
    }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Inspection could not be loaded.') })
    return () => { active = false }
  }, [accessToken, inspectionId, refreshKey])

  if (error) return <ErrorState title="Inspection could not be loaded" description={error} />
  if (!inspection || !evidence) return <LoadingState label="Loading inspection record" />
  return <>
    <header className="inspector-page-heading"><div><p className="eyebrow">{inspection.interventionNumber} · assigned inspection</p><h1>{inspection.roadSegmentName}</h1><p>{inspection.interventionDescription}</p></div><StatusIndicator label={label(inspection.status)} tone={tone(inspection.status)} /></header>
    <nav className="inspection-steps" aria-label="Inspection steps"><a href="#location">Location</a><a href="#expected-work">Expected work</a><a href="#checklist">Checklist</a><a href="#evidence">Evidence</a><a href="#result">Result</a></nav>
    <div id="location"><InspectionLocation inspection={inspection} /></div>
    <section id="expected-work" className="panel"><div className="panel-header"><h2>Review expected work</h2><p>Authoritative intervention scope supplied by the backend.</p></div><dl className="provenance-grid"><div><dt>Type</dt><dd>{label(inspection.interventionType)}</dd></div><div><dt>Agency</dt><dd>{inspection.agencyName}</dd></div><div><dt>Planned window</dt><dd>{formatDate(inspection.plannedStart)} — {formatDate(inspection.plannedEnd)}</dd></div><div><dt>Lifecycle state</dt><dd>{label(inspection.interventionStatus)}</dd></div></dl></section>
    {inspection.status === 'SCHEDULED' && user.permissions.includes('INSPECTION_CREATE') ? <StartInspection accessToken={accessToken} inspection={inspection} onChanged={() => setRefreshKey((value) => value + 1)} /> : null}
    <div id="checklist">{user.permissions.includes('INSPECTION_COMPLETE') || inspection.status === 'COMPLETED' ? <InspectionResultForm accessToken={accessToken} inspection={inspection} onChanged={() => setRefreshKey((value) => value + 1)} /> : <PermissionBoundary text="Inspection completion permission is required to record a result." />}</div>
    <div id="evidence">{user.permissions.includes('EVIDENCE_UPLOAD') ? <EvidenceCapture accessToken={accessToken} interventionId={inspection.interventionId} onUploaded={() => setRefreshKey((value) => value + 1)} /> : <PermissionBoundary text="Evidence upload permission is required to capture new proof." />}<EvidenceGallery evidence={evidence} title="Inspection evidence and provenance" /></div>
  </>
}

function PermissionBoundary({ text }: { text: string }) { return <section className="panel"><div className="panel-header"><h2>Permission required</h2><p>{text}</p></div></section> }

function StartInspection({ accessToken, inspection, onChanged }: { accessToken: string; inspection: Inspection; onChanged: () => void }) {
  const [submitting, setSubmitting] = useState(false); const [error, setError] = useState<string | null>(null)
  async function start() { setSubmitting(true); setError(null); try { await startInspection(accessToken, inspection); onChanged() } catch (failure) { setError(failure instanceof Error ? failure.message : 'Inspection was not started. No record was changed.') } finally { setSubmitting(false) } }
  return <section className="panel start-inspection"><div><h2>Ready to inspect this location?</h2><p>Starting creates an audited, versioned state change.</p></div>{error ? <div className="inspector-alert" role="alert">{error}</div> : null}<Button type="button" onClick={start} disabled={submitting}>{submitting ? 'Starting inspection...' : 'Start inspection'}</Button></section>
}

function InspectionResultForm({ accessToken, inspection, onChanged }: { accessToken: string; inspection: Inspection; onChanged: () => void }) {
  const [checks, setChecks] = useState<Record<string, boolean>>({})
  const [result, setResult] = useState('PASSED'); const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false); const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  const allChecked = checklist.every((item) => checks[item])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (result === 'PASSED' && !allChecked) { setMessage({ text: 'Confirm every checklist item before recording a passing result.', error: true }); return }
    if (result !== 'PASSED' && !notes.trim()) { setMessage({ text: 'Observations are required for a non-passing result.', error: true }); return }
    setSubmitting(true); setMessage(null)
    try { await completeInspection(accessToken, inspection, result, notes); setMessage({ text: 'Official inspection result submitted. Verification remains a separate human decision.', error: false }); onChanged() }
    catch (failure) { setMessage({ text: failure instanceof Error ? failure.message : 'The result was not saved. Evidence already uploaded remains saved.', error: true }) }
    finally { setSubmitting(false) }
  }
  if (inspection.status === 'COMPLETED') return <section id="result" className="panel"><div className="panel-header"><h2>Inspection result</h2><p>Completed records are immutable.</p></div><StatusIndicator label={label(inspection.result)} tone={tone(inspection.result)} /><p>{inspection.notes ?? 'No additional observations recorded.'}</p></section>
  return <section id="result" className="panel"><div className="panel-header"><h2>Checklist and result</h2><p>This field review supports the official result; the backend stores the authoritative result, notes, evidence, and audit trail.</p></div>
    <form className="inspector-form-stack" onSubmit={submit}>{checklist.map((item) => <label className="check-row" key={item}><input type="checkbox" checked={Boolean(checks[item])} onChange={(event) => setChecks((current) => ({ ...current, [item]: event.target.checked }))} />{item}</label>)}
      <label>Inspection result<select value={result} onChange={(event) => setResult(event.target.value)}><option value="PASSED">Pass</option><option value="FAILED">Fail</option><option value="CONDITIONAL">Needs correction / conditional</option></select></label>
      <label>Field observations<textarea rows={4} value={notes} onChange={(event) => setNotes(event.target.value)} required={result !== 'PASSED'} /></label>
      {message ? <div className={message.error ? 'inspector-alert' : 'inspector-success'} role={message.error ? 'alert' : 'status'}>{message.text}</div> : null}
      <Button type="submit" disabled={inspection.status !== 'IN_PROGRESS' || submitting}>{submitting ? 'Submitting result...' : 'Submit inspection result'}</Button>
    </form>
  </section>
}
