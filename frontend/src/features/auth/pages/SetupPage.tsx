import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { ApiError } from '@shared/api/http'
import { useSession } from '@shared/auth/session'
import { Logo } from '@shared/ui/Logo'
import { login, registerFirstAdmin } from '../api/auth'
import './LoginPage.css'

type FieldName = 'fullName' | 'email' | 'password'

/** Registro inicial (HU-01): crea al primer administrador y abre su sesión. */
export function SetupPage() {
  const { user, start } = useSession()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<FieldName, string>>>({})
  const [formError, setFormError] = useState<string>()
  const [closed, setClosed] = useState(false)
  const [sending, setSending] = useState(false)

  if (user) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFieldErrors({})
    setFormError(undefined)
    setSending(true)
    try {
      await registerFirstAdmin(fullName, email, password)
      start(await login(email, password))
    } catch (failure) {
      showError(failure)
      setSending(false)
    }
  }

  function showError(failure: unknown) {
    if (!(failure instanceof ApiError)) {
      setFormError('Algo salió mal. Inténtalo otra vez.')
      return
    }
    if (failure.status === 403) {
      setClosed(true)
      return
    }
    const errors: Partial<Record<FieldName, string>> = {}
    for (const item of failure.fieldErrors) {
      if (item.field === 'fullName' || item.field === 'email' || item.field === 'password') {
        errors[item.field] = item.message
      }
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
    } else {
      setFormError(failure.message)
    }
  }

  if (closed) {
    return (
      <main className="login">
        <div className="login__card card">
          <Logo height={40} />
          <div>
            <h1 className="login__title">El registro inicial ya se hizo</h1>
            <p className="muted">
              Ya existe un administrador. Ingresa con tu cuenta o pídele a un administrador que cree la tuya.
            </p>
          </div>
          <Link className="button" to="/ingresar">
            Ir a ingresar
          </Link>
        </div>
      </main>
    )
  }

  return (
    <main className="login">
      <div className="login__card card">
        <Logo height={40} />
        <div>
          <h1 className="login__title">Crea al primer administrador</h1>
          <p className="muted">Solo se puede hacer una vez, cuando el sistema aún no tiene usuarios.</p>
        </div>
        <form className="login__form" onSubmit={handleSubmit}>
          <TextField
            id="fullName"
            label="Nombre completo"
            value={fullName}
            onChange={setFullName}
            error={fieldErrors.fullName}
            autoComplete="name"
          />
          <TextField
            id="email"
            label="Correo"
            type="email"
            value={email}
            onChange={setEmail}
            error={fieldErrors.email}
            autoComplete="username"
          />
          <TextField
            id="password"
            label="Contraseña"
            type="password"
            value={password}
            onChange={setPassword}
            error={fieldErrors.password}
            autoComplete="new-password"
          />
          {formError ? (
            <p className="login__error" role="alert">
              {formError}
            </p>
          ) : null}
          <button className="button" type="submit" disabled={sending}>
            {sending ? 'Creando…' : 'Crear administrador'}
          </button>
        </form>
        <p className="muted">
          ¿Ya tienes cuenta? <Link to="/ingresar">Ingresar</Link>
        </p>
      </div>
    </main>
  )
}

interface TextFieldProps {
  id: FieldName
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  type?: 'text' | 'email' | 'password'
  autoComplete: string
}

function TextField({ id, label, value, onChange, error, type = 'text', autoComplete }: TextFieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        name={id}
        type={type}
        autoComplete={autoComplete}
        required
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <p id={`${id}-error`} className="field__error">
          {error}
        </p>
      ) : null}
    </div>
  )
}
