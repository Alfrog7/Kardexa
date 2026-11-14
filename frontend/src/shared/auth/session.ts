import { createContext, useContext } from 'react'

export type Role = 'ADMIN' | 'EMPLOYEE'

export interface SessionUser {
  id: string
  fullName: string
  email: string
  role: Role
  active: boolean
}

export interface Session {
  token: string
  user: SessionUser
}

export interface SessionContextValue {
  /** Usuario con la sesión abierta; `undefined` si no hay sesión. */
  user?: SessionUser
  /** La sesión se cerró porque el token venció o dejó de ser válido. */
  expired: boolean
  /** Abre la sesión. Se guarda en `sessionStorage` (ADR 0011): una recarga no la cierra; cerrar la pestaña sí. */
  start: (session: Session) => void
  /** Cierra la sesión y descarta el token. */
  end: () => void
}

/**
 * Sin proveedor (por ejemplo en los tests de una sola pantalla) la interfaz no oculta nada: ocultar
 * acciones es una comodidad, quien decide los permisos es siempre la API (403).
 */
export const SessionContext = createContext<SessionContextValue | null>(null)

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession debe usarse dentro de <SessionProvider>.')
  }
  return context
}

/** `true` si la persona puede administrar (crear, editar, ajustar). Sin proveedor, no oculta nada. */
export function useCanManage(): boolean {
  const context = useContext(SessionContext)
  return context === null || context.user?.role === 'ADMIN'
}
