import { apiRequest } from '@shared/api/http'

/** Esquema `Site` de docs/api/openapi.yaml. */
export interface Site {
  id: string
  name: string
  address: string | null
  active: boolean
}

/** Esquema `SiteRequest`: la dirección es opcional. */
export interface SiteData {
  name: string
  address: string | null
}

/** Todas las sedes, habilitadas e inhabilitadas, por nombre. */
export function listSites(): Promise<Site[]> {
  return apiRequest<Site[]>('/sites')
}

/** La sede de quien consulta: la de su sesión si es trabajadora; la principal si es administrador. */
export function getMySite(): Promise<Site> {
  return apiRequest<Site>('/sites/mine')
}

export function createSite(data: SiteData): Promise<Site> {
  return apiRequest<Site>('/sites', { method: 'POST', body: JSON.stringify(data) })
}

export function updateSite(id: string, data: SiteData): Promise<Site> {
  return apiRequest<Site>(`/sites/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

/** Elimina una sede que nunca se usó; con historial o trabajadores la API responde 409. */
export function deleteSite(id: string): Promise<void> {
  return apiRequest<void>(`/sites/${id}`, { method: 'DELETE' })
}

/** Inhabilita o vuelve a habilitar una sede; conserva su historial. */
export function setSiteActive(id: string, active: boolean): Promise<Site> {
  return apiRequest<Site>(`/sites/${id}`, { method: 'PATCH', body: JSON.stringify({ active }) })
}
