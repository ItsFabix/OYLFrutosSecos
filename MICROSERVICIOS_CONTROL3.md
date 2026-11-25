# Control 3 — tres microservicios Spring Boot

## Arquitectura real

| Módulo Gradle | Puerto | Base de datos H2 independiente | Responsabilidad |
| --- | --- | --- | --- |
| :server | 8080 | server/oyl-data.mv.db | Productos, inventario y pedidos transaccionales; fachada para Android |
| :pickup-service | 8081 | pickup-service/oyl-pickup-data.mv.db | CRUD de puntos de retiro ficticios |
| :notification-service | 8082 | notification-service/oyl-notifications-data.mv.db | CRUD de eventos persistentes e idempotentes |

Los tres son aplicaciones Spring Boot distintas, cada una con clase main, JAR, proceso, puerto y base H2 exclusivos. No se cuentan controladores como microservicios.

Se preserva la transacción de stock/pedido en :server para evitar sobreventa. Cuando se crea un pedido o cambia de estado, la misma transacción escribe un evento en integration_outbox. Un publicador periódico lo entrega al servicio de notificaciones en el puerto 8082. Si ese servicio está caído, la compra se confirma igualmente y el evento sigue pendiente para su reintento. El destino usa eventId único para evitar duplicaciones.

Android consulta puntos de retiro mediante Retrofit en la ruta /api/pickup-points de :8080, que se comunica por HTTP con el servicio :8081. Si no está disponible, la app conserva sus puntos ficticios de respaldo. Las compras Android generan eventos en el microservicio :8082 mediante :server, y esos eventos pueden consultarse por el UUID del pedido.

Limitación visible: la pantalla de Notificaciones Android aún muestra sus avisos locales de SQLite; el historial del servicio remoto puede consultarse por API, pero no aparece mezclado en esa pantalla. No afirmar que existe sincronización visual de avisos si no se desarrolla.

## Preparación y arranque en Windows

Abrir **la raíz del repositorio completo** de la rama control3-tres-microservicios. El ZIP antiguo que contenía solo server no tiene los módulos nuevos.

1. Usar Java 21. El JBR incluido con Android Studio funciona.
2. Desde la raíz ejecutar en PowerShell:

    powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-microservicios.ps1

3. Ingresar un token interno de 16 caracteres o más y la clave del administrador (16 caracteres o más) cuando el script lo pida. Las variables se heredan por las tres ventanas nuevas sin mostrarlas en argumentos.
4. Comprobar que cada ventana termina con Started y que los tres puertos responden.
5. Iniciar el emulador Android y la app. La base Retrofit sigue siendo http://10.0.2.2:8080/.

También se pueden arrancar en tres PowerShell independientes con .\gradlew.bat :pickup-service:bootRun, .\gradlew.bat :notification-service:bootRun y .\gradlew.bat :server:bootRun. Las tres necesitan el mismo OYL_SERVICE_TOKEN para notificaciones y operaciones internas; :server necesita OYL_ADMIN_KEY para la administración.

**El APK antiguo no incorpora la consulta remota del mapa.** Ejecutar desde Android Studio el código de esta rama, o volver a generar un APK release **firmado** para presentar esa funcionalidad. No reutilizar una firma anterior declarando que se recompiló.

## Pruebas de humo manuales

- http://127.0.0.1:8080/api/health — servidor ventas
- http://127.0.0.1:8081/api/pickup-points — dos ubicaciones ficticias
- http://127.0.0.1:8082/api/notifications/health — servicio de eventos
- http://127.0.0.1:8080/api/pickup-points — la misma respuesta delegada por HTTP desde ventas
- http://127.0.0.1:8080/api/products — catálogo
- Android > Mapa — debe mostrar los puntos ofrecidos por la API.
- Android > Carrito > Confirmar pedido — se descuenta el stock en ventas.
- http://127.0.0.1:8080/api/orders/UUID_REAL_DEL_PEDIDO/notifications — debe listar el evento remoto después de publicarse.
- Administrador > avanzar pedido — se genera otro evento de cambio de estado.
- Parar solo el servicio de notificaciones y crear un pedido: la venta debe funcionar y el evento debe aparecer cuando reinicies el servicio de notificaciones.
- Editar un punto por API y reiniciar :pickup-service para comprobar la persistencia H2 sin que data.sql sobrescriba cambios.

## Endpoints

Ventas :8080: GET /api/health, GET /api/products, POST /api/orders, GET /api/orders/{id}, GET /api/orders/{id}/notifications, GET /api/pickup-points. Con cabecera X-OYL-ADMIN-KEY: POST /api/admin/products, PUT /api/admin/products/{id}, POST /api/admin/products/{id}/stock, GET /api/admin/orders, POST /api/admin/orders/{id}/advance, GET /api/admin/inventory/movements, POST/PUT/DELETE /api/admin/pickup-points/{id}.

Puntos :8081: GET /api/pickup-points, GET /api/pickup-points/{id}; POST/PUT/DELETE /api/pickup-points/{id} con X-OYL-SERVICE-TOKEN.

Eventos :8082: GET /api/notifications/health; POST /api/notifications/events, GET /api/notifications/events?orderId=UUID, PUT /api/notifications/events/{eventId}/read y DELETE /api/notifications/events/{eventId} con X-OYL-SERVICE-TOKEN.

En producción se necesitarían autenticación por usuarios, HTTPS, autenticación servicio a servicio más fuerte, gestión de secretos y autorización por propietario de pedido. Esta configuración usa loopback (127.0.0.1) y datos ficticios para demostración académica.

## Pruebas automatizadas y cobertura

Ejecutar en la raíz: .\gradlew.bat :server:test :pickup-service:test :notification-service:test; después .\gradlew.bat :app:testDebugUnitTest. Pruebas instrumentadas con emulador activo: .\gradlew.bat :app:connectedDebugAndroidTest. Los reportes HTML se generan en las carpetas build/reports/tests respectivas.

La pauta del Control 3 exige **80 % de cobertura de código lógico Android**. Los conteos de tests pasados no demuestran ese porcentaje; hay que generar y documentar una medición de cobertura, o declararlo explícitamente como pendiente.

## Checklist de entrega

- [ ] Confirmar arranque de los tres procesos y consulta a las tres bases independientes.
- [ ] Ejecutar pruebas JUnit de los tres servicios, y capturar reportes.
- [ ] Probar la consulta del mapa, CRUD del catálogo/inventario, compra y estados.
- [ ] Demostrar evento de pedido y reintento si 8082 no está disponible.
- [ ] Ejecutar pruebas Android y medir/documentar cobertura real (80 %).
- [ ] Generar de nuevo APK release firmado con un .jks propio y verificar instalación offline de Metro.
- [ ] README, integrantes y endpoints; evidencia de firma, GitHub y Trello.
- [ ] Confirmar con el profesor la autorización de cambio de entrega: la pauta advierte sanciones por commits posteriores al cierre formal.

La clave .jks debe manejarse con cuidado: la pauta solicita entregarla, pero no corresponde publicarla con contraseñas en texto plano en un repositorio público.
