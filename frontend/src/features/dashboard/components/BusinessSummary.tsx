import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { routes } from '@app/navigation'
import { ApiError } from '@shared/api/http'
import { formatMoney } from '@shared/lib/format'
import { getDashboard, type Dashboard } from '../api/dashboard'
import { FROM_PARAM, TO_PARAM, formatBusinessDate, rangeFromParams, type DateRange } from '../lib/range'
import { RangePicker } from './RangePicker'
import { CriticalStock } from './CriticalStock'
import { SalesChart } from './SalesChart'
import { TopProducts } from './TopProducts'

interface Loaded {
  key: string
  dashboard?: Dashboard
  error?: string
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'No se pudieron cargar los indicadores. Inténtalo otra vez.'
}

/** Indicadores del negocio para el `ADMIN` (HU-17): el rango se guarda en la URL para que una recarga lo conserve. */
export function BusinessSummary({ today }: { today: string }) {
  const [params, setParams] = useSearchParams()
  const range = rangeFromParams(params, today)
  const [attempt, setAttempt] = useState(0)
  const [loaded, setLoaded] = useState<Loaded>()

  const rangeError = range.from > range.to ? 'La fecha inicial no puede ser posterior a la final.' : undefined
  const key = `${range.from}|${range.to}|${attempt}`

  const { from, to } = range

  useEffect(() => {
    if (rangeError) return
    const request = new AbortController()
    getDashboard(from, to)
      .then((dashboard) => {
        if (!request.signal.aborted) setLoaded({ key, dashboard })
      })
      .catch((error: unknown) => {
        if (!request.signal.aborted) setLoaded({ key, error: messageOf(error) })
      })
    return () => request.abort()
  }, [key, from, to, rangeError])

  function change(next: DateRange) {
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous)
        updated.set(FROM_PARAM, next.from)
        updated.set(TO_PARAM, next.to)
        return updated
      },
      { replace: true },
    )
  }

  const shown = loaded?.key === key ? loaded : undefined
  const summary = shown?.dashboard?.summary

  return (
    <section className="home__summary" aria-labelledby="home-summary-title">
      <div className="home__summary-header">
        <h2 id="home-summary-title">Resumen del negocio</h2>
        <RangePicker range={range} today={today} onChange={change} />
      </div>

      {rangeError ? (
        <p className="alert" role="alert">
          {rangeError}
        </p>
      ) : null}

      {!rangeError && !shown ? (
        <p className="muted" role="status">
          Cargando indicadores…
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

      {summary ? (
        <>
          <p className="home__period muted">
            Del {formatBusinessDate(range.from)} al {formatBusinessDate(range.to)}
            {summary.count === 0 ? ' · No hubo ventas en este rango.' : ''}
          </p>
          <ul className="home__cards" aria-label="Indicadores">
            <li className="card home__card">
              <span className="home__card-label">Total vendido</span>
              <strong className="home__card-value">{formatMoney(summary.total)}</strong>
              <Link to={routes.sales} className="home__card-link">
                Ver ventas
              </Link>
            </li>
            <li className="card home__card">
              <span className="home__card-label">Ventas</span>
              <strong className="home__card-value">{summary.count}</strong>
              <Link to={routes.sales} className="home__card-link">
                Ver historial
              </Link>
            </li>
            <li className="card home__card">
              <span className="home__card-label">Ticket promedio</span>
              <strong className="home__card-value">{formatMoney(summary.averageTicket)}</strong>
              <Link to={routes.sales} className="home__card-link">
                Ver ventas
              </Link>
            </li>
            <li className={summary.criticalCount > 0 ? 'card home__card home__card--alert' : 'card home__card'}>
              <span className="home__card-label">Stock crítico (hoy)</span>
              <strong className="home__card-value">{summary.criticalCount}</strong>
              <Link to={`${routes.products}?critico=1`} className="home__card-link">
                Ver productos
              </Link>
            </li>
          </ul>
          <SalesChart days={shown?.dashboard?.salesByDay ?? []} />
          <div className="home__panels">
            <TopProducts products={shown?.dashboard?.topProducts ?? []} />
            <CriticalStock products={shown?.dashboard?.criticalStock ?? []} />
          </div>
        </>
      ) : null}
    </section>
  )
}
