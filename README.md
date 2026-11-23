# OYL Frutos Secos

Proyecto académico de **DSY1105 — Desarrollo de Aplicaciones Móviles**, Evaluación Parcial 4 (6 %), entrega Control 3. Repositorio: [ItsFabix/OYLFrutosSecos](https://github.com/ItsFabix/OYLFrutosSecos). Rama de trabajo: `control3-integracion`.

OYL permite consultar frutos secos, semillas y deshidratados, administrar un carrito, registrar pedidos en Spring Boot y seguir sus estados. Conserva el diseño verde y crema, las ilustraciones propias y la autenticación local. El administrador gestiona productos, existencias y pedidos con autorización HTTP.

## Funcionalidades

- Catálogo de dos columnas, detalle y carrito con validaciones.
- Pedidos REST con descuento transaccional de stock y recuperación de la misma solicitud ante una respuesta perdida. Los pedidos locales antiguos se mantienen separados.
- Historial por usuario y estados Pendiente → Confirmado → Preparado → Entregado.
- Creación y edición de productos, cámara y fotografías privadas; entradas y salidas de inventario.
- Perfil editable en SQLite, notificaciones locales y lector QR con resultados visibles y cancelación silenciosa.
- Puntos de retiro con vista inicial Ver todos, selección Centro/Ñuñoa y clima Open-Meteo independiente.
- Cuenta → Configuración administrativa, exclusivamente ADMIN. La clave se introduce enmascarada, se conserva en memoria y se elimina al cerrar sesión o terminar el proceso. Las operaciones protegidas ofrecen configurar el acceso cuando falta autorización.

## Tecnologías y arquitectura

Android: Kotlin 2.0.21, Jetpack Compose, Material 3, Navigation Compose, ViewModel, StateFlow, coroutines, SQLiteOpenHelper y Retrofit 2.11.0 con Gson. Mapa osmdroid 6.1.20/OpenStreetMap y Google Code Scanner 16.1.0. compileSdk/targetSdk 36; minSdk 26; Android Gradle Plugin 8.12.3 y Gradle Wrapper 8.13.

Backend: Java 17, Spring Boot 3.5.7, Spring Web, Spring JDBC, transacciones y H2. Hay **un único backend Spring Boot**, con controladores y servicios internos. No son microservicios independientes. La exigencia de múltiples servicios desplegables debe aclararse con la pauta; esta entrega no acredita esa arquitectura.

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
| GET | `/api/admin/orders` | Listar todos los pedidos del servidor. | ADMIN |
| POST | `/api/admin/orders/{id}/advance` | Avanzar Pendiente, Confirmado, Preparado y Entregado. | ADMIN |
| POST | `/api/admin/products` | Crear producto y su movimiento de stock inicial. | ADMIN |
| PUT | `/api/admin/products/{id}` | Editar datos; el stock solo cambia mediante movimientos. | ADMIN |
| GET | `/api/admin/inventory/movements` | Listar movimientos de inventario. | ADMIN |
| POST | `/api/admin/products/{id}/stock` | Registrar entrada o salida; rechazar stock negativo. | ADMIN |

Las seis rutas ADMIN exigen la cabecera `X-OYL-ADMIN-KEY`, comparada en el servidor con `OYL_ADMIN_KEY` de al menos 16 caracteres. El servidor devuelve 403 cuando falta o es incorrecta. La aplicación valida pedidos y movimientos antes de conservar la clave. No se incorporan claves privadas al APK ni se guardan en SQLite o preferencias.

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

Para el repositorio completo: Android Studio con soporte de AGP 8.12.3, Android SDK Platform 36, herramientas de compilación/SDK y JDK 17 o 21. Gradle usa el wrapper incluido y requiere Internet para descargar dependencias la primera vez. El backend aislado del ZIP no requiere Android SDK. `local.properties` se genera localmente por Android Studio y no se distribuye.

### Iniciar Spring Boot

Desde la raíz del repositorio completo o desde la raíz del ZIP Spring Boot ya extraído, en PowerShell:

```powershell
$env:OYL_ADMIN_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Clave de administración del servidor' -AsSecureString)).Password
.\gradlew.bat :server:bootRun --console=plain
```

Elegir una clave propia de al menos 16 caracteres; no copiar su valor a archivos ni al historial de comandos. Para terminar, Ctrl+C y retirar la variable de esa terminal con `Remove-Item Env:OYL_ADMIN_KEY`. En una segunda terminal:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
Invoke-RestMethod http://127.0.0.1:8080/api/products
```

Debe responder salud `ok` y un catálogo con ocho productos en una base vacía. No borrar una base persistente para repetir esa comprobación; usar una base aislada de pruebas. Alternativa JAR: `:server:bootJar` y `java -jar server/build/libs/server-0.0.1-SNAPSHOT.jar` desde un directorio estable. Mantener la misma variable de entorno y no cambiar el directorio que determina la ubicación de H2.

### Ejecutar Android

1. Extraer el ZIP Android completo y abrir su carpeta raíz (con `settings.gradle.kts`) en Android Studio, o abrir el repositorio completo. Sincronizar Gradle, instalar Platform 36 y configurar el SDK localmente.
2. Iniciar un AVD con Google Play y API 26 o superior; se recomienda un AVD actual con acceso a Internet. Iniciar primero Spring Boot en el PC.
3. Para ejecutar desde código, seleccionar módulo `app` y Run en Android Studio. La URL predeterminada es `http://10.0.2.2:8080/`. La APK release de entrega se distribuye por separado y no se reconstruye al preparar los ZIP.
4. Registrar un cliente desde la aplicación y probar catálogo/carrito. La cuenta ADMIN de demostración ya existe en la base local; sus datos de demostración están definidos en `OylDatabase.onCreate` y las instrucciones privadas de entrega.
5. Como ADMIN, entrar en Cuenta → Configuración administrativa e introducir la misma clave temporal del servidor. Conectar y volver a Inventario/Pedidos. No se pide otra vez durante la sesión autorizada; un HTTP 401/403 la invalida.

`10.0.2.2` representa el host solamente dentro del emulador Android. En un teléfono físico no apunta al PC: se necesita un servidor accesible con HTTPS, configurar su URL mediante `-PoylApiBaseUrl=https://servidor.example/` y generar una variante adecuada. El servidor actual escucha en `127.0.0.1` y Android permite HTTP únicamente a `10.0.2.2`; cambiar solo la IP no basta. Esta entrega no modifica esa configuración de desarrollo.

## Demostración cliente y administrador

1. Cliente: registrar/iniciar sesión, elegir un producto con stock, añadir cantidad al carrito y confirmar el pedido.
2. Comprobar número REST y estado Pendiente; consultar catálogo para ver el stock descontado por el servidor.
3. Cerrar sesión, entrar como ADMIN y configurar el acceso desde Cuenta. Consultar todos los pedidos y los movimientos de inventario.
4. Avanzar el pedido a Confirmado, Preparado y Entregado; registrar una reposición si se desea y verificar existencias.
5. Volver al cliente original y actualizar Mis pedidos: debe ver el estado nuevo, sin duplicados ni pedidos de otros usuarios locales.
6. Para demostrar errores, usar datos de prueba y una base aislada; no repetir escrituras inciertas con un UUID nuevo. El checkout pendiente conserva la misma solicitud aunque se edite el perfil.

## Pruebas y cobertura

Resultados disponibles: 126 pruebas unitarias Android aprobadas, 16 pruebas de UI/navegación de la última corrección administrativa y una sonda Retrofit real GET que confirmó protección HTTP 403. La actualización del mapa tiene una ejecución previa de 11 pruebas y la integración REST/SQLite una previa de 25; son ejecuciones de alcance distinto y se solapan, por lo que no se suman como pruebas únicas. Spring Boot dispone de 15 pruebas aprobadas sobre SQL, stock, idempotencia y protección administrativa.

**No existe una medición acreditada del 80 % de cobertura Android.** Cantidad de pruebas aprobadas no equivale a porcentaje de cobertura. Los reportes HTML/JUnit y logs se entregan como evidencias de ejecución, con su alcance identificado.

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
.\gradlew.bat :server:test --console=plain
git diff --check
```

Ejecutar instrumentación en un AVD dedicado: la instalación/reinstalación de pruebas puede afectar datos. Las sondas opcionales necesitan los argumentos indicados en sus clases; las pruebas REST que escriben deben usar un backend y H2 aislados. Los ZIP individuales incluyen solo el módulo correspondiente: las tareas del otro módulo se ejecutan en su propio ZIP o en el repositorio completo.

## APK release y firma

La entrega incluye una APK release firmada existente, copiada sin reconstruirla. `apksigner verify --verbose app-release.apk` verifica su firma; el SHA-256 permite comprobar que la copia coincide con el original. `app/build.gradle.kts` no contiene signingConfig con credenciales: la firma se realizó mediante el asistente de Android Studio y una llave privada externa. Para reproducir exactamente esa identidad de firma se requiere el keystore y alias originales, custodiados fuera del repositorio. No se incluyen llaves ni credenciales de firma.

Para instalar la release, usar un AVD de pruebas sin datos importantes. Si existe una instalación debug con otra firma, Android puede rechazar la actualización. No desinstalar ni borrar sus datos para resolverlo: usar otro AVD o un dispositivo de prueba apropiado. No reemplazar el APK ya firmado por una compilación debug.

## GitHub Trello y evidencias

GitHub conserva el código y ramas del proyecto. Trello se utiliza para acreditar planificación y distribución de tareas; el enlace del tablero, los responsables y su captura real deben completarse en la documentación privada de entrega. No se inventa un tablero ni se presume una distribución no acreditada.

El Word privado de entrega contiene los espacios de integrantes, RUT y capturas de firma/Trello. No debe publicarse con datos personales en GitHub. Los ZIP de código excluyen builds, cachés, `local.properties`, keystores y bases locales de ejecución. Cada paquete lleva wrapper y archivos raíz de Gradle; sus ajustes de empaquetado se explican en `EMPAQUETADO.md`.

## Límites conocidos

- Backend único modular, no múltiples microservicios independientes; el requisito requiere confirmación de la pauta.
- Autenticación local y clave administrativa compartida de desarrollo; no hay identidad remota segura ni sincronización de usuarios entre dispositivos.
- El GET de pedido se consulta por UUID, sin autenticación remota del cliente; no es un despliegue de producción.
- Creación/lectura/actualización de productos y movimientos disponibles; no existe DELETE.
- No se acredita cobertura Android del 80 %. La firma válida no prueba por sí sola correspondencia binaria reproducible con el código sin el entorno y keystore originales.
- Mapas con referencias y horarios de demostración. La lectura óptica real de QR depende del dispositivo/Google Play; sus eventos y resultados se probaron mediante simulación.
