import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { ApiError } from '../../lib/api/client'
import { signInCoordinator } from './session'
import type { CurrentUser } from './types'

type CoordinatorSignInProps = {
  onAuthenticated: (accessToken: string, user: CurrentUser) => void
}

export function CoordinatorSignIn({ onAuthenticated }: CoordinatorSignInProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const session = await signInCoordinator(email, password)
      onAuthenticated(session.accessToken, session.user)
    } catch (failure) {
      setError(failure instanceof ApiError || failure instanceof Error
        ? failure.message
        : 'Coordinator sign in failed. Check your details and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="coordinator-access-layout">
      <section className="coordinator-access-intro" aria-labelledby="coordinator-access-title">
        <p className="eyebrow">Cross-agency coordination</p>
        <h1 id="coordinator-access-title">See one physical-road problem across agency boundaries.</h1>
        <p>Deterministic conflicts lead to accountable human decisions. AI remains optional and advisory.</p>
      </section>
      <form className="coordinator-sign-in panel" onSubmit={submit}>
        <div className="panel-header"><h2>Sign in as coordinator</h2><p>Only accounts with the coordinator role can continue.</p></div>
        <div className="coordinator-form-stack">
          {error ? <div className="coordinator-alert" role="alert">{error}</div> : null}
          <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label>Password<input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <Button type="submit" disabled={submitting}>{submitting ? 'Signing in...' : 'Sign in'}</Button>
        </div>
      </form>
    </div>
  )
}
