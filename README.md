> **Estado de cierre 25/11:** los cuatro eventos del pedido conocido volvieron a entregarse desde los registros originales del outbox, conservando IDs y fechas, después de respaldar ambas H2. Se verificaron también 20 comprobaciones aisladas de compra, estados, caída/recuperación y persistencia. Se detuvo la búsqueda de datos Android antiguos por decisión del equipo; la demostración usa un AVD independiente. La APK definitiva debe acreditarse con la firma original y una prueba de instalación, sin confundirla con debug.

> **Control 3:** tres aplicaciones Spring Boot independientes: ventas (8080), retiro (8081) y notificaciones (8082). Consultar [MICROSERVICIOS_CONTROL3.md](MICROSERVICIOS_CONTROL3.md) y [el informe de revisión final](INFORME_VERSION_FINAL_CONTROL3.md). La release del 25/11 está firmada con el certificado original y verificada en un AVD independiente. La APK anterior se conserva intacta.

# OYL Frutos Secos

## Comandos de presentación

Ejecutar desde la raíz. El inicio solicita las claves en entradas ocultas; si los puertos ya están ocupados, usar directamente la comprobación, sin detener los servicios existentes.

```powershell
.\scripts\iniciar-microservicios.ps1 -VisibleConsoles
.\scripts\comprobar-microservicios.ps1 -OrderId 'ec121ba3-466c-4ebc-9d26-bfeece0143bc'
Invoke-RestMethod 'http://127.0.0.1:8080/api/orders/ec121ba3-466c-4ebc-9d26-bfeece0143bc/notifications'
.\gradlew.bat :server:test :pickup-service:test :notification-service:test :app:testDebugUnitTest :app:jacocoDebugLogicReport --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\comprobar-microservicios.Tests.ps1
# Opcional: procesos, puertos y H2 exclusivos de prueba; no utiliza los servicios de la demostración.
.\gradlew.bat :server:bootJar :pickup-service:bootJar :notification-service:bootJar --console=plain
python .\scripts\tests\comprobar-integracion-aislada.py
```

Reportes HTML: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/jacoco/jacocoDebugLogicReport/html/index.html`, `app/build/reports/androidTests/connected/debug/index.html` y `<modulo>/build/reports/tests/test/index.html`. El 25/11 se reejecutaron las 249 pruebas JVM (221 Android y 20/4/4 de servicios), todas aprobadas. Los 80 resultados instrumentados conservados corresponden a la ejecución anterior; no ejecutarlos sobre un AVD con datos sin respaldo. Las 20 comprobaciones aisladas son adicionales, no se suman como pruebas JUnit.

Firma alternativa al asistente Android Studio: después de `:app:assembleRelease`, ejecutar `powershell -ExecutionPolicy Bypass -File .\scripts\firmar-release.ps1`. Requiere la contraseña original y verifica la huella antes de acreditar la APK. No crear otro keystore. La base predeterminada `http://10.0.2.2:8080/` corresponde al emulador en este PC; los servicios 8081/8082 se consultan a través de ventas. Un teléfono físico requiere una configuración de red distinta.

Proyecto académico de **DSY1105 — Desarrollo de Aplicaciones Móviles**, Evaluación Parcial 4 (6 %), entrega Control 3. Repositorio: [ItsFabix/OYLFrutosSecos](https://github.com/ItsFabix/OYLFrutosSecos). Rama de trabajo: `control3-tres-microservicios` (la original `control3-integracion` se mantiene intacta).

OYL permite consultar frutos secos, semillas y deshidratados, administrar un carrito, registrar pedidos en Spring Boot y seguir sus estados. Conserva el diseño verde y crema, las ilustraciones propias y la autenticación local. El administrador gestiona productos, existencias y pedidos con autorización HTTP.

## Integrantes y entrega

- Integrante(s), sección y docente: **pendiente de completar con datos reales antes de entregar**. No inventar integrantes ni participación.
- Nueva versión: generar APK firmado desde esta rama. El APK ya versionado pertenece a la versión anterior y no prueba los cambios de microservicios.

## Funcionalidades

- Catálogo de dos columnas, detalle y carrito con validaciones.
- Pedidos REST con descuento transaccional de stock y recuperación de la misma solicitud ante una respuesta perdida. Los pedidos locales antiguos se mantienen separados.
- Historial por usuario y estados Pendiente → Confirmado → Preparado → Entregado.
- Creación y edición de productos, cámara y fotografías privadas; entradas y salidas de inventario.
- Perfil editable en SQLite, notificaciones locales y lector QR con resultados visibles y cancelación silenciosa.
- Puntos de retiro con vista inicial Ver todos, selección Centro/Ñuñoa y clima Open-Meteo independiente.
- Cuenta → Configuración administrativa, exclusivamente ADMIN. La clave se introduce enmascarada, se conserva en memoria y se elimina al cerrar sesión o terminar el proceso. Las operaciones protegidas ofrecen configurar el acceso cuando falta autorización.
- Textos orientados al cliente y estados en español. Las notificaciones antiguas se normalizan al mostrarlas sin reescribir el historial; el seguimiento remoto permanece separado de los avisos SQLite. Las herramientas de comprobación de avisos se reservan a ADMIN dentro de Preferencias de notificaciones.

## Tecnologías y arquitectura

Android: Kotlin 2.0.21, Jetpack Compose, Material 3, Navigation Compose, ViewModel, StateFlow, coroutines, SQLiteOpenHelper y Retrofit 2.11.0 con Gson. Mapa osmdroid 6.1.20/OpenStreetMap y Google Code Scanner 16.1.0. compileSdk/targetSdk 36; minSdk 26; Android Gradle Plugin 8.12.3 y Gradle Wrapper 8.13.

Backend: Java 17, Spring Boot 3.5.7, Spring Web, Spring JDBC, transacciones y H2. Hay **tres microservicios independientes**: ventas/inventario (módulo `:server`, puerto 8080), puntos de retiro (`:pickup-service`, 8081) y eventos de pedidos (`:notification-service`, 8082). Cada uno tiene JAR, puerto y base de datos propios. Las llamadas cruzadas son HTTP; el servidor 8080 actúa como fachada para Android. El checkout sigue transaccional en `:server` y los avisos se publican mediante una outbox persistente. Ver [MICROSERVICIOS_CONTROL3.md](MICROSERVICIOS_CONTROL3.md).

## Pantallas reales

Existen 20 destinos de navegación. Crear/editar producto y pedidos cliente/administrador reutilizan destinos con distintos modos; los diálogos no se cuentan como pantallas nuevas.

| N.º | Pantalla | Ruta | Acceso |
| --- | --- | --- | --- |
| 1 | Bienvenida | `welcome` | Todos |
| 2 | Iniciar sesión | `login` | Todos |
| 3 | Registro de cliente | `register` | Todos |
| 4 | Inicio y dashboard | `store_home` | Cliente y ADMIN |
| 5 | Catálogo | `catalog` | Cliente y ADMIN |
| 6 | Detalle del producto | `product/{id}` | Cliente y ADMIN |
| 7 | Carrito | `cart` | Cliente |
| 8 | Confirmación del pedido | `order_confirmation` | Cliente |
| 9 | Pedido registrado | `order_success` | Cliente |
| 10 | Historial y gestión de pedidos | `orders` | Cliente y ADMIN |
| 11 | Cuenta | `profile` | Cliente y ADMIN |
| 12 | Editar perfil | `profile_edit` | Cliente y ADMIN |
| 13 | Notificaciones | `notifications` | Cliente y ADMIN |
| 14 | Escáner QR | `qr_scanner` | Cliente y ADMIN |
| 15 | Puntos de retiro y clima | `pickup_map` | Cliente y ADMIN |
| 16 | Gestión de productos | `manage` | ADMIN |
| 17 | Crear o editar producto | `editor/{id}` | ADMIN |
| 18 | Gestión de inventario | `inventory` | ADMIN |
| 19 | Movimiento de inventario | `inventory_adjust/{id}` | ADMIN |
| 20 | Configuración administrativa | `admin_settings` | ADMIN |

## API propia

Base local para el PC: `http://127.0.0.1:8080/`. Para el emulador: `http://10.0.2.2:8080/`.

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| GET | `/api/health` | Estado del servicio. | Público |
| GET | `/api/products` | Catálogo y existencias actuales. | Público |
| POST | `/api/orders` | Registrar pedido; validar y descontar stock transaccionalmente con requestId idempotente. | Público |
| GET | `/api/orders/{id}` | Consultar pedido y estado por UUID. | Público por UUID |
| GET | `/api/pickup-points` | Consulta HTTP delegada a `:pickup-service`; ubicaciones de ejemplo. | Público |
| GET | `/api/orders/{id}/notifications` | Eventos del pedido desde `:notification-service`. | Público por UUID |
| POST/PUT/DELETE | `/api/admin/pickup-points/{id}` | CRUD del servicio de retiro delegado vía HTTP. | ADMIN |
| GET | `/api/admin/orders` | Listar todos los pedidos del servidor. | ADMIN |
| POST | `/api/admin/orders/{id}/advance` | Avanzar Pendiente, Confirmado, Preparado y Entregado. | ADMIN |
| POST | `/api/admin/products` | Crear producto y su movimiento de stock inicial. | ADMIN |
| PUT | `/api/admin/products/{id}` | Editar datos; el stock solo cambia mediante movimientos. | ADMIN |
| GET | `/api/admin/inventory/movements` | Listar movimientos de inventario. | ADMIN |
| POST | `/api/admin/products/{id}/stock` | Registrar entrada o salida; rechazar stock negativo. | ADMIN |

Las rutas ADMIN exigen la cabecera `X-OYL-ADMIN-KEY`, comparada en el servidor con `OYL_ADMIN_KEY` de al menos 16 caracteres. El servidor devuelve 403 cuando falta o es incorrecta. La aplicación valida el acceso mediante lecturas de pedidos y movimientos antes de conservar la clave. No se incorporan claves privadas al APK ni se guardan en SQLite o preferencias.

El POST de pedidos requiere `requestId` UUID, `customerAlias`, `items` con `productId`/`quantity`, `delivery` (`PICKUP` o `DELIVERY`) y datos de contacto. Un reintento debe conservar la solicitud exacta y el mismo UUID. Un UUID reutilizado con otro contenido se rechaza. No hay endpoint DELETE ni eliminación de productos desde la interfaz.

## API externa Open-Meteo

- Base: `https://api.open-meteo.com/`.
- Método y endpoint: `GET /v1/forecast`.
- Parámetros: `latitude`, `longitude`, `current=temperature_2m,weather_code`, `temperature_unit=celsius`, `timezone=auto`.
- Pantalla: Puntos de retiro, después de seleccionar un local. Cliente Retrofit independiente: `OpenMeteoClient`/`OpenMeteoService` → `PickupWeatherRepository` → `PickupWeatherViewModel` → `PickupMapScreen`.
- Coordenadas existentes: Centro/Plaza de Armas `-33.4372, -70.6506`; Ñuñoa/Plaza Ñuñoa `-33.4554, -70.5982`. Son referencias geográficas de demostración, no sucursales comerciales verificadas.
- Caché pública en memoria de 10 minutos, descripción WMO en español, carga/error/reintento sin bloquear el mapa. La vista general no selecciona un local por una respuesta meteorológica.
- Documentación: [Open-Meteo](https://open-meteo.com/en/docs).

## Persistencia y funcionamiento sin conexión

SQLite (`oyl_auth.db`, esquema 8) conserva usuarios, sesiones, perfil, carrito, pedidos históricos, copia REST, fotografías privadas y solicitudes exactas de checkout pendientes. Las migraciones existentes se conservan. El perfil solo actualiza nombre, teléfono y dirección de la sesión activa; no modifica identidad, rol ni contraseña.

Spring Boot es la autoridad de catálogo, stock y pedidos remotos. `schema.sql` crea tablas con IF NOT EXISTS y aplica ampliaciones aditivas. `data.sql` inserta ocho productos solo cuando falta su ID; no sobrescribe stock ni datos anteriores. `DemoCatalogSeeder` no es un bean activo, por lo que no hay doble semilla. H2 usa archivo persistente relativo al directorio de ejecución; se debe mantener el mismo lugar de arranque para reutilizarlo.

Sin servidor siguen disponibles usuarios/sesión/perfil, datos locales y la última copia guardada, marcada como desactualizada cuando corresponde. Crear pedidos y operaciones administrativas requieren REST. Ante red incierta no se confirma falsamente la compra: se recupera el intento pendiente. Open-Meteo y mosaicos nuevos necesitan Internet; el QR de cámara puede requerir descargar el lector de Google Play.

## Requisitos de ejecución

Para el repositorio completo: Android Studio con soporte de AGP 8.12.3, Android SDK Platform 36, herramientas de compilación/SDK y JDK 21 recomendado. Gradle usa el wrapper incluido y requiere Internet para descargar dependencias la primera vez. `local.properties` se genera localmente por Android Studio y no se distribuye. Los ZIP de entregas anteriores deben actualizarse para incluir los tres módulos y los archivos raíz necesarios; todavía no acreditan esta versión.

### Iniciar Spring Boot

Desde la raíz del repositorio completo, en Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-microservicios.ps1 -VisibleConsoles
```

El script pide un token interno común (`OYL_SERVICE_TOKEN`) y una clave administrativa (`OYL_ADMIN_KEY`), ambos de al menos 16 caracteres, sin mostrarlos. Los procesos heredan esas variables; el script restaura el entorno de la consola de origen al terminar. No copiar valores a archivos, argumentos ni al historial. `-VisibleConsoles` permite ver el arranque y terminar cada proceso con Ctrl+C cuando corresponda; sin ese parámetro se abren ocultos. El script rechaza puertos ocupados sin detener servicios existentes. Si ya están activos, usar directamente la comprobación:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\comprobar-microservicios.ps1
# Opcional: UUID remoto completo, no el numero de pedido #5 mostrado en Android.
powershell -ExecutionPolicy Bypass -File .\scripts\comprobar-microservicios.ps1 -OrderId a9209d94-cc09-43fb-ad38-0fa9788eca72
```

Comprueba salud, al menos ocho productos y dos puntos, igualdad del contenido directo/delegado, UTF-8 y, cuando se indica un UUID existente, sus eventos. El UUID del ejemplo pertenece a la prueba manual de este entorno; en otra base usar el propio. Distingue conexión rechazada, timeout, HTTP 404 y JSON inválido. No borra ni escribe datos.

Alternativa: en tres terminales independientes configurar el mismo token interno y ejecutar `:pickup-service:bootRun`, `:notification-service:bootRun` y `:server:bootRun`; ventas también necesita la clave administrativa. Ver instrucciones completas en [microservicios](MICROSERVICIOS_CONTROL3.md). No borrar H2 para repetir la carga inicial; las pruebas usan bases aisladas.

### Ejecutar Android

1. Extraer el ZIP Android completo y abrir su carpeta raíz (con `settings.gradle.kts`) en Android Studio, o abrir el repositorio completo. Sincronizar Gradle, instalar Platform 36 y configurar el SDK localmente.
2. Iniciar un AVD con Google Play y API 26 o superior; se recomienda un AVD actual con acceso a Internet. Iniciar primero los tres servicios en el PC.
3. Para ejecutar desde código, seleccionar módulo `app` y Run en Android Studio. La URL predeterminada es `http://10.0.2.2:8080/`. La APK release de entrega se distribuye por separado y no se reconstruye al preparar los ZIP.
4. Registrar un cliente desde la aplicación y probar catálogo/carrito. La cuenta ADMIN de demostración ya existe en la base local; sus datos de demostración están definidos en `OylDatabase.onCreate` y las instrucciones privadas de entrega.
5. Como ADMIN, entrar en Cuenta → Configuración administrativa e introducir la misma clave temporal del servidor. Conectar y volver a Inventario/Pedidos. No se pide otra vez durante la sesión autorizada; un HTTP 401/403 la invalida.

`10.0.2.2` representa el host solamente dentro del emulador Android. En un teléfono físico no apunta al PC: se necesita un servidor accesible con HTTPS, configurar su URL mediante `-PoylApiBaseUrl=https://servidor.example/` y generar una variante adecuada. El servidor actual escucha en `127.0.0.1` y Android permite HTTP únicamente a `10.0.2.2`; cambiar solo la IP no basta. Esta entrega no modifica esa configuración de desarrollo.

## Demostración cliente y administrador

1. Cliente: registrar/iniciar sesión, elegir un producto con stock, añadir cantidad al carrito y confirmar el pedido.
2. Comprobar número de pedido y estado Pendiente; consultar catálogo para ver el stock descontado por el servidor.
3. Cerrar sesión, entrar como ADMIN y configurar el acceso desde Cuenta. Consultar todos los pedidos y los movimientos de inventario.
4. Avanzar el pedido a Confirmado, Preparado y Entregado; registrar una reposición si se desea y verificar existencias.
5. Volver al cliente original y actualizar Mis pedidos: debe ver el estado nuevo, sin duplicados ni pedidos de otros usuarios locales.
6. Para demostrar errores, usar datos de prueba y una base aislada; no repetir escrituras inciertas con un UUID nuevo. El checkout pendiente conserva la misma solicitud aunque se edite el perfil.

## Pruebas y cobertura

La medición de JaCoCo y los conteos exactos de la revisión están en [INFORME_COBERTURA_CONTROL3.md](INFORME_COBERTURA_CONTROL3.md) y [el informe final](INFORME_VERSION_FINAL_CONTROL3.md). Se evalúan `domain`, `data/repository` y `ui/viewmodel`; no toda la aplicación ni el backend. Se conservan las 114 clases originales y se agrega la lógica de presentación. El objetivo indicado es al menos 80 % de instrucciones y líneas. Las pruebas aprobadas no equivalen a un porcentaje de cobertura.

```powershell
.\gradlew.bat :server:test :pickup-service:test :notification-service:test :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:jacocoDebugLogicReport --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.oylReadOnlyProbeUrl=http://10.0.2.2:8080/' --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\comprobar-microservicios.Tests.ps1
git diff --check
```

Abrir `app/build/reports/jacoco/jacocoDebugLogicReport/html/index.html`, los reportes `build/reports/tests/` de cada módulo y `app/build/reports/androidTests/connected/debug/index.html`. Las pruebas SQLite usan archivos temporales; las de microservicios usan H2 independiente y destinatarios HTTP de prueba. La sonda opcional del emulador solo hace GET a los servicios activos. Confirmar identidad de firma antes de actualizar una app instalada; no desinstalar ni borrar datos para hacer pasar una prueba.

La revisión detectó que AGP desinstala las APK al finalizar instrumentación de forma predeterminada. Se agregó `android.injected.androidTest.leaveApksInstalledAfterRun=true` a `gradle.properties` y al helper para impedir esa limpieza en futuras ejecuciones. Usar un AVD dedicado y respaldar sus datos antes de instrumentar; la coincidencia de firmas no evita por sí sola la limpieza del runner. El incidente ocurrido y sus límites de recuperación se documentan en el informe final.

## APK release y firma

El repositorio conserva `app/release/app-release.apk` de la versión anterior, sin modificarla. La nueva release del 25/11 se generó con Android Studio y el keystore original. Se verificó con apksigner, se compararon sus DEX/recursos/manifest con la compilación actual y se instaló en `OYL_Release_Control3`, sin reemplazar ninguna instalación previa. Allí se comprobó compra, descuento de stock, los cuatro estados desde administración, seguimiento remoto y persistencia de sesión/historial tras reiniciar la app.

El artefacto privado está en `%USERPROFILE%\Documents\OYL-Control3\OYL-Control3-release-20261125.apk`. SHA-256 del archivo: `3cb01e22cd78064cd7d5c12d53c8e859c59652a871c94b0015ca7a3b6083dbe3`. Certificado SHA-256, idéntico al anterior: `9d1c01df179ec7901200ce8d8f373cb1dcfb2146404a20dffe9711b313131547`. La copia instalada también coincide byte a byte. `app/build.gradle.kts` no contiene signingConfig con credenciales; se utiliza una llave externa. Para volver a firmar se requieren el keystore, alias y contraseñas originales mediante entradas privadas, sin incluirlos en Git.

Para instalar la release, usar un AVD de pruebas sin datos importantes. Si existe una instalación debug con otra firma, Android puede rechazar la actualización. No desinstalar ni borrar sus datos para resolverlo: usar otro AVD o un dispositivo de prueba apropiado. No reemplazar el APK ya firmado por una compilación debug.

## GitHub Trello y evidencias

GitHub conserva el código y ramas del proyecto. Trello se utiliza para acreditar planificación y distribución de tareas; el enlace del tablero, los responsables y su captura real deben completarse en la documentación privada de entrega. No se inventa un tablero ni se presume una distribución no acreditada.

El Word privado de entrega debe completarse con integrantes, RUT y capturas reales de firma/Trello y actualizarse a los resultados de esta rama. No debe publicarse con datos personales en GitHub. Al preparar los ZIP de código, excluir builds, cachés, `local.properties`, keystores y bases locales de ejecución, e incluir wrappers y archivos raíz de Gradle necesarios para reconstruir los módulos. Los ZIP antiguos siguen pendientes de actualización; no se regeneraron en esta revisión. La APK actual se distribuye por separado.

## Límites conocidos

- Tres procesos Spring Boot independientes, con tres bases H2 y comunicación HTTP. La correspondencia con los criterios académicos exactos debe contrastarse con la pauta oficial, no disponible en este repositorio.
- Autenticación local y clave administrativa compartida de desarrollo; no hay identidad remota segura ni sincronización de usuarios entre dispositivos.
- El GET de pedido se consulta por UUID, sin autenticación remota del cliente; no es un despliegue de producción.
- Creación/lectura/actualización de productos y movimientos disponibles; no existe DELETE.
- La cobertura acreditada se limita a los paquetes indicados. La firma válida no prueba por sí sola correspondencia de una APK anterior con el código actual.
- Mapas con referencias y horarios de demostración. La lectura óptica real de QR depende del dispositivo/Google Play; sus eventos y resultados se probaron mediante simulación.
