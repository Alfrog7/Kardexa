import type { ReactNode } from 'react'

/** Iconos de interfaz: 24 × 24, trazo de 1,75 px, un solo estilo (el mismo de la barra lateral). */
const PATHS = {
  plus: <path d="M12 5v14M5 12h14" />,
  chevronLeft: <path d="m15 6-6 6 6 6" />,
  chevronRight: <path d="m9 6 6 6-6 6" />,
  chevronUp: <path d="m6 15 6-6 6 6" />,
  sliders: <path d="M4 7h9M17 7h3M4 17h3M11 17h9M13 4.5v5M7 14.5v5" />,
  panelClose: (
    <>
      <rect x="3.5" y="4.5" width="17" height="15" rx="2.5" />
      <path d="M9.5 4.5v15M16 10l-2 2 2 2" />
    </>
  ),
  panelOpen: (
    <>
      <rect x="3.5" y="4.5" width="17" height="15" rx="2.5" />
      <path d="M9.5 4.5v15M14 10l2 2-2 2" />
    </>
  ),
  key: (
    <>
      <circle cx="8" cy="15" r="3.75" />
      <path d="m10.7 12.3 8.3-8.3M16 7l2.5 2.5M13.5 9.5 15.5 11.5" />
    </>
  ),
  logout: (
    <>
      <path d="M14 4.5H6.5a1.5 1.5 0 0 0-1.5 1.5v12a1.5 1.5 0 0 0 1.5 1.5H14" />
      <path d="M10.5 12H20M17 8.5l3.5 3.5-3.5 3.5" />
    </>
  ),
} satisfies Record<string, ReactNode>

export type IconName = keyof typeof PATHS

export function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {PATHS[name]}
    </svg>
  )
}

/** Isotipo K de la marca (guía de marca §1) con trazo del color indicado por la hoja de estilos. */
export function KMark({ size = 28 }: { size?: number }) {
  return (
    <svg
      className="k-mark"
      width={size}
      height={size}
      viewBox="12 8 40 48"
      fill="none"
      stroke="currentColor"
      strokeWidth="7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M20 14V50" />
      <path d="M27.5 33L44 16.5" />
      <path d="M33.5 15.5H45V27" />
      <path d="M31 31L45.5 49" />
    </svg>
  )
}
