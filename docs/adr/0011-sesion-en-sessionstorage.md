# 0011 · La sesión del panel sobrevive a una recarga

- **Estado:** Aceptado
- **Fecha:** 2026-11-12
- **Reemplaza:** el punto 5 del [ADR 0008](0008-autenticacion-con-jwt.md) (almacenamiento del token en el panel)

## Contexto

Con el token solo en memoria, recargar la página (F5 o Ctrl+R) cierra la sesión y obliga a escribir otra vez el correo y la contraseña. En el uso diario, un vendedor recarga a menudo, y la molestia pesa más que la ventaja de seguridad: un script inyectado (XSS) ya puede usar la sesión abierta haciendo peticiones desde la propia página, aunque no lea el token.

## Decisión

1. Al ingresar, el panel guarda el token y los datos del usuario en **`sessionStorage`**.
2. Al cargar la página, restaura esa sesión si el token no venció (se lee el `exp` del JWT, con 30 s de margen).
3. Se borra al salir, cuando la API responde 401 y al cerrar la pestaña (comportamiento propio de `sessionStorage`).
4. No se usa `localStorage`: la sesión no queda abierta en el equipo después de cerrar la pestaña.
5. Siguen sin existir los *refresh tokens*: el token dura lo que diga `JWT_EXPIRATION_MINUTES` (60 min por defecto).

## Consecuencias

- ✅ Recargar la página ya no cierra la sesión.
- ⚠️ Un XSS podría leer el token mientras la pestaña esté abierta. Se mitiga con la política de contenido y la revisión de seguridad prevista al terminar las fases.
- ⚠️ Cada pestaña nueva pide ingresar; es lo esperado con `sessionStorage`.
