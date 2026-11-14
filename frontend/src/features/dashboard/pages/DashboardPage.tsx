import { useState } from 'react'
import { Link } from 'react-router'
import { routes } from '@app/navigation'
import { useSession } from '@shared/auth/session'
import { businessDate } from '@shared/lib/format'
import { BusinessSummary } from '../components/BusinessSummary'
import './DashboardPage.css'

/** Accesos del `EMPLEADO`: no ve indicadores del negocio. */
const SHORTCUTS = [
  { to: routes.sales, label: 'Registrar venta', hint: 'Agrega los productos, cobra y entrega el comprobante.' },
  { to: routes.products, label: 'Productos', hint: 'Busca productos y mira su precio y stock.' },
  { to: routes.inventory, label: 'Inventario', hint: 'Registra entradas y salidas y mira el stock de cada producto.' },
]

function greeting(hour: number) {
  if (hour < 12) return 'Buenos días'
  if (hour < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

/** Inicio: el `ADMIN` ve los indicadores del negocio (HU-17); el `EMPLEADO`, accesos rápidos. */
export function DashboardPage() {
  const { user } = useSession()
  const [now] = useState(() => new Date())
  const firstName = user?.fullName.split(' ')[0]
  const today = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima' }).format(now)
  const admin = user?.role === 'ADMIN'

  return (
    <section className="page home">
      <header className="home__header">
        <h1>Inicio</h1>
        <p className="home__date">{today}</p>
      </header>
      <p className="home__greeting">
        {greeting(now.getHours())}
        {firstName ? `, ${firstName}` : ''}.
      </p>
      {admin ? (
        <BusinessSummary today={businessDate(now)} />
      ) : (
        <nav className="home__shortcuts" aria-label="Accesos rápidos">
          <ul>
            {SHORTCUTS.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className="home__shortcut">
                  <span className="home__shortcut-name">{item.label}</span>
                  <span className="home__shortcut-hint">{item.hint}</span>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M5 12h14M13 6l6 6-6 6" />
                  </svg>
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </section>
  )
}
