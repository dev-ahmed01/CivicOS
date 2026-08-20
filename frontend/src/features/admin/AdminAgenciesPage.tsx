import { useEffect, useState, type FormEvent } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import { createAgency, listAgencies, updateAgency } from './api'
import { AdminHeading } from './AdminUsersPage'
import type { Agency } from './types'

export function AdminAgenciesPage({ accessToken, canManage }: { accessToken: string; canManage: boolean }) {
  const [agencies, setAgencies] = useState<Agency[] | null>(null); const [error, setError] = useState<string | null>(null); const [refresh, setRefresh] = useState(0)
  useEffect(() => { let active = true; listAgencies(accessToken).then((response) => { if (active) setAgencies(response.data) }).catch((failure: unknown) => { if (active) setError(failure instanceof Error ? failure.message : 'Agencies could not be loaded.') }); return () => { active = false } }, [accessToken, refresh])
  return <><AdminHeading eyebrow="Organisation governance" title="Agencies" text="Maintain participating organisation records without altering their operational decisions." />{error ? <ErrorState title="Agencies could not be loaded" description={error} /> : null}{!error && !agencies ? <LoadingState label="Loading governed agencies" /> : null}{agencies ? <div className="admin-split">{canManage ? <CreateAgencyForm accessToken={accessToken} onChanged={() => setRefresh((value) => value + 1)} /> : <section className="panel"><h2>Policy permission required</h2><p>You can inspect agencies but cannot change them.</p></section>}<section className="panel"><div className="panel-header"><h2>Participating agencies</h2><p>{agencies.length} governed records.</p></div><div className="admin-list">{agencies.map((agency) => <AgencyCard key={agency.id} agency={agency} accessToken={accessToken} canManage={canManage} onChanged={() => setRefresh((value) => value + 1)} />)}</div></section></div> : null}</>
}

function CreateAgencyForm({ accessToken, onChanged }: { accessToken: string; onChanged: () => void }) {
  const [code, setCode] = useState(''); const [name, setName] = useState(''); const [type, setType] = useState('UTILITY'); const [busy, setBusy] = useState(false); const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setBusy(true); setMessage(null); try { await createAgency(accessToken, { code, name, type, active: true, reason: 'Administrator created a governed participating agency.' }); setMessage({ text: 'Agency created and audited.', error: false }); setCode(''); setName(''); onChanged() } catch (failure) { setMessage({ text: failure instanceof Error ? failure.message : 'Agency was not created.', error: true }) } finally { setBusy(false) } }
  return <section className="panel"><div className="panel-header"><h2>Add agency</h2><p>Agency codes become immutable after creation.</p></div><form className="admin-form" onSubmit={submit}>{message ? <div className={message.error ? 'admin-alert' : 'admin-success'} role={message.error ? 'alert' : 'status'}>{message.text}</div> : null}<label>Code<input required value={code} onChange={(event) => setCode(event.target.value.toUpperCase())} /></label><label>Name<input required value={name} onChange={(event) => setName(event.target.value)} /></label><label>Type<select value={type} onChange={(event) => setType(event.target.value)}>{['GOVERNMENT', 'UTILITY', 'TELECOM', 'CONTRACTOR', 'OTHER'].map((value) => <option key={value}>{value}</option>)}</select></label><Button disabled={busy}>{busy ? 'Creating...' : 'Create agency'}</Button></form></section>
}

function AgencyCard({ agency, accessToken, canManage, onChanged }: { agency: Agency; accessToken: string; canManage: boolean; onChanged: () => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null)
  async function toggle() { setBusy(true); setError(null); try { await updateAgency(accessToken, agency, !agency.active); onChanged() } catch (failure) { setError(failure instanceof Error ? failure.message : 'Agency status was not changed.') } finally { setBusy(false) } }
  return <article className="admin-record"><div><p className="eyebrow">{agency.code} · {agency.type}</p><h3>{agency.name}</h3><p>{agency.jurisdiction ?? 'Jurisdiction not recorded'}</p></div><div className="admin-record-actions"><StatusIndicator label={agency.active ? 'Active' : 'Inactive'} tone={agency.active ? 'success' : 'warning'} />{canManage ? <Button type="button" variant="secondary" disabled={busy} onClick={toggle}>{busy ? 'Saving...' : agency.active ? 'Deactivate' : 'Reactivate'}</Button> : null}{error ? <span role="alert" className="admin-inline-error">{error}</span> : null}</div></article>
}
