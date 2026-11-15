import { apiRequest } from '@shared/api/http'

/** Sede que se puede asignar a un empleado (`GET /sites`, solo ADMIN). */
export interface SiteOption {
  id: string
  name: string
  active: boolean
}

/** Las sedes para el selector y para rotular la columna «Sede»; si no se pueden cargar, la lista queda vacía. */
export async function listSiteOptions(): Promise<SiteOption[]> {
  try {
    const sites = await apiRequest<SiteOption[]>('/sites')
    return Array.isArray(sites) ? sites : []
  } catch {
    return []
  }
}
