interface ComingSoonProps {
  title: string
  description: string
  /** Fase del plan en que se implementa (README → Plan por fases). */
  phase: number
  stories?: string
}

/** Marcador de una pantalla que todavía no se implementa. */
export function ComingSoon({ title, description, phase, stories }: ComingSoonProps) {
  return (
    <section className="page">
      <header className="page__header">
        <h1>{title}</h1>
        <span className="badge">Fase {phase}</span>
      </header>
      <div className="card">
        <p>{description}</p>
        {stories ? <p className="muted">Historias de usuario: {stories}</p> : null}
      </div>
    </section>
  )
}
