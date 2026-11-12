import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'

/** Límites de `Sale.VOID_REASON_*` en la API. */
const REASON_MIN_LENGTH = 3
const REASON_MAX_LENGTH = 255

interface VoidSaleFormProps {
  onSubmit: (reason: string) => Promise<void>
  onCancel: () => void
}

/** Pide el motivo de la anulación (HU-15). Si la API la rechaza, muestra el motivo y no cambia nada. */
export function VoidSaleForm({ onSubmit, onCancel }: VoidSaleFormProps) {
  const [reason, setReason] = useState('')
  const [reasonError, setReasonError] = useState<string>()
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(undefined)
    const trimmed = reason.trim()
    if (trimmed.length < REASON_MIN_LENGTH) {
      setReasonError(`Escribe el motivo (al menos ${REASON_MIN_LENGTH} caracteres).`)
      return
    }
    setReasonError(undefined)
    setSaving(true)
    try {
      await onSubmit(trimmed)
    } catch (error) {
      if (!(error instanceof ApiError)) {
        setFormError('No pudimos anular la venta. Inténtalo otra vez.')
      } else {
        const fieldError = error.fieldErrors.find((item) => item.field === 'reason')
        if (fieldError) setReasonError(fieldError.message)
        else setFormError(error.message)
      }
      setSaving(false)
    }
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <p className="muted">
        Se devuelve el stock de cada producto y la venta queda anulada con su número.
      </p>
      <div className="field">
        <label htmlFor="void-reason">Motivo de la anulación</label>
        <textarea
          data-autofocus
          id="void-reason"
          rows={3}
          value={reason}
          maxLength={REASON_MAX_LENGTH}
          aria-invalid={reasonError ? true : undefined}
          aria-describedby={reasonError ? 'void-reason-error' : undefined}
          onChange={(event) => setReason(event.target.value)}
        />
        {reasonError ? (
          <p id="void-reason-error" className="field__error">
            {reasonError}
          </p>
        ) : null}
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <div className="sale-receipt__actions">
        <button className="button button--secondary" type="button" onClick={onCancel}>
          Volver
        </button>
        <button className="button" type="submit" disabled={saving}>
          Anular venta
        </button>
      </div>
    </form>
  )
}
