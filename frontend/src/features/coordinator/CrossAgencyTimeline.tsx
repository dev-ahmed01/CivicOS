import type { Intervention } from './types'
import { formatDate, label } from './formatters'

export function CrossAgencyTimeline({ interventions }: { interventions: Intervention[] }) {
  if (interventions.length === 0) return null
  const starts = interventions.map(({ plannedStart }) => new Date(plannedStart).getTime())
  const ends = interventions.map(({ plannedEnd }) => new Date(plannedEnd).getTime())
  const minimum = Math.min(...starts)
  const maximum = Math.max(...ends)
  const duration = Math.max(maximum - minimum, 1)
  const overlapStart = Math.max(...starts)
  const overlapEnd = Math.min(...ends)
  const hasSharedOverlap = overlapStart <= overlapEnd

  return (
    <section className="coordination-timeline" aria-labelledby="cross-agency-timeline-title">
      <div className="coordination-section-heading">
        <div><p className="eyebrow">Who · where · when</p><h3 id="cross-agency-timeline-title">Cross-agency timeline</h3></div>
        <span>{formatDate(new Date(minimum).toISOString())} — {formatDate(new Date(maximum).toISOString())}</span>
      </div>
      <div className="timeline-grid">
        {interventions.map((item) => {
          const left = ((new Date(item.plannedStart).getTime() - minimum) / duration) * 100
          const width = Math.max(((new Date(item.plannedEnd).getTime() - new Date(item.plannedStart).getTime()) / duration) * 100, 4)
          return (
            <div className="timeline-row" key={item.id}>
              <div className="timeline-label"><strong>{item.agencyName}</strong><span>{item.interventionNumber} · {label(item.type)}</span></div>
              <div className="timeline-track">
                <span
                  className="timeline-bar"
                  style={{ left: `${left}%`, width: `${Math.min(width, 100 - left)}%` }}
                  aria-label={`${item.agencyName}: ${formatDate(item.plannedStart)} to ${formatDate(item.plannedEnd)}`}
                >{item.interventionNumber}</span>
              </div>
            </div>
          )
        })}
      </div>
      <p className="timeline-fact">
        <strong>{hasSharedOverlap ? 'Shared overlap detected.' : 'Sequence risk detected.'}</strong>{' '}
        All affected work references the same CivicOS road segment; the conflict explanation remains authoritative.
      </p>
    </section>
  )
}
