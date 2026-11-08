# Kardexa · Guía de marca v1.0

**Kardexa** es un sistema web de inventario y ventas para bodegas, ferreterías y minimarkets del Perú. Reemplaza el cuaderno y el Excel: registra compras y ventas, lleva el kárdex de cada producto y avisa cuando el stock está por agotarse.

Personalidad: **confiable, clara, ordenada y cercana**. Moderna, sin frialdad corporativa.

El nombre se escribe siempre **Kardexa**: con K mayúscula, el resto en minúsculas, sin tilde y sin espacios.

## 1. Concepto del logo

El isotipo es una **K** construida como una tarjeta de kárdex:

- **Columna vertical:** es la columna de la tarjeta donde se anota cada movimiento. Va separada de los brazos, como una línea de registro.
- **Trazo inferior (salida):** baja hacia la derecha y representa la salida de stock, es decir, las ventas.
- **Trazo superior con flecha (entrada):** sube y termina en una flecha. Representa las compras que entran y el crecimiento del negocio.

El logotipo es la palabra *Kardexa* compuesta en **Inter Bold** (700) con tracking −2 % y convertida a trazados.

## 2. Cambios respecto a la identidad anterior

| Qué | Por qué |
|---|---|
| Trazos | Terminales redondeados y grosor uniforme (8/64). Se leen mejor a tamaño pequeño y suavizan la K: más cercana, menos corporativa. |
| Columna | La barra vertical queda separada de los brazos por un pequeño espacio: es la columna de la tarjeta de kárdex, donde se anotan las fechas, y le da al símbolo una forma propia frente a una K genérica. |
| Flecha | La punta de la flecha se dibuja como un ángulo abierto, no como un triángulo relleno, para que no se empaste a 32 px. |
| Ficha contenedora | Se agrega la versión dentro de una ficha redondeada para tamaños de 48 px o menos (favicon, avatar, app). Es el mismo dibujo, con trazo algo más grueso. |
| Ámbar de acento | #F59E0B sobre blanco mide 2,15:1 y no cumple el mínimo de 3:1 (WCAG 1.4.11) ni siquiera para íconos. En tema claro pasa a #D97706 (3,19:1), de la misma familia ámbar. En tema oscuro se mantiene #FBBF24 (10,4:1). |
| Texto de alertas | El ámbar nunca se usa como color de texto. Los avisos de stock usan #92400E sobre #FEF3C7 (6,37:1). |
| Texto atenuado | #64748B da 4,34:1 sobre la superficie suave #F1F5F9 y no llega a AA. Pasa a #5B6A7F (5,03:1). |

## 3. Versiones

| Versión | Archivo | Uso |
|---|---|---|
| Horizontal claro | [`logo.svg`](../assets/branding/logo.svg) | Uso principal sobre fondos claros (#FFFFFF, #F8FAFC) |
| Horizontal oscuro | [`logo-oscuro.svg`](../assets/branding/logo-oscuro.svg) | Sobre fondos oscuros (#0B1220) o tema oscuro |
| Monocromo | [`logo-monocromo.svg`](../assets/branding/logo-monocromo.svg) | Impresión a una tinta, sellos, grabados. Puede ir en blanco sobre fondos oscuros |
| Isotipo | [`isotipo.svg`](../assets/branding/isotipo.svg) | Cuando el nombre ya está presente o el espacio es cuadrado (desde 24 px) |
| Ficha | [`frontend/public/favicon.svg`](../frontend/public/favicon.svg) y los PNG junto a él | Favicon, avatar, ícono de app (48 px o menos) |

## 4. Margen de seguridad y tamaño mínimo

- **Margen de seguridad:** X = la mitad de la altura del isotipo. Ningún texto, borde ni imagen debe entrar en ese margen, en ninguno de los cuatro lados.
- **Logo horizontal:** mínimo 100 px de ancho en pantalla o 25 mm impreso.
- **Isotipo sin ficha:** mínimo 24 px o 6 mm.
- **Ficha:** funciona desde 16 px (favicon).

## 5. Usos incorrectos

- No deformar, estirar ni comprimir.
- No cambiar los colores fuera de la paleta.
- No rotar ni inclinar.
- No poner el logo a color sobre el verde primario ni sobre fondos con poco contraste (usar el monocromo blanco).
- No agregar sombras, degradados, contornos ni efectos 3D.
- No volver a escribir el nombre con otra fuente ni separar la columna del resto del isotipo.
- No usar el ámbar como color de marca ni de decoración: está reservado para las alertas de stock.

## 6. Paleta de color

### Tema claro

| Token | HEX | RGB | HSL | Uso |
|---|---|---|---|---|
| `--color-primary` | #0F766E | 15, 118, 110 | hsl(175 77% 26%) | Botones primarios, enlaces, isotipo |
| `--color-primary-hover` | #115E59 | 17, 94, 89 | hsl(176 69% 22%) | Hover/pressed del primario |
| `--color-primary-contrast` | #FFFFFF | 255, 255, 255 | hsl(0 0% 100%) | Texto e íconos sobre el primario |
| `--color-accent` | #D97706 | 217, 119, 6 | hsl(32 95% 44%) | Solo alertas de stock: íconos, puntos, bordes (nunca texto) |
| `--color-text` | #0F172A | 15, 23, 42 | hsl(222 47% 11%) | Texto principal, títulos |
| `--color-text-secondary` | #475569 | 71, 85, 105 | hsl(215 19% 35%) | Texto de apoyo, descripciones |
| `--color-text-muted` | #5B6A7F | 91, 106, 127 | hsl(215 17% 43%) | Ayudas, metadatos, placeholders |
| `--color-bg` | #F8FAFC | 248, 250, 252 | hsl(210 40% 98%) | Fondo de la aplicación |
| `--color-surface` | #FFFFFF | 255, 255, 255 | hsl(0 0% 100%) | Tarjetas, tablas, modales |
| `--color-surface-soft` | #F1F5F9 | 241, 245, 249 | hsl(210 40% 96%) | Filas alternas, cabeceras de tabla |
| `--color-border` | #E2E8F0 | 226, 232, 240 | hsl(214 32% 91%) | Bordes y divisores |
| `--color-danger` | #B91C1C | 185, 28, 28 | hsl(0 74% 42%) | Errores, stock agotado, eliminar |
| `--color-warning-bg` | #FEF3C7 | 254, 243, 199 | hsl(48 96% 89%) | Fondo de aviso de stock bajo |
| `--color-warning-text` | #92400E | 146, 64, 14 | hsl(23 82% 31%) | Texto del aviso de stock bajo |

### Tema oscuro

| Token | HEX | RGB | HSL | Uso |
|---|---|---|---|---|
| `--color-primary` | #2DD4BF | 45, 212, 191 | hsl(172 66% 50%) | Botones primarios, enlaces, isotipo |
| `--color-primary-hover` | #5EEAD4 | 94, 234, 212 | hsl(171 77% 64%) | Hover/pressed del primario |
| `--color-primary-contrast` | #042F2E | 4, 47, 46 | hsl(179 84% 10%) | Texto e íconos sobre el primario |
| `--color-accent` | #FBBF24 | 251, 191, 36 | hsl(43 96% 56%) | Solo alertas de stock: íconos, puntos, bordes (nunca texto) |
| `--color-text` | #F1F5F9 | 241, 245, 249 | hsl(210 40% 96%) | Texto principal, títulos |
| `--color-text-secondary` | #CBD5E1 | 203, 213, 225 | hsl(213 27% 84%) | Texto de apoyo, descripciones |
| `--color-text-muted` | #94A3B8 | 148, 163, 184 | hsl(215 20% 65%) | Ayudas, metadatos, placeholders |
| `--color-bg` | #0B1220 | 11, 18, 32 | hsl(220 49% 8%) | Fondo de la aplicación |
| `--color-surface` | #111A2B | 17, 26, 43 | hsl(219 43% 12%) | Tarjetas, tablas, modales |
| `--color-surface-soft` | #1A2438 | 26, 36, 56 | hsl(220 37% 16%) | Filas alternas, cabeceras de tabla |
| `--color-border` | #2A364C | 42, 54, 76 | hsl(219 29% 23%) | Bordes y divisores |
| `--color-danger` | #F87171 | 248, 113, 113 | hsl(0 91% 71%) | Errores, stock agotado, eliminar |
| `--color-warning-bg` | #3A2306 | 58, 35, 6 | hsl(33 81% 13%) | Fondo de aviso de stock bajo |
| `--color-warning-text` | #FCD34D | 252, 211, 77 | hsl(46 97% 65%) | Texto del aviso de stock bajo |

### Contrastes medidos (WCAG 2.1)

Texto normal AA ≥ 4,5:1 · Elementos gráficos ≥ 3:1. Fórmula de luminancia relativa de WCAG.

| Primer plano | Fondo | Claro | Oscuro | Resultado |
|---|---|---|---|---|
| text | bg | 17.06:1 | 17.09:1 | AA ✓ |
| text | surface | 17.85:1 | 15.89:1 | AA ✓ |
| text-secondary | surface | 7.58:1 | 11.72:1 | AA ✓ |
| text-secondary | surface-soft | 6.92:1 | 10.45:1 | AA ✓ |
| text-muted | surface | 5.51:1 | 6.79:1 | AA ✓ |
| text-muted | bg | 5.26:1 | 7.30:1 | AA ✓ |
| text-muted | surface-soft | 5.03:1 | 6.05:1 | AA ✓ |
| primary-contrast | primary | 5.47:1 | 7.77:1 | AA ✓ |
| primary-contrast | primary-hover | 7.58:1 | 9.78:1 | AA ✓ |
| primary | surface | 5.47:1 | 9.35:1 | AA ✓ |
| primary | bg | 5.23:1 | 10.06:1 | AA ✓ |
| danger | surface | 6.47:1 | 6.29:1 | AA ✓ |
| warning-text | warning-bg | 6.37:1 | 10.24:1 | AA ✓ |
| accent | surface | 3.19:1 | 10.43:1 | Gráfico ✓ (≥3:1) |
| border | surface | 1.23:1 | 1.43:1 | Divisor (decorativo) |


El acento ámbar es solo un indicador gráfico (ícono, punto o borde de una alerta de stock) y siempre va acompañado de texto en `--color-warning-text`.

## 7. Tipografía

**Inter** (Google Fonts, licencia SIL Open Font License). Descarga: https://fonts.google.com/specimen/Inter

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

| Nivel | Fuente y peso | Tamaño / interlineado | Tracking |
|---|---|---|---|
| H1 · Título de página | Inter 700 | 36 / 44 px | -0.02em |
| H2 · Sección | Inter 700 | 28 / 36 px | -0.01em |
| H3 · Tarjeta / módulo | Inter 600 | 20 / 28 px | 0 |
| Cuerpo | Inter 400 | 16 / 24 px | 0 |
| Cuerpo pequeño / tablas | Inter 400 | 14 / 20 px | 0 |
| Etiqueta / botón | Inter 600 | 14 / 20 px | 0 |
| Leyenda / metadato | Inter 500 | 12 / 16 px | +0.02em |

En tablas de inventario, cantidades y precios usa `font-variant-numeric: tabular-nums` para que los números queden alineados en columna. Los montos se escriben como `S/ 1 240.50`.

## 8. Tono de voz

Kardexa habla como un buen encargado de tienda: directo, ordenado y amable. Tutea, usa palabras del día a día del negocio y siempre dice qué hacer después.

**Así sí**

- Te quedan 4 unidades de aceite de 1 L. ¿Lo agregas al próximo pedido?
- Venta registrada: S/ 38.50. Ya se descontó del stock.
- Hoy vendiste S/ 1 240 en 63 ventas. Tu producto estrella: gaseosa de 500 ml.
- No pudimos guardar la compra. Revisa tu conexión e inténtalo otra vez; no se perdió nada.
- Tu kárdex está al día: entradas, salidas y saldo de cada producto, sin cuaderno.

**Así no**

- ¡¡ALERTA CRÍTICA!! Inventario en estado deficiente.
- Transacción procesada exitosamente en el módulo de egresos.
- Error 500: excepción no controlada.
- Optimiza tu supply chain con nuestra solución disruptiva.
- Estimado usuario, se le comunica que...

## 9. Archivos

| Ubicación | Contenido |
|---|---|
| `assets/branding/` | Logo e isotipo (SVG y PNG), banner del README y [guía en PDF](../assets/branding/guia-de-marca.pdf) |
| `assets/branding/redes/` | Avatar, banner de LinkedIn, Open Graph (1200 × 630) y vista previa de GitHub (se sube en Settings del repo) |
| `assets/branding/papeleria/` | Hoja membretada y tarjeta de presentación |
| `frontend/public/` | Favicons, íconos de la app, `manifest.webmanifest` y el logo que usa el panel |
| `frontend/src/shared/styles/tokens.css` | Colores de esta guía como variables CSS |

El panel elige el logo según el tema del sistema con `shared/ui/Logo.tsx`. El README hace lo mismo con el banner, mediante `<picture>`.
