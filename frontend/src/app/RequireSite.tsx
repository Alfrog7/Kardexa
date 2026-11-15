import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from '@shared/auth/session'
import { useActiveSite } from '@shared/site/activeSite'
import { routes } from './navigation'

/**
 * El administrador trabaja siempre con una sede (o con «todas»): sin elección, va a elegirla. El empleado no
 * elige, su sede la decide la API.
 */
export function RequireSite() {
  const { user } = useSession()
  const active = useActiveSite()
  const location = useLocation()

  if (user?.role === 'ADMIN' && !active) {
    return <Navigate to={routes.pickSite} replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
