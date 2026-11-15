import { apiRequest } from '@shared/api/http'

/** Esquema `Dashboard` de docs/api/openapi.yaml: indicadores del inicio, solo para `ADMIN` (HU-17, ADR 0012). */
export interface Dashboard {
  from: string
  to: string
  summary: DashboardSummary
  salesByDay: DaySales[]
  topProducts: TopProduct[]
  criticalStock: CriticalProduct[]
  /** Lo recibido por medio de pago en los turnos cerrados del rango. */
  payments: PaymentTotals
  /** Una fila por sede; las ventas de todas suman el total del negocio. */
  sites: SiteSales[]
}

export interface PaymentTotals {
  cash: string
  yape: string
  plin: string
  transfer: string
  total: string
}

export interface SiteSales {
  siteId: string
  name: string
  total: string
  count: number
  /** Suma de las diferencias de sus turnos cerrados: negativa si faltó dinero. */
  difference: string
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
  imageUrl?: string
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

/** Un turno de la lista del dueño (`GET /shifts`, solo `ADMIN`); `payments` y `difference` son nulos mientras sigue abierto. */
export interface ShiftRow {
  id: string
  siteId: string
  responsible: string
  status: 'OPEN' | 'CLOSED'
  openedAt: string
  closedAt: string | null
  expectedAmount: number | null
  receivedAmount: number | null
  difference: number | null
  payments: { cash: number; yape: number; plin: number; transfer: number } | null
  note: string | null
  items: { productId: string; productName: string; soldByCount: string | null; unitPrice: number | null }[]
}

/** Turnos abiertos en el rango (fechas `AAAA-MM-DD`), del más reciente al más antiguo. */
export function listShifts(from: string, to: string): Promise<ShiftRow[]> {
  return apiRequest<ShiftRow[]>(`/shifts?${new URLSearchParams({ from, to })}`)
}

/** Un turno con lo vendido por producto. */
export function getShift(id: string): Promise<ShiftRow> {
  return apiRequest<ShiftRow>(`/shifts/${id}`)
}
