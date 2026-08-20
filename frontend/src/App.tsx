import './App.css'

const foundationCapabilities = [
  'Cross-agency visibility',
  'Deterministic conflict detection',
  'Evidence-backed verification',
] as const

function App() {
  return (
    <main className="foundation-shell">
      <section className="foundation-hero" aria-labelledby="product-title">
        <p className="eyebrow">CivicOS · Bengaluru road-work coordination</p>
        <h1 id="product-title">Coordinate before the road is cut again.</h1>
        <p className="product-summary">
          CivicOS connects road-impacting interventions across agencies, surfaces
          coordination risks, and preserves the path from decision to verified
          outcome.
        </p>

        <ul className="capability-list" aria-label="Core product capabilities">
          {foundationCapabilities.map((capability) => (
            <li key={capability}>{capability}</li>
          ))}
        </ul>
      </section>

      <aside className="foundation-status" aria-labelledby="foundation-title">
        <p className="status-label">Implementation status</p>
        <h2 id="foundation-title">Phase 1 foundation</h2>
        <p>
          The reproducible application, configuration, and PostGIS development
          environment are ready. Operational workflows are introduced in the
          specification&apos;s required phase order.
        </p>
      </aside>
    </main>
  )
}

export default App
