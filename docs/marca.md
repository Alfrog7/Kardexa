# Identidad visual de Kardexa

## Concepto

El isotipo es una **K** construida con dos trazos:

- El trazo inferior (blanco) representa una **salida** de stock.
- El trazo superior (ámbar) termina en una **flecha ascendente**: entradas, crecimiento y alerta oportuna.

La barra vertical evoca la columna de una tarjeta de **kárdex**, de donde viene el nombre.

## Recursos

| Archivo | Uso |
|---|---|
| [`assets/branding/isotipo.svg`](../assets/branding/isotipo.svg) | Favicon, avatar, ícono de la app |
| [`assets/branding/logo.svg`](../assets/branding/logo.svg) | Logo horizontal sobre fondo claro |
| [`assets/branding/logo-oscuro.svg`](../assets/branding/logo-oscuro.svg) | Logo horizontal sobre fondo oscuro |
| [`assets/branding/banner.svg`](../assets/branding/banner.svg) | Cabecera del README (tema claro) |
| [`assets/branding/banner-oscuro.svg`](../assets/branding/banner-oscuro.svg) | Cabecera del README (tema oscuro) |

Todos los recursos son **SVG**: escalan sin perder calidad, pesan poco y sus cambios se pueden revisar en un diff.

## Paleta

| Rol | Claro | Oscuro | Uso |
|---|---|---|---|
| Primario | `#0F766E` | `#2DD4BF` | Marca, acciones principales, enlaces |
| Acento | `#F59E0B` | `#FBBF24` | Alertas de stock, resaltados |
| Texto | `#0F172A` | `#F8FAFC` | Títulos y texto principal |
| Texto secundario | `#334155` | `#CBD5E1` | Subtítulos |
| Texto atenuado | `#64748B` | `#94A3B8` | Etiquetas, metadatos |
| Fondo suave | `#F0FDFA` | `#042F2E` | Secciones destacadas |

El acento ámbar se reserva para **estados que requieren atención** (stock mínimo, alertas). No se usa como decoración.

## Tipografía

- **Inter** para la interfaz y la landing (fase 8).
- En los SVG se usa una pila de fuentes del sistema (`Segoe UI`, `Inter`, `Helvetica Neue`, `Arial`) porque GitHub no carga fuentes web dentro de imágenes.

## Uso del logo

- Respetar un margen libre alrededor del logo de al menos la mitad del alto del isotipo.
- No deformar, rotar ni cambiar los colores del isotipo.
- Sobre fondos oscuros usar `logo-oscuro.svg`.
