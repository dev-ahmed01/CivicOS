type EmptyStateProps = {
  title: string
  description: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <div className="state-content">
        <span className="state-symbol" aria-hidden="true">0</span>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  )
}
