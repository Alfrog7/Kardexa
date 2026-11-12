import { useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { useCanManage } from '@shared/auth/session'
import { formatDateTime, formatMoney } from '@shared/lib/format'
import { Icon } from '@shared/ui/Icon'
import { listSales, listSellers, type Sale, type SalePage, type Seller } from '../api/sales'

const STATUS_LABEL = { REGISTERED: 'Registrada', VOIDED: 'Anulada' } as const

interface Filters {
  from: string
  to: string
  sellerId: string
}

interface Loaded {
  key: string
  page?: SalePage
  error?: string
}

interface SaleHistoryProps {
  /** Fecha del negocio de hoy, `AAAA-MM-DD`: el rango empieza en el mes actual. */
  today: string
  /** Cambia cuando se registra o se anula una venta, para volver a cargar la lista. */
  version: number
  onOpen: (sale: Sale) => void
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** Historial de ventas (HU-16): por fechas y, para el `ADMIN`, por vendedor. El `EMPLEADO` solo ve las suyas. */
export function SaleHistory({ today, version, onOpen }: SaleHistoryProps) {
  const admin = useCanManage()
  const [filters, setFilters] = useState<Filters>({ from: `${today.slice(0, 8)}01`, to: today, sellerId: '' })
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<Loaded>()
  const [sellers, setSellers] = useState<Seller[]>([])

  const rangeError = filters.from && filters.to && filters.from > filters.to ? 'La fecha inicial no puede ser posterior a la final.' : undefined
  const key = `${filters.from}|${filters.to}|${filters.sellerId}|${page}|${attempt}|${version}`

  useEffect(() => {
    if (!admin) return
    let current = true
    listSellers()
      .then((found) => {
        if (current) setSellers(found)
      })
      .catch(() => {
        // Sin la lista de vendedores el historial sigue funcionando; solo falta el filtro.
      })
    return () => {
      current = false
    }
  }, [admin])

  useEffect(() => {
    if (!filters.from || !filters.to || rangeError) return
    let current = true
    listSales(filters.from, filters.to, filters.sellerId, page)
      .then((found) => {
        if (current) setLoaded({ key, page: found })
      })
      .catch((error: unknown) => {
        if (current) setLoaded({ key, error: messageOf(error) })
      })
    return () => {
      current = false
    }
  }, [key, filters.from, filters.to, filters.sellerId, page, rangeError])

  const shown = loaded?.key === key ? loaded : undefined
  const found = shown?.page

  function change(change: Partial<Filters>) {
    setFilters((current) => ({ ...current, ...change }))
    setPage(0)
  }

  return (
    <div className="sales__history">
      <h2>Historial</h2>
      <div className="sales__filters" role="search">
        <div className="field">
          <label htmlFor="sales-from">Desde</label>
          <input id="sales-from" type="date" value={filters.from} max={filters.to} onChange={(event) => change({ from: event.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="sales-to">Hasta</label>
          <input id="sales-to" type="date" value={filters.to} min={filters.from} onChange={(event) => change({ to: event.target.value })} />
        </div>
        {admin ? (
          <div className="field">
            <label htmlFor="sales-seller">Vendedor</label>
            <select id="sales-seller" value={filters.sellerId} onChange={(event) => change({ sellerId: event.target.value })}>
              <option value="">Todos</option>
              {sellers.map((seller) => (
                <option key={seller.id} value={seller.id}>
                  {seller.fullName}
                </option>
              ))}
            </select>
          </div>
        ) : null}
      </div>

      {rangeError ? (
        <p className="alert" role="alert">
          {rangeError}
        </p>
      ) : null}

      {!rangeError && !shown ? (
        <p className="muted" role="status">
          Cargando ventas…
        </p>
      ) : null}

      {shown?.error ? (
        <div className="alert" role="alert">
          <p>{shown.error}</p>
          <button className="button button--secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Reintentar
          </button>
        </div>
      ) : null}

      {found && found.content.length === 0 ? <p className="muted">No hay ventas en estas fechas.</p> : null}

      {found && found.content.length > 0 ? (
        <div className="card data-table-card">
          <table className="data-table" aria-label="Ventas">
            <thead>
              <tr>
                <th scope="col">Número</th>
                <th scope="col">Fecha</th>
                <th scope="col">Vendedor</th>
                <th scope="col" className="data-table__number">
                  Total
                </th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {found.content.map((sale) => (
                <tr key={sale.id} className={sale.status === 'VOIDED' ? 'sales__row--voided' : undefined}>
                  <td data-label="Número">
                    <button className="link-button" type="button" aria-label={`Ver comprobante ${sale.number}`} onClick={() => onOpen(sale)}>
                      {sale.number}
                    </button>
                  </td>
                  <td data-label="Fecha">{formatDateTime(sale.soldAt)}</td>
                  <td data-label="Vendedor">{sale.seller}</td>
                  <td data-label="Total" className="data-table__number">
                    {formatMoney(sale.total)}
                  </td>
                  <td data-label="Estado">
                    <span className={sale.status === 'VOIDED' ? 'status status--disabled' : 'status'}>{STATUS_LABEL[sale.status]}</span>
                    {sale.voidReason ? <span className="sales__reason muted">{sale.voidReason}</span> : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {found && (found.totalPages > 1 || found.page > 0) ? (
        <nav className="sales__pagination" aria-label="Páginas de ventas">
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Anterior"
            title="Anterior"
            disabled={found.page === 0}
            onClick={() => setPage(found.page - 1)}
          >
            <Icon name="chevronLeft" />
          </button>
          <span>
            Página {found.page + 1} de {Math.max(found.totalPages, found.page + 1)}
          </span>
          <button
            className="button button--secondary button--icon"
            type="button"
            aria-label="Siguiente"
            title="Siguiente"
            disabled={found.page + 1 >= found.totalPages}
            onClick={() => setPage(found.page + 1)}
          >
            <Icon name="chevronRight" />
          </button>
        </nav>
      ) : null}
    </div>
  )
}
