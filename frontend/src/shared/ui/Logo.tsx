/** Proporción de los SVG del logo horizontal (525.35 × 122). */
const LOGO_RATIO = 525.35 / 122

interface LogoProps {
  /** Alto en píxeles. La guía de marca pide un ancho mínimo de 100 px. */
  height?: number
  /** Fuerza la versión para fondos oscuros (paneles de marca que no cambian con el tema). */
  onDark?: boolean
}

/** Logo horizontal de Kardexa: el navegador elige la versión según el tema del sistema. */
export function Logo({ height = 32, onDark = false }: LogoProps) {
  const width = Math.round(height * LOGO_RATIO)
  if (onDark) {
    return (
      <span className="logo">
        <img src="/logo-oscuro.svg" alt="Kardexa" height={height} width={width} />
      </span>
    )
  }
  return (
    <picture className="logo">
      <source media="(prefers-color-scheme: dark)" srcSet="/logo-oscuro.svg" />
      <img src="/logo.svg" alt="Kardexa" height={height} width={width} />
    </picture>
  )
}
