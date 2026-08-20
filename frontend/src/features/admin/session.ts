import { apiRequest } from '../../lib/api/client'
import type { CurrentUser } from './types'
const ACCESS_TOKEN_KEY = 'civicos.admin.accessToken'; const REFRESH_TOKEN_KEY = 'civicos.admin.refreshToken'
type TokenPair = { accessToken: string; refreshToken: string }
export function readAdminAccessToken() { try { return window.sessionStorage.getItem(ACCESS_TOKEN_KEY) } catch { return null } }
export async function loadAdminIdentity(accessToken: string) { const user = await apiRequest<CurrentUser>('/api/v1/auth/me', { accessToken }); if (!user.roles.includes('ADMIN')) throw new Error('This account does not have access to the Admin workspace.'); return user }
export async function signInAdmin(email: string, password: string) { const pair = await apiRequest<TokenPair>('/api/v1/auth/login', { method: 'POST', body: { email, password } }); const user = await loadAdminIdentity(pair.accessToken); window.sessionStorage.setItem(ACCESS_TOKEN_KEY, pair.accessToken); window.sessionStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken); return { accessToken: pair.accessToken, user } }
