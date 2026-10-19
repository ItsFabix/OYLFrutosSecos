# Paso 11D — Integración del carrito Android con pedidos Spring Boot

Esta rama está en **pruebas**. El checkout normal del cliente ahora envía su carrito a Spring Boot mediante Retrofit, y el servidor calcula el total y descuenta stock transaccionalmente en H2.

## Antes de ejecutar

1. **Detén** Spring Boot anterior con Ctrl+C.
2. Verifica que no haya cambios locales sin guardar: `git status --short`.
3. Obtén la rama `paso-11d-carrito-pedidos-remotos` desde GitHub.
4. En la terminal del proyecto ejecuta:
   ```powershell
   .\gradlew.bat :server:test
   .\gradlew.bat :app:assembleDebug
   ```
5. Si ambos finalizan `BUILD SUCCESSFUL`, inicia servidor:
   ```powershell
   .\gradlew.bat :server:bootRun
   ```
6. Mantén la terminal abierta y ejecuta la app con Run ▶.

## Prueba E2E en el emulador

1. Inicia sesión como **CLIENTE**, no como administrador.
2. Consulta el stock de almendras en `http://localhost:8080/api/products`; anótalo antes de comprar (debería ser 17 si siguió igual).
3. En la app abre Catálogo, agrega **1 bolsa de almendras** al carrito.
4. En Carrito elige `Continuar al pedido`; selecciona Retiro/Entrega.
5. Presiona `Confirmar pedido en servidor` **solo una vez**.
6. Debe aparecer `Pedido registrado` con un ID de servidor (UUID).
7. En `Mis pedidos`, debe existir un registro con `Registrado en Spring Boot`.
8. Consulta nuevamente la API: el stock debe haber disminuido **una** bolsa.
9. Comprueba que el carrito esté vacío, y que fotografías y pedidos locales anteriores sigan accesibles.

## Reintentos seguros

- El UUID y JSON del pedido se guardan en una nueva tabla SQLite `pending_remote_checkout` **antes** del POST.
- Si hay un timeout, la app no puede saber si el servidor aceptó la compra. NO creará una solicitud nueva en el reintento; recupera el mismo UUID.
- Mientras haya un intento pendiente, el carrito no permite editar cantidades.
- Si el servidor rechaza explícitamente por validación o stock (400/404/409), se elimina el intento pendiente para que el usuario pueda corregir el carrito.
- Cuando el pedido remoto es confirmado, se guarda en el historial SQLite con `server_order_id` único y se limpia el carrito en la misma transacción.
- El catálogo sincroniza precios/stock desde H2 cuando puede; SQLite se usa como caché sin conexión. Se conserva `image_key`, para no perder fotos.

## Migración y limitaciones

- SQLite pasa de la versión 6 a la **7** con migración aditiva; no reinstales la app.
- El servidor H2 agrega columnas de entrega y contacto sin destruir datos previos.
- Los pedidos antiguos siguen marcados como **locales**; los nuevos como **Spring Boot**.
- El administrador aún no gestiona pedidos remotos desde la app: su edición/inventario actual permanece en modo local de demostración. Los cambios de stock oficiales son los realizados por el backend.
- Este es un backend **académico con datos ficticios**, sin login de cliente validado por servidor, sin pagos y expuesto solo en localhost; no usar con datos reales ni publicarlo en Internet.
- Los estados de pedidos remotos mostrados en SQLite pueden quedar desactualizados hasta implementar sincronización/autorización en una fase posterior.

## Check de aceptación

- [ ] `:server:test` pasa.
- [ ] `:app:assembleDebug` pasa.
- [ ] En emulador cliente confirma un pedido y obtiene UUID remoto.
- [ ] Stock H2 baja una sola vez.
- [ ] Carrito se vacía e historial muestra ID remoto.
- [ ] Fotos e historial previo se conservan.
- [ ] Reconexión sin duplicar y gestión admin restantes se verifican en fases siguientes.
