type LoadingStateProps = {
  label: string
}

export function LoadingState({ label }: LoadingStateProps) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <div className="state-content">
        <span className="state-symbol" aria-hidden="true">...</span>
        <h3>{label}</h3>
        <p>The page remains usable while authoritative data is requested.</p>
      </div>
    </div>
  )
}
