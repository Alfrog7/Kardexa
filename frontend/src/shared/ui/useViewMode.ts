import { useState } from 'react'

export type ViewMode = 'cards' | 'list' | 'table'

/** Vista elegida por pantalla; se recuerda en este navegador. */
export function useViewMode(key: string, fallback: ViewMode = 'table'): [ViewMode, (mode: ViewMode) => void] {
  const storageKey = `kardexa.view.${key}`
  const [mode, setMode] = useState<ViewMode>(() => {
    try {
      const saved = localStorage.getItem(storageKey)
      return saved === 'cards' || saved === 'list' || saved === 'table' ? saved : fallback
    } catch {
      return fallback
    }
  })

  function change(next: ViewMode) {
    setMode(next)
    try {
      localStorage.setItem(storageKey, next)
    } catch {
      // Sin almacenamiento la vista solo dura mientras la página está abierta.
    }
  }

  return [mode, change]
}
