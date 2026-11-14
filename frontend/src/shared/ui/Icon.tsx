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
  eye: (
    <>
      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  pencil: <path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4ZM13.5 6.5l4 4" />,
  trash: <path d="M4.5 7h15M10 11v6M14 11v6M6 7l1 12.5h10L18 7M9.5 7V4.5h5V7" />,
  grid: (
    <>
      <rect x="4" y="4" width="6.5" height="6.5" rx="1.5" />
      <rect x="13.5" y="4" width="6.5" height="6.5" rx="1.5" />
      <rect x="4" y="13.5" width="6.5" height="6.5" rx="1.5" />
      <rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.5" />
    </>
  ),
  list: <path d="M9 6.5h11M9 12h11M9 17.5h11M4.5 6.5h.01M4.5 12h.01M4.5 17.5h.01" />,
  table: (
    <>
      <rect x="3.5" y="4.5" width="17" height="15" rx="2" />
      <path d="M3.5 9.5h17M3.5 14.5h17M9.5 9.5v10" />
    </>
  ),
  image: (
    <>
      <rect x="3.5" y="4.5" width="17" height="15" rx="2" />
      <circle cx="9" cy="10" r="1.75" />
      <path d="m20.5 16-5-5-9 8.5" />
    </>
  ),
  history: <path d="M4 12a8 8 0 1 0 2.4-5.7L4 8.5M4 4v4.5h4.5M12 8v4l3 2" />,
  refresh: <path d="M19.5 12a7.5 7.5 0 1 1-2.2-5.3L19.5 9M19.5 4.5V9H15" />,
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
