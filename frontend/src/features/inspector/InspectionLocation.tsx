import type { Inspection } from './types'

type Point = { x: number; y: number }

export function InspectionLocation({ inspection }: { inspection: Inspection }) {
  const points = parseWkt(inspection.geometryWkt)
  const bounds = points.length > 1 ? getBounds(points) : null
  return <section className="panel inspection-location" aria-labelledby="inspection-location-title">
    <div className="panel-header"><h2 id="inspection-location-title">Confirm location</h2><p>Stored intervention geometry; schematic only, not for navigation.</p></div>
    {bounds ? <svg viewBox="0 0 640 260" role="img" aria-label={`Stored work geometry for ${inspection.roadSegmentName}`}>
      <title>Stored intervention geometry</title><rect width="640" height="260" rx="16" />
      <polyline points={points.map((point) => project(point, bounds)).map(({ x, y }) => `${x},${y}`).join(' ')} fill="none" stroke="currentColor" strokeWidth="12" strokeLinecap="round" />
    </svg> : <p>No valid WKT line geometry is available.</p>}
    <dl className="provenance-grid"><div><dt>Road segment</dt><dd>{inspection.roadSegmentName}</dd></div><div><dt>Agency</dt><dd>{inspection.agencyName}</dd></div><div><dt>Stored geometry</dt><dd><code>{inspection.geometryWkt}</code></dd></div></dl>
  </section>
}

function parseWkt(wkt: string): Point[] {
  const matches = wkt.match(/-?\d+(?:\.\d+)?\s+-?\d+(?:\.\d+)?/g) ?? []
  return matches.map((pair) => { const [x, y] = pair.trim().split(/\s+/).map(Number); return { x, y } })
    .filter(({ x, y }) => Number.isFinite(x) && Number.isFinite(y))
}

function getBounds(points: Point[]) {
  const xs = points.map(({ x }) => x); const ys = points.map(({ y }) => y)
  return { minX: Math.min(...xs), maxX: Math.max(...xs), minY: Math.min(...ys), maxY: Math.max(...ys) }
}

function project(point: Point, bounds: ReturnType<typeof getBounds>) {
  const xRange = Math.max(bounds.maxX - bounds.minX, 0.000001); const yRange = Math.max(bounds.maxY - bounds.minY, 0.000001)
  return { x: 40 + ((point.x - bounds.minX) / xRange) * 560, y: 220 - ((point.y - bounds.minY) / yRange) * 180 }
}
