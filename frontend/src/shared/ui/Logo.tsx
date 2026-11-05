interface LogoProps {
  /** Muestra el nombre junto al isotipo. */
  withWordmark?: boolean
  size?: number
}

/** Isotipo de Kardexa (assets/branding/isotipo.svg) en línea, para heredar el tamaño y el tema. */
export function Logo({ withWordmark = true, size = 32 }: LogoProps) {
  return (
    <span className="logo">
      <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
        <rect width="64" height="64" rx="14" fill="#0F766E" />
        <rect x="16" y="14" width="8" height="36" rx="2" fill="#FFFFFF" />
        <line x1="28" y1="33" x2="44" y2="49" stroke="#FFFFFF" strokeWidth="8" strokeLinecap="round" />
        <line x1="28" y1="31" x2="40" y2="19" stroke="#F59E0B" strokeWidth="8" strokeLinecap="round" />
        <polygon points="48,11 48,25 34,11" fill="#F59E0B" />
      </svg>
      {withWordmark ? <span className="logo__text">Kardexa</span> : <span className="visually-hidden">Kardexa</span>}
    </span>
  )
}
