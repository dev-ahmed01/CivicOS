import { useEffect, useState, type FormEvent } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import { listEvidence, listInterventions, submitEvidence, uploadInterventionEvidence } from './api'
import { formatDate, label, statusTone } from './formatters'
import type { CurrentUser, Evidence, Intervention } from './types'

export function AgencyEvidencePage({ accessToken, user }: { accessToken: string; user: CurrentUser }) {
  const [interventions, setInterventions] = useState<Intervention[] | null>(null)
  const [evidence, setEvidence] = useState<Evidence[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    Promise.all([
      listInterventions(accessToken, { page: 0, size: 100 }),
      listEvidence(accessToken),
    ]).then(([work, evidenceResponse]) => {
      if (!active) return
      setInterventions(work.data)
      setEvidence(evidenceResponse.data)
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'Evidence workspace could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, refreshKey])

  return (
    <>
      <header className="agency-page-heading"><div><p className="eyebrow">Provenance-preserving records</p><h1>Evidence</h1><p>Upload required field evidence, inspect its review state, and submit it for independent review.</p></div></header>
      {error ? <ErrorState title="Evidence workspace could not be loaded" description={error} /> : null}
      {!error && (!interventions || !evidence) ? <LoadingState label="Loading agency evidence" /> : null}
      {interventions && evidence ? (
        <div className="evidence-workspace">
          {user.permissions.includes('EVIDENCE_UPLOAD') ? <EvidenceUploadForm accessToken={accessToken} interventions={interventions} onUploaded={() => setRefreshKey((value) => value + 1)} /> : null}
          <section className="panel" aria-labelledby="agency-evidence-list-title">
            <div className="panel-header"><h2 id="agency-evidence-list-title">Uploaded evidence</h2><p>{evidence.length} visible records in your agency scope.</p></div>
            {evidence.length === 0 ? <EmptyState title="No evidence uploaded" description="Evidence attached to your agency interventions will appear here." /> : (
              <div className="evidence-list">{evidence.map((item) => (
                <EvidenceCard key={item.id} accessToken={accessToken} evidence={item} user={user} intervention={interventions.find(({ id }) => id === item.targetId)} onChanged={() => setRefreshKey((value) => value + 1)} />
              ))}</div>
            )}
          </section>
        </div>
      ) : null}
    </>
  )
}

function EvidenceUploadForm(props: { accessToken: string; interventions: Intervention[]; onUploaded: () => void }) {
  const [interventionId, setInterventionId] = useState(props.interventions[0]?.id ?? '')
  const [type, setType] = useState('BEFORE_WORK')
  const [file, setFile] = useState<File | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!interventionId || !file) { setError('Select an intervention and evidence file before uploading.'); return }
    setSubmitting(true); setError(null); setSaved(false)
    try {
      await uploadInterventionEvidence(props.accessToken, interventionId, type, file)
      setSaved(true); setFile(null); props.onUploaded()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'The file was not uploaded. Your intervention was not changed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="panel evidence-upload" aria-labelledby="evidence-upload-title">
      <div className="panel-header"><h2 id="evidence-upload-title">Upload evidence</h2><p>Files are checksum-validated and stored with trusted provenance metadata.</p></div>
      <form className="agency-form-stack" onSubmit={submit}>
        {error ? <div className="agency-alert" role="alert">{error}</div> : null}
        {saved ? <div className="agency-success" role="status">Evidence uploaded. Submit the new record for independent review when ready.</div> : null}
        <label>Intervention<select value={interventionId} onChange={(event) => setInterventionId(event.target.value)}><option value="">Select intervention</option>{props.interventions.map((item) => <option key={item.id} value={item.id}>{item.interventionNumber} · {item.roadSegmentName}</option>)}</select></label>
        <label>Evidence type<select value={type} onChange={(event) => setType(event.target.value)}>{['BEFORE_WORK', 'DURING_WORK', 'COMPLETION', 'RESTORATION', 'DOCUMENT'].map((value) => <option key={value} value={value}>{label(value)}</option>)}</select></label>
        <label>Evidence file<input type="file" accept="image/*,application/pdf" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /><small>{file?.name ?? 'Choose a photo or document'}</small></label>
        <Button type="submit" disabled={submitting || props.interventions.length === 0}>{submitting ? 'Uploading evidence...' : 'Upload evidence'}</Button>
      </form>
    </section>
  )
}

function EvidenceCard(props: { accessToken: string; evidence: Evidence; user: CurrentUser; intervention?: Intervention; onChanged: () => void }) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const canSubmit = props.evidence.status === 'UPLOADED'
    && props.evidence.uploadedBy === props.user.id
    && props.user.permissions.includes('EVIDENCE_UPLOAD')

  async function submitForReview() {
    setSubmitting(true); setError(null)
    try {
      await submitEvidence(props.accessToken, props.evidence, 'Agency evidence ready for independent review.')
      props.onChanged()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Evidence remains uploaded and was not submitted for review.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <article className="evidence-card">
      <div><p className="eyebrow">{props.intervention?.interventionNumber ?? props.evidence.targetId}</p><h3>{label(props.evidence.type)}</h3><p>{props.evidence.originalFilename ?? 'Stored evidence file'}</p><small>Captured {formatDate(props.evidence.capturedAt)} · Uploaded {formatDate(props.evidence.createdAt)}</small></div>
      <div className="evidence-card-actions"><StatusIndicator label={label(props.evidence.status)} tone={statusTone(props.evidence.status)} />{canSubmit ? <Button variant="secondary" type="button" disabled={submitting} onClick={submitForReview}>{submitting ? 'Submitting...' : 'Submit for review'}</Button> : null}{error ? <span className="inline-error" role="alert">{error}</span> : null}</div>
    </article>
  )
}
