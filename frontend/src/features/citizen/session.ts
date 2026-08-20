import { apiRequest } from '../../lib/api/client'

const ACCESS_TOKEN_KEY = 'civicos.citizen.accessToken'
const REFRESH_TOKEN_KEY = 'civicos.citizen.refreshToken'

type TokenPair = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
}

type CurrentUser = {
  roles: string[]
}

export function readCitizenAccessToken(): string | null {
  try {
    return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

export async function signInCitizen(email: string, password: string): Promise<string> {
  const pair = await apiRequest<TokenPair>('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
  })
  const currentUser = await apiRequest<CurrentUser>('/api/v1/auth/me', {
    accessToken: pair.accessToken,
  })
  if (!currentUser.roles.includes('CITIZEN')) {
    throw new Error('This account does not have access to the citizen workspace.')
  }
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, pair.accessToken)
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
  return pair.accessToken
}
