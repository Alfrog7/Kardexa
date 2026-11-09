import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setAccessToken, setUnauthorizedHandler } from '@shared/api/http'
import { SessionContext, type Session, type SessionUser } from './session'

interface SessionProviderProps {
  children: ReactNode
  /** Sesión ya abierta, para los tests. */
  initialSession?: Session
}

export function SessionProvider({ children, initialSession }: SessionProviderProps) {
  const [user, setUser] = useState<SessionUser | undefined>(initialSession?.user)
  const [expired, setExpired] = useState(false)

  if (initialSession) {
    setAccessToken(initialSession.token)
  }

  const start = useCallback((session: Session) => {
    setAccessToken(session.token)
    setExpired(false)
    setUser(session.user)
  }, [])

  const end = useCallback(() => {
    setAccessToken(undefined)
    setUser(undefined)
  }, [])

  // Un 401 con la sesión abierta significa que el token venció: se vuelve al ingreso con un aviso.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setAccessToken(undefined)
      setUser(undefined)
      setExpired(true)
    })
    return () => setUnauthorizedHandler(undefined)
  }, [])

  const value = useMemo(() => ({ user, expired, start, end }), [user, expired, start, end])
  return <SessionContext value={value}>{children}</SessionContext>
}
