import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { listMyObservations } from './api'
import { categoryLabel, formatDate, statusLabel, statusTone } from './formatters'
import type { CitizenObservation } from './types'

type CitizenReportsPageProps = {
  accessToken: string
}

export function CitizenReportsPage({ accessToken }: CitizenReportsPageProps) {
  const [reports, setReports] = useState<CitizenObservation[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    listMyObservations(accessToken)
      .then((response) => {
        if (!controller.signal.aborted) setReports(response.data)
      })
      .catch((failure: unknown) => {
        if (!controller.signal.aborted) {
          setError(failure instanceof Error ? failure.message : 'Your reports could not be loaded.')
        }
      })
    return () => controller.abort()
  }, [accessToken])

  return (
    <>
      <header className="citizen-page-heading">
        <div>
          <p className="eyebrow">Citizen tracking</p>
          <h1>My reports</h1>
          <p>Public-safe updates from receipt through verified completion.</p>
        </div>
        <a className="button button-primary" href="/app/citizen/report">Report an issue</a>
      </header>
      <section className="panel" aria-labelledby="citizen-reports-title">
        <div className="panel-header">
          <h2 id="citizen-reports-title">Submitted reports</h2>
          <p>Only reports submitted by your citizen account are shown.</p>
        </div>
        {error ? <ErrorState title="Reports could not be loaded" description={error} /> : null}
        {!error && reports === null ? <LoadingState label="Loading your reports" /> : null}
        {!error && reports?.length === 0 ? (
          <EmptyState title="No reports yet" description="Report a road issue to receive a tracking ID and public updates." />
        ) : null}
        {reports && reports.length > 0 ? (
          <div className="report-list">
            {reports.map((report) => (
              <article className="report-list-item" key={report.observationId}>
                <div>
                  <p className="report-id">{report.trackingId}</p>
                  <h3>{categoryLabel(report.category)}</h3>
                  <p>{report.detectedRoad.name}</p>
                </div>
                <div className="report-list-status">
                  <StatusIndicator label={statusLabel(report.publicStatus)} tone={statusTone(report.publicStatus)} />
                  <time dateTime={report.submittedAt}>{formatDate(report.submittedAt)}</time>
                  <a href={`/app/citizen/reports/${report.observationId}`}>View report</a>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </>
  )
}
