import { apiRequest } from '@shared/api/http'

/** Esquema `Supplier` de docs/api/openapi.yaml. */
export interface Supplier {
  id: string
  ruc: string
  businessName: string
  phone: string | null
  email: string | null
  active: boolean
}

/** Esquema `SupplierRequest`: teléfono y correo son opcionales. */
export interface SupplierData {
  ruc: string
  businessName: string
  phone: string | null
  email: string | null
  active: boolean
}

/** Busca por parte del RUC o de la razón social; sin texto, devuelve todos. */
export function listSuppliers(q = ''): Promise<Supplier[]> {
  const query = q ? `?q=${encodeURIComponent(q)}` : ''
  return apiRequest<Supplier[]>(`/suppliers${query}`)
}

export function createSupplier(data: SupplierData): Promise<Supplier> {
  return apiRequest<Supplier>('/suppliers', { method: 'POST', body: JSON.stringify(data) })
}

export function updateSupplier(id: string, data: SupplierData): Promise<Supplier> {
  return apiRequest<Supplier>(`/suppliers/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export function toSupplierData(supplier: Supplier): SupplierData {
  const { ruc, businessName, phone, email, active } = supplier
  return { ruc, businessName, phone, email, active }
}
