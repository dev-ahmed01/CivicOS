type ErrorStateProps = {
  title: string
  description: string
  saved?: boolean
}

export function ErrorState({ title, description, saved = false }: ErrorStateProps) {
  return (
    <div className="error-state" role="alert">
      <div className="state-content">
        <span className="state-symbol" aria-hidden="true">!</span>
        <h3>{title}</h3>
        <p>{description}</p>
        <p>{saved ? 'Your previous changes were saved.' : 'No changes were saved.'}</p>
      </div>
    </div>
  )
}
