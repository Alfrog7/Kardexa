import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { businessDate, formatDateTime, formatQuantity } from '@shared/lib/format'
import { Toast } from '@shared/ui/Toast'
import {
  listReceiptProducts,
  listReceiptsOfDay,
  registerReceipt,
  type ReceiptEntry,
  type ReceiptProduct,
} from '../api/receipts'
import './ReceiptsPage.css'

/** Acepta `2`, `2.5` o `2,5` con hasta 3 decimales; devuelve el número con punto o `undefined` si no es válido. */
function parseQuantity(text: string): string | undefined {
  const value = text.trim().replace(',', '.')
  return /^\d{1,9}(\.\d{1,3})?$/.test(value) && Number(value) > 0 ? value : undefined
}

/**
 * Recepción de mercadería (HU-21): la trabajadora marca lo que llegó a su sede y confirma, sin costo ni
 * proveedor. Pensada para el celular: una lista de productos con su cantidad y un botón.
 */
export function ReceiptsPage() {
  const [products, setProducts] = useState<ReceiptProduct[]>()
  const [entries, setEntries] = useState<ReceiptEntry[]>([])
  const [quantities, setQuantities] = useState<Record<string, string>>({})
  const [search, setSearch] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  const loadEntries = useCallback(() => {
    listReceiptsOfDay(businessDate(new Date()))
      .then(setEntries)
      .catch(() => setEntries([]))
  }, [])

  const loadProducts = useCallback(() => {
    listReceiptProducts()
      .then(setProducts)
      .catch(() => setError('No se pudieron cargar los productos. Recarga la página e inténtalo de nuevo.'))
  }, [])

  useEffect(() => {
    loadProducts()
    loadEntries()
  }, [loadProducts, loadEntries])

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (products ?? []).filter(
      (product) => !term || product.name.toLowerCase().includes(term) || product.code.toLowerCase().includes(term),
    )
  }, [products, search])

  const filled = Object.entries(quantities).filter(([, text]) => text.trim() !== '')
  const invalid = filled.some(([, text]) => parseQuantity(text) === undefined)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (filled.length === 0 || invalid) {
      setError('Indica una cantidad mayor que 0 (hasta 3 decimales) en cada producto que llegó.')
      return
    }
    setError(undefined)
    setSaving(true)
    try {
      await registerReceipt({
        items: filled.map(([productId, text]) => ({ productId, quantity: parseQuantity(text) as string })),
      })
      setQuantities({})
      setNotice(filled.length === 1 ? 'Recepción registrada: 1 producto.' : `Recepción registrada: ${filled.length} productos.`)
      loadProducts()
      loadEntries()
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'No se pudo registrar la recepción. Inténtalo de nuevo.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="page">
      <header className="page__header">
        <h1>Recepción de mercadería</h1>
      </header>

      <form className="card receipts__form" noValidate onSubmit={handleSubmit}>
        <h2>Lo que llegó a la sede</h2>
        <div className="field">
          <label htmlFor="receipt-search">Buscar producto</label>
          <input
            id="receipt-search"
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Nombre o código"
          />
        </div>

        {products === undefined ? (
          <p className="muted">Cargando productos…</p>
        ) : visible.length === 0 ? (
          <p className="muted">No hay productos que coincidan.</p>
        ) : (
          <ul className="receipts__list" aria-label="Productos">
            {visible.map((product) => {
              const text = quantities[product.id] ?? ''
              const wrong = text.trim() !== '' && parseQuantity(text) === undefined
              return (
                <li key={product.id} className="receipts__row">
                  <label htmlFor={`receipt-${product.id}`} className="receipts__product">
                    <strong>{product.name}</strong>
                    <span className="muted">Stock: {formatQuantity(product.stock)}</span>
                  </label>
                  <input
                    id={`receipt-${product.id}`}
                    className="receipts__quantity"
                    type="text"
                    inputMode="decimal"
                    placeholder="0"
                    value={text}
                    aria-invalid={wrong || undefined}
                    onChange={(event) => setQuantities((current) => ({ ...current, [product.id]: event.target.value }))}
                  />
                </li>
              )
            })}
          </ul>
        )}

        {error ? (
          <p className="alert" role="alert">
            {error}
          </p>
        ) : null}
        <button className="button receipts__submit" type="submit" disabled={saving || filled.length === 0}>
          {saving ? 'Registrando…' : 'Confirmar recepción'}
        </button>
      </form>

      <section className="card receipts__history" aria-labelledby="receipts-history-title">
        <h2 id="receipts-history-title">Recibido hoy</h2>
        {entries.length === 0 ? (
          <p className="muted">Todavía no se registró ninguna recepción hoy.</p>
        ) : (
          <ul className="receipts__entries">
            {entries.map((entry) => (
              <li key={entry.movement.id}>
                <span>
                  <strong>{entry.productName}</strong> +{formatQuantity(entry.movement.quantity)}
                </span>
                <span className="muted">
                  {formatDateTime(entry.movement.occurredAt)} · {entry.movement.user}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
