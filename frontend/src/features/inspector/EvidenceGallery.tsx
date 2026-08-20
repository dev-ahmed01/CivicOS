import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { formatDate, label, tone } from './formatters'
import type { Evidence } from './types'

export function EvidenceGallery({ evidence, title = 'Observed evidence' }: { evidence: Evidence[]; title?: string }) {
  return <section className="panel evidence-gallery" aria-labelledby="inspector-evidence-title">
    <div className="panel-header"><h2 id="inspector-evidence-title">{title}</h2><p>Before, during, after, inspection, and citizen records retain their original provenance.</p></div>
    {evidence.length === 0 ? <EmptyState title="No visible evidence" description="Evidence attached to your assigned inspections will appear here." /> : <div className="inspector-card-list">
      {evidence.map((item) => <article className="inspector-evidence-card" key={item.id}>
        <div><p className="eyebrow">{label(item.type)}</p><h3>{item.originalFilename ?? 'Stored evidence file'}</h3><p>Captured {formatDate(item.capturedAt)} · Uploaded {formatDate(item.createdAt)}</p></div>
        <StatusIndicator label={label(item.status)} tone={tone(item.status)} />
        <dl className="provenance-grid">
          <div><dt>Uploaded by</dt><dd>{item.uploadedBy}</dd></div><div><dt>Location</dt><dd>{item.latitude != null && item.longitude != null ? `${item.latitude}, ${item.longitude}` : 'Not captured'}</dd></div>
          <div><dt>Checksum</dt><dd><code>{item.checksum ?? 'Pending'}</code></dd></div><div><dt>Provenance</dt><dd>{label(String(item.metadata.provenance ?? 'Recorded'))}</dd></div>
        </dl>
      </article>)}
    </div>}
  </section>
}
