import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../App'
import type { Approval, CurrentUser, Intervention } from './types'

const user: CurrentUser = {
  id: '11111111-1111-1111-1111-111111111111',
  agencyId: '22222222-2222-2222-2222-222222222222',
  fullName: 'Agency Officer',
  email: 'officer@agency.gov.in',
  roles: ['AGENCY_OFFICER'],
  permissions: [
    'INTERVENTION_VIEW', 'INTERVENTION_START', 'APPROVAL_VIEW', 'APPROVAL_APPROVE',
    'CONFLICT_VIEW', 'EVIDENCE_VIEW', 'EVIDENCE_UPLOAD', 'SLA_VIEW', 'AUDIT_VIEW',
  ],
}

const intervention: Intervention = {
  id: '33333333-3333-3333-3333-333333333333',
  interventionNumber: 'INT-AGENCY-001',
  caseId: '44444444-4444-4444-4444-444444444444',
  caseNumber: 'CASE-001',
  agencyId: user.agencyId,
  agencyName: 'BESCOM',
  type: 'ELECTRICAL',
  description: 'Underground electrical utility work before road restoration.',
  roadSegmentId: '55555555-5555-5555-5555-555555555555',
  roadSegmentName: 'Demo Service Road',
  geometryWkt: 'LINESTRING (77.59 12.97, 77.60 12.98)',
  plannedStart: '2026-09-01T08:00:00Z',
  plannedEnd: '2026-09-03T18:00:00Z',
  status: 'SCHEDULED',
  priority: 'HIGH',
  createdBy: '99999999-9999-9999-9999-999999999999',
  version: 3,
  createdAt: '2026-08-20T08:00:00Z',
  updatedAt: '2026-08-20T09:00:00Z',
}

afterEach(() => vi.restoreAllMocks())

describe('Phase 15 agency workspace', () => {
  it('requires an agency-scoped officer account before operational data is shown', () => {
    render(<App pathname="/app/agency/dashboard" accessToken={null} />)
    expect(screen.getByRole('heading', { name: /move assigned work through governed execution/i })).toBeInTheDocument()
    expect(screen.getByText(/separation of duties, and audit/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('prioritizes API-backed agency actions and metrics on the dashboard', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/api/v1/auth/me')) return Promise.resolve(jsonResponse(user))
      if (url.includes('/api/v1/interventions?')) {
        if (url.includes('VERIFICATION_PENDING')) return Promise.resolve(jsonResponse(page([], 2)))
        if (url.includes('EVIDENCE_PENDING')) return Promise.resolve(jsonResponse(page([], 1)))
        if (url.includes('status=')) return Promise.resolve(jsonResponse(page([intervention], 5)))
        return Promise.resolve(jsonResponse(page([intervention], 12)))
      }
      if (url.includes('/api/v1/approval-requests?')) return Promise.resolve(jsonResponse(page([], 3)))
      if (url.includes('/api/v1/slas?')) return Promise.resolve(jsonResponse(page([], 2)))
      if (url.includes('/api/v1/conflicts?')) return Promise.resolve(jsonResponse(page([], 4)))
      if (url.includes('/api/v1/evidence?')) return Promise.resolve(jsonResponse(page([], 1)))
      return Promise.reject(new Error(`Unexpected request: ${url}`))
    })

    render(<App pathname="/app/agency/dashboard" accessToken="agency-token" />)

    expect(await screen.findByRole('heading', { name: /agency overview for agency officer/i })).toBeInTheDocument()
    expect(within(screen.getByText('Assigned interventions').parentElement!).getByText('12')).toBeInTheDocument()
    expect(within(screen.getByText('Pending actions').parentElement!).getByText('5')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Priority work queue' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: intervention.interventionNumber })).toBeInTheDocument()
  })

  it('shows only permitted lifecycle actions and records the reason through the action API', async () => {
    const actionUser = { ...user, permissions: ['INTERVENTION_VIEW', 'INTERVENTION_START'] }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/api/v1/auth/me')) return Promise.resolve(jsonResponse(actionUser))
      if (url.endsWith(`/api/v1/interventions/${intervention.id}`)) return Promise.resolve(jsonResponse(intervention))
      if (url.endsWith(`/api/v1/interventions/${intervention.id}/dependencies`)) return Promise.resolve(jsonResponse([]))
      if (url.endsWith(`/api/v1/interventions/${intervention.id}/actions/start`)) return Promise.resolve(jsonResponse({ toStatus: 'IN_PROGRESS' }))
      return Promise.reject(new Error(`Unexpected request: ${url}`))
    })

    render(<App pathname={`/app/agency/interventions/${intervention.id}`} accessToken="agency-token" />)

    expect(await screen.findByRole('heading', { name: 'Electrical' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Start work' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'Verification' }))
    expect(screen.getByText(/only an authorized inspector can record the official verification result/i)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Action reason'), { target: { value: 'Field team and safety controls are ready.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Start work' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith(`/actions/start`))).toBe(true))
    const actionCall = fetchMock.mock.calls.find(([input]) => String(input).endsWith('/actions/start'))
    expect(JSON.parse(String(actionCall?.[1]?.body))).toMatchObject({ expectedVersion: 3, reason: 'Field team and safety controls are ready.' })
  })

  it('enforces separation-of-duties messaging before approval controls', async () => {
    const approval: Approval = {
      id: '66666666-6666-6666-6666-666666666666', interventionId: intervention.id,
      interventionNumber: intervention.interventionNumber, agencyId: user.agencyId,
      status: 'PENDING', conditions: [], version: 0, createdAt: '2026-08-20T10:00:00Z',
    }
    const creatorIntervention = { ...intervention, createdBy: user.id, status: 'APPROVAL_PENDING' }
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)
      if (url.endsWith('/api/v1/auth/me')) return Promise.resolve(jsonResponse(user))
      if (url.includes('/api/v1/approval-requests?')) return Promise.resolve(jsonResponse(page([approval], 1)))
      if (url.endsWith(`/api/v1/interventions/${intervention.id}`)) return Promise.resolve(jsonResponse(creatorIntervention))
      if (url.endsWith(`/api/v1/interventions/${intervention.id}/dependencies`)) return Promise.resolve(jsonResponse([]))
      if (url.includes('/api/v1/conflicts?') || url.includes('/api/v1/evidence?') || url.includes('/api/v1/slas?')) return Promise.resolve(jsonResponse(page([], 0)))
      return Promise.reject(new Error(`Unexpected request: ${url}`))
    })

    render(<App pathname="/app/agency/approvals" accessToken="agency-token" />)
    expect(await screen.findByText(/the officer who created this intervention cannot approve it/i)).toBeInTheDocument()
    expect(screen.queryByLabelText('Decision')).not.toBeInTheDocument()
  })
})

function page<T>(data: T[], totalElements = data.length) {
  return { data, pagination: { page: 0, size: 20, totalElements, totalPages: totalElements ? 1 : 0 }, meta: {} }
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}
