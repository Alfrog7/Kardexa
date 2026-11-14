import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import type { AdjustmentReason, Direction, StockAdjustmentRequest } from '../api/inventory'

/** Misma regla que `Decimal3` en la API; además, un ajuste no puede ser cero. */
const QUANTITY_PATTERN = /^\d{1,9}(\.\d{1,3})?$/
const NOTE_MAX_LENGTH = 255

const REASONS: { value: AdjustmentReason; label: string }[] = [
  { value: 'SHRINKAGE', label: 'Merma' },
  { value: 'PHYSICAL_COUNT', label: 'Conteo físico' },
  { value: 'CORRECTION', label: 'Corrección' },
]

interface AdjustmentFormProps {
  productId: string
  onSubmit: (request: StockAdjustmentRequest) => Promise<void>
}

/** Ajuste de stock (HU-09). Si la API lo rechaza, conserva lo escrito y explica el motivo. */
export function AdjustmentForm({ productId, onSubmit }: AdjustmentFormProps) {
  const [direction, setDirection] = useState<Direction>('OUT')
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState<AdjustmentReason>('SHRINKAGE')
  const [note, setNote] = useState('')
  const [quantityError, setQuantityError] = useState<string>()
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(undefined)
    const value = quantity.trim()
    if (!QUANTITY_PATTERN.test(value) || Number(value) === 0) {
      setQuantityError('Cantidad mayor que 0, con hasta 3 decimales, por ejemplo 2.5.')
      return
    }
    setQuantityError(undefined)
    setSaving(true)
    try {
      const trimmedNote = note.trim()
      await onSubmit({ productId, direction, quantity: value, reason, ...(trimmedNote ? { note: trimmedNote } : {}) })
    } catch (error) {
      showError(error)
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos registrar el ajuste. Inténtalo otra vez.')
      return
    }
    const fieldError = error.fieldErrors.find((item) => item.field === 'quantity')
    if (error.rule === 'RN-01') {
      setQuantityError(error.message)
    } else if (fieldError) {
      setQuantityError(fieldError.message)
    } else {
      setFormError(error.message)
    }
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="adjustment-direction">Tipo</label>
        <select
          data-autofocus
          id="adjustment-direction"
          value={direction}
          onChange={(event) => setDirection(event.target.value as Direction)}
        >
          <option value="OUT">Salida (resta stock)</option>
          <option value="IN">Entrada (suma stock)</option>
        </select>
      </div>
      <div className="field">
        <label htmlFor="adjustment-quantity">Cantidad</label>
        <input
          id="adjustment-quantity"
          inputMode="decimal"
          value={quantity}
          aria-invalid={quantityError ? true : undefined}
          aria-describedby={quantityError ? 'adjustment-quantity-error' : undefined}
          onChange={(event) => setQuantity(event.target.value)}
        />
        {quantityError ? (
          <p id="adjustment-quantity-error" className="field__error">
            {quantityError}
          </p>
        ) : null}
      </div>
      <div className="field">
        <label htmlFor="adjustment-reason">Motivo</label>
        <select
          id="adjustment-reason"
          value={reason}
          onChange={(event) => setReason(event.target.value as AdjustmentReason)}
        >
          {REASONS.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="adjustment-note">Nota (opcional)</label>
        <textarea
          id="adjustment-note"
          rows={2}
          value={note}
          maxLength={NOTE_MAX_LENGTH}
          onChange={(event) => setNote(event.target.value)}
        />
      </div>
      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        Registrar ajuste
      </button>
    </form>
  )
}
