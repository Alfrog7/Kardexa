import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { formatMoney, formatQuantity } from '@shared/lib/format'
import { QUANTITY_PATTERN, centsToAmount, exceeds, subtotalCents } from '@shared/lib/amounts'
import type { ProductOption, SaleRequest } from '../api/sales'
import { ProductSearch } from './ProductSearch'

interface ItemRow {
  product: ProductOption
  quantity: string
}

/** Claves: `items` e `items.N.quantity`. */
type Errors = Record<string, string>

interface SaleFormProps {
  onSubmit: (request: SaleRequest) => Promise<void>
}

function quantityError(row: ItemRow): string | undefined {
  const quantity = row.quantity.trim()
  if (!QUANTITY_PATTERN.test(quantity)) return 'Cantidad con hasta 3 decimales, por ejemplo 2.5.'
  if (Number(quantity) <= 0) return 'La cantidad debe ser mayor que 0.'
  if (exceeds(quantity, row.product.stock)) return `Solo hay ${formatQuantity(row.product.stock)} en stock.`
  return undefined
}

function validate(rows: ItemRow[]): Errors {
  const errors: Errors = {}
  if (rows.length === 0) errors.items = 'Agrega al menos un producto.'
  rows.forEach((row, index) => {
    const message = quantityError(row)
    if (message) errors[`items.${index}.quantity`] = message
  })
  return errors
}

/** `items[1].quantityPositive` → `items.1.quantity`; `itemsWithoutRepeatedProducts` → `items`. */
function errorKey(field: string): string {
  const item = /^items\[(\d+)\]\.quantity/.exec(field)
  if (item) return `items.${item[1]}.quantity`
  return field.startsWith('items') ? 'items' : field
}

/**
 * Registro de una venta (HU-14): el precio viene del producto y el total se calcula en vivo. Una cantidad
 * mayor que el stock se marca antes de enviar; si la API la rechaza (RN-01), se muestra qué producto no alcanza.
 */
export function SaleForm({ onSubmit }: SaleFormProps) {
  const [rows, setRows] = useState<ItemRow[]>([])
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  const subtotals = rows.map((row) => subtotalCents(row.quantity.trim(), row.product.salePrice))
  const total = rows.length > 0 && subtotals.every((value) => value !== null)
    ? subtotals.reduce<bigint>((sum, value) => sum + (value ?? 0n), 0n)
    : null

  function updateQuantity(index: number, quantity: string) {
    setRows((current) => current.map((row, i) => (i === index ? { ...row, quantity } : row)))
    setErrors((current) => {
      const rest = { ...current }
      delete rest[`items.${index}.quantity`]
      return rest
    })
  }

  function addProduct(product: ProductOption) {
    setRows((current) => [...current, { product, quantity: '1' }])
    setErrors({})
    setFormError(undefined)
  }

  function removeRow(index: number) {
    setRows((current) => current.filter((_, i) => i !== index))
    setErrors({})
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const invalid = validate(rows)
    setErrors(invalid)
    setFormError(undefined)
    if (Object.keys(invalid).length > 0) return

    setSaving(true)
    try {
      await onSubmit({ items: rows.map((row) => ({ productId: row.product.id, quantity: row.quantity.trim() })) })
    } catch (error) {
      showError(error)
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos registrar la venta. Inténtalo otra vez.')
      return
    }
    const fromApi: Errors = {}
    for (const { field, message } of error.fieldErrors) {
      fromApi[errorKey(field)] ??= message
    }
    setErrors(fromApi)
    if (Object.keys(fromApi).length > 0) return
    setFormError(
      error.status === 409 ? 'Otra venta cambió el stock al mismo tiempo. Revisa las cantidades e inténtalo otra vez.' : error.message,
    )
  }

  function describe(key: string) {
    return errors[key] ? { 'aria-invalid': true, 'aria-describedby': `sale-${key.replaceAll('.', '-')}-error` } : {}
  }

  function errorOf(key: string) {
    return errors[key] ? (
      <p id={`sale-${key.replaceAll('.', '-')}-error`} className="field__error">
        {errors[key]}
      </p>
    ) : null
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <ProductSearch excludedIds={rows.map((row) => row.product.id)} onSelect={addProduct} />
      {errors.items ? (
        <p className="field__error" role="alert">
          {errors.items}
        </p>
      ) : null}

      {rows.length > 0 ? (
        <ul className="sale-form__items" aria-label="Productos de la venta">
          {rows.map((row, index) => {
            const subtotal = subtotals[index]
            return (
              <li key={row.product.id} className="sale-form__item card">
                <div className="sale-form__item-head">
                  <strong>{row.product.name}</strong>
                  <button className="link-button" type="button" onClick={() => removeRow(index)}>
                    Quitar
                  </button>
                </div>
                <div className="sale-form__item-fields">
                  <div className="field">
                    <label htmlFor={`sale-items-${index}-quantity`}>Cantidad de {row.product.name}</label>
                    <input
                      id={`sale-items-${index}-quantity`}
                      inputMode="decimal"
                      value={row.quantity}
                      maxLength={13}
                      onChange={(event) => updateQuantity(index, event.target.value)}
                      {...describe(`items.${index}.quantity`)}
                    />
                    {errorOf(`items.${index}.quantity`)}
                  </div>
                  <p className="sale-form__price muted">
                    {formatMoney(row.product.salePrice)} c/u · stock {formatQuantity(row.product.stock)}
                  </p>
                </div>
                <p className="sale-form__subtotal">
                  Subtotal: {subtotal == null ? '—' : formatMoney(centsToAmount(subtotal))}
                </p>
              </li>
            )
          })}
        </ul>
      ) : null}

      <p className="sale-form__total" aria-live="polite">
        Total: <strong>{total === null ? '—' : formatMoney(centsToAmount(total))}</strong>
      </p>

      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        Registrar venta
      </button>
    </form>
  )
}
