import { apiRequest } from '../../lib/api/client'
import type { CurrentUser } from './types'

const ACCESS_TOKEN_KEY = 'civicos.agency.accessToken'
const REFRESH_TOKEN_KEY = 'civicos.agency.refreshToken'

type TokenPair = {
  accessToken: string
  refreshToken: string
}

export function readAgencyAccessToken(): string | null {
  try {
    return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

export async function loadAgencyIdentity(accessToken: string) {
  const user = await apiRequest<CurrentUser>('/api/v1/auth/me', { accessToken })
  assertAgencyOfficer(user)
  return user
}

export async function signInAgency(email: string, password: string) {
  const pair = await apiRequest<TokenPair>('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  const user = await loadAgencyIdentity(pair.accessToken)
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, pair.accessToken)
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
  return { accessToken: pair.accessToken, user }
}

function assertAgencyOfficer(user: CurrentUser) {
  if (!user.roles.includes('AGENCY_OFFICER') || !user.agencyId) {
    throw new Error('This account does not have access to an agency workspace.')
  }
}
