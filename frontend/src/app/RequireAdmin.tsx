import { Navigate, Outlet } from 'react-router'
import { useSession } from '@shared/auth/session'
import { routes } from './navigation'

/** Guarda de las rutas de administración: un empleado vuelve al inicio. */
export function RequireAdmin() {
  const { user } = useSession()

  if (user?.role !== 'ADMIN') {
    return <Navigate to={routes.dashboard} replace />
  }
  return <Outlet />
}
