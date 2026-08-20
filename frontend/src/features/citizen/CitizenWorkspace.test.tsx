import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../App'
import type { CitizenObservation } from './types'

const road = {
  roadSegmentId: '11111111-1111-1111-1111-111111111111',
  externalReference: 'R003',
  name: 'Demo Service Road - Whitefield',
  classification: 'LOCAL',
  surfaceType: 'BITUMINOUS',
}

const completedReport: CitizenObservation = {
  observationId: '22222222-2222-2222-2222-222222222222',
  caseId: '33333333-3333-3333-3333-333333333333',
  trackingId: 'CASE-CIT-001',
  category: 'RESTORATION_ISSUE',
  description: 'The restored surface remains uneven and unsafe.',
  latitude: 12.9716,
  longitude: 77.5946,
  detectedRoad: road,
  submittedAt: '2026-08-20T10:00:00Z',
  status: 'RESOLVED',
  caseStatus: 'PENDING_VERIFICATION',
  publicStatus: 'COMPLETED',
  publicMessage: 'The reported issue is ready for your resolution feedback.',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('Phase 14 citizen workspace', () => {
  it('requires citizen authentication and explains the workflow boundary', () => {
    render(<App pathname="/app/citizen/report" accessToken={null} />)

    expect(screen.getByRole('heading', { name: /report it\. track it\. verify the outcome/i })).toBeInTheDocument()
    expect(screen.getByText(/does not bypass official inspection or approval rules/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('completes the citizen-controlled report flow and returns a tracking ID', async () => {
    const submittedReport = { ...completedReport, status: 'SUBMITTED', caseStatus: 'OPEN', publicStatus: 'REPORT_RECEIVED' as const }
    vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue('44444444-4444-4444-8444-444444444444')
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse([road]))
      .mockResolvedValueOnce(jsonResponse(submittedReport, 201))
      .mockResolvedValueOnce(jsonResponse({ evidenceId: 'E-001' }, 201))

    render(<App pathname="/app/citizen/report" accessToken="citizen-token" />)

    fireEvent.change(screen.getByLabelText(/issue photo/i), {
      target: { files: [new File(['photo'], 'road.jpg', { type: 'image/jpeg' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))

    fireEvent.change(screen.getByLabelText('Latitude'), { target: { value: '12.9716' } })
    fireEvent.change(screen.getByLabelText('Longitude'), { target: { value: '77.5946' } })
    fireEvent.click(screen.getByRole('button', { name: 'Detect road' }))
    await screen.findByText(road.name)
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))

    fireEvent.change(screen.getByLabelText('Issue category'), { target: { value: 'RESTORATION_ISSUE' } })
    fireEvent.change(screen.getByLabelText(/^Description/), {
      target: { value: 'The restored surface remains uneven and unsafe.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))

    expect(screen.getByText('Review your report')).toBeInTheDocument()
    expect(screen.getByText(road.name)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Submit report' }))

    expect(await screen.findByRole('heading', { name: /tracking ID is CASE-CIT-001/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Track this report' })).toHaveAttribute(
      'href',
      `/app/citizen/reports/${completedReport.observationId}`,
    )
    expect(fetchMock.mock.calls[2]?.[0]).toBe(
      `http://localhost:8080/api/v1/observations/${completedReport.observationId}/evidence`,
    )
  })

  it('shows a public-safe timeline and records feedback as a supporting signal', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse(completedReport))
      .mockResolvedValueOnce(jsonResponse({ authoritativeWorkflowChanged: false }))
    vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue('55555555-5555-4555-8555-555555555555')

    render(
      <App
        pathname={`/app/citizen/reports/${completedReport.observationId}`}
        accessToken="citizen-token"
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Restoration Issue' })).toBeInTheDocument()
    expect(screen.getByText(/internal approvals, notes, and actor details are intentionally excluded/i)).toBeInTheDocument()
    expect(screen.getByText(/does not directly reopen or close the official case/i)).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('Still unresolved'))
    fireEvent.change(screen.getByLabelText('What remains unresolved?'), {
      target: { value: 'The edge is still unsafe.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Submit feedback' }))

    expect(await screen.findByText(/feedback has been recorded/i)).toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  })
})

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
