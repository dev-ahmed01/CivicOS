import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'

describe('CivicOS API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('sends authentication and idempotency metadata without storing server state', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      JSON.stringify({ interventionId: 'INT-001' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))

    await apiRequest('/api/v1/interventions/INT-001', {
      method: 'POST',
      accessToken: 'access-token',
      idempotencyKey: 'request-001',
      body: { action: 'SUBMIT' },
    })

    const [, request] = fetchMock.mock.calls[0]
    const headers = new Headers(request?.headers)
    expect(headers.get('Authorization')).toBe('Bearer access-token')
    expect(headers.get('Idempotency-Key')).toBe('request-001')
    expect(request?.body).toBe(JSON.stringify({ action: 'SUBMIT' }))
  })

  it('preserves actionable backend error details', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      JSON.stringify({
        code: 'STALE_ENTITY_VERSION',
        message: 'The intervention changed. Refresh before trying again.',
        requestId: 'req-42',
      }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    ))

    await expect(apiRequest('/api/v1/interventions/INT-001')).rejects.toMatchObject({
      status: 409,
      code: 'STALE_ENTITY_VERSION',
      requestId: 'req-42',
    } satisfies Partial<ApiError>)
  })
})
