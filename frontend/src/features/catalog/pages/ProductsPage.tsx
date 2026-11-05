import { ComingSoon } from '@shared/ui/ComingSoon'

export function ProductsPage() {
  return (
    <ComingSoon
      title="Productos"
      description="Registra productos con su código, categoría, precio de venta y stock mínimo, y búscalos por nombre o código."
      phase={4}
      stories="HU-05, HU-06, HU-07"
    />
  )
}
