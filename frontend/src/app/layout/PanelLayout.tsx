import { NavLink, Outlet } from 'react-router'
import { Logo } from '@shared/ui/Logo'
import { navigation } from '../navigation'
import './PanelLayout.css'

/** Estructura del panel: barra lateral con los módulos y el contenido de la ruta activa. */
export function PanelLayout() {
  return (
    <div className="panel">
      <aside className="panel__sidebar">
        <div className="panel__brand">
          <Logo />
        </div>
        <nav aria-label="Módulos">
          <ul className="panel__nav">
            {navigation.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.to === '/'} className="panel__link">
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
      <main className="panel__content">
        <Outlet />
      </main>
    </div>
  )
}
