import { ComingSoon } from '@shared/ui/ComingSoon'

export function InventoryPage() {
  return (
    <ComingSoon
      title="Inventario"
      description="Consulta el kárdex de cada producto, registra ajustes por merma o conteo físico y revisa las alertas de stock mínimo."
      phase={4}
      stories="HU-08, HU-09, HU-10"
    />
  )
}
