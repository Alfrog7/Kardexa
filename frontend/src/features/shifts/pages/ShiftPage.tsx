import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '@shared/api/http'
import { useSession } from '@shared/auth/session'
import { formatDateTime, formatMoney, formatQuantity } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
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

/** Lo vendido de un producto según lo contado: lo registrado menos lo que sobró; `undefined` si el conteo no vale. */
function soldOf(product: CountProduct, text: string): number | undefined {
  const leftover = parseLeftover(text)
  if (leftover === undefined) return undefined
  const sold = Number(product.stock) - Number(leftover)
  return sold < 0 ? undefined : Math.round(sold * 1000) / 1000
}

/**
 * Mi turno (HU-22): quien lleva la caja marca su ingreso (abre el turno de la sede) y, al terminar, cuenta lo que
 * sobró de cada producto para cerrarlo. El sistema calcula lo vendido; no se anota venta por venta.
 */
export function ShiftPage() {
  const { user } = useSession()
  const [loading, setLoading] = useState(true)
  const [shift, setShift] = useState<Shift | null>(null)
  const [closed, setClosed] = useState<Shift>()
  const [products, setProducts] = useState<CountProduct[]>([])
  const [leftovers, setLeftovers] = useState<Record<string, string>>({})
  const [confirming, setConfirming] = useState(false)
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

  /** Revisa el conteo y, si está completo, pide confirmar antes de cerrar. */
  function handleReview(event: FormEvent) {
    event.preventDefault()
    const problem = products.find((product) => soldOf(product, leftovers[product.id] ?? '') === undefined)
    if (problem) {
      setError(
        `Revisa «${problem.name}»: escribe lo que te quedó (0 si no quedó nada) y que no sea más de lo registrado (${formatQuantity(problem.stock)}).`,
      )
      return
    }
    setError(undefined)
    setConfirming(true)
  }

  async function handleClose() {
    if (!shift) return
    setBusy(true)
    try {
      const result = await closeShift(
        shift.id,
        products.map((product) => ({
          productId: product.id,
          leftover: parseLeftover(leftovers[product.id] ?? '') as string,
        })),
      )
      setConfirming(false)
      setClosed(result)
      setShift(null)
      setProducts([])
      setNotice('Turno cerrado.')
    } catch (failure) {
      setConfirming(false)
      setError(messageOf(failure, 'No se pudo cerrar el turno. Inténtalo de nuevo.'))
    } finally {
      setBusy(false)
    }
  }

  const countedTotal = products.reduce((sum, product) => {
    const sold = soldOf(product, leftovers[product.id] ?? '')
    return sum + (sold === undefined ? 0 : sold * Number(product.salePrice))
  }, 0)
  const canClose = shift !== null && (user?.role === 'ADMIN' || shift.responsibleId === user?.id)

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
          {error ? (
            <p className="alert" role="alert">
              {error}
            </p>
          ) : null}
          <button className="button shift__action" type="button" disabled={busy} onClick={() => void handleOpen()}>
            {busy ? 'Marcando…' : 'Marcar ingreso'}
          </button>
        </div>
      ) : null}

      {shift ? (
        <form className="card shift__card" noValidate onSubmit={handleReview}>
          <h2>Turno abierto</h2>
          <p>
            Ingreso a las <strong>{formatDateTime(shift.openedAt)}</strong> · {shift.responsible}
          </p>
          {canClose ? (
            <>
              <h3>Al terminar tu horario, cuenta lo que te quedó</h3>
              <p className="muted">
                Mira lo que queda de cada producto y escríbelo (0 si no quedó nada). El sistema calcula lo que vendiste: lo
                registrado menos lo que te quedó. Si llegó mercadería que no anotaste, regístrala antes en Inventario.
              </p>
              {products.length === 0 ? (
                <p className="muted">No hay productos con stock en la sede: puedes cerrar el turno.</p>
              ) : (
                <ul className="shift__list" aria-label="Productos">
                  {products.map((product) => {
                    const text = leftovers[product.id] ?? ''
                    const sold = soldOf(product, text)
                    const wrong = text.trim() !== '' && sold === undefined
                    return (
                      <li key={product.id} className="shift__row">
                        <label htmlFor={`left-${product.id}`} className="shift__product">
                          <strong>{product.name}</strong>
                          <span className="muted">Registrado: {formatQuantity(product.stock)}</span>
                          {sold !== undefined && text.trim() !== '' ? (
                            <span className="shift__sold">
                              Vendiste {formatQuantity(String(sold))} · {formatMoney(String(sold * Number(product.salePrice)))}
                            </span>
                          ) : null}
                          {wrong ? <span className="shift__wrong">No puede ser más de lo registrado.</span> : null}
                        </label>
                        <input
                          id={`left-${product.id}`}
                          className="shift__quantity"
                          type="text"
                          inputMode="decimal"
                          placeholder="Te quedó"
                          value={text}
                          aria-invalid={wrong || undefined}
                          onChange={(event) => setLeftovers((current) => ({ ...current, [product.id]: event.target.value }))}
                        />
                      </li>
                    )
                  })}
                </ul>
              )}
              {products.length > 0 ? (
                <p className="shift__preview">
                  Vendido por conteo hasta ahora: <strong>{formatMoney(String(countedTotal))}</strong>
                </p>
              ) : null}
              {error ? (
                <p className="alert" role="alert">
                  {error}
                </p>
              ) : null}
              <button className="button shift__action" type="submit" disabled={busy}>
                Cerrar turno
              </button>
            </>
          ) : (
            <p className="alert">
              Este turno lo abrió {shift.responsible}. Solo esa persona o un administrador pueden cerrarlo.
            </p>
          )}
        </form>
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
          <p className="muted">Tu turno terminó. Cuando empieces el siguiente, vuelve a marcar tu ingreso.</p>
          <button className="button button--secondary shift__action" type="button" disabled={busy} onClick={() => void handleOpen()}>
            Marcar nuevo ingreso
          </button>
        </section>
      ) : null}

      {confirming ? (
        <Dialog title="¿Cerrar tu turno?" onClose={() => setConfirming(false)}>
          <div className="shift__confirm">
            <p>
              Tu turno empezó a las <strong>{shift ? formatDateTime(shift.openedAt) : ''}</strong>. Si todavía no termina tu
              horario, espera: al cerrar se descuenta lo vendido del stock y el turno no se puede reabrir.
            </p>
            <div className="shift__confirm-actions">
              <button className="button button--secondary" type="button" disabled={busy} onClick={() => setConfirming(false)}>
                Todavía no
              </button>
              <button className="button" type="button" disabled={busy} onClick={() => void handleClose()}>
                {busy ? 'Cerrando…' : 'Sí, cerrar turno'}
              </button>
            </div>
          </div>
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </section>
  )
}
