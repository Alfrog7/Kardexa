# 0008 · Autenticación con JWT propio

- **Estado:** Aceptado
- **Fecha:** 2026-11-09
- **Requisitos relacionados:** RF-01, RF-02, RF-05, RNF-01, RNF-02

## Contexto

La API es sin estado y la consume un panel web (SPA). Hay que identificar al usuario en cada petición, restringir operaciones por rol (RN-05: ajustes solo `ADMIN`) y guardar las contraseñas de forma segura. El requisito RNF-02 fija un JWT propio con expiración configurable (60 min por defecto).

## Alternativas

| Opción | A favor | En contra |
|---|---|---|
| Sesión en servidor (cookie de sesión) | Revocación inmediata; simple | Estado en el servidor; contradice una API sin estado |
| **JWT de acceso firmado (HS256) emitido por la API** | Sin estado; encaja con SPA; lo valida Spring Security | No se puede revocar antes de vencer |
| JWT + *refresh token* | Sesiones largas y revocables | Más tablas, rotación y superficie de ataque; excesivo para el alcance |
| Proveedor externo (OIDC/Keycloak) | Delegado y estándar | Infraestructura y dependencia ajenas al objetivo del proyecto |

## Decisión

1. **Spring Security** con el *resource server* JWT de Spring (`spring-boot-starter-security` y `spring-boot-starter-oauth2-resource-server`, basado en Nimbus JOSE). No se agrega una librería JWT aparte.
2. **JWT de acceso HS256**, emitido por `POST /auth/login`, con `sub` = id del usuario, `role` y `exp`. La clave sale de la variable `JWT_SECRET` (mínimo 32 bytes; no se versiona).
3. **Expiración:** 60 min por defecto, configurable con `JWT_EXPIRATION_MINUTES`. **Sin *refresh tokens***: al vencer, el usuario vuelve a ingresar.
4. **Contraseñas con BCrypt** (`BCryptPasswordEncoder`, incluido en Spring Security). Nunca se guardan ni se registran en texto plano.
5. **Almacenamiento en el panel:** el token vive **en memoria** (estado de la app). Al recargar la página se pide ingresar de nuevo. Se descartan `localStorage`/`sessionStorage` (legibles por cualquier XSS) y la cookie `HttpOnly` (exigiría CSRF y mismo sitio).
6. **Endpoints públicos:** `POST /auth/setup`, `POST /auth/login`, Swagger/OpenAPI y `/actuator/health`. Todo lo demás exige `Authorization: Bearer <JWT>`.
7. **Errores** en Problem Details: `401` sin token, inválido o vencido; `403` con token válido pero rol insuficiente.
8. El dominio no depende de Spring Security: la emisión del token y el hash son puertos de salida con adaptadores en `infrastructure`.

## Consecuencias

- ✅ API sin estado y alineada con RNF-01 y RNF-02.
- ✅ Una sola dependencia nueva de seguridad, mantenida con Spring.
- ⚠️ Un token no se puede revocar antes de vencer; se mitiga con la expiración corta.
- ⚠️ Recargar la página cierra la sesión; es un costo aceptado frente al riesgo de XSS.
- ⚠️ Si más adelante se necesitan sesiones largas o revocación, un nuevo ADR añadirá *refresh tokens* y reemplazará a este.
