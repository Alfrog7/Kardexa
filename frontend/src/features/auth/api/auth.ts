import { apiRequest } from '@shared/api/http'
import type { Session, SessionUser } from '@shared/auth/session'

interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  /** Segundos hasta el vencimiento. */
  expiresIn: number
  user: SessionUser
}

/** `POST /auth/login` (HU-02). Con credenciales incorrectas la API responde 401 con un mensaje genérico. */
export async function login(email: string, password: string): Promise<Session> {
  const response = await apiRequest<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  return { token: response.accessToken, user: response.user }
}

/**
 * `GET /auth/setup-status`: `true` solo mientras no existe ningún usuario. Ante cualquier fallo devuelve `undefined`
 * (no se sabe): el servidor igual rechaza el registro inicial si ya hay cuentas.
 */
export async function isSetupPending(): Promise<boolean | undefined> {
  try {
    const status = await apiRequest<{ pending?: boolean }>('/auth/setup-status')
    return typeof status?.pending === 'boolean' ? status.pending : undefined
  } catch {
    return undefined
  }
}

/** `POST /auth/setup` (HU-01): crea al primer administrador; con usuarios existentes la API responde 403. */
export async function registerFirstAdmin(fullName: string, email: string, password: string): Promise<SessionUser> {
  return apiRequest<SessionUser>('/auth/setup', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  })
}

/** `PUT /auth/me/password` (HU-04): con la actual incorrecta la API responde 422 con `rule: RF-05`. */
export function changeOwnPassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiRequest<void>('/auth/me/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}
