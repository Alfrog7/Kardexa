import { useEffect, useMemo, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { formatDateTime, formatQuantity } from '@shared/lib/format'
import { ActionButton } from '@shared/ui/ActionButton'
import { Icon } from '@shared/ui/Icon'
import { listMovements, type MovementPage, type MovementType, type ProductOption } from '../api/inventory'
import { MOVEMENT_LABELS } from './movementLabels'

interface HistoryResult {
  query: object
  page?: MovementPage
  error?: string
}

interface MovementHistoryProps {
  from: string
  to: string
  /** Abre el kárdex del producto del movimiento. */
  onOpenProduct: (product: ProductOption) => void
}

const TYPES = Object.entries(MOVEMENT_LABELS) as [MovementType, string][]

/** Historial general del inventario: qué entró y salió, de qué producto, cuándo y quién lo hizo. */
export function MovementHistory({ from: initialFrom, to: initialTo, onOpenProduct }: MovementHistoryProps) {
  const [from, setFrom] = useState(initialFrom)
  const [to, setTo] = useState(initialTo)
  const [type, setType] = useState<MovementType | ''>('')
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<HistoryResult>()

  const invalidRange = Boolean(from && to && from > to)
  const query = useMemo(() => ({ from, to, type, page, attempt }), [from, to, type, page, attempt])

  useEffect(() => {
    if (query.from && query.to && query.from > query.to) return
    let current = true
    listMovements(query.from, query.to, query.type, query.page)
      .then((found) => {
        if (current) setResult({ query, page: found })
      })
      .catch((error: unknown) => {
        if (current) {
          setResult({ query, error: error instanceof ApiError ? error.message : 'No pudimos cargar el historial.' })
        }
      })
    return () => {
      current = false
    }
  }, [query])

  const loading = !invalidRange && result?.query !== query
  const found = invalidRange ? undefined : result?.page

  return (
    <section className="inventory__history" aria-labelledby="history-title">
      <div className="inventory__history-head">
        <h2 id="history-title">Historial de movimientos</h2>
        <button
          className="button button--secondary"
          type="button"
          onClick={() => setAttempt((value) => value + 1)}
        >
          <Icon name="refresh" size={18} />
          Recargar
        </button>
      </div>

      <div className="toolbar">
        <div className="field">
          <label htmlFor="history-from">Desde</label>
          <input id="history-from" type="date" value={from} onChange={(event) => { setFrom(event.target.value); setPage(0) }} />
        </div>
        <div className="field">
          <label htmlFor="history-to">Hasta</label>
          <input id="history-to" type="date" value={to} onChange={(event) => { setTo(event.target.value); setPage(0) }} />
        </div>
        <div className="field">
          <label htmlFor="history-type">Tipo</label>
          <select
            id="history-type"
            value={type}
            onChange={(event) => {
              setType(event.target.value as MovementType | '')
              setPage(0)
            }}
          >
            <option value="">Todos</option>
            {TYPES.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {invalidRange ? (
        <p className="alert" role="alert">
          La fecha inicial no puede ser posterior a la final.
        </p>
      ) : null}

      {loading ? (
        <p className="muted" role="status">
          Cargando historial…
        </p>
      ) : null}

      {result?.error && !loading && !invalidRange ? (
        <div className="alert" role="alert">
          <p>{result.error}</p>
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : null}

      {found && found.content.length === 0 && !loading ? (
        <div className="empty-state card">
          <p>No hay movimientos en estas fechas. Las compras, ventas y ajustes aparecerán aquí.</p>
        </div>
      ) : null}

      {found && found.content.length > 0 ? (
        <div className="card data-table-card">
          <table className="data-table" aria-label="Historial de movimientos">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Producto</th>
                <th scope="col">Tipo</th>
                <th scope="col" className="data-table__number">
                  Cantidad
                </th>
                <th scope="col" className="data-table__number">
                  Saldo
                </th>
                <th scope="col">Usuario</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {found.content.map(({ productId, productCode, productName, movement }) => (
                <tr key={movement.id}>
                  <td data-label="Fecha">{formatDateTime(movement.occurredAt)}</td>
                  <td data-label="Producto">
                    {productName}
                    <span className="inventory__code">{productCode}</span>
                  </td>
                  <td data-label="Tipo">
                    <span className={`movement-type movement-type--${movement.direction === 'IN' ? 'in' : 'out'}`}>
                      {MOVEMENT_LABELS[movement.type]}
                    </span>
                  </td>
                  <td data-label="Cantidad" className="data-table__number">
                    <span className={movement.direction === 'IN' ? 'inventory__in' : 'inventory__out'}>
                      {movement.direction === 'IN' ? '+' : '−'}
                      {formatQuantity(movement.quantity)}
                    </span>
                  </td>
                  <td data-label="Saldo" className="data-table__number">
                    {formatQuantity(movement.balanceAfter)}
                  </td>
                  <td data-label="Usuario">{movement.user}</td>
                  <td data-label="Acciones">
                    <ActionButton
                      icon="history"
                      label={`Ver kárdex de ${productName}`}
                      onClick={() => onOpenProduct({ id: productId, code: productCode, name: productName })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {found && found.totalPages > 1 ? (
        <nav className="inventory__pagination" aria-label="Páginas del historial">
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Anterior"
            title="Anterior"
            disabled={found.page === 0 || loading}
            onClick={() => setPage(found.page - 1)}
          >
            <Icon name="chevronLeft" />
          </button>
          <span>
            Página {found.page + 1} de {found.totalPages} · {found.totalElements} movimientos
          </span>
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Siguiente"
            title="Siguiente"
            disabled={found.page + 1 >= found.totalPages || loading}
            onClick={() => setPage(found.page + 1)}
          >
            <Icon name="chevronRight" />
          </button>
        </nav>
      ) : null}
    </section>
  )
}
