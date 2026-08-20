import { useEffect, useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { getObservation, validateObservation } from './api'
import { categoryLabel, formatDate, statusLabel, statusTone } from './formatters'
import type { CitizenObservation, PublicReportStatus } from './types'

type CitizenReportDetailProps = {
  accessToken: string
  observationId: string
}

const timeline: ReadonlyArray<{ status: PublicReportStatus; label: string }> = [
  { status: 'REPORT_RECEIVED', label: 'Report received' },
  { status: 'UNDER_REVIEW', label: 'Under review' },
  { status: 'MATCHED_TO_WORK', label: 'Matched to work' },
  { status: 'ACTION_ASSIGNED', label: 'Action assigned' },
  { status: 'WORK_IN_PROGRESS', label: 'Work in progress' },
  { status: 'VERIFICATION', label: 'Verification' },
  { status: 'COMPLETED', label: 'Completed' },
]

export function CitizenReportDetail({ accessToken, observationId }: CitizenReportDetailProps) {
  const [report, setReport] = useState<CitizenObservation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [validationSaved, setValidationSaved] = useState(false)

  useEffect(() => {
    let active = true
    getObservation(accessToken, observationId)
      .then((response) => { if (active) setReport(response) })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'The report could not be loaded.')
      })
    return () => { active = false }
  }, [accessToken, observationId])

  if (error) return <ErrorState title="Report could not be loaded" description={error} />
  if (!report) return <LoadingState label="Loading report details" />

  return (
    <>
      <header className="citizen-page-heading">
        <div>
          <p className="eyebrow">Tracking ID {report.trackingId}</p>
          <h1>{categoryLabel(report.category)}</h1>
          <p>{report.detectedRoad.name}</p>
        </div>
        <StatusIndicator label={statusLabel(report.publicStatus)} tone={statusTone(report.publicStatus)} />
      </header>

      <div className="report-detail-grid">
        <section className="panel report-summary" aria-labelledby="report-summary-title">
          <div className="panel-header"><h2 id="report-summary-title">Report details</h2></div>
          <dl className="detail-list">
            <div><dt>Status</dt><dd>{report.publicMessage}</dd></div>
            <div><dt>Location</dt><dd>{report.detectedRoad.name}<small>{report.latitude.toFixed(5)}, {report.longitude.toFixed(5)}</small></dd></div>
            <div><dt>Issue</dt><dd>{report.description}</dd></div>
            <div><dt>Photos</dt><dd>Evidence attachments are stored with provenance and are not used as official inspection findings.</dd></div>
            <div><dt>Submitted</dt><dd><time dateTime={report.submittedAt}>{formatDate(report.submittedAt)}</time></dd></div>
          </dl>
        </section>

        <section className="panel" aria-labelledby="report-timeline-title">
          <div className="panel-header">
            <h2 id="report-timeline-title">Public timeline</h2>
            <p>Internal approvals, notes, and actor details are intentionally excluded.</p>
          </div>
          <ReportTimeline currentStatus={report.publicStatus} submittedAt={report.submittedAt} />
        </section>
      </div>

      {report.publicStatus === 'COMPLETED' ? (
        <CitizenResolutionForm
          accessToken={accessToken}
          observationId={report.observationId}
          saved={validationSaved}
          onSaved={() => setValidationSaved(true)}
        />
      ) : null}
    </>
  )
}

function ReportTimeline({ currentStatus, submittedAt }: { currentStatus: PublicReportStatus; submittedAt: string }) {
  const currentIndex = currentStatus === 'CLOSED'
    ? timeline.length - 1
    : timeline.findIndex(({ status }) => status === currentStatus)
  return (
    <ol className="public-timeline">
      {timeline.map((item, index) => {
        const state = index < currentIndex ? 'complete' : index === currentIndex ? 'current' : 'upcoming'
        return (
          <li key={item.status} data-state={state}>
            <span className="timeline-dot" aria-hidden="true" />
            <div>
              <strong>{item.label}</strong>
              <span>{state === 'complete' ? 'Completed' : state === 'current' ? 'Current status' : 'Upcoming'}</span>
              {index === 0 ? <time dateTime={submittedAt}>{formatDate(submittedAt)}</time> : null}
            </div>
          </li>
        )
      })}
    </ol>
  )
}

type CitizenResolutionFormProps = {
  accessToken: string
  observationId: string
  saved: boolean
  onSaved: () => void
}

function CitizenResolutionForm(props: CitizenResolutionFormProps) {
  const [decision, setDecision] = useState<'LOOKS_RESOLVED' | 'STILL_UNRESOLVED'>('LOOKS_RESOLVED')
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await validateObservation(props.accessToken, props.observationId, { decision, reason })
      props.onSaved()
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Your feedback could not be recorded.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="resolution-panel panel" aria-labelledby="citizen-resolution-title">
      <div className="panel-header">
        <p className="eyebrow">Supporting verification signal</p>
        <h2 id="citizen-resolution-title">We believe this issue has been resolved.</h2>
        <p>Your feedback supports review and does not directly reopen or close the official case.</p>
      </div>
      {props.saved ? (
        <div className="resolution-saved" role="status">Thank you. Your resolution feedback has been recorded.</div>
      ) : (
        <form className="form-stack" onSubmit={submit}>
          {error ? <div className="form-alert" role="alert">{error}</div> : null}
          <fieldset>
            <legend>What can you see now?</legend>
            <label><input type="radio" name="resolution" checked={decision === 'LOOKS_RESOLVED'} onChange={() => setDecision('LOOKS_RESOLVED')} /> Looks resolved</label>
            <label><input type="radio" name="resolution" checked={decision === 'STILL_UNRESOLVED'} onChange={() => setDecision('STILL_UNRESOLVED')} /> Still unresolved</label>
          </fieldset>
          {decision === 'STILL_UNRESOLVED' ? (
            <label>What remains unresolved?<textarea rows={4} required value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          ) : null}
          <Button type="submit" disabled={submitting}>{submitting ? 'Recording feedback...' : 'Submit feedback'}</Button>
        </form>
      )}
    </section>
  )
}
