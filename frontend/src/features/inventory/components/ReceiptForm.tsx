import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { formatQuantity } from '@shared/lib/format'
import { listReceiptProducts, registerReceipt, type ReceiptProduct } from '../api/inventory'
import './ReceiptForm.css'

/** Acepta `2`, `2.5` o `2,5` con hasta 3 decimales; devuelve el número con punto o `undefined` si no es válido. */
function parseQuantity(text: string): string | undefined {
  const value = text.trim().replace(',', '.')
  return /^\d{1,9}(\.\d{1,3})?$/.test(value) && Number(value) > 0 ? value : undefined
}

interface ReceiptFormProps {
  /** Se llama con la cantidad de productos registrados. */
  onRegistered: (products: number) => void
}

/**
 * Registrar entrada (HU-21): se marca lo que llegó a la sede (pan, bocadillos) y se confirma, sin costo ni
 * proveedor. Pensada para el celular: una lista de productos con su cantidad y un botón.
 */
export function ReceiptForm({ onRegistered }: ReceiptFormProps) {
  const [products, setProducts] = useState<ReceiptProduct[]>()
  const [quantities, setQuantities] = useState<Record<string, string>>({})
  const [search, setSearch] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()

  useEffect(() => {
    listReceiptProducts()
      .then(setProducts)
      .catch(() => setError('No se pudieron cargar los productos. Cierra esta ventana e inténtalo de nuevo.'))
  }, [])

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
      onRegistered(filled.length)
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : 'No se pudo registrar la entrada. Inténtalo de nuevo.')
      setSaving(false)
    }
  }

  return (
    <form className="receipts__form" noValidate onSubmit={handleSubmit}>
      <p className="muted">Marca cuánto de cada producto llegó a la sede. El stock sube sin costo ni proveedor.</p>
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
        {saving ? 'Registrando…' : 'Confirmar entrada'}
      </button>
    </form>
  )
}
