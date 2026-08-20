import { useState, type ChangeEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { ApiError } from '../../lib/api/client'
import {
  createObservation,
  findRoadCandidates,
  uploadObservationEvidence,
} from './api'
import type { CitizenObservation, RoadCandidate } from './types'

type CitizenReportFormProps = {
  accessToken: string
}

type Draft = {
  latitude: string
  longitude: string
  category: string
  description: string
}

const steps = ['Photo', 'Location', 'Issue', 'Review'] as const
const categories = [
  ['ROAD_DAMAGE', 'Road damage'],
  ['OPEN_EXCAVATION', 'Open excavation'],
  ['REPEAT_EXCAVATION', 'Repeated road cutting'],
  ['RESTORATION_ISSUE', 'Poor restoration'],
  ['SAFETY_HAZARD', 'Safety hazard'],
  ['OTHER', 'Other'],
] as const

export function CitizenReportForm({ accessToken }: CitizenReportFormProps) {
  const [step, setStep] = useState(0)
  const [draft, setDraft] = useState<Draft>({ latitude: '', longitude: '', category: '', description: '' })
  const [primaryPhoto, setPrimaryPhoto] = useState<File | null>(null)
  const [additionalPhoto, setAdditionalPhoto] = useState<File | null>(null)
  const [roadCandidates, setRoadCandidates] = useState<RoadCandidate[]>([])
  const [selectedRoadId, setSelectedRoadId] = useState('')
  const [locating, setLocating] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState<CitizenObservation | null>(null)
  const [evidenceWarning, setEvidenceWarning] = useState<string | null>(null)

  function update(field: keyof Draft, value: string) {
    setDraft((current) => ({ ...current, [field]: value }))
  }

  function fileChange(setFile: (file: File | null) => void) {
    return (event: ChangeEvent<HTMLInputElement>) => setFile(event.target.files?.[0] ?? null)
  }

  function useCurrentLocation() {
    setError(null)
    if (!navigator.geolocation) {
      setError('Location services are unavailable. Enter the coordinates manually.')
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setDraft((current) => ({
          ...current,
          latitude: coords.latitude.toFixed(6),
          longitude: coords.longitude.toFixed(6),
        }))
        setLocating(false)
      },
      () => {
        setError('Location permission was not available. Enter or adjust the location manually.')
        setLocating(false)
      },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  async function confirmLocation() {
    const latitude = Number(draft.latitude)
    const longitude = Number(draft.longitude)
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      setError('Enter a valid latitude and longitude before detecting the road.')
      return
    }
    setLocating(true)
    setError(null)
    try {
      const candidates = await findRoadCandidates(accessToken, latitude, longitude)
      setRoadCandidates(candidates)
      setSelectedRoadId(candidates[0]?.roadSegmentId ?? '')
      if (candidates.length === 0) {
        setError('No active road was found within 150 metres. Adjust the marker and try again.')
      }
    } catch (failure) {
      setError(message(failure, 'The road could not be detected. Adjust the location and retry.'))
    } finally {
      setLocating(false)
    }
  }

  function next() {
    setError(null)
    if (step === 0 && !primaryPhoto) {
      setError('Add a clear photo of the road issue before continuing.')
      return
    }
    if (step === 1 && !selectedRoadId) {
      setError('Confirm the location and select the detected road before continuing.')
      return
    }
    if (step === 2 && (!draft.category || draft.description.trim().length < 10)) {
      setError('Select an issue category and add a description of at least 10 characters.')
      return
    }
    setStep((current) => Math.min(current + 1, steps.length - 1))
  }

  async function submit() {
    const latitude = Number(draft.latitude)
    const longitude = Number(draft.longitude)
    setSubmitting(true)
    setError(null)
    setEvidenceWarning(null)
    try {
      const observation = await createObservation(accessToken, {
        category: draft.category,
        description: draft.description.trim(),
        latitude,
        longitude,
        roadSegmentId: selectedRoadId,
      })
      setSubmitted(observation)
      const photos = [primaryPhoto, additionalPhoto].filter((photo): photo is File => photo !== null)
      const uploadResults = await Promise.allSettled(
        photos.map((photo) => uploadObservationEvidence(accessToken, observation, photo)),
      )
      if (uploadResults.some(({ status }) => status === 'rejected')) {
        setEvidenceWarning(
          'Your report was saved, but one or more photos could not be uploaded. Keep the tracking ID and retry evidence later.',
        )
      }
    } catch (failure) {
      setError(message(failure, 'The report was not submitted. Review the details and try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (submitted) {
    return (
      <section className="citizen-success panel" aria-labelledby="report-submitted-title">
        <span className="success-mark" aria-hidden="true">✓</span>
        <p className="eyebrow">Report received</p>
        <h1 id="report-submitted-title">Your tracking ID is {submitted.trackingId}</h1>
        <p>{submitted.publicMessage}</p>
        {evidenceWarning ? <div className="form-alert" role="alert">{evidenceWarning}</div> : null}
        <a className="button button-primary" href={`/app/citizen/reports/${submitted.observationId}`}>
          Track this report
        </a>
      </section>
    )
  }

  return (
    <div className="citizen-report-layout">
      <header className="citizen-page-heading">
        <div>
          <p className="eyebrow">Citizen observation</p>
          <h1>Report a road issue</h1>
          <p>Photo, location, category, description, review, then submit.</p>
        </div>
        <span className="privacy-note">Your description remains authoritative</span>
      </header>

      <ol className="report-steps" aria-label="Report progress">
        {steps.map((label, index) => (
          <li key={label} aria-current={index === step ? 'step' : undefined} data-complete={index < step}>
            <span>{index + 1}</span>{label}
          </li>
        ))}
      </ol>

      <section className="report-form-card panel" aria-labelledby="report-step-title">
        <div className="panel-header">
          <p className="step-count">Step {step + 1} of {steps.length}</p>
          <h2 id="report-step-title">{stepTitle(step)}</h2>
        </div>
        <div className="report-step-body">
          {error ? <div className="form-alert" role="alert">{error}</div> : null}
          {step === 0 ? (
            <PhotoStep
              primaryPhoto={primaryPhoto}
              additionalPhoto={additionalPhoto}
              onPrimaryChange={fileChange(setPrimaryPhoto)}
              onAdditionalChange={fileChange(setAdditionalPhoto)}
            />
          ) : null}
          {step === 1 ? (
            <LocationStep
              draft={draft}
              locating={locating}
              roadCandidates={roadCandidates}
              selectedRoadId={selectedRoadId}
              onUpdate={update}
              onUseCurrentLocation={useCurrentLocation}
              onConfirmLocation={confirmLocation}
              onRoadChange={setSelectedRoadId}
            />
          ) : null}
          {step === 2 ? <IssueStep draft={draft} onUpdate={update} /> : null}
          {step === 3 ? (
            <ReviewStep
              draft={draft}
              primaryPhoto={primaryPhoto}
              additionalPhoto={additionalPhoto}
              road={roadCandidates.find(({ roadSegmentId }) => roadSegmentId === selectedRoadId)}
            />
          ) : null}
        </div>
        <footer className="report-actions">
          <Button variant="secondary" type="button" disabled={step === 0 || submitting} onClick={() => setStep((current) => current - 1)}>
            Back
          </Button>
          {step < steps.length - 1 ? (
            <Button type="button" onClick={next}>Continue</Button>
          ) : (
            <Button type="button" disabled={submitting} onClick={submit}>
              {submitting ? 'Submitting report...' : 'Submit report'}
            </Button>
          )}
        </footer>
      </section>
    </div>
  )
}

type PhotoStepProps = {
  primaryPhoto: File | null
  additionalPhoto: File | null
  onPrimaryChange: (event: ChangeEvent<HTMLInputElement>) => void
  onAdditionalChange: (event: ChangeEvent<HTMLInputElement>) => void
}

function PhotoStep({ primaryPhoto, additionalPhoto, onPrimaryChange, onAdditionalChange }: PhotoStepProps) {
  return (
    <div className="photo-grid">
      <label className="upload-field">
        <span>Issue photo <strong>Required</strong></span>
        <input type="file" accept="image/*" capture="environment" onChange={onPrimaryChange} />
        <small>{primaryPhoto?.name ?? 'Take a photo or choose an image'}</small>
      </label>
      <label className="upload-field">
        <span>Additional photo <em>Optional</em></span>
        <input type="file" accept="image/*" capture="environment" onChange={onAdditionalChange} />
        <small>{additionalPhoto?.name ?? 'Add another angle if useful'}</small>
      </label>
    </div>
  )
}

type LocationStepProps = {
  draft: Draft
  locating: boolean
  roadCandidates: RoadCandidate[]
  selectedRoadId: string
  onUpdate: (field: keyof Draft, value: string) => void
  onUseCurrentLocation: () => void
  onConfirmLocation: () => void
  onRoadChange: (roadId: string) => void
}

function LocationStep(props: LocationStepProps) {
  const { draft, locating, roadCandidates, selectedRoadId, onUpdate } = props
  return (
    <div className="location-layout">
      <div className="location-fields form-stack">
        <div className="coordinate-grid">
          <label>Latitude<input inputMode="decimal" value={draft.latitude} onChange={(event) => onUpdate('latitude', event.target.value)} /></label>
          <label>Longitude<input inputMode="decimal" value={draft.longitude} onChange={(event) => onUpdate('longitude', event.target.value)} /></label>
        </div>
        <div className="inline-actions">
          <Button variant="secondary" type="button" disabled={locating} onClick={props.onUseCurrentLocation}>Use my location</Button>
          <Button type="button" disabled={locating} onClick={props.onConfirmLocation}>{locating ? 'Detecting...' : 'Detect road'}</Button>
        </div>
        {roadCandidates.length > 0 ? (
          <label>
            Detected road
            <select value={selectedRoadId} onChange={(event) => props.onRoadChange(event.target.value)}>
              {roadCandidates.map((road) => <option key={road.roadSegmentId} value={road.roadSegmentId}>{road.name}</option>)}
            </select>
          </label>
        ) : null}
      </div>
      <div className="citizen-map" role="img" aria-label="Adjustable location preview with selected road marker">
        <span className="map-road map-road-a" />
        <span className="map-road map-road-b" />
        <span className="map-marker">Selected location</span>
        <small>{draft.latitude && draft.longitude ? `${draft.latitude}, ${draft.longitude}` : 'Location not selected'}</small>
      </div>
    </div>
  )
}

type IssueStepProps = { draft: Draft; onUpdate: (field: keyof Draft, value: string) => void }

function IssueStep({ draft, onUpdate }: IssueStepProps) {
  return (
    <div className="form-stack">
      <label>
        Issue category
        <select value={draft.category} onChange={(event) => onUpdate('category', event.target.value)} required>
          <option value="">Select a category</option>
          {categories.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label>
        Description
        <textarea rows={6} maxLength={4000} value={draft.description} onChange={(event) => onUpdate('description', event.target.value)} placeholder="Describe what you can see and why it needs attention." />
        <small>{draft.description.length}/4000 characters</small>
      </label>
      <p className="advisory-note">If a suggested category is offered later, you can change it before submission.</p>
      <p className="timestamp-note">Date and time will be recorded securely when you submit.</p>
    </div>
  )
}

type ReviewStepProps = {
  draft: Draft
  primaryPhoto: File | null
  additionalPhoto: File | null
  road?: RoadCandidate
}

function ReviewStep({ draft, primaryPhoto, additionalPhoto, road }: ReviewStepProps) {
  return (
    <dl className="review-list">
      <div><dt>Issue</dt><dd>{categories.find(([value]) => value === draft.category)?.[1]}</dd></div>
      <div><dt>Description</dt><dd>{draft.description}</dd></div>
      <div><dt>Detected road</dt><dd>{road?.name ?? 'Not selected'}</dd></div>
      <div><dt>Approximate location</dt><dd>{draft.latitude}, {draft.longitude}</dd></div>
      <div><dt>Photos</dt><dd>{[primaryPhoto, additionalPhoto].filter(Boolean).map((file) => file?.name).join(', ')}</dd></div>
    </dl>
  )
}

function stepTitle(step: number) {
  return ['Add a clear photo', 'Confirm the location', 'Describe the issue', 'Review your report'][step]
}

function message(failure: unknown, fallback: string) {
  return failure instanceof ApiError || failure instanceof Error ? failure.message : fallback
}
