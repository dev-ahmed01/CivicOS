import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { listEvidence, listInspections } from './api'
import { EvidenceGallery } from './EvidenceGallery'
import type { Evidence } from './types'

export function InspectorEvidencePage({ accessToken }: { accessToken: string }) {
  const [evidence, setEvidence] = useState<Evidence[] | null>(null); const [error, setError] = useState<string | null>(null)
  useEffect(() => { let active = true; Promise.all([listInspections(accessToken), listEvidence(accessToken)]).then(([tasks, records]) => { if (!active) return; const assigned = new Set(tasks.data.map(({ interventionId }) => interventionId)); setEvidence(records.data.filter(({ targetId }) => assigned.has(targetId))) }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Evidence could not be loaded.') }); return () => { active = false } }, [accessToken])
  return <><header className="inspector-page-heading"><div><p className="eyebrow">Evidence-first verification</p><h1>Evidence</h1><p>Review timestamp, location, uploader, checksum, type, and review state for records attached to assigned inspections.</p></div></header>{error ? <ErrorState title="Evidence could not be loaded" description={error} /> : null}{!error && !evidence ? <LoadingState label="Loading evidence provenance" /> : null}{evidence ? <EvidenceGallery evidence={evidence} /> : null}</>
}
