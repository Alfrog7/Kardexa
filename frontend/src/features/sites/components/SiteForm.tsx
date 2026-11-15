import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Site, SiteData } from '../api/sites'

/** Límites de `SiteRequest` en la API. */
const NAME_MAX_LENGTH = 80
const ADDRESS_MAX_LENGTH = 200

type FieldName = 'name' | 'address'
type FieldErrors = Partial<Record<FieldName, string>>

interface SiteFormProps {
  /** Sede que se edita; sin ella, el formulario crea una nueva. */
  site?: Site
  onSubmit: (data: SiteData) => Promise<void>
}

function validate(data: SiteData): FieldErrors {
  const errors: FieldErrors = {}
  if (data.name.length < 2) errors.name = 'Escribe el nombre de la sede.'
  return errors
}

/** Alta y edición de una sede (HU-19). Un nombre repetido se muestra junto a su campo. */
export function SiteForm({ site, onSubmit }: SiteFormProps) {
  const [name, setName] = useState(site?.name ?? '')
  const [address, setAddress] = useState(site?.address ?? '')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const data: SiteData = { name: name.trim(), address: address.trim() || null }
    const invalid = validate(data)
    setErrors(invalid)
    setFormError(undefined)
    if (Object.keys(invalid).length > 0) return

    setSaving(true)
    try {
      await onSubmit(data)
    } catch (error) {
      showError(error)
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos guardar la sede. Inténtalo otra vez.')
      return
    }
    const fromApi: FieldErrors = {}
    for (const { field, message } of error.fieldErrors) {
      fromApi[field as FieldName] ??= message
    }
    if (error.status === 409) fromApi.name = error.message
    setErrors(fromApi)
    if (Object.keys(fromApi).length === 0) setFormError(error.message)
  }

  function describe(field: FieldName) {
    return errors[field] ? { 'aria-invalid': true, 'aria-describedby': `site-${field}-error` } : {}
  }

  function errorOf(field: FieldName) {
    return errors[field] ? (
      <p id={`site-${field}-error`} className="field__error">
        {errors[field]}
      </p>
    ) : null
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="site-name">Nombre</label>
        <input
          data-autofocus
          id="site-name"
          value={name}
          maxLength={NAME_MAX_LENGTH}
          onChange={(event) => setName(event.target.value)}
          {...describe('name')}
        />
        {errorOf('name')}
      </div>
      <div className="field">
        <label htmlFor="site-address">Dirección (opcional)</label>
        <input
          id="site-address"
          value={address}
          maxLength={ADDRESS_MAX_LENGTH}
          onChange={(event) => setAddress(event.target.value)}
          {...describe('address')}
        />
        {errorOf('address')}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        {site ? 'Guardar cambios' : 'Crear sede'}
      </button>
    </form>
  )
}
