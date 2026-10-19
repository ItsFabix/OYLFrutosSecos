# Paso 11C — Enviar un pedido REST desde Android (prueba aislada)

**Objetivo:** comprobar que la app Android (Kotlin + Retrofit) crea un pedido en Spring Boot y que el servidor descuenta stock en H2, con reintentos seguros por `requestId`.

**Importante:** esta fase NO reemplaza aún la compra normal, el carrito, inventario ni historial SQLite. Solo el botón dentro de Cuenta → Probar conexión REST (Paso 11) envía pedidos al servidor. Usa únicamente datos ficticios.

## Preparación

1. En tu PC, desde la rama de trabajo `paso-11c-pedidos-retrofit-android`, inicia Spring Boot:
   ```powershell
   .\gradlew.bat :server:bootRun
   ```
   No cierres esa terminal.
2. En Chrome: http://localhost:8080/api/products
   - Localiza el stock de `id: 1` (almendras); **anótalo antes de comprar**.
   - Tras pruebas anteriores debería ser 18 si mantuviste la misma base H2, pero usa el valor actual.
3. Ejecuta Android en el emulador con Run ▶, entra con usuario administrador.
4. Ve a **Cuenta → Probar conexión REST (Paso 11)**.
5. Debes ver `Conexión establecida` y `8 productos recibidos`.

## Crear pedido de prueba desde Android

1. Dentro de `Pedido de prueba desde Android` pulsa `Crear pedido remoto de prueba`.
2. Se enviará 1 bolsa del producto 1. Debería aparecer ID del pedido, estado `PENDING` y total 3490 CLP.
3. El stock servidor de almendras debería disminuir exactamente **una** bolsa (18 → 17 si era 18).
4. Pulsa `Reenviar el MISMO pedido (prueba sin duplicar)`.
5. Debe devolver el mismo ID, sin un descuento adicional.

La app guarda el identificador UUID en preferencias privadas **antes de enviar el POST**. Si falla la conexión o se cierra la aplicación, vuelve a abrir la pantalla y presiona `Reintentar solicitud pendiente`. No borres los datos del emulador.

## Precauciones

- Cada clic en `Preparar otro pedido de prueba` habilita un ID nuevo; **solo úsalo si deseas una nueva compra ficticia y otro descuento**.
- El stock del catálogo SQLite no cambiará todavía. Es intencional: en la fase siguiente conectaremos la compra normal con el servidor y sincronizaremos la caché local.
- Sin servidor corriendo, aparecerá un error de red; el botón de reintento reutiliza el ID guardado.
- El emulador usa `10.0.2.2:8080` para acceder al servidor del PC; no uses esa dirección para un celular físico o producción.
- Aún no hay login/autorización real del lado servidor. La pantalla de pruebas se oculta a usuarios no administrativos dentro de la app, pero **eso no constituye seguridad del endpoint**. No expongas el servidor a Internet.

## Git / QA

La rama y PR de esta fase están en borrador, **no fusionar hasta probar en Android Studio**:
- [ ] Compilación Android `BUILD SUCCESSFUL`.
- [ ] Pantalla REST lista 8 productos.
- [ ] Creación remota de un pedido desde emulador.
- [ ] Stock H2 disminuye una sola bolsa.
- [ ] Reintentar devuelve mismo pedido sin descontar.
- [ ] Funciones locales anteriores siguen funcionando.
