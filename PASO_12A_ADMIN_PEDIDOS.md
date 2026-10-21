# Paso 12A — Consulta protegida de pedidos para administrador

## Objetivo
El servidor Spring Boot ya procesa pedidos reales de demostración desde el carrito Android, pero la pantalla del administrador sigue mostrando copias SQLite. Esta subfase agrega un endpoint **de solo lectura** para consultar pedidos H2 desde el servidor, sin cambiar la app todavía.

## Endpoint

`GET /api/admin/orders`

- Header obligatorio: `X-OYL-ADMIN-KEY`.
- La clave se configura con la variable de entorno del servidor `OYL_ADMIN_KEY` (mínimo 16 caracteres).
- Por defecto devuelve hasta 20 pedidos recientes, ordenados por fecha descendente.
- `limit`: entre 1 y 100.
- `status`: opcional; `PENDING`, `CONFIRMED`, `PREPARED` o `DELIVERED`.
- Devuelve datos de pedido, líneas, modalidad y contacto como el modelo `OrderResponse`.
- Sin clave o con clave incorrecta devuelve HTTP 403; parámetros inválidos devuelven HTTP 400.

## Prueba en Windows

En una terminal del proyecto, después de actualizar a esta rama y **sin borrar la base H2**:

```powershell
.\gradlew.bat :server:test
```

Resultado esperado: `BUILD SUCCESSFUL`. El test `AdminOrderListingTest` comprueba lectura, autorización, filtro de estados, límite y parámetros inválidos con una base H2 **en memoria separada**.

Para iniciar manualmente el servidor con una clave creada **por ti**, configura `OYL_ADMIN_KEY` en el entorno de la misma terminal antes de ejecutar `.:server:bootRun` (la clave no debe estar en código ni subirse a GitHub).

## Limitaciones y próximas fases

- Esta función es **solo backend**. La app Android todavía no consume esta lista.
- No incrustar `OYL_ADMIN_KEY` en el APK, incluso ofuscada. Antes de integrar administración móvil, diseñar autenticación/roles del servidor que no requiera entregar su secreto permanente al dispositivo.
- La configuración del servidor sigue limitada a localhost; no desplegarlo en Internet tal cual.
- En 12B, conectar la interfaz de pedidos del administrador a un backend que autorice sesiones administrativas.
- Mantener los pedidos anteriores de SQLite y los datos del emulador.
