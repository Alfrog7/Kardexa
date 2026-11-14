import { afterEach, describe, expect, it } from 'vitest'
import { clearSession, loadSession, saveSession } from './sessionStore'
import type { Session } from './session'

function tokenExpiringIn(seconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'u1', exp: Math.floor(Date.now() / 1000) + seconds }))
  return `h.${payload.replaceAll('=', '')}.s`
}

const user = { id: 'u1', fullName: 'Ana', email: 'ana@bodega.pe', role: 'ADMIN' as const, active: true }

describe('sessionStore', () => {
  afterEach(() => sessionStorage.clear())

  it('restaura una sesión vigente después de recargar', () => {
    const session: Session = { token: tokenExpiringIn(3600), user }
    saveSession(session)
    expect(loadSession()).toEqual(session)
  })

  it('descarta un token vencido o ilegible', () => {
    saveSession({ token: tokenExpiringIn(-10), user })
    expect(loadSession()).toBeUndefined()
    saveSession({ token: 'no-es-un-jwt', user })
    expect(loadSession()).toBeUndefined()
  })

  it('al cerrar la sesión no queda nada guardado', () => {
    saveSession({ token: tokenExpiringIn(3600), user })
    clearSession()
    expect(loadSession()).toBeUndefined()
  })
})
