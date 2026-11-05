import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './http'

function mockFetch(response: Response | Error) {
  const fn = vi.fn((_input: RequestInfo | URL, _init?: RequestInit) =>
    response instanceof Error ? Promise.reject(response) : Promise.resolve(response),
  )
  vi.stubGlobal('fetch', fn)
  return fn
}

const json = (body: unknown, status: number, type = 'application/json') =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': type } })

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('prefija la ruta con /api/v1 y devuelve el JSON', async () => {
    const fetchMock = mockFetch(json({ id: '1', name: 'Arroz 5 kg' }, 200))

    const product = await apiRequest<{ name: string }>('/products/1')

    expect(product.name).toBe('Arroz 5 kg')
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/products/1', expect.anything())
  })

  it('envía JSON con Content-Type cuando hay cuerpo', async () => {
    const fetchMock = mockFetch(json({}, 201))

    await apiRequest('/sales', { method: 'POST', body: JSON.stringify({ items: [] }) })

    const init = fetchMock.mock.calls[0]?.[1]
    expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json')
  })

  it('devuelve undefined en 204', async () => {
    mockFetch(new Response(null, { status: 204 }))
    await expect(apiRequest('/auth/me/password', { method: 'PUT', body: '{}' })).resolves.toBeUndefined()
  })

  it('convierte un Problem Details de regla de negocio en ApiError', async () => {
    mockFetch(
      json(
        {
          title: 'Stock insuficiente',
          status: 422,
          detail: 'Stock insuficiente para "Arroz 5 kg": disponible 2, solicitado 3.',
          rule: 'RN-01',
        },
        422,
        'application/problem+json',
      ),
    )

    const error = await apiRequest('/sales', { method: 'POST', body: '{}' }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 422,
      title: 'Stock insuficiente',
      rule: 'RN-01',
      message: 'Stock insuficiente para "Arroz 5 kg": disponible 2, solicitado 3.',
    })
  })

  it('expone los errores por campo de una validación', async () => {
    mockFetch(
      json(
        { title: 'Datos inválidos', status: 400, errors: [{ field: 'name', message: 'El nombre es obligatorio' }] },
        400,
        'application/problem+json',
      ),
    )

    const error = (await apiRequest('/products', { method: 'POST', body: '{}' }).catch((e: unknown) => e)) as ApiError

    expect(error.fieldErrors).toEqual([{ field: 'name', message: 'El nombre es obligatorio' }])
  })

  it('marca el 409 como conflicto reintentable (ADR 0006)', async () => {
    mockFetch(json({ title: 'Conflicto', status: 409 }, 409, 'application/problem+json'))

    const error = (await apiRequest('/sales', { method: 'POST', body: '{}' }).catch((e: unknown) => e)) as ApiError

    expect(error.isConflict).toBe(true)
  })

  it('tolera respuestas de error sin cuerpo JSON', async () => {
    mockFetch(new Response('Bad Gateway', { status: 502, headers: { 'Content-Type': 'text/plain' } }))

    await expect(apiRequest('/products')).rejects.toMatchObject({ status: 502, title: 'Error inesperado' })
  })

  it('traduce un fallo de red a un mensaje en español', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    await expect(apiRequest('/products')).rejects.toMatchObject({ status: 0, title: 'Sin conexión' })
  })
})
