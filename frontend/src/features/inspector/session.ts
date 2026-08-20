import { apiRequest } from '../../lib/api/client'
import type { CurrentUser } from './types'

const ACCESS_TOKEN_KEY = 'civicos.inspector.accessToken'
const REFRESH_TOKEN_KEY = 'civicos.inspector.refreshToken'

type TokenPair = { accessToken: string; refreshToken: string }

export function readInspectorAccessToken(): string | null {
  try { return window.sessionStorage.getItem(ACCESS_TOKEN_KEY) } catch { return null }
}

export async function loadInspectorIdentity(accessToken: string) {
  const user = await apiRequest<CurrentUser>('/api/v1/auth/me', { accessToken })
  if (!user.roles.includes('INSPECTOR')) throw new Error('This account does not have access to the Inspector workspace.')
  return user
}

export async function signInInspector(email: string, password: string) {
  const pair = await apiRequest<TokenPair>('/api/v1/auth/login', { method: 'POST', body: { email, password } })
  const user = await loadInspectorIdentity(pair.accessToken)
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, pair.accessToken)
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
  return { accessToken: pair.accessToken, user }
}
