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

/** `POST /auth/setup` (HU-01): crea al primer administrador; con usuarios existentes la API responde 403. */
export async function registerFirstAdmin(fullName: string, email: string, password: string): Promise<SessionUser> {
  return apiRequest<SessionUser>('/auth/setup', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  })
}
