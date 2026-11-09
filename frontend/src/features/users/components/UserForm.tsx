import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Role } from '@shared/auth/session'
import type { User } from '../api/users'

export interface UserFormValues {
  fullName: string
  email: string
  password: string
  role: Role
}

interface UserFormProps {
  /** Usuario que se edita; sin él, el formulario crea uno nuevo. */
  user?: User
  onSubmit: (values: UserFormValues) => Promise<void>
}

type FieldName = 'fullName' | 'email' | 'password' | 'role'

/** Alta y edición. Al editar solo se cambian el nombre y el rol; el correo y la contraseña no. */
export function UserForm({ user, onSubmit }: UserFormProps) {
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>(user?.role ?? 'EMPLOYEE')
  const [errors, setErrors] = useState<Partial<Record<FieldName, string>>>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setFormError(undefined)
    try {
      await onSubmit({ fullName, email, password, role })
    } catch (failure) {
      showError(failure)
      setSaving(false)
    }
  }

  function showError(failure: unknown) {
    if (!(failure instanceof ApiError)) {
      setFormError('No pudimos guardar el usuario. Inténtalo otra vez.')
      return
    }
    const found: Partial<Record<FieldName, string>> = {}
    for (const item of failure.fieldErrors) {
      if (item.field === 'fullName' || item.field === 'email' || item.field === 'password' || item.field === 'role') {
        found[item.field] = item.message
      }
    }
    if (Object.keys(found).length > 0) {
      setErrors(found)
    } else {
      setFormError(failure.message)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="user-name">Nombre completo</label>
        <input
          data-autofocus
          id="user-name"
          name="fullName"
          value={fullName}
          required
          aria-invalid={errors.fullName ? true : undefined}
          onChange={(event) => setFullName(event.target.value)}
        />
        {errors.fullName ? <p className="field__error">{errors.fullName}</p> : null}
      </div>
      {user ? null : (
        <>
          <div className="field">
            <label htmlFor="user-email">Correo</label>
            <input
              id="user-email"
              name="email"
              type="email"
              value={email}
              required
              aria-invalid={errors.email ? true : undefined}
              onChange={(event) => setEmail(event.target.value)}
            />
            {errors.email ? <p className="field__error">{errors.email}</p> : null}
          </div>
          <div className="field">
            <label htmlFor="user-password">Contraseña inicial</label>
            <input
              id="user-password"
              name="password"
              type="password"
              autoComplete="new-password"
              value={password}
              required
              aria-invalid={errors.password ? true : undefined}
              onChange={(event) => setPassword(event.target.value)}
            />
            {errors.password ? <p className="field__error">{errors.password}</p> : null}
          </div>
        </>
      )}
      <div className="field">
        <label htmlFor="user-role">Rol</label>
        <select id="user-role" name="role" value={role} onChange={(event) => setRole(event.target.value as Role)}>
          <option value="EMPLOYEE">Empleado</option>
          <option value="ADMIN">Administrador</option>
        </select>
        {errors.role ? <p className="field__error">{errors.role}</p> : null}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        {user ? 'Guardar cambios' : 'Crear usuario'}
      </button>
    </form>
  )
}
