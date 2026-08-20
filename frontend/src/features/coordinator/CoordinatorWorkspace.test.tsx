import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../App'
import type { AiRecommendation, Conflict, CoordinationDecision, CurrentUser, Intervention } from './types'

const user: CurrentUser = {
  id: '11111111-1111-1111-1111-111111111111',
  agencyId: null,
  fullName: 'City Coordinator',
  email: 'coordinator@civicos.test',
  roles: ['COORDINATOR'],
  permissions: [
    'CONFLICT_VIEW', 'CONFLICT_RESOLVE', 'COORDINATION_VIEW', 'COORDINATION_UPDATE',
    'INTERVENTION_VIEW', 'APPROVAL_VIEW', 'SLA_VIEW',
  ],
}

const conflict: Conflict = {
  id: '22222222-2222-2222-2222-222222222222',
  conflictNumber: 'C001',
  roadSegmentId: '33333333-3333-3333-3333-333333333333',
  type: 'MULTI_AGENCY_COORDINATION',
  severity: 'HIGH',
  status: 'OPEN',
  detectedAt: '2026-08-20T08:00:00Z',
  explanation: 'Three interventions affect the same road segment inside the coordination window.',
  interventionIds: [
    '44444444-4444-4444-4444-444444444441',
    '44444444-4444-4444-4444-444444444442',
    '44444444-4444-4444-4444-444444444443',
  ],
  version: 2,
}

const interventions: Intervention[] = [
  intervention('44444444-4444-4444-4444-444444444441', 'INT-001', 'BESCOM', 'ELECTRICAL', '2026-09-10T08:00:00Z', '2026-09-12T18:00:00Z'),
  intervention('44444444-4444-4444-4444-444444444442', 'INT-002', 'BWSSB', 'WATER', '2026-09-11T08:00:00Z', '2026-09-14T18:00:00Z'),
  intervention('44444444-4444-4444-4444-444444444443', 'INT-003', 'BBMP', 'RESTORATION', '2026-09-13T08:00:00Z', '2026-09-16T18:00:00Z'),
]

const recommendation: AiRecommendation = {
  recommendationId: '55555555-5555-5555-5555-555555555555',
  runId: '66666666-6666-6666-6666-666666666666',
  conflictId: conflict.id,
  recommendation: {
    recommendation: 'Complete utility work before consolidated restoration.',
    recommendedSequence: interventions.map((item, index) => ({ interventionId: item.id, order: index + 1 })),
    reasons: ['Sequence respects the supplied deterministic conflict context.'],
    risks: ['Agency-confirmed schedules may require revision.'],
    uncertainties: ['No official schedule has been changed.'],
  },
  confidence: 0.84,
  status: 'PENDING_REVIEW',
}

afterEach(() => vi.restoreAllMocks())

describe('Phase 16 coordinator workspace', () => {
  it('requires a coordinator role before citywide operational facts are shown', () => {
    render(<App pathname="/app/coordinator/dashboard" accessToken={null} />)
    expect(screen.getByRole('heading', { name: /one physical-road problem across agency boundaries/i })).toBeInTheDocument()
    expect(screen.getByText(/ai remains optional and advisory/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('turns separate agency records into one deterministic road coordination problem', async () => {
    mockCoordinatorApi({ recommendations: [recommendation] })
    render(<App pathname="/app/coordinator/dashboard" accessToken="coordinator-token" />)

    expect(await screen.findByRole('heading', { name: 'Coordination Command Center' })).toBeInTheDocument()
    expect(within(screen.getByText('High-severity conflicts').closest('a')!).getByText('1')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Outer Ring Road — Demo Segment' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cross-agency timeline' })).toBeInTheDocument()
    expect(screen.getAllByText('BESCOM').length).toBeGreaterThan(0)
    expect(screen.getAllByText('BWSSB').length).toBeGreaterThan(0)
    expect(screen.getAllByText('BBMP').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'AI-assisted recommendation' })).toBeInTheDocument()
    expect(screen.getByText('Advisory only')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /geometry map of affected intervention zones/i })).toBeInTheDocument()
    expect(screen.queryByText(/no approval, schedule, or workflow state was changed/i)).not.toBeInTheDocument()
  })

  it('reviews advisory output then records a normal human decision without resolving the conflict', async () => {
    const fetchMock = mockCoordinatorApi({ recommendations: [recommendation] })
    const path = `/app/coordinator/coordination/${conflict.id}`
    render(<App pathname={path} accessToken="coordinator-token" />)

    expect(await screen.findByRole('heading', { name: 'Outer Ring Road — Demo Segment' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Decision reason or modified sequence'), {
      target: { value: 'BESCOM, then BWSSB, then one BBMP restoration window.' },
    })
    fireEvent.click(screen.getByRole('button', { name: /accept recommendation/i }))

    expect(await screen.findByText(/human coordination decision recorded/i)).toBeInTheDocument()
    const reviewCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith(`/ai/recommendations/${recommendation.recommendationId}/review`))
    expect(JSON.parse(String(reviewCall?.[1]?.body))).toMatchObject({ decision: 'ACCEPT' })
    const decisionCall = fetchMock.mock.calls.find(([input, init]) => String(input).endsWith(`/conflicts/${conflict.id}/coordination-decisions`) && init?.method === 'POST')
    expect(JSON.parse(String(decisionCall?.[1]?.body))).toMatchObject({
      decisionType: 'ACCEPT', acceptedRecommendationId: recommendation.recommendationId,
    })
    expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith(`/conflicts/${conflict.id}/resolve`))).toBe(false)
  })

  it('keeps the manual path usable when AI is disabled', async () => {
    mockCoordinatorApi({ recommendations: [], aiDisabled: true })
    render(<App pathname={`/app/coordinator/coordination/${conflict.id}`} accessToken="coordinator-token" />)

    expect(await screen.findByText(/no ai recommendation exists/i)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Generate advisory recommendation' }))
    expect(await screen.findByText(/ai advisory capabilities are disabled/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Decision reason or modified sequence')).toBeEnabled()
  })
})

function mockCoordinatorApi({ recommendations, aiDisabled = false }: { recommendations: AiRecommendation[]; aiDisabled?: boolean }) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    const url = String(input)
    if (url.endsWith('/api/v1/auth/me')) return Promise.resolve(jsonResponse(user))
    if (url.endsWith(`/api/v1/conflicts/${conflict.id}`)) return Promise.resolve(jsonResponse(conflict))
    if (url.endsWith(`/api/v1/conflicts/${conflict.id}/recommendations`)) return Promise.resolve(jsonResponse(recommendations))
    if (url.endsWith(`/api/v1/conflicts/${conflict.id}/coordination-decisions`)) {
      if (init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        const decision: CoordinationDecision = {
          decisionId: '77777777-7777-7777-7777-777777777777', conflictId: conflict.id,
          coordinatorId: user.id, decisionType: body.decisionType, decisionText: body.decisionText,
          acceptedRecommendationId: body.acceptedRecommendationId, createdAt: '2026-08-20T10:00:00Z',
        }
        return Promise.resolve(jsonResponse(decision, 201))
      }
      return Promise.resolve(jsonResponse([]))
    }
    if (url.endsWith(`/api/v1/ai/recommendations/${recommendation.recommendationId}/review`)) {
      return Promise.resolve(jsonResponse({ ...recommendation, status: 'ACCEPTED', reviewedBy: user.id }))
    }
    if (url.endsWith('/api/v1/ai/coordination-recommendations')) {
      return Promise.resolve(jsonResponse(aiDisabled
        ? { execution: { status: 'CANCELLED', humanReviewRequired: true, errorMessage: 'AI advisory capabilities are disabled.' }, recommendation: null }
        : { execution: { status: 'COMPLETED', humanReviewRequired: true }, recommendation }))
    }
    const matchingIntervention = interventions.find(({ id }) => url.endsWith(`/api/v1/interventions/${id}`))
    if (matchingIntervention) return Promise.resolve(jsonResponse(matchingIntervention))
    if (url.includes('/api/v1/conflicts?')) {
      return Promise.resolve(jsonResponse(url.includes('status=OPEN') ? page([conflict], 1) : page([], 0)))
    }
    if (url.includes('/api/v1/slas?')) return Promise.resolve(jsonResponse(page([], 0)))
    if (url.includes('/api/v1/approval-requests?')) return Promise.resolve(jsonResponse(page([], 0)))
    if (url.includes('/api/v1/interventions?')) return Promise.resolve(jsonResponse(page([], 0)))
    return Promise.reject(new Error(`Unexpected request: ${url}`))
  })
}

function intervention(id: string, number: string, agency: string, type: string, start: string, end: string): Intervention {
  return {
    id, interventionNumber: number, caseId: '88888888-8888-8888-8888-888888888888', caseNumber: 'CASE-001',
    agencyId: `${id.slice(0, -1)}9`, agencyName: agency, type,
    description: `${agency} road-impacting work`, roadSegmentId: conflict.roadSegmentId,
    roadSegmentName: 'Outer Ring Road — Demo Segment', geometryWkt: 'LINESTRING (77.59 12.97, 77.60 12.98)',
    plannedStart: start, plannedEnd: end, status: 'COORDINATION_REQUIRED', priority: 'HIGH', version: 1,
  }
}

function page<T>(data: T[], totalElements = data.length) {
  return { data, pagination: { page: 0, size: 20, totalElements, totalPages: totalElements ? 1 : 0 }, meta: {} }
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
