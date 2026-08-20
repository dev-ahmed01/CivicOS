export type ApiErrorBody = {
  code?: string
  message?: string
  requestId?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly fieldErrors: Readonly<Record<string, string>>

  constructor(status: number, body: ApiErrorBody) {
    super(body.message ?? `CivicOS API request failed with status ${status}.`)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code ?? 'API_REQUEST_FAILED'
    this.requestId = body.requestId
    this.fieldErrors = Object.freeze({ ...(body.fieldErrors ?? {}) })
  }
}

export type ApiRequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
  accessToken?: string
  idempotencyKey?: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { accessToken, body, idempotencyKey, ...requestOptions } = options
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  if (idempotencyKey) {
    headers.set('Idempotency-Key', idempotencyKey)
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...requestOptions,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorBody(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export async function apiMultipartRequest<T>(
  path: string,
  formData: FormData,
  accessToken: string,
  idempotencyKey: string,
): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'Idempotency-Key': idempotencyKey,
    },
    body: formData,
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorBody(response))
  }

  return response.json() as Promise<T>
}

async function readErrorBody(response: Response): Promise<ApiErrorBody> {
  try {
    return await response.json() as ApiErrorBody
  } catch {
    return { message: 'The server returned an unreadable error response.' }
  }
}
