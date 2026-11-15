import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { routes } from '@app/navigation'
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
  type Payments,
  type Shift,
} from '../api/shifts'
import './ShiftPage.css'

/** Acepta `2`, `2.5` o `2,5` con hasta 3 decimales (0 es válido); `undefined` si no es un número válido. */
function parseLeftover(text: string): string | undefined {
  const value = text.trim().replace(',', '.')
  return /^\d{1,9}(\.\d{1,3})?$/.test(value) ? value : undefined
}

const MEDIA = [
  { key: 'cash', label: 'Efectivo' },
  { key: 'yape', label: 'Yape' },
  { key: 'plin', label: 'Plin' },
  { key: 'transfer', label: 'Transferencia' },
] as const

type Medium = (typeof MEDIA)[number]['key']

/** Acepta `10`, `10.5` o `10,50` (hasta 2 decimales; 0 es válido); `undefined` si no es un monto válido. */
function parseMoney(text: string): number | undefined {
  const value = text.trim().replace(',', '.')
  return /^\d{1,9}(\.\d{1,2})?$/.test(value) ? Number(value) : undefined
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
  const [step, setStep] = useState<'count' | 'payments'>('count')
  const [paid, setPaid] = useState<Record<Medium, string>>({ cash: '', yape: '', plin: '', transfer: '' })
  const [note, setNote] = useState('')
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
      setPaid({ cash: '', yape: '', plin: '', transfer: '' })
      setNote('')
      setStep('count')
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
    setStep('payments')
  }

  /** Revisa los montos por medio de pago y pide confirmar antes de cerrar. */
  function handlePayments(event: FormEvent) {
    event.preventDefault()
    const problem = MEDIA.find((medium) => parseMoney(paid[medium.key]) === undefined)
    if (problem) {
      setError(`Revisa «${problem.label}»: escribe lo que recibiste (0 si no hubo), con hasta 2 decimales.`)
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
        Object.fromEntries(MEDIA.map((medium) => [medium.key, parseMoney(paid[medium.key]) as number])) as unknown as Payments,
        note,
      )
      setConfirming(false)
      setClosed(result)
      setShift(null)
      setProducts([])
      setNotice('Turno cerrado.')
    } catch (failure) {
      setConfirming(false)
      setStep('count')
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

      {shift && step === 'payments' && canClose ? (
        <form className="card shift__card" noValidate onSubmit={handlePayments}>
          <h2>Paso 2 de 2 · ¿Cuánto recibiste?</h2>
          <p className="muted">
            Por conteo vendiste <strong>{formatMoney(String(countedTotal))}</strong>. Escribe lo que recibiste por cada medio (0 si no hubo). Al
            cerrar verás si cuadra con lo esperado.
          </p>
          <ul className="shift__list" aria-label="Medios de pago">
            {MEDIA.map((medium) => (
              <li key={medium.key} className="shift__row">
                <label htmlFor={`pay-${medium.key}`}>
                  <strong>{medium.label}</strong>
                </label>
                <input
                  id={`pay-${medium.key}`}
                  className="shift__quantity"
                  type="text"
                  inputMode="decimal"
                  placeholder="S/ 0.00"
                  value={paid[medium.key]}
                  onChange={(event) => setPaid((current) => ({ ...current, [medium.key]: event.target.value }))}
                />
              </li>
            ))}
          </ul>
          <p className="shift__preview">
            Total recibido:{' '}
            <strong>{formatMoney(String(MEDIA.reduce((sum, medium) => sum + (parseMoney(paid[medium.key]) ?? 0), 0)))}</strong>
          </p>
          <label className="shift__note">
            Observación (opcional)
            <textarea value={note} maxLength={255} rows={2} onChange={(event) => setNote(event.target.value)} />
          </label>
          {error ? (
            <p className="alert" role="alert">
              {error}
            </p>
          ) : null}
          <div className="shift__steps">
            <button className="button button--secondary" type="button" disabled={busy} onClick={() => setStep('count')}>
              Volver al conteo
            </button>
            <button className="button" type="submit" disabled={busy}>
              Cerrar turno
            </button>
          </div>
        </form>
      ) : null}

      {shift && step === 'count' ? (
        <form className="card shift__card" noValidate onSubmit={handleReview}>
          <h2>Turno abierto</h2>
          <p>
            Ingreso a las <strong>{formatDateTime(shift.openedAt)}</strong> · {shift.responsible}
          </p>
          {canClose ? (
            <>
              <h3>Paso 1 de 2 · Cuenta lo que te quedó</h3>
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
                Continuar
              </button>
              <nav className="shift__links" aria-label="Accesos del turno">
                <Link to={routes.inventory}>Registrar entrada</Link>
                <Link to={routes.sales}>Ir a Ventas</Link>
              </nav>
            </>
          ) : (
            <p className="alert">
              Este turno lo abrió {shift.responsible}. Solo esa persona o un administrador pueden cerrarlo.
            </p>
          )}
        </form>
      ) : null}

      {closed ? (
        <section className="card shift__card shift__summary" aria-labelledby="shift-summary">
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
            {closed.payments
              ? MEDIA.map((medium) => (
                  <div key={medium.key}>
                    <dt>Recibido en {medium.label.toLowerCase()}</dt>
                    <dd>{formatMoney(String(closed.payments?.[medium.key] ?? 0))}</dd>
                  </div>
                ))
              : null}
            {closed.payments ? (
              <div className="shift__expected">
                <dt>Total recibido</dt>
                <dd>{formatMoney(String(closed.receivedAmount ?? 0))}</dd>
              </div>
            ) : null}
          </dl>
          {closed.difference !== null ? (
            <p
              className={closed.difference < 0 ? 'alert shift__difference' : 'shift__difference shift__difference--ok'}
              role={closed.difference < 0 ? 'alert' : undefined}
            >
              {closed.difference < 0
                ? `Falta ${formatMoney(String(-closed.difference))}`
                : closed.difference > 0
                  ? `Sobra ${formatMoney(String(closed.difference))}`
                  : 'Cuadra justo'}
            </p>
          ) : null}
          {closed.note ? <p className="muted">Observación: {closed.note}</p> : null}
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
          <div className="shift__steps shift__noprint">
            <button className="button button--secondary" type="button" onClick={() => window.print()}>
              Imprimir resumen
            </button>
            <button className="button button--secondary" type="button" disabled={busy} onClick={() => void handleOpen()}>
              Marcar nuevo ingreso
            </button>
          </div>
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
