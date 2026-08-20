import { useState, type FormEvent } from 'react'
import { ApiError } from '../../lib/api/client'
import { Button } from '../../components/ui/Button'
import { signInCitizen } from './session'

type CitizenSignInProps = {
  onAuthenticated: (accessToken: string) => void
}

export function CitizenSignIn({ onAuthenticated }: CitizenSignInProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      onAuthenticated(await signInCitizen(email, password))
    } catch (failure) {
      setError(failure instanceof ApiError || failure instanceof Error
        ? failure.message
        : 'Sign in failed. Check your details and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="citizen-access-layout">
      <section className="citizen-intro" aria-labelledby="citizen-access-title">
        <p className="eyebrow">Citizen access</p>
        <h1 id="citizen-access-title">Report it. Track it. Verify the outcome.</h1>
        <p>
          Your report becomes an observation inside a coordinated civic case. It does not bypass
          official inspection or approval rules.
        </p>
      </section>
      <form className="citizen-sign-in panel" onSubmit={submit}>
        <div className="panel-header">
          <h2>Sign in to your reports</h2>
          <p>Use your CivicOS citizen account. Credentials are sent only to the configured API.</p>
        </div>
        <div className="form-stack">
          {error ? <div className="form-alert" role="alert">{error}</div> : null}
          <label>
            Email address
            <input
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Sign in'}
          </Button>
        </div>
      </form>
    </div>
  )
}
