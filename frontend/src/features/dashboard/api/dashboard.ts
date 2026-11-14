import { apiRequest } from '@shared/api/http'

/** Esquema `Dashboard` de docs/api/openapi.yaml: indicadores del inicio, solo para `ADMIN` (HU-17, ADR 0012). */
export interface Dashboard {
  from: string
  to: string
  summary: DashboardSummary
  salesByDay: DaySales[]
  topProducts: TopProduct[]
  criticalStock: CriticalProduct[]
}

export interface DashboardSummary {
  total: string
  count: number
  averageTicket: string
  /** Productos en stock crítico hoy; no depende del rango. */
  criticalCount: number
}

export interface DaySales {
  date: string
  total: string
  count: number
}

export interface TopProduct {
  productId: string
  productCode: string
  productName: string
  quantity: string
  amount: string
}

export interface CriticalProduct {
  productId: string
  productCode: string
  productName: string
  categoryName: string
  stock: string
  minStock: string
  imageUrl?: string
}

/** Fechas `AAAA-MM-DD` del negocio (hora de Lima), ambas incluidas. */
export function getDashboard(from: string, to: string): Promise<Dashboard> {
  return apiRequest<Dashboard>(`/dashboard?${new URLSearchParams({ from, to })}`)
}
