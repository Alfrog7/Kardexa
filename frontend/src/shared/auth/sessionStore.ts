import type { Session } from './session'

const KEY = 'kardexa.session'

/** Segundos de margen: un token a punto de vencer no se restaura. */
const EXPIRY_MARGIN_SECONDS = 30

/** `exp` del JWT en segundos, o `undefined` si el token no se puede leer. */
function expiresAt(token: string): number | undefined {
  try {
    const payload = token.split('.')[1]
    if (!payload) return undefined
    const json = atob(payload.replaceAll('-', '+').replaceAll('_', '/'))
    const exp = (JSON.parse(json) as { exp?: unknown }).exp
    return typeof exp === 'number' ? exp : undefined
  } catch {
    return undefined
  }
}

/**
 * Sesión guardada en `sessionStorage` (ADR 0011): sobrevive a una recarga (F5 o Ctrl+R) y se borra al
 * cerrar la pestaña. Un token vencido o ilegible no se restaura.
 */
export function loadSession(): Session | undefined {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return undefined
    const session = JSON.parse(raw) as Session
    const exp = session.token ? expiresAt(session.token) : undefined
    if (!session.user || exp === undefined || exp - EXPIRY_MARGIN_SECONDS <= Date.now() / 1000) {
      sessionStorage.removeItem(KEY)
      return undefined
    }
    return session
  } catch {
    return undefined
  }
}

export function saveSession(session: Session): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  } catch {
    // Sin almacenamiento (modo privado estricto): la sesión sigue solo en memoria.
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // Nada que borrar.
  }
}
