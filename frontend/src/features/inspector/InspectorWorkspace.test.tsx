import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../App'
import type { CurrentUser, Evidence, Inspection } from './types'

const user: CurrentUser = {
  id: '11111111-1111-1111-1111-111111111111', fullName: 'Field Inspector',
  email: 'inspector@civicos.test', roles: ['INSPECTOR'],
  permissions: ['INSPECTION_VIEW', 'INSPECTION_CREATE', 'INSPECTION_COMPLETE', 'EVIDENCE_VIEW', 'EVIDENCE_UPLOAD', 'VERIFICATION_PASS', 'VERIFICATION_FAIL'],
}

const inspection: Inspection = {
  id: '22222222-2222-2222-2222-222222222222', interventionId: '33333333-3333-3333-3333-333333333333',
  interventionNumber: 'INT-001', interventionDescription: 'Restore the road after coordinated utility works.',
  interventionType: 'ROAD_RESTORATION', interventionStatus: 'VERIFICATION_PENDING', interventionVersion: 4,
  agencyId: '44444444-4444-4444-4444-444444444444', agencyName: 'BBMP',
  roadSegmentId: '55555555-5555-5555-5555-555555555555', roadSegmentName: 'Demo Main Road',
  geometryWkt: 'LINESTRING (77.59 12.97, 77.60 12.98)', plannedStart: '2026-09-01T08:00:00Z',
  plannedEnd: '2026-09-03T18:00:00Z', inspectorId: user.id, status: 'SCHEDULED', version: 0,
  createdAt: '2026-08-20T08:00:00Z',
}

const evidence: Evidence = {
  id: '66666666-6666-6666-6666-666666666666', targetType: 'INTERVENTION', targetId: inspection.interventionId,
  uploadedBy: '77777777-7777-7777-7777-777777777777', type: 'RESTORATION', originalFilename: 'restored-road.jpg',
  mimeType: 'image/jpeg', fileSizeBytes: 1024, checksum: 'abc123', capturedAt: '2026-09-03T10:00:00Z',
  latitude: 12.97, longitude: 77.59, metadata: { provenance: 'AGENCY_SUBMITTED' }, status: 'ACCEPTED', version: 2,
  createdAt: '2026-09-03T10:05:00Z',
}

afterEach(() => vi.restoreAllMocks())

describe('Phase 17 inspector workspace', () => {
  it('requires an Inspector account before assigned field work is shown', () => {
    render(<App pathname="/app/inspector/inspections" accessToken={null} />)
    expect(screen.getByRole('heading', { name: /record what is physically present, with proof/i })).toBeInTheDocument()
    expect(screen.getByText(/only assigned inspections are visible/i)).toBeInTheDocument()
  })

  it('renders the assigned mobile inspection queue with an explicit next action', async () => {
    mockApi({ inspections: [inspection] })
    render(<App pathname="/app/inspector/inspections" accessToken="inspector-token" />)
    expect(await screen.findByRole('link', { name: 'Demo Main Road' })).toHaveAttribute('href', `/app/inspector/inspections/${inspection.id}`)
    expect(screen.getByText(/next: confirm location and start/i)).toBeInTheDocument()
  })

  it('submits the official result only after the documented field checklist', async () => {
    const inProgress = { ...inspection, status: 'IN_PROGRESS' as const, version: 1 }
    const fetchMock = mockApi({ inspections: [inProgress], evidence: [evidence] })
    render(<App pathname={`/app/inspector/inspections/${inspection.id}`} accessToken="inspector-token" />)
    expect(await screen.findByRole('heading', { name: 'Demo Main Road' })).toBeInTheDocument()
    for (const item of ['Work completed in approved area', 'Road restored', 'Work zone cleared', 'No visible obstruction', 'Required evidence captured']) fireEvent.click(screen.getByLabelText(item))
    fireEvent.change(screen.getByLabelText('Field observations'), { target: { value: 'Restoration matches the approved work area.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Submit inspection result' }))
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith(`/inspections/${inspection.id}/complete`))).toBe(true))
    const call = fetchMock.mock.calls.find(([input]) => String(input).endsWith('/complete'))
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({ result: 'PASSED', notes: 'Restoration matches the approved work area.', expectedVersion: 1 })
  })

  it('keeps verification human-authored and submits the compatible inspection result', async () => {
    const completed = { ...inspection, status: 'COMPLETED' as const, result: 'FAILED' as const, notes: 'Visible obstruction remains.', version: 2 }
    const fetchMock = mockApi({ inspections: [completed], evidence: [evidence] })
    render(<App pathname="/app/inspector/verification" accessToken="inspector-token" />)
    expect(await screen.findByText(/no advisory ai observation was requested/i)).toBeInTheDocument()
    const card = screen.getByRole('heading', { name: 'Demo Main Road' }).closest('article')!
    fireEvent.click(within(card).getByRole('button', { name: 'Reject completion' }))
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).endsWith(`/inspections/${inspection.id}/verification`))).toBe(true))
    const call = fetchMock.mock.calls.find(([input]) => String(input).endsWith('/verification'))
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ interventionId: inspection.interventionId, result: 'FAILED', reason: 'Visible obstruction remains.', expectedInterventionVersion: 4 })
  })

  it('shows evidence provenance and a non-map location alternative', async () => {
    mockApi({ inspections: [inspection], evidence: [evidence] })
    render(<App pathname={`/app/inspector/inspections/${inspection.id}`} accessToken="inspector-token" />)
    expect(await screen.findByRole('img', { name: /stored work geometry/i })).toBeInTheDocument()
    expect(screen.getAllByText('LINESTRING (77.59 12.97, 77.60 12.98)').length).toBeGreaterThan(0)
    expect(screen.getByText('abc123')).toBeInTheDocument()
    expect(screen.getByText('Agency Submitted')).toBeInTheDocument()
  })
})

function mockApi({ inspections, evidence: records = [] }: { inspections: Inspection[]; evidence?: Evidence[] }) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = String(input)
    if (url.endsWith('/api/v1/auth/me')) return Promise.resolve(jsonResponse(user))
    if (url.includes('/api/v1/inspections?')) return Promise.resolve(jsonResponse(page(inspections)))
    if (url.endsWith(`/api/v1/inspections/${inspection.id}`)) return Promise.resolve(jsonResponse(inspections[0]))
    if (url.includes('/api/v1/evidence?')) return Promise.resolve(jsonResponse(page(records)))
    if (url.endsWith(`/inspections/${inspection.id}/complete`)) return Promise.resolve(jsonResponse({ status: 'COMPLETED' }))
    if (url.endsWith(`/inspections/${inspection.id}/verification`)) return Promise.resolve(jsonResponse({ result: inspections[0]?.result }))
    return Promise.reject(new Error(`Unexpected request: ${url}`))
  })
}

function page<T>(data: T[]) { return { data, pagination: { page: 0, size: 100, totalElements: data.length, totalPages: data.length ? 1 : 0 }, meta: {} } }
function jsonResponse(body: unknown) { return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }) }
