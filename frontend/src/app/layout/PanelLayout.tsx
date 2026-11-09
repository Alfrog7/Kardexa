import { useCallback, useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { ChangePasswordForm } from '@features/auth'
import { useSession } from '@shared/auth/session'
import { Dialog } from '@shared/ui/Dialog'
import { Logo } from '@shared/ui/Logo'
import { Toast } from '@shared/ui/Toast'
import { navigation } from '../navigation'
import './PanelLayout.css'

/** Estructura del panel: barra lateral con los módulos y el contenido de la ruta activa. */
export function PanelLayout() {
  const { user, end } = useSession()
  const [changingPassword, setChangingPassword] = useState(false)
  const [notice, setNotice] = useState<string>()
  const dismissNotice = useCallback(() => setNotice(undefined), [])
  const visibleNavigation = navigation.filter((item) => !('adminOnly' in item) || user?.role === 'ADMIN')

  return (
    <div className="panel">
      <aside className="panel__sidebar">
        <div className="panel__brand">
          <Logo />
        </div>
        <nav aria-label="Módulos">
          <ul className="panel__nav">
            {visibleNavigation.map((item) => (
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
          <button className="link-button" type="button" onClick={() => setChangingPassword(true)}>
            Cambiar contraseña
          </button>
          <button className="button button--secondary" type="button" onClick={end}>
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="panel__content">
        <Outlet />
      </main>
      {changingPassword ? (
        <Dialog title="Cambiar contraseña" onClose={() => setChangingPassword(false)}>
          <ChangePasswordForm
            onChanged={() => {
              setChangingPassword(false)
              setNotice('Contraseña actualizada.')
            }}
          />
        </Dialog>
      ) : null}
      {notice ? <Toast message={notice} onDismiss={dismissNotice} /> : null}
    </div>
  )
}
