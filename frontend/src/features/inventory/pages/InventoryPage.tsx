import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { businessDate, formatDateTime, formatQuantity } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Toast } from '@shared/ui/Toast'
import {
  getKardex,
  registerAdjustment,
  type Kardex,
  type MovementType,
  type ProductOption,
  type StockAdjustmentRequest,
} from '../api/inventory'
import { AdjustmentForm } from '../components/AdjustmentForm'
import { ProductPicker } from '../components/ProductPicker'
import './InventoryPage.css'

/** La API del kárdex no pagina: se empieza por un rango acotado. */
const DEFAULT_RANGE_DAYS = 30

const MOVEMENT_LABELS: Record<MovementType, string> = {
  PURCHASE: 'Compra',
  SALE: 'Venta',
  ADJUSTMENT: 'Ajuste',
  PURCHASE_VOID: 'Anulación de compra',
  SALE_VOID: 'Anulación de venta',
}

interface KardexResult {
  /** Consulta que produjo este resultado: si no es la actual, hay otra en curso. */
  query: object
  kardex?: Kardex
  error?: string
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

function daysAgo(days: number): string {
  return businessDate(new Date(Date.now() - days * 24 * 60 * 60 * 1000))
}

/** Inventario (HU-08, HU-09): kárdex de un producto y ajustes de stock. */
export function InventoryPage() {
  const [product, setProduct] = useState<ProductOption>()
  const [from, setFrom] = useState(() => daysAgo(DEFAULT_RANGE_DAYS))
  const [to, setTo] = useState(() => daysAgo(0))
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<KardexResult>()
  const [adjusting, setAdjusting] = useState(false)
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  const productId = product?.id
  const query = useMemo(() => ({ productId, from, to, attempt }), [productId, from, to, attempt])
  const invalidRange = Boolean(from && to && from > to)

  useEffect(() => {
    if (!query.productId || (query.from && query.to && query.from > query.to)) return
    let current = true
    getKardex(query.productId, query.from, query.to)
      .then((kardex) => {
        if (current) setResult({ query, kardex })
      })
      .catch((error: unknown) => {
        if (current) setResult({ query, error: messageOf(error) })
      })
    return () => {
      current = false
    }
  }, [query])

  const loading = !invalidRange && result?.query !== query
  // Al cambiar de producto, o con un rango inválido, no se muestra el kárdex anterior.
  const kardex = !invalidRange && result?.kardex?.productId === productId ? result?.kardex : undefined

  async function handleAdjustment(request: StockAdjustmentRequest) {
    await registerAdjustment(request)
    setAdjusting(false)
    setNotice('Ajuste registrado. Ya se actualizó el stock.')
    setAttempt((value) => value + 1)
  }

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Inventario</h1>
        {product ? (
          <button className="button" type="button" onClick={() => setAdjusting(true)}>
            Registrar ajuste
          </button>
        ) : null}
      </header>

      {adjusting && product ? (
        <Dialog title={`Ajustar stock de ${product.name}`} onClose={() => setAdjusting(false)}>
          <AdjustmentForm productId={product.id} onSubmit={handleAdjustment} />
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      {product ? (
        <p className="inventory__selected">
          <strong>{product.name}</strong> <span className="muted">· {product.code} · </span>
          <button className="link-button" type="button" onClick={() => setProduct(undefined)}>
            Cambiar producto
          </button>
        </p>
      ) : (
        <>
          <ProductPicker onSelect={setProduct} />
          <p className="muted">Elige un producto para ver su kárdex.</p>
        </>
      )}

      {product ? (
        <div className="inventory__range">
          <div className="field">
            <label htmlFor="kardex-from">Desde</label>
            <input id="kardex-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="kardex-to">Hasta</label>
            <input id="kardex-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} />
          </div>
        </div>
      ) : null}

      {product && invalidRange ? (
        <p className="alert" role="alert">
          La fecha inicial no puede ser posterior a la final.
        </p>
      ) : null}

      {product && loading ? (
        <p className="muted" role="status">
          Cargando kárdex…
        </p>
      ) : null}

      {product && result?.error && !loading && !invalidRange ? (
        <div className="alert" role="alert">
          <p>{result.error}</p>
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : null}

      {kardex ? (
        <>
          <dl className="inventory__balances">
            <div className="card">
              <dt>Saldo inicial</dt>
              <dd>{formatQuantity(kardex.openingBalance)}</dd>
            </div>
            <div className="card">
              <dt>Saldo final</dt>
              <dd>{formatQuantity(kardex.closingBalance)}</dd>
            </div>
          </dl>

          {kardex.movements.length === 0 ? (
            <p className="muted">No hay movimientos en este rango de fechas.</p>
          ) : (
            <div className="card data-table-card">
              <table className="data-table">
                <thead>
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Documento</th>
                    <th scope="col" className="data-table__number">
                      Entrada
                    </th>
                    <th scope="col" className="data-table__number">
                      Salida
                    </th>
                    <th scope="col" className="data-table__number">
                      Saldo
                    </th>
                    <th scope="col">Usuario</th>
                  </tr>
                </thead>
                <tbody>
                  {kardex.movements.map((movement) => (
                    <tr key={movement.id}>
                      <td data-label="Fecha">{formatDateTime(movement.occurredAt)}</td>
                      <td data-label="Tipo">{MOVEMENT_LABELS[movement.type]}</td>
                      <td data-label="Documento">{movement.sourceLabel ?? '—'}</td>
                      <td data-label="Entrada" className="data-table__number">
                        {movement.direction === 'IN' ? formatQuantity(movement.quantity) : '—'}
                      </td>
                      <td data-label="Salida" className="data-table__number">
                        {movement.direction === 'OUT' ? formatQuantity(movement.quantity) : '—'}
                      </td>
                      <td data-label="Saldo" className="data-table__number">
                        {formatQuantity(movement.balanceAfter)}
                      </td>
                      <td data-label="Usuario">{movement.user}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : null}
    </section>
  )
}
