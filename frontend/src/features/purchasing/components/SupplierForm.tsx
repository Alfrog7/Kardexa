import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { Supplier, SupplierData } from '../api/suppliers'

/** Límites de `Supplier` en la API. */
const BUSINESS_NAME_MAX_LENGTH = 150
const PHONE_MAX_LENGTH = 20
const EMAIL_MAX_LENGTH = 254

type FieldName = 'ruc' | 'businessName' | 'phone' | 'email'
type FieldErrors = Partial<Record<FieldName, string>>

interface SupplierFormProps {
  /** Proveedor que se edita; sin él, el formulario crea uno nuevo. */
  supplier?: Supplier
  onSubmit: (data: SupplierData) => Promise<void>
}

function validate(data: SupplierData): FieldErrors {
  const errors: FieldErrors = {}
  if (!/^\d{11}$/.test(data.ruc)) errors.ruc = 'El RUC debe tener exactamente 11 dígitos.'
  if (data.businessName.length < 2) errors.businessName = 'Escribe la razón social.'
  return errors
}

/** Alta y edición (HU-11). El RUC repetido (RN-10) se muestra junto a su campo. */
export function SupplierForm({ supplier, onSubmit }: SupplierFormProps) {
  const [ruc, setRuc] = useState(supplier?.ruc ?? '')
  const [businessName, setBusinessName] = useState(supplier?.businessName ?? '')
  const [phone, setPhone] = useState(supplier?.phone ?? '')
  const [email, setEmail] = useState(supplier?.email ?? '')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const data: SupplierData = {
      ruc: ruc.trim(),
      businessName: businessName.trim(),
      phone: phone.trim() || null,
      email: email.trim() || null,
      active: supplier?.active ?? true,
    }
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
      setFormError('No pudimos guardar el proveedor. Inténtalo otra vez.')
      return
    }
    const fromApi: FieldErrors = {}
    for (const { field, message } of error.fieldErrors) {
      fromApi[field as FieldName] ??= message
    }
    if (error.rule === 'RN-10') fromApi.ruc = error.message
    setErrors(fromApi)
    if (Object.keys(fromApi).length === 0) setFormError(error.message)
  }

  function describe(field: FieldName) {
    return errors[field] ? { 'aria-invalid': true, 'aria-describedby': `supplier-${field}-error` } : {}
  }

  function errorOf(field: FieldName) {
    return errors[field] ? (
      <p id={`supplier-${field}-error`} className="field__error">
        {errors[field]}
      </p>
    ) : null
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="supplier-ruc">RUC</label>
        <input
          data-autofocus
          id="supplier-ruc"
          inputMode="numeric"
          value={ruc}
          maxLength={11}
          onChange={(event) => setRuc(event.target.value)}
          {...describe('ruc')}
        />
        {errorOf('ruc')}
      </div>
      <div className="field">
        <label htmlFor="supplier-businessName">Razón social</label>
        <input
          id="supplier-businessName"
          value={businessName}
          maxLength={BUSINESS_NAME_MAX_LENGTH}
          onChange={(event) => setBusinessName(event.target.value)}
          {...describe('businessName')}
        />
        {errorOf('businessName')}
      </div>
      <div className="field">
        <label htmlFor="supplier-phone">Teléfono (opcional)</label>
        <input
          id="supplier-phone"
          type="tel"
          value={phone}
          maxLength={PHONE_MAX_LENGTH}
          onChange={(event) => setPhone(event.target.value)}
          {...describe('phone')}
        />
        {errorOf('phone')}
      </div>
      <div className="field">
        <label htmlFor="supplier-email">Correo (opcional)</label>
        <input
          id="supplier-email"
          type="email"
          value={email}
          maxLength={EMAIL_MAX_LENGTH}
          onChange={(event) => setEmail(event.target.value)}
          {...describe('email')}
        />
        {errorOf('email')}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        {supplier ? 'Guardar cambios' : 'Crear proveedor'}
      </button>
    </form>
  )
}
