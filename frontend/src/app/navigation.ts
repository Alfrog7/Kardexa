/** Rutas del panel. Las URL van en español porque las ve el usuario (ADR 0003). */
export const routes = {
  login: '/ingresar',
  dashboard: '/',
  sales: '/ventas',
  products: '/productos',
  inventory: '/inventario',
  purchases: '/compras',
} as const

export const navigation = [
  { to: routes.dashboard, label: 'Inicio' },
  { to: routes.sales, label: 'Ventas' },
  { to: routes.products, label: 'Productos' },
  { to: routes.inventory, label: 'Inventario' },
  { to: routes.purchases, label: 'Compras' },
] as const
