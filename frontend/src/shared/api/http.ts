/**
 * Cliente HTTP base de la API de Kardexa.
 *
 * - Prefija las rutas con `VITE_API_URL` (por defecto `/api/v1`, que Vite redirige al backend en desarrollo).
 * - Convierte las respuestas `application/problem+json` (RFC 9457) en un {@link ApiError} tipado.
 */

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

export const API_BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1'

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
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
    throw new ApiError(await readProblem(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
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
