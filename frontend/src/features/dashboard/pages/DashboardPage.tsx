import { useState } from 'react'
import { Link } from 'react-router'
import { routes } from '@app/navigation'
import { useSession } from '@shared/auth/session'
import './DashboardPage.css'

const SHORTCUTS = [
  { to: routes.inventory, label: 'Inventario', hint: 'Registra entradas y salidas y mira el stock de cada producto.' },
  { to: routes.products, label: 'Productos', hint: 'Crea y edita tu catálogo con precios y stock mínimo.' },
  { to: routes.categories, label: 'Categorías', hint: 'Ordena los productos para encontrarlos rápido.' },
  { to: routes.users, label: 'Usuarios', hint: 'Da acceso a tu equipo y elige qué puede hacer.', adminOnly: true },
]

function greeting(hour: number) {
  if (hour < 12) return 'Buenos días'
  if (hour < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

/** Inicio: saludo y accesos a los módulos ya disponibles. Los reportes llegan en la fase 7 (HU-17). */
export function DashboardPage() {
  const { user } = useSession()
  const [now] = useState(() => new Date())
  const firstName = user?.fullName.split(' ')[0]
  const today = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' }).format(now)
  const shortcuts = SHORTCUTS.filter((item) => !item.adminOnly || user?.role === 'ADMIN')

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
      <nav className="home__shortcuts" aria-label="Accesos rápidos">
        <ul>
          {shortcuts.map((item) => (
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
      <p className="home__soon">
        <span className="badge">Fase 7</span>
        Ventas por día, productos más vendidos y stock crítico aparecerán aquí (HU-17).
      </p>
    </section>
  )
}
