import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setAccessToken, setUnauthorizedHandler } from '@shared/api/http'
import { SessionContext, type Session, type SessionUser } from './session'
import { clearSession, loadSession, saveSession } from './sessionStore'

interface SessionProviderProps {
  children: ReactNode
  /** Sesión ya abierta, para los tests. */
  initialSession?: Session
}

export function SessionProvider({ children, initialSession }: SessionProviderProps) {
  // Sin sesión de prueba, se restaura la guardada: recargar la página no obliga a ingresar de nuevo.
  const [restored] = useState(() => initialSession ?? loadSession())
  const [user, setUser] = useState<SessionUser | undefined>(restored?.user)
  const [expired, setExpired] = useState(false)

  if (restored) {
    setAccessToken(restored.token)
  }

  const start = useCallback((session: Session) => {
    setAccessToken(session.token)
    saveSession(session)
    setExpired(false)
    setUser(session.user)
  }, [])

  const end = useCallback(() => {
    setAccessToken(undefined)
    clearSession()
    setUser(undefined)
  }, [])

  // Un 401 con la sesión abierta significa que el token venció: se vuelve al ingreso con un aviso.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setAccessToken(undefined)
      clearSession()
      setUser(undefined)
      setExpired(true)
    })
    return () => setUnauthorizedHandler(undefined)
  }, [])

  const value = useMemo(() => ({ user, expired, start, end }), [user, expired, start, end])
  return <SessionContext value={value}>{children}</SessionContext>
}
