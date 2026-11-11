import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '@shared/api/http'
import { businessDate, formatMoney, formatQuantity } from '@shared/lib/format'
import { Dialog } from '@shared/ui/Dialog'
import { Icon } from '@shared/ui/Icon'
import { Toast } from '@shared/ui/Toast'
import { listPurchases, registerPurchase, voidPurchase, type Purchase, type PurchasePage, type PurchaseRequest } from '../api/purchases'
import { listSuppliers, type Supplier } from '../api/suppliers'
import { PurchaseForm } from '../components/PurchaseForm'
import { VoidPurchaseForm } from '../components/VoidPurchaseForm'
import './PurchasesPage.css'

const STATUS_LABEL = { REGISTERED: 'Registrada', VOIDED: 'Anulada' } as const

interface Range {
  from: string
  to: string
}

interface Loaded {
  key: string
  page?: PurchasePage
  error?: string
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Algo salió mal. Inténtalo otra vez.'
}

/** `"2026-11-10"` → `"10/11/2026"`, sin pasar por la zona horaria del navegador. */
function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

/** Compras (HU-12): lista por rango de fechas, detalle y registro. Solo para `ADMIN`. */
export function PurchasesPage() {
  const [today] = useState(() => businessDate(new Date()))
  const [range, setRange] = useState<Range>({ from: `${today.slice(0, 8)}01`, to: today })
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<Loaded>()
  const [detail, setDetail] = useState<Purchase>()
  const [voiding, setVoiding] = useState(false)
  const [suppliers, setSuppliers] = useState<Supplier[]>()
  const [formOpen, setFormOpen] = useState(false)
  const [formLoadError, setFormLoadError] = useState<string>()
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])

  const rangeError = range.from && range.to && range.from > range.to ? 'La fecha inicial no puede ser posterior a la final.' : undefined
  const key = `${range.from}|${range.to}|${page}|${attempt}`

  useEffect(() => {
    if (!range.from || !range.to || rangeError) return
    let current = true
    listPurchases(range.from, range.to, page)
      .then((found) => {
        if (current) setLoaded({ key, page: found })
      })
      .catch((error: unknown) => {
        if (current) setLoaded({ key, error: messageOf(error) })
      })
    return () => {
      current = false
    }
  }, [key, range.from, range.to, page, rangeError])

  const shown = loaded?.key === key ? loaded : undefined
  const loading = !rangeError && !shown

  function changeRange(change: Partial<Range>) {
    setRange((current) => ({ ...current, ...change }))
    setPage(0)
  }

  function openForm() {
    setFormOpen(true)
    setFormLoadError(undefined)
    listSuppliers()
      .then((loadedSuppliers) => setSuppliers(loadedSuppliers.filter((supplier) => supplier.active)))
      .catch((error: unknown) => setFormLoadError(messageOf(error)))
  }

  async function handleRegister(request: PurchaseRequest) {
    const created = await registerPurchase(request)
    setFormOpen(false)
    setNotice(`Compra a ${created.supplier.businessName} registrada por ${formatMoney(created.total)}.`)
    setAttempt((value) => value + 1)
  }

  function openDetail(purchase: Purchase) {
    setDetail(purchase)
    setVoiding(false)
  }

  async function handleVoid(reason: string) {
    if (!detail) return
    const voided = await voidPurchase(detail.id, reason)
    setDetail(voided)
    setVoiding(false)
    setLoaded((current) =>
      current?.page
        ? { ...current, page: { ...current.page, content: current.page.content.map((item) => (item.id === voided.id ? voided : item)) } }
        : current,
    )
    setNotice(`Compra a ${voided.supplier.businessName} anulada.`)
  }

  const found = shown?.page

  return (
    <section className="page">
      <header className="page__header page__header--spread">
        <h1>Compras</h1>
        <button className="button button--icon" type="button" aria-label="Registrar compra" title="Registrar compra" onClick={openForm}>
          <Icon name="plus" />
        </button>
      </header>

      {formOpen ? (
        <Dialog title="Registrar compra" variant="drawer" onClose={() => setFormOpen(false)}>
          {formLoadError ? (
            <p className="alert" role="alert">
              {formLoadError}
            </p>
          ) : suppliers === undefined ? (
            <p className="muted" role="status">
              Cargando proveedores…
            </p>
          ) : suppliers.length === 0 ? (
            <p className="muted">No hay proveedores activos. Registra uno en Proveedores antes de la compra.</p>
          ) : (
            <PurchaseForm suppliers={suppliers} today={today} onSubmit={handleRegister} />
          )}
        </Dialog>
      ) : null}

      {detail ? (
        <Dialog
          title={voiding ? `Anular la compra del ${formatDate(detail.purchaseDate)}` : `Compra del ${formatDate(detail.purchaseDate)}`}
          onClose={() => setDetail(undefined)}
        >
          {voiding ? (
            <VoidPurchaseForm onSubmit={handleVoid} onCancel={() => setVoiding(false)} />
          ) : (
            <>
              <dl className="purchase-detail">
                <dt>Proveedor</dt>
                <dd>
                  {detail.supplier.businessName} · RUC {detail.supplier.ruc}
                </dd>
                <dt>Documento</dt>
                <dd>{detail.supplierDocument ?? '—'}</dd>
                <dt>Estado</dt>
                <dd>{STATUS_LABEL[detail.status]}</dd>
                {detail.voidReason ? (
                  <>
                    <dt>Motivo de anulación</dt>
                    <dd>{detail.voidReason}</dd>
                  </>
                ) : null}
              </dl>
              <div className="card data-table-card">
                <table className="data-table" aria-label="Productos de la compra">
                  <thead>
                    <tr>
                      <th scope="col">Producto</th>
                      <th scope="col" className="data-table__number">
                        Cantidad
                      </th>
                      <th scope="col" className="data-table__number">
                        Costo unitario
                      </th>
                      <th scope="col" className="data-table__number">
                        Subtotal
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {detail.items.map((item) => (
                      <tr key={item.productId}>
                        <td data-label="Producto">{item.productName}</td>
                        <td data-label="Cantidad" className="data-table__number">
                          {formatQuantity(item.quantity)}
                        </td>
                        <td data-label="Costo unitario" className="data-table__number">
                          {formatMoney(item.unitCost)}
                        </td>
                        <td data-label="Subtotal" className="data-table__number">
                          {formatMoney(item.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="purchase-form__total">
                Total: <strong>{formatMoney(detail.total)}</strong>
              </p>
              {detail.status === 'REGISTERED' ? (
                <div className="purchase-detail__actions">
                  <button className="button button--secondary" type="button" onClick={() => setVoiding(true)}>
                    Anular compra
                  </button>
                </div>
              ) : null}
            </>
          )}
        </Dialog>
      ) : null}

      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}

      <div className="purchases__filters" role="search">
        <div className="field">
          <label htmlFor="purchases-from">Desde</label>
          <input id="purchases-from" type="date" value={range.from} max={range.to} onChange={(event) => changeRange({ from: event.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="purchases-to">Hasta</label>
          <input id="purchases-to" type="date" value={range.to} min={range.from} onChange={(event) => changeRange({ to: event.target.value })} />
        </div>
      </div>

      {rangeError ? (
        <p className="alert" role="alert">
          {rangeError}
        </p>
      ) : null}

      {loading ? (
        <p className="muted" role="status">
          Cargando compras…
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

      {found && found.content.length === 0 ? <p className="muted">No hay compras en estas fechas.</p> : null}

      {found && found.content.length > 0 ? (
        <div className="card data-table-card">
          <table className="data-table" aria-label="Compras">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Proveedor</th>
                <th scope="col">Documento</th>
                <th scope="col" className="data-table__number">
                  Total
                </th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              {found.content.map((purchase) => (
                <tr key={purchase.id} className={purchase.status === 'VOIDED' ? 'purchases__row--voided' : undefined}>
                  <td data-label="Fecha">
                    <button
                      className="link-button"
                      type="button"
                      aria-label={`Ver compra del ${formatDate(purchase.purchaseDate)} a ${purchase.supplier.businessName}`}
                      onClick={() => openDetail(purchase)}
                    >
                      {formatDate(purchase.purchaseDate)}
                    </button>
                  </td>
                  <td data-label="Proveedor">{purchase.supplier.businessName}</td>
                  <td data-label="Documento">{purchase.supplierDocument ?? '—'}</td>
                  <td data-label="Total" className="data-table__number">
                    {formatMoney(purchase.total)}
                  </td>
                  <td data-label="Estado">
                    <span className={purchase.status === 'VOIDED' ? 'status status--disabled' : 'status'}>
                      {STATUS_LABEL[purchase.status]}
                    </span>
                    {purchase.voidReason ? <span className="purchases__reason muted">{purchase.voidReason}</span> : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {found && (found.totalPages > 1 || found.page > 0) ? (
        <nav className="purchases__pagination" aria-label="Páginas de compras">
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
    </section>
  )
}
