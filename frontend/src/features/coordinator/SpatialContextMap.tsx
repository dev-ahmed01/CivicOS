import type { Intervention } from './types'
import { label } from './formatters'

type Point = { x: number; y: number }

const palette = ['#0a6b68', '#b05b24', '#4f65a6', '#7b4c91', '#45733d', '#9a3f50']

export function SpatialContextMap({ interventions }: { interventions: Intervention[] }) {
  const shapes = interventions.map((item) => ({ item, points: parseWkt(item.geometryWkt) }))
    .filter(({ points }) => points.length > 1)
  const allPoints = shapes.flatMap(({ points }) => points)
  const bounds = getBounds(allPoints)

  return (
    <section className="coordinator-map-card" aria-labelledby="geometry-map-title">
      <div className="coordination-section-heading">
        <div><p className="eyebrow">Geometry-backed view</p><h3 id="geometry-map-title">Road-impact map</h3></div>
        <span>Operational schematic · not for navigation</span>
      </div>
      {shapes.length > 0 && bounds ? (
        <svg className="geometry-map" viewBox="0 0 800 360" role="img" aria-label="Geometry map of affected intervention zones">
          <title>Affected intervention geometries from CivicOS records</title>
          <rect width="800" height="360" rx="16" className="geometry-map-background" />
          {shapes.map(({ item, points }, index) => (
            <polyline
              key={item.id}
              points={points.map((point) => project(point, bounds)).map(({ x, y }) => `${x},${y}`).join(' ')}
              fill="none"
              stroke={palette[index % palette.length]}
              strokeWidth={12 - Math.min(index, 5)}
              strokeLinecap="round"
              strokeLinejoin="round"
              opacity={0.82}
            />
          ))}
        </svg>
      ) : <div className="geometry-map-empty">No valid WKT line geometry is available for this view.</div>}
      <ul className="map-equivalent-list" aria-label="Accessible intervention geometry list">
        {interventions.map((item, index) => (
          <li key={item.id}>
            <span className="map-key" style={{ backgroundColor: palette[index % palette.length] }} aria-hidden="true" />
            <strong>{item.interventionNumber} · {item.agencyName}</strong>
            <span>{label(item.type)} · {item.roadSegmentName}</span>
            <code>{item.geometryWkt}</code>
          </li>
        ))}
      </ul>
    </section>
  )
}

function parseWkt(wkt: string): Point[] {
  const matches = wkt.match(/-?\d+(?:\.\d+)?\s+-?\d+(?:\.\d+)?/g) ?? []
  return matches.map((pair) => {
    const [x, y] = pair.trim().split(/\s+/).map(Number)
    return { x, y }
  }).filter(({ x, y }) => Number.isFinite(x) && Number.isFinite(y))
}

function getBounds(points: Point[]) {
  if (points.length === 0) return null
  const xs = points.map(({ x }) => x)
  const ys = points.map(({ y }) => y)
  return { minX: Math.min(...xs), maxX: Math.max(...xs), minY: Math.min(...ys), maxY: Math.max(...ys) }
}

function project(point: Point, bounds: NonNullable<ReturnType<typeof getBounds>>) {
  const xRange = Math.max(bounds.maxX - bounds.minX, 0.000001)
  const yRange = Math.max(bounds.maxY - bounds.minY, 0.000001)
  return {
    x: 48 + ((point.x - bounds.minX) / xRange) * 704,
    y: 312 - ((point.y - bounds.minY) / yRange) * 264,
  }
}
