# Control 3 — tres microservicios Spring Boot

> **Cierre 25/11:** la tabla de notificaciones estaba vacía; ventas conservaba los cuatro eventos originales ya reconocidos en su outbox. Con respaldos H2 previos, se reenviaron exactamente esos registros y se acreditó su persistencia en una copia H2. La ruta y los tokens coincidían; no hay auditoría suficiente para atribuir el vaciado a una acción concreta. El publicador reintenta pendientes, no eventos ya reconocidos. También aprobaron 20 comprobaciones aisladas de compra, estados, caída/recuperación y reinicio de las tres H2; ejecutables con `scripts/tests/comprobar-integracion-aislada.py`. Los servicios de demostración no fueron reiniciados.

## Arquitectura real

| Módulo Gradle | Puerto | Base de datos H2 independiente | Responsabilidad |
| --- | --- | --- | --- |
| :server | 8080 | server/oyl-data.mv.db | Productos, inventario y pedidos transaccionales; fachada para Android |
| :pickup-service | 8081 | pickup-service/oyl-pickup-data.mv.db | CRUD de puntos de retiro ficticios |
| :notification-service | 8082 | notification-service/oyl-notifications-data.mv.db | CRUD de eventos persistentes e idempotentes |

Los tres son aplicaciones Spring Boot distintas, cada una con clase main, JAR, proceso, puerto y base H2 exclusivos. No se cuentan controladores como microservicios.

Se preserva la transacción de stock/pedido en :server para evitar sobreventa. Cuando se crea un pedido o cambia de estado, la misma transacción escribe un evento en integration_outbox. Un publicador periódico lo entrega al servicio de notificaciones en el puerto 8082. Si ese servicio está caído, la compra se confirma igualmente y el evento sigue pendiente para su reintento. El destino usa eventId único para evitar duplicaciones.

Android consulta puntos de retiro mediante Retrofit en la ruta /api/pickup-points de :8080, que se comunica por HTTP con el servicio :8081. Si no está disponible, la app conserva sus puntos ficticios de respaldo. Las compras Android generan eventos en el microservicio :8082 mediante :server, y esos eventos se muestran en el detalle de un pedido REST mediante Retrofit (ruta /api/orders/{id}/notifications).

La pantalla de Notificaciones Android conserva sus avisos locales en SQLite, mientras que la pantalla Mis pedidos muestra también el historial de eventos remotos al expandir un pedido REST. Son dos vistas diferentes; no afirmar que el historial remoto se sincroniza con todas las notificaciones locales.

## Preparación y arranque en Windows

Abrir **la raíz del repositorio completo** de la rama control3-tres-microservicios. El ZIP antiguo que contenía solo server no tiene los módulos nuevos.

1. Usar Java 21. El JBR incluido con Android Studio funciona.
2. Desde la raíz ejecutar en PowerShell:

    powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-microservicios.ps1 -VisibleConsoles

3. Ingresar un token interno de 16 caracteres o más y la clave del administrador (16 caracteres o más) cuando el script lo pida. Las variables se heredan por las tres ventanas nuevas sin mostrarlas en argumentos y se retiran de la consola de origen al finalizar. Sin `-VisibleConsoles`, las consolas se abren ocultas; para la demostración se recomienda el parámetro explícito.
4. Comprobar que cada ventana termina con Started y que los tres puertos responden.
5. Iniciar el emulador Android y la app. La base Retrofit sigue siendo http://10.0.2.2:8080/.

El script rechaza puertos 8080/8081/8082 ocupados; no detiene los procesos existentes ni vuelve a pedir claves para iniciarlos encima. Si ya están activos, ejecutar solo `scripts/comprobar-microservicios.ps1`.

También se pueden arrancar en tres PowerShell independientes con .\gradlew.bat :pickup-service:bootRun, .\gradlew.bat :notification-service:bootRun y .\gradlew.bat :server:bootRun. Las tres necesitan el mismo OYL_SERVICE_TOKEN para notificaciones y operaciones internas; :server necesita OYL_ADMIN_KEY para la administración.

**El APK antiguo no incorpora la consulta remota del mapa.** Usar la nueva release del 25/11, firmada con el certificado original, cuyo código se comparó con la compilación de esta rama y se probó en `OYL_Release_Control3`. El archivo anterior se conserva por separado; no sustituye esta versión.

## Pruebas de humo manuales

- http://127.0.0.1:8080/api/health — servidor ventas
- http://127.0.0.1:8081/api/pickup-points — dos ubicaciones ficticias
- http://127.0.0.1:8082/api/notifications/health — servicio de eventos
- http://127.0.0.1:8080/api/pickup-points — la misma respuesta delegada por HTTP desde ventas
- http://127.0.0.1:8080/api/products — catálogo
- Verificar los cuatro endpoints automáticamente con scripts/comprobar-microservicios.ps1
- En Windows PowerShell, `powershell -ExecutionPolicy Bypass -File .\scripts\comprobar-microservicios.ps1 -OrderId UUID_REMOTO` consulta además los eventos. Sustituir `UUID_REMOTO` por el UUID completo que devuelve la API, no por el número `#5` de Android. Se decodifica UTF-8 y se compara el contenido de los puntos, con orden de propiedades estable.
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

Ejecutar en la raíz: .\gradlew.bat :server:test :pickup-service:test :notification-service:test; después .\gradlew.bat :app:testDebugUnitTest. Pruebas instrumentadas con emulador activo: .\gradlew.bat :app:connectedDebugAndroidTest. Los reportes HTML se generan en las carpetas build/reports/tests respectivas. Para la cobertura de lógica Android ejecutar .\\gradlew.bat :app:jacocoDebugLogicReport y abrir app/build/reports/jacoco/jacocoDebugLogicReport/html/index.html.

El objetivo comunicado para esta revisión es **80 % de instrucciones y líneas de lógica Android**. No se encontró la pauta oficial en el repositorio: confirmar su métrica y condiciones originales. Los resultados medidos y sus límites están en [INFORME_COBERTURA_CONTROL3.md](INFORME_COBERTURA_CONTROL3.md) y [INFORME_VERSION_FINAL_CONTROL3.md](INFORME_VERSION_FINAL_CONTROL3.md). Los conteos de tests aprobados no se usan como porcentaje de cobertura.

## Checklist de entrega

- [x] Confirmar respuesta de los tres procesos activos; bases independientes verificadas con pruebas de persistencia aisladas.
- [x] Ejecutar pruebas JUnit de los tres servicios y generar reportes.
- [ ] Probar la consulta del mapa, CRUD del catálogo/inventario, compra y estados.
- [x] Verificar eventos y reintento ante indisponibilidad mediante pruebas HTTP aisladas, sin detener 8082 del usuario.
- [x] Ejecutar pruebas JVM Android y medir/documentar cobertura real superior al 80 % de instrucciones y líneas del alcance indicado.
- [x] Release del 25/11 generada con el keystore original, firma/código verificados e instalada en un AVD vacío independiente; compra y administración comprobadas.
- [ ] README, integrantes y endpoints; evidencia de firma, GitHub y Trello.
- [ ] Contrastar con la pauta oficial los plazos, cambios permitidos y evidencias exigidas antes de publicar la entrega.

El keystore y sus contraseñas no se publican en GitHub. Si la pauta original exige entregarlos, acordar un canal privado apropiado y conservar la identidad de firma existente.
