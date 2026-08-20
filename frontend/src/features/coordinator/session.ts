import { apiRequest } from '../../lib/api/client'
import type { CurrentUser } from './types'

const ACCESS_TOKEN_KEY = 'civicos.coordinator.accessToken'
const REFRESH_TOKEN_KEY = 'civicos.coordinator.refreshToken'

type TokenPair = {
  accessToken: string
  refreshToken: string
}

export function readCoordinatorAccessToken(): string | null {
  try {
    return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

export async function loadCoordinatorIdentity(accessToken: string) {
  const user = await apiRequest<CurrentUser>('/api/v1/auth/me', { accessToken })
  if (!user.roles.includes('COORDINATOR')) {
    throw new Error('This account does not have access to the coordinator workspace.')
  }
  return user
}

export async function signInCoordinator(email: string, password: string) {
  const pair = await apiRequest<TokenPair>('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  const user = await loadCoordinatorIdentity(pair.accessToken)
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, pair.accessToken)
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
  return { accessToken: pair.accessToken, user }
}
