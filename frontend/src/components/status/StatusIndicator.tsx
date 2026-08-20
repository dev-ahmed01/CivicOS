type StatusTone = 'info' | 'success' | 'warning' | 'danger'

type StatusIndicatorProps = {
  label: string
  tone?: StatusTone
}

export function StatusIndicator({ label, tone = 'info' }: StatusIndicatorProps) {
  return <span className="status-indicator" data-tone={tone} role="status">{label}</span>
}
