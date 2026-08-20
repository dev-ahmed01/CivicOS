import { useEffect, useState } from 'react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { StatusIndicator } from '../../components/status/StatusIndicator'
import { Button } from '../../components/ui/Button'
import {
  generateRecommendation,
  getConflict,
  getIntervention,
  listCoordinationDecisions,
  listRecommendations,
  recordCoordinationDecision,
  resolveConflict,
  reviewRecommendation,
} from './api'
import { CrossAgencyTimeline } from './CrossAgencyTimeline'
import { formatDate, label, statusTone, stringList, stringValue } from './formatters'
import { SpatialContextMap } from './SpatialContextMap'
import type { AiRecommendation, Conflict, CoordinationDecision, CurrentUser, Intervention } from './types'

type CoordinationData = {
  conflict: Conflict
  interventions: Intervention[]
  recommendations: AiRecommendation[]
  decisions: CoordinationDecision[]
}

type DecisionType = 'ACCEPT' | 'ACCEPT_WITH_MODIFICATION' | 'REJECT' | 'REQUEST_INFORMATION'

const decisionActions: ReadonlyArray<{ type: DecisionType; label: string; explanation: string }> = [
  { type: 'ACCEPT', label: 'Accept recommendation', explanation: 'Accept the current advisory sequence as a human coordination decision.' },
  { type: 'ACCEPT_WITH_MODIFICATION', label: 'Modify sequence', explanation: 'Reject the current advisory version and record the coordinator’s modified sequence.' },
  { type: 'REJECT', label: 'Reject', explanation: 'Reject the recommendation and record why it is unsuitable.' },
  { type: 'REQUEST_INFORMATION', label: 'Request more information', explanation: 'Keep review open and record the information agencies must provide.' },
]

export function CoordinationWorkspace({
  accessToken,
  user,
  conflictId,
  headingLevel = 'h2',
}: {
  accessToken: string
  user: CurrentUser
  conflictId: string
  headingLevel?: 'h2' | 'h1'
}) {
  const [data, setData] = useState<CoordinationData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [aiNotice, setAiNotice] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    Promise.all([
      getConflict(accessToken, conflictId),
      listRecommendations(accessToken, conflictId),
      listCoordinationDecisions(accessToken, conflictId),
    ]).then(async ([conflict, recommendations, decisions]) => {
      const interventions = await Promise.all(
        conflict.interventionIds.map((interventionId) => getIntervention(accessToken, interventionId)),
      )
      if (active) setData({ conflict, interventions, recommendations, decisions })
    }).catch((failure: unknown) => {
      if (active) setError(failure instanceof Error ? failure.message : 'The coordination workspace could not be loaded.')
    })
    return () => { active = false }
  }, [accessToken, conflictId])

  async function requestRecommendation() {
    if (!data) return
    setSubmitting('GENERATE')
    setAiNotice(null)
    try {
      const result = await generateRecommendation(accessToken, data.conflict, data.interventions)
      if (result.recommendation) {
        setData((current) => current ? {
          ...current,
          recommendations: [result.recommendation!, ...current.recommendations],
        } : current)
        setAiNotice('A new advisory recommendation is ready for human review.')
      } else {
        setAiNotice(result.execution.errorMessage ?? 'AI assistance is unavailable. Continue with the manual coordination path.')
      }
    } catch (failure) {
      setAiNotice(failure instanceof Error ? failure.message : 'AI assistance is unavailable. Continue manually.')
    } finally {
      setSubmitting(null)
    }
  }

  async function decide(decisionType: DecisionType) {
    if (!data || !reason.trim()) return
    const recommendation = data.recommendations[0]
    if (decisionType === 'ACCEPT' && !recommendation) {
      setNotice('Generate or load an advisory recommendation before accepting it. Manual decisions can use Modify or Request more information.')
      return
    }
    setSubmitting(decisionType)
    setNotice(null)
    try {
      let reviewedRecommendation = recommendation
      if (recommendation?.status === 'PENDING_REVIEW' && decisionType === 'ACCEPT') {
        reviewedRecommendation = await reviewRecommendation(accessToken, recommendation.recommendationId, 'ACCEPT', reason)
      } else if (recommendation?.status === 'PENDING_REVIEW' && ['ACCEPT_WITH_MODIFICATION', 'REJECT'].includes(decisionType)) {
        reviewedRecommendation = await reviewRecommendation(accessToken, recommendation.recommendationId, 'REJECT', reason)
      }
      const decision = await recordCoordinationDecision(
        accessToken,
        data.conflict.id,
        decisionType,
        reason,
        decisionType === 'ACCEPT' ? reviewedRecommendation?.recommendationId : undefined,
      )
      setData((current) => current ? {
        ...current,
        recommendations: reviewedRecommendation
          ? current.recommendations.map((item) => item.recommendationId === reviewedRecommendation!.recommendationId ? reviewedRecommendation! : item)
          : current.recommendations,
        decisions: [...current.decisions, decision],
      } : current)
      setReason('')
      setNotice('Human coordination decision recorded. No approval, schedule, or workflow state was changed automatically.')
    } catch (failure) {
      setNotice(failure instanceof Error ? failure.message : 'The coordination decision was not recorded.')
    } finally {
      setSubmitting(null)
    }
  }

  async function finalizeConflict(outcome: 'RESOLVED' | 'DISMISSED') {
    if (!data || !reason.trim()) return
    setSubmitting(outcome)
    setNotice(null)
    try {
      const result = await resolveConflict(accessToken, data.conflict, outcome, reason)
      setData((current) => current ? {
        ...current,
        conflict: { ...current.conflict, status: result.status, version: result.version, resolvedAt: result.resolvedAt },
      } : current)
      setReason('')
      setNotice(`Conflict ${label(result.status)} through the authoritative conflict-resolution command.`)
    } catch (failure) {
      setNotice(failure instanceof Error ? failure.message : 'The conflict resolution was not recorded.')
    } finally {
      setSubmitting(null)
    }
  }

  if (error) return <ErrorState title="Coordination workspace could not be loaded" description={error} />
  if (!data) return <LoadingState label="Loading cross-agency coordination facts" />

  const { conflict, interventions, recommendations, decisions } = data
  const recommendation = recommendations[0]
  const roadName = interventions[0]?.roadSegmentName ?? conflict.roadSegmentId
  const Heading = headingLevel
  const canGenerate = user.permissions.includes('COORDINATION_UPDATE')
  const canDecide = user.permissions.includes('COORDINATION_UPDATE')
  const canResolve = user.permissions.includes('CONFLICT_RESOLVE')
  const isFinal = ['RESOLVED', 'DISMISSED'].includes(conflict.status)
  const recommendationCanBeAccepted = recommendation != null
    && ['PENDING_REVIEW', 'ACCEPTED'].includes(recommendation.status)
    && !decisions.some(({ acceptedRecommendationId }) => acceptedRecommendationId === recommendation.recommendationId)

  return (
    <article className="coordination-workspace" aria-labelledby={`coordination-title-${conflict.id}`}>
      <header className="coordination-hero">
        <div>
          <p className="eyebrow">Coordination command center · {conflict.conflictNumber}</p>
          <Heading id={`coordination-title-${conflict.id}`}>{roadName}</Heading>
          <p>{conflict.explanation}</p>
        </div>
        <div className="coordination-status-stack">
          <StatusIndicator label={`${label(conflict.severity)} risk`} tone={statusTone(conflict.severity)} />
          <StatusIndicator label={label(conflict.status)} tone={statusTone(conflict.status)} />
        </div>
      </header>

      <dl className="coordination-facts" aria-label="Deterministic conflict facts">
        <div><dt>Conflict</dt><dd>{label(conflict.type)}</dd></div>
        <div><dt>Affected road</dt><dd>{roadName}</dd></div>
        <div><dt>Affected work</dt><dd>{interventions.length} interventions · {new Set(interventions.map(({ agencyId }) => agencyId)).size} agencies</dd></div>
        <div><dt>Detected</dt><dd>{formatDate(conflict.detectedAt)}</dd></div>
      </dl>

      <div className="coordination-context-grid">
        <section className="affected-work panel" aria-labelledby={`affected-work-${conflict.id}`}>
          <div className="panel-header"><h3 id={`affected-work-${conflict.id}`}>One road, connected work</h3><p>These records are linked by the deterministic conflict engine.</p></div>
          <ol>
            {interventions.map((item) => (
              <li key={item.id}>
                <span>{item.interventionNumber}</span>
                <strong>{item.agencyName}</strong>
                <small>{label(item.type)} · {label(item.status)}</small>
              </li>
            ))}
          </ol>
        </section>
        <CrossAgencyTimeline interventions={interventions} />
      </div>

      <SpatialContextMap interventions={interventions} />

      <section className="ai-advisory-panel" aria-labelledby={`ai-advisory-${conflict.id}`}>
        <div className="ai-advisory-heading">
          <div><p className="eyebrow">Optional support</p><h3 id={`ai-advisory-${conflict.id}`}>AI-assisted recommendation</h3></div>
          <StatusIndicator label="Advisory only" tone="info" />
        </div>
        {recommendation ? (
          <div className="recommendation-grid">
            <div className="recommendation-primary">
              <span className="recommendation-status">{label(recommendation.status)}</span>
              <h4>Recommendation</h4>
              <p>{stringValue(recommendation.recommendation.recommendation)}</p>
              <h4>Recommended sequence</h4>
              <ol>{sequenceItems(recommendation, interventions).map((item) => <li key={item}>{item}</li>)}</ol>
            </div>
            <dl>
              <div><dt>Why</dt><dd>{listCopy(recommendation.recommendation.reasons)}</dd></div>
              <div><dt>Assumptions</dt><dd>{listCopy(recommendation.recommendation.assumptions)}</dd></div>
              <div><dt>Potential risks</dt><dd>{listCopy(recommendation.recommendation.risks)}</dd></div>
              <div><dt>Uncertainties</dt><dd>{listCopy(recommendation.recommendation.uncertainties)}</dd></div>
              <div><dt>Confidence</dt><dd>{recommendation.confidence == null ? 'Not supplied' : `${Math.round(recommendation.confidence * 100)}%`}</dd></div>
            </dl>
          </div>
        ) : (
          <p className="manual-path-copy">No AI recommendation exists. The deterministic facts and manual coordinator decision path remain fully available.</p>
        )}
        {aiNotice ? <div className="coordinator-notice" role="status">{aiNotice}</div> : null}
        {canGenerate && !isFinal ? <Button type="button" variant="secondary" disabled={submitting !== null} onClick={requestRecommendation}>{submitting === 'GENERATE' ? 'Generating advisory...' : 'Generate advisory recommendation'}</Button> : null}
      </section>

      <section className="human-decision-panel panel" aria-labelledby={`human-decision-${conflict.id}`}>
        <div className="panel-header"><h3 id={`human-decision-${conflict.id}`}>Coordinator decision</h3><p>This human record does not approve work, alter official dates, or bypass lifecycle rules.</p></div>
        <div className="decision-body">
          {notice ? <div className="coordinator-notice" role="status">{notice}</div> : null}
          {isFinal ? (
            <div className="coordinator-success">This conflict is final: {label(conflict.status)} at {formatDate(conflict.resolvedAt)}.</div>
          ) : null}
          <label>Decision reason or modified sequence<textarea required rows={4} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Record agency order, constraints, assumptions, and accountable reasoning." /></label>
          {canDecide && !isFinal ? (
            <div className="decision-actions">
              {decisionActions.map((action) => (
                <button
                  key={action.type}
                  type="button"
                  disabled={submitting !== null || !reason.trim() || action.type === 'ACCEPT' && !recommendationCanBeAccepted}
                  onClick={() => decide(action.type)}
                >
                  <strong>{submitting === action.type ? 'Recording...' : action.label}</strong><span>{action.explanation}</span>
                </button>
              ))}
            </div>
          ) : null}
          {canResolve && !isFinal ? (
            <div className="resolution-actions">
              <div><strong>Separate authoritative resolution</strong><span>Use only after coordination work is complete or the conflict is proven inapplicable.</span></div>
              <Button type="button" disabled={submitting !== null || !reason.trim()} onClick={() => finalizeConflict('RESOLVED')}>{submitting === 'RESOLVED' ? 'Resolving...' : 'Resolve conflict'}</Button>
              <Button type="button" variant="secondary" disabled={submitting !== null || !reason.trim()} onClick={() => finalizeConflict('DISMISSED')}>{submitting === 'DISMISSED' ? 'Dismissing...' : 'Dismiss conflict'}</Button>
            </div>
          ) : null}
        </div>
      </section>

      <section className="decision-history panel" aria-labelledby={`decision-history-${conflict.id}`}>
        <div className="panel-header"><h3 id={`decision-history-${conflict.id}`}>Decision history</h3><p>Append-only human coordination records for this conflict.</p></div>
        {decisions.length > 0 ? (
          <ol>{decisions.map((decision) => (
            <li key={decision.decisionId}>
              <time>{formatDate(decision.createdAt)}</time>
              <strong>{label(decision.decisionType)}</strong>
              <p>{decision.decisionText}</p>
              {decision.acceptedRecommendationId ? <small>Supported by accepted AI recommendation {decision.acceptedRecommendationId}</small> : null}
            </li>
          ))}</ol>
        ) : <p className="manual-path-copy">No human coordination decision has been recorded yet.</p>}
      </section>
    </article>
  )
}

function listCopy(value: unknown) {
  const items = stringList(value)
  return items.length > 0 ? items.join(' ') : 'Not supplied by the advisory response.'
}

function sequenceItems(recommendation: AiRecommendation, interventions: Intervention[]) {
  const sequence = recommendation.recommendation.recommendedSequence
  if (!Array.isArray(sequence)) return ['No structured sequence was supplied.']
  const interventionById = new Map(interventions.map((item) => [item.id, item]))
  return sequence.map((entry, index) => {
    if (!entry || typeof entry !== 'object') return `Step ${index + 1}: Unavailable record`
    const id = 'interventionId' in entry ? String(entry.interventionId) : ''
    const item = interventionById.get(id)
    return item ? `${item.agencyName} — ${item.interventionNumber} (${label(item.type)})` : `Intervention ${id || 'not supplied'}`
  })
}
