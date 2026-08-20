import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { signInAdmin } from './session'
import type { CurrentUser } from './types'
export function AdminSignIn({ onAuthenticated }: { onAuthenticated: (token: string, user: CurrentUser) => void }) {
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setBusy(true); setError(null); try { const session = await signInAdmin(email, password); onAuthenticated(session.accessToken, session.user) } catch (failure) { setError(failure instanceof Error ? failure.message : 'Admin sign in failed.') } finally { setBusy(false) } }
  return <div className="admin-access-layout"><section><p className="eyebrow">Governance and accountability</p><h1>Administer access without becoming an operational approver.</h1><p>Administrative changes are permission-controlled and auditable. Audit history cannot be edited or deleted.</p></section><form className="panel admin-form" onSubmit={submit}>{error ? <div className="admin-alert" role="alert">{error}</div> : null}<label>Email address<input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label><label>Password<input required type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label><Button disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</Button></form></div>
}
