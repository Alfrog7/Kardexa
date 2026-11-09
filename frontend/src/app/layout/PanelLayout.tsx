import { NavLink, Outlet } from 'react-router'
import { useSession } from '@shared/auth/session'
import { Logo } from '@shared/ui/Logo'
import { navigation } from '../navigation'
import './PanelLayout.css'

/** Estructura del panel: barra lateral con los módulos y el contenido de la ruta activa. */
export function PanelLayout() {
  const { user, end } = useSession()

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
        <div className="panel__user">
          <p className="panel__user-name">{user?.fullName}</p>
          <p className="muted panel__user-role">{user?.role === 'ADMIN' ? 'Administrador' : 'Empleado'}</p>
          <button className="button button--secondary" type="button" onClick={end}>
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="panel__content">
        <Outlet />
      </main>
    </div>
  )
}
