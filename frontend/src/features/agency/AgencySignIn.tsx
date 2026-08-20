import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { ApiError } from '../../lib/api/client'
import { signInAgency } from './session'
import type { CurrentUser } from './types'

type AgencySignInProps = {
  onAuthenticated: (accessToken: string, user: CurrentUser) => void
}

export function AgencySignIn({ onAuthenticated }: AgencySignInProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const session = await signInAgency(email, password)
      onAuthenticated(session.accessToken, session.user)
    } catch (failure) {
      setError(failure instanceof ApiError || failure instanceof Error
        ? failure.message
        : 'Agency sign in failed. Check your details and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="agency-access-layout">
      <section className="agency-access-intro" aria-labelledby="agency-access-title">
        <p className="eyebrow">Agency operations</p>
        <h1 id="agency-access-title">Move assigned work through governed execution.</h1>
        <p>Actions shown here remain subject to backend permissions, lifecycle rules, separation of duties, and audit.</p>
      </section>
      <form className="agency-sign-in panel" onSubmit={submit}>
        <div className="panel-header"><h2>Sign in to your agency</h2><p>Only agency-officer accounts with an agency scope can continue.</p></div>
        <div className="agency-form-stack">
          {error ? <div className="agency-alert" role="alert">{error}</div> : null}
          <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label>Password<input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <Button type="submit" disabled={submitting}>{submitting ? 'Signing in...' : 'Sign in'}</Button>
        </div>
      </form>
    </div>
  )
}
