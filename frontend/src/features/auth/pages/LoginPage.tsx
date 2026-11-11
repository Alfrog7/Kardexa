import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { ApiError } from '@shared/api/http'
import { useSession } from '@shared/auth/session'
import { login } from '../api/auth'
import { AuthLayout } from '../components/AuthLayout'

/** Inicio de sesión (HU-02): `POST /api/v1/auth/login`. Si ya hay sesión, va al panel. */
export function LoginPage() {
  const { user, expired, start } = useSession()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [sending, setSending] = useState(false)

  if (user) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/'} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(undefined)
    setSending(true)
    try {
      start(await login(email, password))
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'Algo salió mal. Inténtalo otra vez.')
      setSending(false)
    }
  }

  return (
    <AuthLayout title="Ingresa a tu negocio" description="Controla tu inventario y tus ventas en un solo lugar.">
      {expired && !error ? (
        <p className="auth__notice" role="status">
          Tu sesión venció. Ingresa de nuevo para continuar.
        </p>
      ) : null}
      <form className="auth__form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Correo</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>
        {error ? (
          <p className="auth__error" role="alert">
            {error}
          </p>
        ) : null}
        <button className="button" type="submit" disabled={sending}>
          {sending ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
      <p className="muted auth__alt">
        ¿Primera vez? <Link to="/registro-inicial">Crea al primer administrador</Link>
      </p>
    </AuthLayout>
  )
}
