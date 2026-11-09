import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from '@shared/auth/session'
import { routes } from './navigation'

/** Guarda del panel: sin sesión, cualquier ruta lleva a `/ingresar` y recuerda a dónde se iba. */
export function RequireSession() {
  const { user } = useSession()
  const location = useLocation()

  if (!user) {
    return <Navigate to={routes.login} replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
