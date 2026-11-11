import type { ReactNode } from 'react'
import { routes } from '../navigation'

/** Iconos de la barra lateral: trazo de 1,75 px, 24 × 24, un solo estilo. */
const ICONS: Record<string, ReactNode> = {
  [routes.dashboard]: (
    <>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5.5 10v9.5h13V10" />
      <path d="M10 19.5v-5h4v5" />
    </>
  ),
  [routes.sales]: (
    <>
      <path d="M6 3.5h12v17l-3-2-3 2-3-2-3 2z" />
      <path d="M9.5 8.5h5M9.5 12h5" />
    </>
  ),
  [routes.products]: (
    <>
      <path d="M21 8 12 3.5 3 8v8l9 4.5 9-4.5z" />
      <path d="m3 8 9 4.5L21 8M12 12.5v8" />
    </>
  ),
  [routes.categories]: (
    <>
      <rect x="3.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="13.5" width="7" height="7" rx="1.5" />
    </>
  ),
  [routes.inventory]: (
    <>
      <rect x="5" y="4.5" width="14" height="16" rx="2" />
      <path d="M9 4.5V3h6v1.5M9 10h6M9 14h6M9 17.5h3" />
    </>
  ),
  [routes.purchases]: (
    <>
      <path d="M5 8.5h14l-1 11.5H6z" />
      <path d="M9 8.5V7a3 3 0 0 1 6 0v1.5" />
    </>
  ),
  [routes.suppliers]: (
    <>
      <path d="M2.5 6.5h11v10h-11z" />
      <path d="M13.5 10h4l3 3v3.5h-7" />
      <circle cx="6.5" cy="17.5" r="1.75" />
      <circle cx="17" cy="17.5" r="1.75" />
    </>
  ),
  [routes.users]: (
    <>
      <circle cx="9.5" cy="8" r="3.5" />
      <path d="M3 20v-1a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5v1" />
      <path d="M17 4.5a3.5 3.5 0 0 1 0 7M19.5 14.5A5 5 0 0 1 21.5 19v1" />
    </>
  ),
}

export function NavIcon({ to }: { to: string }) {
  return (
    <svg
      className="panel__icon"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {ICONS[to]}
    </svg>
  )
}
