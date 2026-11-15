import { useSyncExternalStore } from 'react'

/** Sede con la que trabaja el administrador. `id` nulo = todas las sedes (solo consulta). */
export interface ActiveSite {
  id: string | null
  name: string
}

const KEY = 'kardexa.active-site'
const listeners = new Set<() => void>()

function read(): ActiveSite | undefined {
  try {
    const raw = sessionStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as ActiveSite) : undefined
  } catch {
    return undefined
  }
}

let current: ActiveSite | undefined = read()

export function getActiveSite(): ActiveSite | undefined {
  return current
}

/** Elige la sede (o la descarta con `undefined`). Dura lo que la sesión: cerrar la pestaña la olvida. */
export function setActiveSite(site: ActiveSite | undefined): void {
  current = site
  try {
    if (site) sessionStorage.setItem(KEY, JSON.stringify(site))
    else sessionStorage.removeItem(KEY)
  } catch {
    /* sin almacenamiento: la elección solo dura mientras la página siga abierta */
  }
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useActiveSite(): ActiveSite | undefined {
  return useSyncExternalStore(subscribe, getActiveSite)
}
