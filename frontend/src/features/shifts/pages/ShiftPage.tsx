import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { formatDateTime, formatMoney, formatQuantity } from '@shared/lib/format'
import { Toast } from '@shared/ui/Toast'
import {
  closeShift,
  getCurrentShift,
  listCountProducts,
  openShift,
  type CountProduct,
  type Shift,
} from '../api/shifts'
import './ShiftPage.css'

/** Acepta `2`, `2.5` o `2,5` con hasta 3 decimales (0 es válido); `undefined` si no es un número válido. */
function parseLeftover(text: string): string | undefined {
  const value = text.trim().replace(',', '.')
  return /^\d{1,9}(\.\d{1,3})?$/.test(value) ? value : undefined
}

/**
 * Mi turno (HU-22): quien lleva la caja marca su ingreso (abre el turno de la sede) y, al terminar, cuenta lo que
 * sobró de cada producto para cerrarlo. El sistema calcula lo vendido; no se anota venta por venta.
 */
export function ShiftPage() {
  const [loading, setLoading] = useState(true)
  const [shift, setShift] = useState<Shift | null>(null)
  const [closed, setClosed] = useState<Shift>()
  const [products, setProducts] = useState<CountProduct[]>([])
  const [leftovers, setLeftovers] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  useEffect(() => {
    getCurrentShift()
      .then(async (current) => {
        setShift(current)
        setProducts(current ? await listCountProducts() : [])
      })
      .catch(() => setError('No se pudo cargar el turno. Recarga la página e inténtalo de nuevo.'))
      .finally(() => setLoading(false))
  }, [])

  function messageOf(failure: unknown, fallback: string) {
    return failure instanceof ApiError ? failure.message : fallback
  }

  async function handleOpen() {
    setBusy(true)
    setError(undefined)
    try {
      const opened = await openShift()
      setClosed(undefined)
      setLeftovers({})
      setShift(opened)
      setProducts(await listCountProducts())
      setNotice('Ingreso marcado: tu turno está abierto.')
    } catch (failure) {
      setError(messageOf(failure, 'No se pudo marcar el ingreso. Inténtalo de nuevo.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleClose(event: FormEvent) {
    event.preventDefault()
    if (!shift) return
    const counts = products.map((product) => ({
      productId: product.id,
      leftover: parseLeftover(leftovers[product.id] ?? ''),
    }))
    if (counts.some((count) => count.leftover === undefined)) {
      setError('Cuenta cada producto: escribe lo que sobró, o 0 si no quedó nada (hasta 3 decimales).')
      return
    }
    setBusy(true)
    setError(undefined)
    try {
      const result = await closeShift(
        shift.id,
        counts.map((count) => ({ productId: count.productId, leftover: count.leftover as string })),
      )
      setClosed(result)
      setShift(null)
      setProducts([])
      setNotice('Turno cerrado.')
    } catch (failure) {
      setError(messageOf(failure, 'No se pudo cerrar el turno. Inténtalo de nuevo.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="page">
      <header className="page__header">
        <h1>Mi turno</h1>
      </header>

      {loading ? <p className="muted">Cargando…</p> : null}

      {!loading && !shift && !closed ? (
        <div className="card shift__card">
          <h2>Todavía no marcaste tu ingreso</h2>
          <p className="muted">Al marcar tu ingreso se abre el turno de la sede con el stock que hay ahora.</p>
          <button className="button shift__action" type="button" disabled={busy} onClick={() => void handleOpen()}>
            {busy ? 'Marcando…' : 'Marcar ingreso'}
          </button>
        </div>
      ) : null}

      {shift ? (
        <form className="card shift__card" noValidate onSubmit={handleClose}>
          <h2>Turno abierto</h2>
          <p>
            Ingreso a las <strong>{formatDateTime(shift.openedAt)}</strong> · {shift.responsible}
          </p>
          <h3>Al terminar, cuenta lo que sobró</h3>
          <p className="muted">Escribe cuánto queda de cada producto (0 si no quedó nada). El sistema calcula lo vendido.</p>
          {products.length === 0 ? (
            <p className="muted">No hay productos con stock en la sede: puedes cerrar el turno.</p>
          ) : (
            <ul className="shift__list" aria-label="Productos">
              {products.map((product) => {
                const text = leftovers[product.id] ?? ''
                const wrong = text.trim() !== '' && parseLeftover(text) === undefined
                return (
                  <li key={product.id} className="shift__row">
                    <label htmlFor={`left-${product.id}`} className="shift__product">
                      <strong>{product.name}</strong>
                      <span className="muted">Registrado: {formatQuantity(product.stock)}</span>
                    </label>
                    <input
                      id={`left-${product.id}`}
                      className="shift__quantity"
                      type="text"
                      inputMode="decimal"
                      placeholder="0"
                      value={text}
                      aria-invalid={wrong || undefined}
                      onChange={(event) => setLeftovers((current) => ({ ...current, [product.id]: event.target.value }))}
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
          <button className="button shift__action" type="submit" disabled={busy}>
            {busy ? 'Cerrando…' : 'Cerrar turno'}
          </button>
        </form>
      ) : null}

      {!shift && error ? (
        <p className="alert" role="alert">
          {error}
        </p>
      ) : null}

      {closed ? (
        <section className="card shift__card" aria-labelledby="shift-summary">
          <h2 id="shift-summary">Turno cerrado</h2>
          <p className="muted">
            {formatDateTime(closed.openedAt)} a {closed.closedAt ? formatDateTime(closed.closedAt) : ''} · {closed.responsible}
          </p>
          <dl className="shift__totals">
            <div>
              <dt>Ventas registradas</dt>
              <dd>{formatMoney(String(closed.registeredSales ?? 0))}</dd>
            </div>
            <div>
              <dt>Vendido por conteo</dt>
              <dd>{formatMoney(String(closed.countedSales ?? 0))}</dd>
            </div>
            <div className="shift__expected">
              <dt>Debes tener en caja</dt>
              <dd>{formatMoney(String(closed.expectedAmount ?? 0))}</dd>
            </div>
          </dl>
          <ul className="shift__list" aria-label="Lo vendido por conteo">
            {closed.items
              .filter((item) => Number(item.soldByCount ?? 0) > 0)
              .map((item) => (
                <li key={item.productId} className="shift__row">
                  <span>{item.productName}</span>
                  <span>{formatQuantity(item.soldByCount ?? '0')} vendidos</span>
                </li>
              ))}
          </ul>
          <button className="button button--secondary shift__action" type="button" disabled={busy} onClick={() => void handleOpen()}>
            Marcar nuevo ingreso
          </button>
        </section>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
