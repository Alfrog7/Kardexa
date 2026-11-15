/**
 * Cliente HTTP base de la API de Kardexa.
 *
 * - Prefija las rutas con `VITE_API_URL` (por defecto `/api/v1`, que Vite redirige al backend en desarrollo).
 * - Convierte las respuestas `application/problem+json` (RFC 9457) en un {@link ApiError} tipado.
 */

import { getActiveSite } from '@shared/site/activeSite'

export interface FieldError {
  field: string
  message: string
}

/** Cuerpo de error que devuelve la API (docs/api/openapi.yaml → Problem). */
export interface Problem {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  /** Regla de negocio violada, por ejemplo `RN-01`. */
  rule?: string
  errors?: FieldError[]
}

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly rule?: string
  readonly fieldErrors: FieldError[]

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiError'
    this.status = problem.status
    this.title = problem.title
    this.rule = problem.rule
    this.fieldErrors = problem.errors ?? []
  }

  /** El stock cambió durante la operación (ADR 0006): el usuario puede reintentar. */
  get isConflict(): boolean {
    return this.status === 409
  }
}

let accessToken: string | undefined
let onUnauthorized: (() => void) | undefined

/** Token de acceso que usan las peticiones. La copia que sobrevive a una recarga la guarda la sesión (ADR 0011). */
export function setAccessToken(token: string | undefined): void {
  accessToken = token
}

/** Se llama cuando la API responde 401 con la sesión abierta (token vencido o inválido). */
export function setUnauthorizedHandler(handler: (() => void) | undefined): void {
  onUnauthorized = handler
}

export const API_BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1'

/**
 * URL absoluta de un recurso que la API devuelve como ruta (`/api/v1/...`), por ejemplo una imagen, para
 * que funcione también cuando el panel y la API están en dominios distintos.
 */
export function apiResourceUrl(path: string): string {
  return `${API_BASE_URL.replace(/\/api\/v1\/?$/, '')}${path}`
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await send(path, init, 'application/json, application/problem+json')
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

/**
 * Descarga un archivo de la API con el token de la sesión (ADR 0012: nunca un enlace directo sin sesión). Usa el
 * nombre de `Content-Disposition` y, si el navegador no lo deja leer, `fallbackName`.
 */
export async function apiDownload(path: string, fallbackName: string): Promise<void> {
  const response = await send(path, {}, 'text/csv, application/problem+json')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filenameOf(response.headers.get('Content-Disposition')) ?? fallbackName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** `attachment; filename="kardex-P-0001.csv"` → `kardex-P-0001.csv` */
export function filenameOf(disposition: string | null): string | undefined {
  return disposition ? /filename="?([^";]+)"?/i.exec(disposition)?.[1] : undefined
}

/** Consultas que dependen de la sede (ADR 0013): reciben `siteId` en la URL. */
const SITE_READS = ['/products', '/stock-movements', '/sales', '/purchases', '/dashboard']
/** Operaciones que se registran en una sede: reciben `siteId` en el cuerpo. */
const SITE_WRITES = ['/sales', '/purchases', '/stock-adjustments', '/receipts']

/**
 * Aplica la sede elegida por el administrador: añade `siteId` a las consultas y a los registros de ventas, compras,
 * ajustes y entradas. Sin sede elegida no toca nada (la API usa la Sede principal en los registros); con «Todas las sedes» las
 * consultas juntan todas y los registros se rechazan: hay que elegir una sede concreta. El empleado nunca elige: su sede la decide la API.
 */
function withSite(path: string, init: RequestInit): { path: string; init: RequestInit } {
  const active = getActiveSite()
  const method = (init.method ?? 'GET').toUpperCase()
  const route = path.split('?')[0] ?? path
  if (active && active.id === null && method === 'POST' && SITE_WRITES.includes(route)) {
    throw new ApiError({
      title: 'Elige una sede',
      status: 0,
      detail: 'Con «Todas las sedes» solo se consulta. Elige una sede concreta para registrar la operación.',
    })
  }
  const siteId = active?.id
  if (!siteId) return { path, init }
  if (method === 'GET' && SITE_READS.some((prefix) => route === prefix || route.startsWith(`${prefix}/`))) {
    if (/[?&]siteId=/.test(path)) return { path, init }
    return { path: `${path}${path.includes('?') ? '&' : '?'}siteId=${siteId}`, init }
  }
  if (method === 'POST' && SITE_WRITES.includes(route) && typeof init.body === 'string') {
    return { path, init: { ...init, body: JSON.stringify({ siteId, ...(JSON.parse(init.body) as object) }) } }
  }
  return { path, init }
}

async function send(rawPath: string, rawInit: RequestInit, accept: string): Promise<Response> {
  const { path, init } = withSite(rawPath, rawInit)
  const headers = new Headers(init.headers)
  headers.set('Accept', accept)
  // Con FormData (subir archivos) el navegador pone el Content-Type con su separador.
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiError({
      title: 'Sin conexión',
      status: 0,
      detail: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
    })
  }

  if (!response.ok) {
    if (response.status === 401 && accessToken) {
      onUnauthorized?.()
    }
    throw new ApiError(await readProblem(response))
  }
  return response
}

async function readProblem(response: Response): Promise<Problem> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (contentType.includes('json')) {
    try {
      const body = (await response.json()) as Partial<Problem>
      return { ...body, title: body.title ?? 'Error', status: body.status ?? response.status }
    } catch {
      // cuerpo vacío o inválido: se usa el genérico de abajo
    }
  }
  return { title: 'Error inesperado', status: response.status, detail: `El servidor respondió ${response.status}.` }
}
