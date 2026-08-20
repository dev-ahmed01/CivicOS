import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { uploadInspectionEvidence } from './api'

export function EvidenceCapture({ accessToken, interventionId, onUploaded }: { accessToken: string; interventionId: string; onUploaded: () => void }) {
  const [files, setFiles] = useState<File[]>([])
  const [coordinates, setCoordinates] = useState<{ latitude: number; longitude: number } | undefined>()
  const [locating, setLocating] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)

  function captureLocation() {
    setLocating(true); setMessage(null)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => { setCoordinates({ latitude: coords.latitude, longitude: coords.longitude }); setLocating(false) },
      () => { setMessage({ text: 'Location was not captured. You can still upload evidence without GPS metadata.', error: true }); setLocating(false) },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (files.length === 0) { setMessage({ text: 'Choose at least one inspection image before uploading.', error: true }); return }
    setSubmitting(true); setMessage(null)
    const results = await Promise.allSettled(files.map((file) => uploadInspectionEvidence(accessToken, interventionId, file, coordinates)))
    const saved = results.filter(({ status }) => status === 'fulfilled').length
    if (saved > 0) { setFiles([]); onUploaded() }
    setMessage(saved === results.length
      ? { text: `${saved} evidence ${saved === 1 ? 'record' : 'records'} uploaded with trusted provenance.`, error: false }
      : { text: `${saved} of ${results.length} files were saved. Retry only the failed files. The inspection itself was not changed.`, error: true })
    setSubmitting(false)
  }

  return <section className="panel evidence-capture" aria-labelledby="capture-evidence-title">
    <div className="panel-header"><h2 id="capture-evidence-title">Capture inspection evidence</h2><p>Multiple images are allowed. Timestamp and uploader are recorded by the system; GPS is optional.</p></div>
    <form className="inspector-form-stack" onSubmit={submit}>
      {message ? <div className={message.error ? 'inspector-alert' : 'inspector-success'} role={message.error ? 'alert' : 'status'}>{message.text}</div> : null}
      <label>Inspection images<input type="file" accept="image/*" capture="environment" multiple onChange={(event) => setFiles(Array.from(event.target.files ?? []))} /><small>{files.length ? `${files.length} selected` : 'No images selected'}</small></label>
      <Button type="button" variant="secondary" onClick={captureLocation} disabled={locating}>{locating ? 'Capturing location...' : coordinates ? 'Location captured' : 'Use current location'}</Button>
      <Button type="submit" disabled={submitting}>{submitting ? 'Uploading evidence...' : 'Upload evidence'}</Button>
    </form>
  </section>
}
