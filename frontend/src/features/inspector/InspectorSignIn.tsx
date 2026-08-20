import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { signInInspector } from './session'
import type { CurrentUser } from './types'

export function InspectorSignIn({ onAuthenticated }: { onAuthenticated: (token: string, user: CurrentUser) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSubmitting(true); setError(null)
    try {
      const session = await signInInspector(email, password)
      onAuthenticated(session.accessToken, session.user)
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Inspector sign in failed. Check your details and try again.')
    } finally { setSubmitting(false) }
  }

  return <div className="inspector-access-layout">
    <section className="inspector-access-intro" aria-labelledby="inspector-access-title">
      <p className="eyebrow">Field verification</p><h1 id="inspector-access-title">Record what is physically present, with proof.</h1>
      <p>Only assigned inspections are visible. Official results remain subject to backend permissions, evidence rules, and separation of duties.</p>
    </section>
    <form className="panel inspector-form-stack" onSubmit={submit}>
      <div className="panel-header"><h2>Inspector sign in</h2><p>Use an account with the Inspector role.</p></div>
      {error ? <div className="inspector-alert" role="alert">{error}</div> : null}
      <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>
      <label>Password<input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
      <Button type="submit" disabled={submitting}>{submitting ? 'Signing in...' : 'Sign in'}</Button>
    </form>
  </div>
}
