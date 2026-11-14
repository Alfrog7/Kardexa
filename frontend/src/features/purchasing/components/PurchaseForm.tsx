import { useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { formatMoney } from '@shared/lib/format'
import type { ProductOption, PurchaseRequest } from '../api/purchases'
import type { Supplier } from '../api/suppliers'
import { AMOUNT_PATTERN, QUANTITY_PATTERN, centsToAmount, subtotalCents } from '@shared/lib/amounts'
import { ProductSearch } from './ProductSearch'

/** Límite de `Purchase.SUPPLIER_DOCUMENT_MAX_LENGTH` en la API. */
const DOCUMENT_MAX_LENGTH = 40

interface ItemRow {
  product: ProductOption
  quantity: string
  unitCost: string
}

/** Claves: `supplierId`, `purchaseDate`, `supplierDocument`, `items` e `items.N.quantity` / `items.N.unitCost`. */
type Errors = Record<string, string>

interface PurchaseFormProps {
  /** Proveedores activos para elegir. */
  suppliers: Supplier[]
  /** Fecha del negocio de hoy, `AAAA-MM-DD`. */
  today: string
  onSubmit: (request: PurchaseRequest) => Promise<void>
  /** Producto ya agregado al abrir, por ejemplo desde el stock crítico del inicio. */
  initialProduct?: ProductOption
}

function validate(supplierId: string, purchaseDate: string, rows: ItemRow[]): Errors {
  const errors: Errors = {}
  if (!supplierId) errors.supplierId = 'Elige el proveedor.'
  if (!purchaseDate) errors.purchaseDate = 'Indica la fecha de compra.'
  if (rows.length === 0) errors.items = 'Agrega al menos un producto.'
  rows.forEach((row, index) => {
    const quantity = row.quantity.trim()
    if (!QUANTITY_PATTERN.test(quantity)) {
      errors[`items.${index}.quantity`] = 'Cantidad con hasta 3 decimales, por ejemplo 2.5.'
    } else if (Number(quantity) <= 0) {
      errors[`items.${index}.quantity`] = 'La cantidad debe ser mayor que 0.'
    }
    if (!AMOUNT_PATTERN.test(row.unitCost.trim())) {
      errors[`items.${index}.unitCost`] = 'Monto con hasta 2 decimales, por ejemplo 3.50.'
    }
  })
  return errors
}

/** `items[1].quantityPositive` → `items.1.quantity`; `itemsWithoutRepeatedProducts` → `items`. */
function errorKey(field: string): string {
  const item = /^items\[(\d+)\]\.(quantity|unitCost)/.exec(field)
  if (item) return `items.${item[1]}.${item[2]}`
  return field.startsWith('items') ? 'items' : field
}

/** Registro de una compra con varios productos (HU-12), con subtotales y total en vivo. */
export function PurchaseForm({ suppliers, today, onSubmit, initialProduct }: PurchaseFormProps) {
  const [supplierId, setSupplierId] = useState('')
  const [purchaseDate, setPurchaseDate] = useState(today)
  const [document, setDocument] = useState('')
  const [rows, setRows] = useState<ItemRow[]>(() => (initialProduct ? [{ product: initialProduct, quantity: '', unitCost: '' }] : []))
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string>()
  const [saving, setSaving] = useState(false)

  const subtotals = rows.map((row) => subtotalCents(row.quantity.trim(), row.unitCost.trim()))
  const total = subtotals.every((value) => value !== null)
    ? subtotals.reduce<bigint>((sum, value) => sum + (value ?? 0n), 0n)
    : null

  function updateRow(index: number, change: Partial<ItemRow>) {
    setRows((current) => current.map((row, i) => (i === index ? { ...row, ...change } : row)))
  }

  function addProduct(product: ProductOption) {
    setRows((current) => [...current, { product, quantity: '', unitCost: '' }])
  }

  function removeRow(index: number) {
    setRows((current) => current.filter((_, i) => i !== index))
    setErrors({})
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const invalid = validate(supplierId, purchaseDate, rows)
    setErrors(invalid)
    setFormError(undefined)
    if (Object.keys(invalid).length > 0) return

    setSaving(true)
    try {
      await onSubmit({
        supplierId,
        supplierDocument: document.trim() || null,
        purchaseDate,
        items: rows.map((row) => ({
          productId: row.product.id,
          quantity: row.quantity.trim(),
          unitCost: row.unitCost.trim(),
        })),
      })
    } catch (error) {
      showError(error)
      setSaving(false)
    }
  }

  function showError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError('No pudimos registrar la compra. Inténtalo otra vez.')
      return
    }
    const fromApi: Errors = {}
    for (const { field, message } of error.fieldErrors) {
      fromApi[errorKey(field)] ??= message
    }
    setErrors(fromApi)
    if (Object.keys(fromApi).length === 0) setFormError(error.message)
  }

  function describe(key: string) {
    return errors[key] ? { 'aria-invalid': true, 'aria-describedby': `purchase-${key.replaceAll('.', '-')}-error` } : {}
  }

  function errorOf(key: string) {
    return errors[key] ? (
      <p id={`purchase-${key.replaceAll('.', '-')}-error`} className="field__error">
        {errors[key]}
      </p>
    ) : null
  }

  return (
    <form className="form" noValidate onSubmit={handleSubmit}>
      <div className="purchase-form__grid">
        <div className="field">
          <label htmlFor="purchase-supplierId">Proveedor</label>
          <select
            data-autofocus
            id="purchase-supplierId"
            value={supplierId}
            onChange={(event) => setSupplierId(event.target.value)}
            {...describe('supplierId')}
          >
            <option value="">Elige un proveedor</option>
            {suppliers.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>
                {supplier.businessName}
              </option>
            ))}
          </select>
          {errorOf('supplierId')}
        </div>
        <div className="field">
          <label htmlFor="purchase-purchaseDate">Fecha de compra</label>
          <input
            id="purchase-purchaseDate"
            type="date"
            value={purchaseDate}
            onChange={(event) => setPurchaseDate(event.target.value)}
            {...describe('purchaseDate')}
          />
          {errorOf('purchaseDate')}
        </div>
        <div className="field">
          <label htmlFor="purchase-supplierDocument">Documento del proveedor (opcional)</label>
          <input
            id="purchase-supplierDocument"
            placeholder="F001-123"
            value={document}
            maxLength={DOCUMENT_MAX_LENGTH}
            onChange={(event) => setDocument(event.target.value)}
            {...describe('supplierDocument')}
          />
          {errorOf('supplierDocument')}
        </div>
      </div>

      <ProductSearch excludedIds={rows.map((row) => row.product.id)} onSelect={addProduct} />
      {errors.items ? (
        <p className="field__error" role="alert">
          {errors.items}
        </p>
      ) : null}

      {rows.length > 0 ? (
        <ul className="purchase-form__items" aria-label="Productos de la compra">
          {rows.map((row, index) => {
            const subtotal = subtotals[index]
            return (
              <li key={row.product.id} className="purchase-form__item card">
                <div className="purchase-form__item-head">
                  <strong>{row.product.name}</strong>
                  <button className="link-button" type="button" onClick={() => removeRow(index)}>
                    Quitar
                  </button>
                </div>
                <div className="purchase-form__item-fields">
                  <div className="field">
                    <label htmlFor={`purchase-items-${index}-quantity`}>Cantidad de {row.product.name}</label>
                    <input
                      id={`purchase-items-${index}-quantity`}
                      inputMode="decimal"
                      value={row.quantity}
                      maxLength={13}
                      onChange={(event) => updateRow(index, { quantity: event.target.value })}
                      {...describe(`items.${index}.quantity`)}
                    />
                    {errorOf(`items.${index}.quantity`)}
                  </div>
                  <div className="field">
                    <label htmlFor={`purchase-items-${index}-unitCost`}>Costo unitario de {row.product.name} (S/)</label>
                    <input
                      id={`purchase-items-${index}-unitCost`}
                      inputMode="decimal"
                      value={row.unitCost}
                      maxLength={13}
                      onChange={(event) => updateRow(index, { unitCost: event.target.value })}
                      {...describe(`items.${index}.unitCost`)}
                    />
                    {errorOf(`items.${index}.unitCost`)}
                  </div>
                </div>
                <p className="purchase-form__subtotal">
                  Subtotal: {subtotal == null ? '—' : formatMoney(centsToAmount(subtotal))}
                </p>
              </li>
            )
          })}
        </ul>
      ) : null}

      <p className="purchase-form__total" aria-live="polite">
        Total: <strong>{total === null ? '—' : formatMoney(centsToAmount(total))}</strong>
      </p>

      {formError ? (
        <p className="alert" role="alert">
          {formError}
        </p>
      ) : null}
      <button className="button" type="submit" disabled={saving}>
        Registrar compra
      </button>
    </form>
  )
}
