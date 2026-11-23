# Control 3 — Integración Android y Spring Boot

Trabajo realizado exclusivamente en `control3-integracion`, a partir del rediseño existente. No se hizo merge de `admin-pedidos-rest`, ni commit, push o merge de ninguna rama. Se consultó su código con `git show`/`git diff` y se reutilizó la lógica necesaria, conservando las pantallas actuales.

## Acceso administrativo — presentación actual e historial

La presentación vigente es **Cuenta → Configuración administrativa**, exclusivamente para ADMIN. Inicio y las secciones principales no muestran el panel permanente. Las operaciones protegidas ofrecen configurar el acceso cuando falta autorización. La clave sigue solo en memoria; navegar y recrear la actividad conservan la sesión autorizada, mientras cerrar sesión o terminar el proceso elimina la clave. Un HTTP 401/403 invalida la credencial rechazada. La última verificación está en [el informe de correcciones](INFORME_CORRECCIONES_PROFESOR.md). Los párrafos y resultados siguientes describen la corrección histórica previa a ese cambio de presentación.

La prueba manual detectó un defecto de la integración inicial: el formulario de clave solo se componía en Gestión, Inventario, Pedidos y los formularios administrativos; no aparecía en Inicio ni Cuenta. Además, el dashboard sustituía los errores específicos por mensajes genéricos. Sin credencial, los repositorios interrumpían localmente la consulta administrativa antes de enviar el GET protegido. En inventario podía haber terminado correctamente el GET público de productos y fallar después la comprobación local de la clave.

Se comprobó el servidor de la prueba manual en `127.0.0.1:8080`: salud y productos respondieron HTTP 200, y los GET administrativos sin credencial respondieron HTTP 403. Estas comprobaciones solo consultaron datos; no se leyó la clave del proceso ni se modificaron sus pedidos/inventario. La comprobación Retrofit desde Android se registra en las pruebas de esta corrección.

En la corrección histórica se añadió **Configurar acceso al servidor** en la parte superior de **Inicio, Cuenta, Gestión, Inventario y Pedidos**. Ese panel permanente fue reemplazado por la configuración dentro de Cuenta. El campo continúa enmascarado, sin estado guardable y con teclado de contraseña: introducir la clave configurada en `OYL_ADMIN_KEY` (mínimo 16 caracteres) y pulsar **Conectar administración**.

Se validan mediante GET las rutas protegidas de pedidos y movimientos antes de declarar acceso conectado. Solo después se conserva la clave en memoria y se refrescan automáticamente inventario, pedidos y catálogo. Un 401/403 muestra rechazo de la clave o falta de configuración en el servidor; un fallo de red muestra un mensaje de conexión; otros códigos HTTP se distinguen sin mostrar cuerpos HTTP, claves ni detalles sensibles. La clave incorrecta se puede corregir en el mismo formulario, sin cerrar sesión. La sesión de acceso se cancela y borra al desconectar/cerrar sesión, incluso si llega tarde una respuesta de validación.

Si Android restaura la sesión local del administrador después de reiniciar el proceso, puede configurar nuevamente el acceso desde Cuenta sin cerrar sesión. El dashboard deja los indicadores protegidos sin valor hasta conectar y conserva los errores operativos de una sesión autorizada. No se modificó ningún endpoint ni su autorización.

Verificación de esta corrección: APK debug compilado; **101 pruebas unitarias Android aprobadas** y **13 pruebas instrumentadas pertinentes aprobadas** (HomeNavigationTest, HomeScreenTest y AdminServerConnectivityTest). Estas últimas se ejecutaron directamente con `adb shell am instrument -w`, cuya salida terminó en `OK (13 tests)`, sin desinstalar la aplicación. Las pruebas de navegación usan archivos SQLite temporales y las del servidor solo hacen GET. La sonda Retrofit desde Android obtuvo salud/catálogo y verificó HTTP 403 sin credencial en pedidos y movimientos administrativos. Las pruebas incluyen corregir una clave rechazada, refrescar inventario/pedidos y reconfigurar el acceso tras recrear la actividad conservando la sesión local.

La ejecución Gradle instrumentada no pudo instalar en el emulador alternativo por falta de espacio; el AVD nuevo no completó su arranque. No se borraron datos para resolverlo: se instaló con `-r` el APK corregido y el paquete de pruebas en el emulador de la prueba manual y se ejecutaron únicamente los casos aislados indicados. El HTML de aquella ejecución Gradle refleja ese fallo de instalación, no el resultado posterior de las 13 pruebas directas. Los resultados de 54 casos que aparecen más adelante corresponden a la integración anterior y se conservan como evidencia histórica.

Después de aquellas pruebas históricas se comprobó el botón permanente en MainActivity. La actualización actual elimina esa presentación y mantiene la entrada en Cuenta. Se conserva aquí el resultado histórico de `git diff --check` sin commit ni push.

## Estado de los requisitos

| Requisito | Estado y alcance |
| --- | --- |
| Catálogo por Retrofit | Implementado. REST es la autoridad; SQLite conserva una copia para consulta sin conexión. |
| Checkout, stock y pedidos REST | Implementado. Pedido, líneas, descuento y auditoría se guardan en una transacción H2. |
| Recuperación e idempotencia | Implementado. UUID y JSON persistidos antes del POST; mismo pedido al reintentar, también con stock cero. |
| Historial del cliente | Implementado para los pedidos asociados a la cuenta en este dispositivo; actualiza y persiste estados REST. |
| Administración | Implementada: listado completo del servidor, avance de estados, productos y movimientos protegidos por clave. |
| SQLite y autenticación | Conservados. Migración aditiva 7 → 8; usuarios, sesiones, carrito, pedidos antiguos e intentos pendientes. |
| Diseño | Componentes, tema, ilustraciones, dos columnas y navegación conservados. Cambios de UI limitados a acceso REST, mensajes, referencias y recuperación. |
| SQL al iniciar H2 | Implementado con `schema.sql` y `data.sql`, sin doble seeder ni reinicio de existencias. |
| Microservicios independientes | Pendiente de aclaración de la pauta. No implementados; se mantiene el servidor existente por instrucción del usuario. |
| Cámara/QR/mapa en hardware físico | Funcionalidad conservada; falta la comprobación manual con cámara, QR físico, permisos y conexión real del dispositivo. |

## Arquitectura real y microservicios

`app/` es una aplicación Android Kotlin/Compose con Repository, ViewModel, Retrofit y SQLiteOpenHelper. `server/` es **una única aplicación Spring Boot 3.5.7**, con un proceso, un puerto, un artefacto JAR y una base H2 compartida. `ProductController`, `OrderController`, `AdminController` y `OrderService` son componentes internos, no microservicios independientes. Tampoco hay autenticación remota de usuarios.

La frase del profesor fue «Código fuente proyecto completo Spring Boot con microservicios en .zip». No define cuántos servicios ni sus fronteras. El usuario indicó continuar con la arquitectura actual hasta confirmar la pauta. Por ello, la integración funcional está implementada, pero **no se declara cumplida la exigencia de microservicios independientes**.

Si la pauta exige varios procesos, una evolución acotada es mantener pedidos e inventario juntos, preservando su transacción, y extraer un servicio de notificaciones con aplicación Spring Boot, puerto, almacenamiento y pruebas propios. El servidor principal publicaría eventos mediante una outbox transaccional; el segundo servicio los consumiría de forma idempotente. La verificación exigiría arrancar ambos JAR por separado y comprobar entrega/reintentos con uno detenido. Es una propuesta, no código ya implementado. Separar catálogo/inventario y pedidos necesitaría además reservas idempotentes y compensación de fallos; no basta con mover controladores a carpetas o abrir dos puertos sobre la misma base.

Referencia conceptual: [características de despliegue independiente de microservicios](https://microservices.io/post/architecture/2022/05/04/microservice-architecture-essentials-deployability.html).

## API disponible

Base de desarrollo: `http://127.0.0.1:8080`. Android Emulator accede mediante `http://10.0.2.2:8080/`.

| Método | Ruta | Protección / comportamiento |
| --- | --- | --- |
| GET | `/api/health` | Salud del servidor. |
| GET | `/api/products` | Catálogo y stock oficiales. |
| POST | `/api/orders` | Crea/recupera un pedido usando `requestId` UUID. |
| GET | `/api/orders/{id}` | Consulta por UUID remoto. |
| GET | `/api/admin/orders` | Todos los pedidos del servidor. |
| POST | `/api/admin/orders/{id}/advance` | PENDING → CONFIRMED → PREPARED → DELIVERED; 409 al terminar. |
| POST | `/api/admin/products` | Crea producto; devuelve 201 y un ID generado por secuencia. |
| PUT | `/api/admin/products/{id}` | Edita datos; conserva stock aunque el formulario envíe otro. |
| POST | `/api/admin/products/{id}/stock` | Movimiento con `delta` distinto de cero, entre -10000 y 10000, motivo de 3–200 caracteres y stock no negativo. |
| GET | `/api/admin/inventory/movements` | Auditoría de entradas, salidas y descuentos de pedidos. |

Todas las rutas `/api/admin/**` exigen `X-OYL-ADMIN-KEY`, comparada con `OYL_ADMIN_KEY` del servidor. Si no se configura o tiene menos de 16 caracteres, se rechaza el acceso. El inicio de sesión local como administrador no sustituye esta autorización REST.

Ejemplo de cuerpo de pedido (generar un UUID nuevo solo para una compra nueva):

```json
{
  "requestId": "UUID-generado-por-el-cliente",
  "customerAlias": "cliente",
  "delivery": "PICKUP",
  "contactPhone": "912345678",
  "contactAddress": "Retiro en tienda",
  "items": [{"productId": 1, "quantity": 1}]
}
```

El servidor calcula precios y total. Admite hasta 30 productos distintos y 100 bolsas por producto. Valida datos y bloquea existencias por ID ascendente. Serializa solicitudes por `requestId` mediante `checkout_requests`; la restricción UNIQUE del pedido constituye una defensa adicional. Reutilizar el UUID con otra identidad, entrega, contacto o líneas devuelve 409 sin crear ni descontar otro pedido.

Un rechazo de stock confirmado devuelve `X-OYL-Rejected-Request`. Android solo libera ese intento si el encabezado coincide con la solicitud pendiente. Un timeout, una respuesta inválida, un 409 genérico o un error incierto conservan el JSON y bloquean la edición del carrito.

## SQLite, sincronización y funcionamiento sin conexión

La base sigue siendo `oyl_auth.db`. Se conserva el esquema anterior y se agrega una migración 7 → 8:

- `products_before_rest`: copia del catálogo anterior a la integración, incluidas referencias de fotografías.
- `products.server_product_id`: vínculo único con el producto del servidor.
- `products.remote_active`: identifica productos presentes en la última descarga completa.

Los productos semilla 1–8 solo reutilizan la fila local cuando coinciden ID, nombre, categoría y peso. Un producto remoto nuevo obtiene una fila local propia; no sobrescribe un producto creado localmente que tenga el mismo número. Las fotografías privadas existentes se conservan. No se envían fotos, rutas privadas ni URLs externas al servidor: H2 recibe solo claves de ilustraciones propias.

El catálogo visible procede de los productos vinculados a REST. Los productos puramente locales se conservan, pero no se publican automáticamente ni se compran como si existieran en el servidor. Un carrito que los contenga exige retirarlos y elegir los productos del catálogo REST. El QR utiliza el ID oficial del servidor y la navegación utiliza el ID local vinculado.

`pending_remote_checkout` conserva por usuario el UUID, JSON exacto, modalidad y huella de líneas. La recuperación no reconstruye la solicitud, no exige stock actual y no consulta el catálogo antes de reenviarla. La confirmación SQLite valida el pedido recibido y, en una transacción, guarda su copia, elimina únicamente las cantidades compradas del usuario y libera el intento. Las huellas antiguas que incluían precio continúan siendo compatibles.

Los pedidos de `demo_orders` sin `server_order_id` se muestran como **históricos locales no registrados en Spring Boot**. Los remotos se identifican por UUID y se deduplican por ese UUID. Un pedido local parecido no se transforma ni se elimina. El administrador puede seguir avanzando estados locales, identificados como tales, sin que eso modifique un pedido REST.

El cliente solo consulta los UUID vinculados a su usuario SQLite. Los cambios de estado se guardan para futuras consultas sin red y generan avisos locales una sola vez por cambio. La sincronización de existencias mantiene los avisos de stock bajo/agotado para administradores, sin repetirlos por descargar la misma cifra. La lista administrativa completa se conserva en memoria; no se asignan a la cuenta del administrador los pedidos de otros dispositivos.

| Función | Sin servidor |
| --- | --- |
| Registro/inicio de sesión local y sesión persistente | Disponible. |
| Perfil, avisos locales y fotos ya guardadas | Disponible. |
| Catálogo | Última copia REST, indicando que precio/stock pueden estar desactualizados. Sin una primera sincronización, no hay catálogo REST guardado. |
| Carrito | Lectura y edición local, con sus validaciones; bloqueado si hay un intento pendiente. |
| Historial | Pedidos locales y copias remotas de la cuenta; se avisa si no se pudo actualizar. |
| Nueva compra o recuperación | Necesita servidor. No se muestra éxito sin confirmación y copia durable. |
| Crear/editar productos, mover stock, listar todos los pedidos y avanzar estados REST | Necesita servidor y clave administrativa. |
| QR/cámara/mapa | Se conservan; el escáner puede requerir descargar el módulo de Google y las teselas del mapa necesitan conexión/caché. |

## Inicialización SQL de H2

Se usa JDBC, no Hibernate. `spring.sql.init.mode=always` ejecuta `schema.sql` y después `data.sql`, en UTF-8, con fallo de arranque ante errores. [Spring Boot documenta este mecanismo](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

`schema.sql` usa `CREATE TABLE IF NOT EXISTS` y alteraciones aditivas para mantener productos, pedidos, líneas y movimientos. Agrega el bloqueo por solicitud y la secuencia de productos. `data.sql` inserta ocho productos mediante `INSERT ... SELECT ... WHERE NOT EXISTS` por ID: un reinicio no cambia precio, descripción ni stock de una fila existente. No contiene `DROP`, `TRUNCATE` ni actualizaciones que repongan existencias.

`DemoCatalogSeeder.java` queda como clase de compatibilidad documental, sin `@Component`, `ApplicationRunner` ni inserciones. Existe una sola fuente de inicialización: los scripts SQL.

La URL predeterminada es `jdbc:h2:file:./oyl-data;DB_CLOSE_ON_EXIT=FALSE`. Con `:server:bootRun`, el directorio de trabajo es `server/`; se conserva allí el archivo H2 existente. Al ejecutar el JAR manualmente se debe usar el mismo directorio o una URL absoluta a la misma base. Nunca borrar el archivo para volver a obtener stock de demostración.

`SqlInitializationTest` crea un archivo exclusivo en `@TempDir`, arranca HTTP real desde cero, verifica las ocho filas, crea un pedido, detiene y vuelve a iniciar sobre ese mismo archivo. Comprueba que sobreviven las ediciones, existencias, pedido, líneas e inventario, sin duplicar la semilla ni descontar otra vez el pedido recuperado. Las demás pruebas usan H2 en memoria. No se abrió ni reinició la base persistente del usuario para probar.

Se corrigió además un conflicto del classpath de plugins: AGP aportaba Commons Compress 1.21 al classloader padre mientras Boot 3.5.7 necesitaba 1.27.1 para `bootJar`. La dependencia de construcción se alinea en el Gradle raíz; no se incorpora al APK como dependencia de la aplicación.

## Inicio y prueba manual

Requisitos: JDK 17 o superior compatible con la instalación Android (se probó JDK 21), Android SDK 36 y un emulador. Ejecutar desde la raíz del repositorio, en PowerShell.

Primera terminal, elegir una clave propia de al menos 16 caracteres e introducirla sin dejarla literal en el historial:

```powershell
$env:OYL_ADMIN_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Clave administrativa REST' -AsSecureString)).Password
.\gradlew.bat :server:bootRun --console=plain
```

El servidor escucha únicamente en `127.0.0.1:8080`. En otra terminal:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
Invoke-RestMethod http://127.0.0.1:8080/api/products
.\gradlew.bat :app:assembleDebug
$oylAdb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $oylAdb devices
& $oylAdb -e install -r app/build/outputs/apk/debug/app-debug.apk
```

El último comando requiere un solo emulador conectado. Usa `-r` para conservar datos; no desinstales ni borres datos para resolver un problema de instalación. También se puede ejecutar `app` desde Android Studio. Retrofit usa `http://10.0.2.2:8080/` por defecto. La propiedad Gradle `'-PoylApiBaseUrl=https://servidor.example/'` permite otra URL base, terminada en `/`. La política de red actual solo permite HTTP sin cifrar hacia `10.0.2.2`; no basta con cambiar a una IP LAN HTTP. La prueba en teléfono físico no está incluida en la verificación automatizada.

Para usar el JAR sin cambiar de archivo persistente:

```powershell
.\gradlew.bat :server:bootJar
Set-Location server
java -jar build/libs/server-0.0.1-SNAPSHOT.jar
```

Mantener la misma variable `OYL_ADMIN_KEY` en esa terminal. Detener con Ctrl+C y luego `Remove-Item Env:OYL_ADMIN_KEY` para retirarla de la sesión de PowerShell.

Procedimiento cliente → servidor → administrador → cliente:

1. Registrar un cliente o iniciar sesión con una cuenta local existente. Abrir Catálogo y comprobar que Spring Boot responde. Elegir un producto con existencias, añadirlo al carrito y confirmar modalidad/contacto.
2. Registrar pedido. La pantalla de éxito debe incluir referencia REST. Comprobar que aparece una sola vez en Mis pedidos y que disminuye el stock. Si falla la conexión después de enviar, el carrito queda protegido: restaurar el servidor y pulsar **Recuperar pedido pendiente**, incluso con stock actual cero.
3. Cerrar sesión e iniciar como administrador local de demostración (`admin` / `Admin1234`, cuenta preexistente). Abrir **Cuenta → Configuración administrativa**, introducir la misma clave configurada en el servidor y pulsar **Conectar administración**. Tras validarla aparecerá **Acceso administrativo conectado** y se actualizarán los datos; volver a Inicio/Pedidos para administrar. También puede abrir primero Pedidos o Inventario y usar **Configurar acceso** cuando falte autorización. La clave se guarda solo en memoria, sin preferencias, SQLite, SavedStateHandle ni BuildConfig.
4. Localizar el UUID del cliente. Avanzar a Confirmado, Preparado y Entregado, verificando el estado tras cada paso. Si se pierde una respuesta, actualizar el listado antes de intentar otro avance. Los históricos locales tienen otra etiqueta y no representan pedidos del servidor.
5. Volver al cliente y pulsar Actualizar pedidos. Debe verse Entregado, una sola fila por UUID y el aviso local del cambio. Cerrar/reabrir debe conservar sesión, historial y estado cacheado.
6. Como administrador, probar Crear producto, editarlo y registrar entrada/salida en Inventario. La edición no altera stock; los movimientos sí. Una salida superior a las existencias se rechaza. La cámara sigue guardando fotografías en el almacenamiento privado del teléfono.

## Pruebas y evidencias

Verificación final del 23 de noviembre de 2026, rama `control3-integracion`, HEAD sin cambios `87ee592`:

| Comprobación | Resultado |
| --- | --- |
| Spring Boot (`:server:test`) | 15 pruebas, 0 fallos/errores. |
| Android JVM (`:app:testDebugUnitTest`) | 97 pruebas, 0 fallos/errores. |
| Suite Android completa | 54 pruebas, 0 fallos, 0 omitidas, Android 17/API 37 en AVD OYL. |
| Verificación dirigida posterior del checkout | 10 pruebas repetidas de SQLite/recuperación y Retrofit contra Spring Boot real, 0 fallos, 0 omitidas. Incluye el último ajuste para no eliminar una fila del carrito sin vínculo coincidente. |
| `:app:assembleDebug` | Correcto, APK debug recompilada con el código final. |
| `:server:bootJar` y arranque del JAR | Correctos. HTTP en 18080 sobre H2 aislada en memoria; salud y catálogo verificados. |
| Base H2 vacía y reinicio con el mismo archivo de prueba | Correctos en `SqlInitializationTest`, sin duplicar ni reponer stock. |
| `git diff --check` | Correcto; solo avisos de normalización LF/CRLF en la copia de trabajo. |

Son 166 pruebas distintas aprobadas; las 10 dirigidas son una repetición adicional de casos incluidos en las 54 instrumentadas. El servidor de integración usó exclusivamente `jdbc:h2:mem:android_control3`; el emulador se abrió con `-read-only -no-snapshot-save`. No se eliminaron datos persistentes del usuario para conseguir estos resultados.

El informe de la suite completa de 54 casos se preservó en [build/control3-evidence/android-full/index.html](build/control3-evidence/android-full/index.html). El reporte Android habitual corresponde a la última ejecución dirigida de 10 casos. Las capturas revisadas están en [build/control3-evidence/visual/](build/control3-evidence/visual/): catálogo, gestión de productos, pedido entregado en administración y el mismo estado visto por el cliente, a 320 dp. Las pruebas también generaron capturas a 360 y 393 dp.

Comprobación habitual sin backend externo (el caso opcional de integración real se omite si no se indican sus argumentos):

```powershell
.\gradlew.bat :server:test :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --console=plain
git diff --check
```

Para repetir la prueba con Spring Boot real sin tocar datos persistentes, primero crear el JAR y abrir un servidor **de pruebas** en otra terminal:

```powershell
.\gradlew.bat :server:bootJar
java -jar server/build/libs/server-0.0.1-SNAPSHOT.jar --server.port=18080 '--spring.datasource.url=jdbc:h2:mem:android_control3;DB_CLOSE_DELAY=-1' --OYL_ADMIN_KEY=control3-isolated-runtime-test-key
```

La clave anterior es exclusivamente un dato público de la prueba aislada en memoria; no usarla para datos reales. Después ejecutar:

```powershell
.\gradlew.bat :server:test :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.oylIntegrationBaseUrl=http://10.0.2.2:18080/' '-Pandroid.testInstrumentationRunnerArguments.oylIntegrationAdminKey=control3-isolated-runtime-test-key' --console=plain
```

Las comillas de los argumentos `-P` son necesarias en PowerShell. No se habilita configuration cache para este comando. Usar un emulador exclusivo de pruebas o abrir el AVD con `-read-only -no-snapshot-save`: Gradle instala/desinstala el paquete de prueba y no debe ejecutarse contra datos de uso real.

Cobertura añadida/adaptada:

- Spring: GET de catálogo/salud, protecciones administrativas con clave ausente/incorrecta/no configurada, CRUD de productos, stock y auditoría, ciclo de estados y consulta del cliente, rechazo de sobreventa, reintentos idénticos concurrentes y UUID reutilizado con otro contenido, arranque vacío y reinicio persistente.
- Android JVM: contrato Retrofit real contra MockWebServer, combinación de historiales sin duplicados, separación de históricos y mapeo de IDs; se conservan las pruebas previas de validación, autenticación, carrito y recuperación.
- Android instrumentado: SQLite real aislado, migración desde v7 con datos, sesión/pedidos/carrito/fotos/pending, colisiones de IDs, estados y avisos por usuario; formularios, diseño, navegación, dos columnas y capturas a 320/360/393 dp. Las pruebas de navegación usan un doble de API determinista con repositorios/SQLite reales.
- Integración adicional Android → Retrofit → JAR Spring Boot → H2: crea/edita producto, simula pérdida de respuesta después del commit HTTP real, reabre SQLite, recupera el mismo UUID, verifica un solo descuento, administra estados, actualiza historial, mueve inventario y consulta el último estado sin conexión.

Informes generados: `server/build/reports/tests/test/index.html`, `app/build/reports/tests/testDebugUnitTest/index.html` y `app/build/reports/androidTests/connected/debug/index.html`. APK **solo debug**: `app/build/outputs/apk/debug/app-debug.apk`. Las capturas son evidencia de desarrollo en `build/`; no se preparó ZIP final ni APK release/firmada de entrega.

## Límites pendientes

- Confirmar con el profesor qué entiende por microservicios. La arquitectura actual no equivale a servicios desplegables independientemente.
- La autenticación es local. No hay recuperación segura del historial en otro teléfono ni listado público por alias: solo se consultan los UUID asociados a cada usuario en SQLite. El GET por UUID conserva el contrato previo y no sustituye autenticación/autorización remota de clientes. No presentar esta demostración como un backend comercial autenticado.
- La clave administrativa compartida conserva la protección existente; se introduce manualmente y se borra al desconectar el acceso, cerrar sesión o terminar el proceso. La recreación de la actividad conserva la autorización en memoria mientras continúe la sesión. No hay claves privadas en el APK.
- Los pedidos locales y productos sin vínculo REST no se suben automáticamente. Un intento antiguo inválido o incompatible conserva su JSON y requiere revisión; no se descarta para desbloquear artificialmente el carrito.
- Crear productos, ajustar stock y avanzar estados no tienen el protocolo persistente de checkout. Ante una respuesta incierta, revisar catálogo/inventario/historial antes de repetir estas acciones. El checkout sí dispone de reintento idempotente durable.
- No hay pagos, logística real ni sincronización push. El cliente obtiene cambios de estado al actualizar o volver a consultar; los avisos siguen siendo locales.
- Cámara y escáner con hardware, descarga del módulo Google, permisos del teléfono y teselas del mapa requieren comprobación manual. Se conservaron las funciones existentes y sus pruebas de UI/fotos privadas.

## Archivos modificados y nuevos

- [INFORME_CONTROL3_INTEGRACION.md](INFORME_CONTROL3_INTEGRACION.md)
- [README.md](README.md)
- [app/build.gradle.kts](app/build.gradle.kts)
- [app/src/androidTest/java/com/example/oylfrutossecos/CheckoutRecoveryUiTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/CheckoutRecoveryUiTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/CheckoutSqliteRecoveryTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/CheckoutSqliteRecoveryTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/HomeScreenTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/HomeScreenTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/LocalCheckoutSqliteTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/LocalCheckoutSqliteTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/NavigationTestApi.kt](app/src/androidTest/java/com/example/oylfrutossecos/NavigationTestApi.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/AdminServerConnectivityTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/AdminServerConnectivityTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/RestRepositoryIntegrationTest.kt](app/src/androidTest/java/com/example/oylfrutossecos/RestRepositoryIntegrationTest.kt)
- [app/src/androidTest/java/com/example/oylfrutossecos/StubOylRestService.kt](app/src/androidTest/java/com/example/oylfrutossecos/StubOylRestService.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/local/OylDatabase.kt](app/src/main/java/com/example/oylfrutossecos/data/local/OylDatabase.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/local/RemoteCheckoutCache.kt](app/src/main/java/com/example/oylfrutossecos/data/local/RemoteCheckoutCache.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/model/Order.kt](app/src/main/java/com/example/oylfrutossecos/data/model/Order.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/model/Product.kt](app/src/main/java/com/example/oylfrutossecos/data/model/Product.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/remote/OylRestClient.kt](app/src/main/java/com/example/oylfrutossecos/data/remote/OylRestClient.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/remote/OylRestService.kt](app/src/main/java/com/example/oylfrutossecos/data/remote/OylRestService.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/repository/CheckoutCoordinator.kt](app/src/main/java/com/example/oylfrutossecos/data/repository/CheckoutCoordinator.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/repository/InventoryRepository.kt](app/src/main/java/com/example/oylfrutossecos/data/repository/InventoryRepository.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/repository/OrderRepository.kt](app/src/main/java/com/example/oylfrutossecos/data/repository/OrderRepository.kt)
- [app/src/main/java/com/example/oylfrutossecos/data/repository/ProductRepository.kt](app/src/main/java/com/example/oylfrutossecos/data/repository/ProductRepository.kt)
- [app/src/main/java/com/example/oylfrutossecos/domain/CheckoutLines.kt](app/src/main/java/com/example/oylfrutossecos/domain/CheckoutLines.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/components/AdminAccessPanel.kt](app/src/main/java/com/example/oylfrutossecos/ui/components/AdminAccessPanel.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/navigation/StoreNavigation.kt](app/src/main/java/com/example/oylfrutossecos/ui/navigation/StoreNavigation.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/CartScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/CartScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/CatalogScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/CatalogScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/HomeScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/HomeScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderConfirmationScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderConfirmationScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderSuccessScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderSuccessScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/OrdersScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/OrdersScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/ProductDetailScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/ProductDetailScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/screens/QrScannerScreen.kt](app/src/main/java/com/example/oylfrutossecos/ui/screens/QrScannerScreen.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AdminSessionViewModel.kt](app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AdminSessionViewModel.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/CatalogViewModel.kt](app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/CatalogViewModel.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/InventoryViewModel.kt](app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/InventoryViewModel.kt)
- [app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/OrderViewModel.kt](app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/OrderViewModel.kt)
- [app/src/test/java/com/example/oylfrutossecos/CheckoutRecoveryTest.kt](app/src/test/java/com/example/oylfrutossecos/CheckoutRecoveryTest.kt)
- [app/src/test/java/com/example/oylfrutossecos/AdminSessionViewModelTest.kt](app/src/test/java/com/example/oylfrutossecos/AdminSessionViewModelTest.kt)
- [app/src/test/java/com/example/oylfrutossecos/OrderHistoryTest.kt](app/src/test/java/com/example/oylfrutossecos/OrderHistoryTest.kt)
- [app/src/test/java/com/example/oylfrutossecos/RetrofitContractTest.kt](app/src/test/java/com/example/oylfrutossecos/RetrofitContractTest.kt)
- [app/src/test/java/com/example/oylfrutossecos/StubOylRestService.kt](app/src/test/java/com/example/oylfrutossecos/StubOylRestService.kt)
- [build.gradle.kts](build.gradle.kts)
- [server/src/main/java/com/oyl/frutossecos/api/AdminController.java](server/src/main/java/com/oyl/frutossecos/api/AdminController.java)
- [server/src/main/java/com/oyl/frutossecos/api/DemoCatalogSeeder.java](server/src/main/java/com/oyl/frutossecos/api/DemoCatalogSeeder.java)
- [server/src/main/java/com/oyl/frutossecos/api/OrderService.java](server/src/main/java/com/oyl/frutossecos/api/OrderService.java)
- [server/src/main/resources/application.properties](server/src/main/resources/application.properties)
- [server/src/main/resources/data.sql](server/src/main/resources/data.sql)
- [server/src/main/resources/schema.sql](server/src/main/resources/schema.sql)
- [server/src/test/java/com/oyl/frutossecos/api/Control3IntegrationTest.java](server/src/test/java/com/oyl/frutossecos/api/Control3IntegrationTest.java)
- [server/src/test/java/com/oyl/frutossecos/api/SqlInitializationTest.java](server/src/test/java/com/oyl/frutossecos/api/SqlInitializationTest.java)
