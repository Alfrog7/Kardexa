import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { changeOwnPassword } from '../api/auth'

interface ChangePasswordFormProps {
  onChanged: () => void
}

/** Cambio de la contraseña propia (HU-04): pide la actual y la nueva. */
export function ChangePasswordForm({ onChanged }: ChangePasswordFormProps) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [currentError, setCurrentError] = useState<string>()
  const [nextError, setNextError] = useState<string>()
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setCurrentError(undefined)
    setNextError(undefined)
    setFormError(undefined)
    try {
      await changeOwnPassword(current, next)
      onChanged()
    } catch (failure) {
      showError(failure)
      setSaving(false)
    }
  }

  function showError(failure: unknown) {
    if (!(failure instanceof ApiError)) {
      setFormError('No pudimos cambiar la contraseña. Inténtalo otra vez.')
      return
    }
    const fieldError = failure.fieldErrors.find((item) => item.field === 'newPassword')
    if (fieldError) {
      setNextError(fieldError.message)
    } else if (failure.rule === 'RF-05') {
      setCurrentError(failure.message)
    } else {
      setFormError(failure.message)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="current-password">Contraseña actual</label>
        <input
          data-autofocus
          id="current-password"
          type="password"
          autoComplete="current-password"
          required
          value={current}
          aria-invalid={currentError ? true : undefined}
          onChange={(event) => setCurrent(event.target.value)}
        />
        {currentError ? <p className="field__error">{currentError}</p> : null}
      </div>
      <div className="field">
        <label htmlFor="new-password">Contraseña nueva</label>
        <input
          id="new-password"
          type="password"
          autoComplete="new-password"
          required
          value={next}
          aria-invalid={nextError ? true : undefined}
          onChange={(event) => setNext(event.target.value)}
        />
        {nextError ? <p className="field__error">{nextError}</p> : null}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        Cambiar contraseña
      </button>
    </form>
  )
}
