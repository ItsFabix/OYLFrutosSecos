# OYL Frutos Secos — revisión final del Control 3

> **Actualización posterior del 25/11:** el pedido vuelve a devolver cuatro eventos. Se recuperaron exclusivamente desde sus filas originales del outbox, sin modificar IDs/fechas, con respaldos H2 previos. Se verificaron veinte comprobaciones aisladas de compra, estados, recuperación y persistencia. La observación de las 09:07 que sigue es histórica. Se deja de investigar datos Android antiguos y se prepara un AVD independiente para datos nuevos; no se presentan como recuperados.

> **Cierre de presentación del 25/11:** release nueva firmada con el certificado original y probada en `OYL_Release_Control3`; compra `ec121ba3-466c-4ebc-9d26-bfeece0143bc`, cuatro cambios/eventos, seguimiento remoto visible y sesión/historial persistentes tras reiniciar la app. Las 249 pruebas JVM se reejecutaron sin fallos y JaCoCo conservó exactamente 115 clases y los porcentajes acreditados; las 80 instrumentadas son resultados anteriores conservados. Los pendientes antiguos de generación de APK que aparecen abajo quedan superados por esta comprobación. No se hizo commit ni push.

> **Observación posterior del 25/11 a las 09:03–09:07:** existe una instalación Android nueva con una sola cuenta ADMIN y ningún pedido/checkout; no acredita recuperación. Se encontró un candidato antiguo en Pixel_9_Pro, aún sin restaurar ni validar sobre un clon. La consulta actual del UUID indicado devuelve **cero eventos**, aunque las consultas anteriores aquí documentadas devolvieron cuatro. Leer el [informe de recuperación y cierre](INFORME_RECUPERACION_CIERRE_CONTROL3.md) antes de dar la entrega por finalizada. Los resultados de pruebas anteriores se conservan.

Fecha: 25 de noviembre de 2026. Rama exclusiva: `control3-tres-microservicios`.

## 1. Resumen y alcance

Se revisaron los textos de los 20 destinos de navegación, componentes compartidos, formularios, diálogos, accesibilidad, estados vacíos/carga/error y contenido recibido de SQLite y HTTP. Se conservaron el diseño verde y crema, ilustraciones, distribución, navegación, autenticación, carrito, perfil, QR, mapa, Open-Meteo y autorización administrativa.

Se implementó una capa de presentación para estados y avisos antiguos, mensajes nuevos de pedidos según su estado real y modalidad, referencias breves para inventario/pedidos y textos de errores comprensibles. Las comprobaciones de avisos quedan dentro de Preferencias de notificaciones, solo para ADMIN; no están disponibles para CLIENTE. El componente de diagnóstico de catálogo se conserva fuera de la navegación habitual y ya no describe incorrectamente el inventario como exclusivamente local.

Único ajuste adicional de validación: el formulario de movimientos ahora informa y admite hasta 10.000 bolsas, igual que el límite **ya existente** en InventoryRepository y Spring Boot; antes ofrecía 100.000, que luego eran rechazadas al registrar el movimiento.

No se modificaron contratos JSON/Retrofit, UUID internos, coordenadas, scripts SQL, migraciones, código de producción de los microservicios, credenciales ni firma. `app/build.gradle.kts` conserva exactamente los cambios anteriores de JaCoCo/Robolectric. Se agregó a `gradle.properties` una protección contra desinstalación automática después de instrumentar. No se hicieron commits, push, merge ni cambios de rama. Los servicios 8080/8081/8082 y sus H2 no se detuvieron ni reiniciaron.

### Incidente importante durante instrumentación

Las 80 pruebas instrumentadas aprobaron, pero se comprobó después que la limpieza automática predeterminada de AGP había desinstalado la aplicación del AVD OYL, incluida su carpeta de datos. Comparar las firmas antes de ejecutar no protegía contra ese comportamiento. La comprobación posterior no encontró el paquete, tampoco como instalación desinstalada conservando datos, ni su carpeta `/data/user/0/com.example.oylfrutossecos`.

No se había generado un respaldo de la SQLite de ese AVD antes de las pruebas. No se puede afirmar que se conservaron ni que se recuperaron sus usuarios, sesiones, carrito, fotos e historial guardados exclusivamente en Android. Las bases H2 y el pedido remoto indicado siguen siendo independientes de esa limpieza. Se informó al usuario y se solicitó identificar una copia de seguridad reciente; las imágenes antiguas encontradas no se restauraron ni sobrescribieron por no acreditar su correspondencia con los datos recientes.

Se corrigió la configuración para futuras pruebas mediante `android.injected.androidTest.leaveApksInstalledAfterRun=true`, opción documentada en el [código oficial de AGP](https://android.googlesource.com/platform/tools/base/+/ec2165b772889874bfd9dc5c039a5d0ae52c80f5/build-system/gradle-core/src/main/java/com/android/build/gradle/options/BooleanOption.kt). Esta protección no recupera por sí sola los datos ya eliminados. La recuperación del AVD queda pendiente de una copia válida o de la indicación del usuario sobre ese entorno de pruebas.

Después de detectar el incidente no se reinstaló la app ni se recrearon usuarios o historiales. Sigue disponible una copia del APK debug que estaba instalado antes (`build/final-polish/installed-before.apk`), pero ese archivo no contiene un respaldo de datos. El historial remoto de H2 no reemplaza las sesiones, credenciales, fotografías, carrito o referencias de pedidos guardados en SQLite.

## 2. Pantallas y presentación

| Área revisada | Resultado |
| --- | --- |
| Bienvenida, login, registro | Identidad visual y validaciones conservadas; sesión sin terminología SQLite. |
| Inicio/dashboards | Textos profesionales, cantidades y singular/plural; acceso administrativo conserva su ubicación discreta en Cuenta. |
| Catálogo y detalle | Dos columnas, imágenes propias y pesos/precios CLP conservados; etiquetas de disponibilidad coherentes, incluida una sola bolsa. |
| Carrito y confirmación | Recuperación pendiente explicada sin UUID largo; advertencias contra duplicados y estados de compra conservados. |
| Pedido registrado | “Recibido” no se confunde con el estado “Confirmado”; número de pedido y estado real visibles. |
| Mis pedidos/gestión | Estados en español; referencia breve en filas administrativas sin número local; pedidos anteriores identificados como guardados en el dispositivo. |
| Seguimiento del pedido | Eventos remotos con mensajes específicos y fecha; deduplicación visual por eventId; error y reintento sin ocultar el pedido. |
| Notificaciones/preferencias | Avisos anteriores normalizados; título/subtítulo natural; comprobación de avisos restringida a ADMIN. |
| Cuenta y perfil | Datos, separación de usuarios, edición y persistencia conservados; ninguna edición de rol o identidad. |
| Acceso administrativo | Explicación sin nombres de variables o frameworks; errores distintos de autorización/conexión; clave enmascarada y exclusivamente en memoria. |
| Mapa y clima | Ver todos, Centro/Ñuñoa, marcadores, encuadre y controles conservados; títulos sin “demo”, con aviso honesto de ubicaciones/horarios de ejemplo y atribución Open-Meteo/OpenStreetMap. |
| QR | Diálogo de resultado, incompatibilidad, inexistencia, cancelación y reintento conservados; texto de existencias y ayuda simplificados. |
| Productos/cámara e inventario | Formularios e imágenes privadas conservados; movimientos con referencia breve, stock y cantidades legibles. |

La revisión gráfica automatizada cubre escenarios de 320 dp, teclado, navegación, mapa y formularios. No equivale a una inspección manual de todas las combinaciones de tamaño, accesibilidad y estado posible.

| Antes | Después |
| --- | --- |
| Pedido REST #5 registrado / El servidor confirmó tu pedido. | Pedido #5 recibido / Hemos recibido tu pedido correctamente. Está pendiente de confirmación. |
| Pedido REST #5: Entregado / El servidor actualizó el estado de tu pedido. | Pedido #5 entregado / Tu pedido ha sido entregado. ¡Gracias por tu compra! |
| Eventos del microservicio | Seguimiento del pedido |
| Avisos de tus pedidos y productos en este dispositivo. | Mantente al día con tus pedidos y novedades. |
| Avisos en el dispositivo / Crear aviso | Preferencias de notificaciones / Comprobar notificaciones (ADMIN). |
| Error HTTP 500 | Ocurrió un problema. Inténtalo más tarde. |
| Movimiento registrado en Spring Boot. | Inventario actualizado correctamente. |
| Pedido REST seguido de un UUID completo en el motivo | Descuento por pedido · referencia breve. |

Se mantienen advertencias explícitas: esta versión académica no realiza cobros, despachos ni reservas reales; los puntos y horarios del mapa son ejemplos, no sucursales comerciales verificadas. No se sustituyó esa advertencia por una afirmación comercial falsa.

## 3. Notificaciones antiguas y compatibilidad

`StorePresentation` interpreta únicamente formatos heredados reconocidos. La presentación de un aviso recibido no afirma que esté confirmado; los avisos de estado utilizan el estado del propio aviso histórico, no el último estado conocido del pedido. Para avisos nuevos, la preparación distingue retiro y despacho cuando la modalidad está disponible. Un aviso antiguo sin modalidad muestra una indicación neutral de consultar los detalles.

La normalización no escribe en SQLite, no cambia IDs, propietario, fecha, contenido original, lectura ni destino. No requiere migración ni recreación de tablas. Las pruebas comparan registros completos antes/después y después de reabrir la base. Los avisos nuevos se crean en los puntos transaccionales existentes; refrescar el mismo estado no añade avisos duplicados.

Los avisos SQLite y los eventos del microservicio **siguen siendo sistemas independientes**. Los segundos se muestran en el detalle del pedido; no se insertan artificialmente como avisos SQLite. Se conserva la separación entre pedidos anteriores y pedidos registrados mediante HTTP. `CANCELLED` tiene traducción de presentación, pero no se agregó un flujo de cancelación: los contratos actuales siguen admitiendo sus cuatro estados existentes.

## 4. Microservicios, persistencia y fallos

| Servicio | Puerto habitual | Verificación |
| --- | ---: | --- |
| Ventas/productos/inventario/pedidos | 8080 | GET salud/productos; compras y stock transaccional; protección ADMIN; estados; outbox; persistencia tras reinicio de un H2 de prueba. |
| Puntos de retiro | 8081 | Dos puntos; igualdad directa/delegada; CRUD y autorización; cambios/altas conservados tras reiniciar un H2 de prueba sin duplicar semillas. |
| Notificaciones de pedidos | 8082 | Salud; cuatro eventos del UUID indicado; autorización e idempotencia; eventos y lectura conservados después de reiniciar un H2 de prueba. |

La comprobación de los servicios activos fue **solo de lectura**: 8 productos, 2 puntos, igualdad de contenido directo/delegado y cuatro eventos para `a9209d94-cc09-43fb-ad38-0fa9788eca72`: ORDER_CREATED/PENDING y ORDER_STATUS/CONFIRMED, PREPARED, DELIVERED. Se verificó “Ñuñoa” desde los bytes UTF-8 recibidos; los errores de visualización de PowerShell al omitir `-Encoding UTF8` no se confundieron con corrupción del backend.

`ServiceCommunicationTest` usa HTTP real por loopback hacia un destinatario de prueba: respuesta 503 conserva el evento pendiente; una respuesta posterior 200 permite marcarlo entregado; ambos envíos tienen el mismo cuerpo/eventId y no duplican el descuento de stock. También verifica delegación efectiva de puntos, UTF-8, coordenadas, propagación de 404, conexión rechazada y consultas de eventos con token interno. Las pruebas del destinatario verifican idempotencia de eventId, incluso después de reiniciar.

Ventas persiste el evento outbox junto con el pedido/estado en la misma transacción. El publicador no participa en la confirmación de compra. Se comprobaron esa independencia y la recuperación sin parar los servicios del usuario. Los reinicios usan archivos H2 temporales; las pruebas instrumentadas de compras utilizan otro proceso de ventas con puerto libre y H2 en memoria, creado y detenido exclusivamente para esa ejecución.

## 5. Resultados finales y cobertura

| Métrica | Antes | Ahora | Cubiertos / total actual |
| --- | ---: | ---: | ---: |
| Instrucciones | 91,27 % | **91,57 %** | 10606 / 11583 |
| Líneas | 92,64 % | **93,07 %** | 1195 / 1284 |
| Ramas | 82,79 % | **83,32 %** | 824 / 989 |
| Métodos | 91,36 % | **91,59 %** | 381 / 416 |

| Paquete | Instrucciones | Líneas | Ramas | Métodos |
| --- | ---: | ---: | ---: | ---: |
| `viewmodel` | 89,63 % | 89,91 % | 77,10 % | 90,15 % |
| `repository` | 94,55 % | 97,48 % | 86,73 % | 94,38 % |
| `domain` | 94,23 % | 99,18 % | 89,20 % | 93,65 % |

| Pruebas JVM | Casos | Aprobadas | Fallidas/errores | Omitidas |
| --- | ---: | ---: | ---: | ---: |
| app | 221 | 221 | 0 | 0 |
| server | 20 | 20 | 0 | 0 |
| pickup-service | 4 | 4 | 0 | 0 |
| notification-service | 4 | 4 | 0 | 0 |
| **Total JVM** | **249** | **249** | **0** | **0** |

Se añadieron 12 casos JVM Android respecto de los 209 anteriores (ahora 221) y 5 casos de los servicios respecto de los 23 anteriores (ahora 28). Las comprobaciones PowerShell ejecutaron **15 aserciones aisladas**, además de sintaxis, preflight y lecturas reales. Los conteos de PowerShell y de instrumentación se informan separados de JUnit JVM.


Mismos filtros JaCoCo: `domain`, `data/repository`, `ui/viewmodel`. Se conservan las 114 clases originales y se agrega `domain.StorePresentation`: **115 clases evaluadas**, sin excluir lógica para mejorar el porcentaje. La métrica no incluye toda la UI Compose, todos los métodos SQLite ni el backend.

Se mantienen las dependencias de pruebas, el dispatcher de corrutinas, los recursos Robolectric, JaCoCo 0.8.13, `isIncludeNoLocationClasses`, los filtros explícitos y la ubicación de los datos de cobertura. La comprobación del estado inicial confirma que `app/build.gradle.kts` no cambió durante esta revisión.

Instrumentación final: **80 pruebas aprobadas**, 0 fallidas, 0 errores y 0 omitidas. Incluye la consulta real a Open-Meteo desde el emulador, la sonda GET/403 a 8080 y la integración Retrofit → Spring Boot → SQLite con respuesta perdida/recuperación y administración sobre H2 temporal. Se corrigieron las expectativas antiguas de textos y títulos de marcadores; la comprobación nueva del recibo busca la etiqueta completa «Estado: Pendiente».

Compilación `:app:assembleDebug`: **BUILD SUCCESSFUL**. La APK generada es `app/build/outputs/apk/debug/app-debug.apk`. La release `app/release/app-release.apk` coincide con su contenido original versionado. **Incidente:** aunque las firmas debug coincidían, la limpieza automática de AGP desinstaló la app y eliminó los datos del AVD. La nueva propiedad de retención no recupera esos datos; se requiere una copia válida y no se afirma recuperación. Consultar el apartado inicial del informe final. `app/build.gradle.kts` se conserva; el cambio adicional está en `gradle.properties`.

`git diff --check`: **código 0**, sin errores; Git puede emitir avisos informativos de conversión LF/CRLF.

Las primeras ejecuciones detectaron dobles instrumentados sin los nuevos endpoints, llamadas antiguas al mapa y expectativas anteriores de textos/diálogos. Se corrigieron esas pruebas conservando sus comprobaciones de navegación, marcadores, estados y callbacks; no se eliminaron pruebas ni se sustituyeron por comprobaciones vacías. Los logs de las iteraciones se conservan como diagnóstico y no se presentan como aprobaciones.

Comandos reproducibles desde la raíz:

```powershell
.\gradlew.bat :server:test :pickup-service:test :notification-service:test :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:jacocoDebugLogicReport --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
# Emulador activo; sonda GET opcional a los servicios de la demostración.
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.oylReadOnlyProbeUrl=http://10.0.2.2:8080/' --console=plain
# Suite completa con las sondas opcionales y compras contra un backend aislado.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\ejecutar-integracion-android.ps1 -DeviceSerial emulator-NNNN
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\tests\comprobar-microservicios.Tests.ps1
git diff --check
```

El comando simple de instrumentación omite las dos sondas optativas si no se aportan sus argumentos. El helper crea una clave efímera en memoria para el backend de prueba y transmite el argumento mediante el entorno de Gradle, no por un archivo de credenciales. No usar como destino de esas compras la base del usuario.

Antes de instrumentar de nuevo, resolver el incidente descrito al comienzo, respaldar el AVD y seleccionar un emulador de pruebas dedicado. Sustituir `emulator-NNNN` por su serial real de `adb devices`. La última modificación del helper exige ese destino y activa explícitamente la retención de APK; su ejecución completa anterior verificó las 80 pruebas, pero la protección de retención añadida después no se ha ensayado de nuevo sobre datos del usuario.

## 6. Scripts y limpieza

- `comprobar-microservicios.ps1` conserva decodificación UTF-8, conteo correcto de arrays e igualdad del contenido de los puntos. Añade comparación independiente del orden de propiedades, validación de UUID, forma de respuesta y diagnóstico de conexión/timeout/HTTP 404/otros HTTP/JSON inválido. `-OrderId` exige UUID remoto, no el número `#5` de Android. Solo ejecuta GET.
- `iniciar-microservicios.ps1` preserva la lectura enmascarada de las claves y su herencia. Rechaza puertos ocupados, permite consolas visibles con `-VisibleConsoles`, usa ventanas ocultas de forma predeterminada y restaura las variables de la consola de origen. Se comprobaron sintaxis y rechazo de puertos ya ocupados; no se reiniciaron los servicios reales para ensayar un arranque adicional.
- `.gitignore` elimina dos líneas residuales de un comando PowerShell y cubre `**/build/`, `**/.gradle/`, metadatos/perfiles generados de release, logs, archivos H2 auxiliares y material privado de firma. No elimina archivos. Conserva fuentes, recursos, pruebas, wrappers y documentación.
- Los patrones existentes de APK/ZIP/keystore se mantienen; la APK release ya versionada sigue versionada e intacta. Los reportes generados se conservan en disco y deben copiarse a la entrega privada antes de ejecutar `clean`; no se confunden con código fuente.

## 7. Archivos y estado Git

Nuevos en esta revisión:

- `app/src/androidTest/java/com/example/oylfrutossecos/StorePresentationUiTest.kt`
- `app/src/main/java/com/example/oylfrutossecos/domain/StorePresentation.kt`
- `app/src/test/java/com/example/oylfrutossecos/NoticeHistoryPersistenceTest.kt`
- `app/src/test/java/com/example/oylfrutossecos/StorePresentationTest.kt`
- `notification-service/src/test/java/com/oyl/notifications/NotificationPersistenceTest.java`
- `pickup-service/src/test/java/com/oyl/pickup/PickupPersistenceTest.java`
- `scripts/tests/comprobar-microservicios.Tests.ps1`
- `scripts/tests/ejecutar-integracion-android.ps1`
- `server/src/test/java/com/oyl/frutossecos/api/ServiceCommunicationTest.java`
- `INFORME_VERSION_FINAL_CONTROL3.md`

Modificados en esta revisión:

- `.gitignore`
- `app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt`
- `app/src/androidTest/java/com/example/oylfrutossecos/PickupMapOverviewUiTest.kt`
- `app/src/androidTest/java/com/example/oylfrutossecos/PickupWeatherUiTest.kt`
- `app/src/androidTest/java/com/example/oylfrutossecos/SmallScreenUiTest.kt`
- `app/src/androidTest/java/com/example/oylfrutossecos/StoreRedesignUiTest.kt`
- `app/src/androidTest/java/com/example/oylfrutossecos/StubOylRestService.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/local/OylDatabase.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/model/Order.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/repository/CheckoutCoordinator.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/repository/InventoryRepository.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/repository/OrderRepository.kt`
- `app/src/main/java/com/example/oylfrutossecos/data/repository/ProductRepository.kt`
- `app/src/main/java/com/example/oylfrutossecos/domain/InventoryFormValidator.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/components/AdminAccessPanel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/components/StoreComponents.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/components/StoreText.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/navigation/StoreNavigation.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/notifications/DemoLocalNotifier.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/CartScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/CatalogScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/InventoryScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/NotificationsScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderConfirmationScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderSuccessScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/OrdersScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/PickupMapScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/QrScannerScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/RemoteCatalogScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/screens/StockAdjustmentScreen.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AdminSessionViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AuthViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/CatalogViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/InventoryViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/NotificationViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/OrderViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/RemoteCatalogViewModel.kt`
- `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/StockAdjustmentViewModel.kt`
- `app/src/test/java/com/example/oylfrutossecos/AdminSessionViewModelTest.kt`
- `app/src/test/java/com/example/oylfrutossecos/CatalogViewModelTest.kt`
- `app/src/test/java/com/example/oylfrutossecos/InventoryFormValidatorTest.kt`
- `app/src/test/java/com/example/oylfrutossecos/NotificationViewModelTest.kt`
- `app/src/test/java/com/example/oylfrutossecos/RemoteCatalogViewModelTest.kt`
- `scripts/comprobar-microservicios.ps1`
- `scripts/iniciar-microservicios.ps1`
- `server/src/test/java/com/oyl/frutossecos/api/SqlInitializationTest.java`
- `gradle.properties`
- `README.md`
- `MICROSERVICIOS_CONTROL3.md`
- `INFORME_COBERTURA_CONTROL3.md`

`app/build.gradle.kts` y las demás pruebas de cobertura anteriores se preservaron. Algunas pruebas nuevas de aquella revisión siguen apareciendo como `??` en Git porque todavía no se han agregado ni confirmado.

Los cambios previos de Gradle y las pruebas de cobertura de la revisión anterior permanecen en el árbol de trabajo. El estado final no está limpio porque deliberadamente no se ha hecho commit. Los builds de los nuevos módulos y metadatos release ya no aparecen como candidatos accidentales; permanecen físicamente en disco.

```text
 M .gitignore
 M MICROSERVICIOS_CONTROL3.md
 M README.md
 M app/build.gradle.kts
 M app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt
 M app/src/androidTest/java/com/example/oylfrutossecos/PickupMapOverviewUiTest.kt
 M app/src/androidTest/java/com/example/oylfrutossecos/PickupWeatherUiTest.kt
 M app/src/androidTest/java/com/example/oylfrutossecos/SmallScreenUiTest.kt
 M app/src/androidTest/java/com/example/oylfrutossecos/StoreRedesignUiTest.kt
 M app/src/androidTest/java/com/example/oylfrutossecos/StubOylRestService.kt
 M app/src/main/java/com/example/oylfrutossecos/data/local/OylDatabase.kt
 M app/src/main/java/com/example/oylfrutossecos/data/model/Order.kt
 M app/src/main/java/com/example/oylfrutossecos/data/repository/CheckoutCoordinator.kt
 M app/src/main/java/com/example/oylfrutossecos/data/repository/InventoryRepository.kt
 M app/src/main/java/com/example/oylfrutossecos/data/repository/OrderRepository.kt
 M app/src/main/java/com/example/oylfrutossecos/data/repository/ProductRepository.kt
 M app/src/main/java/com/example/oylfrutossecos/domain/InventoryFormValidator.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/components/AdminAccessPanel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/components/StoreComponents.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/components/StoreText.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/navigation/StoreNavigation.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/notifications/DemoLocalNotifier.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/CartScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/CatalogScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/HomeScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/InventoryScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/NotificationsScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderConfirmationScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/OrderSuccessScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/OrdersScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/PickupMapScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/QrScannerScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/RemoteCatalogScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/screens/StockAdjustmentScreen.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AdminSessionViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/AuthViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/CatalogViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/InventoryViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/NotificationViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/OrderViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/RemoteCatalogViewModel.kt
 M app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/StockAdjustmentViewModel.kt
 M app/src/test/java/com/example/oylfrutossecos/AdminSessionViewModelTest.kt
 M app/src/test/java/com/example/oylfrutossecos/InventoryFormValidatorTest.kt
 M gradle.properties
 M scripts/comprobar-microservicios.ps1
 M scripts/iniciar-microservicios.ps1
 M server/src/test/java/com/oyl/frutossecos/api/SqlInitializationTest.java
?? INFORME_COBERTURA_CONTROL3.md
?? INFORME_VERSION_FINAL_CONTROL3.md
?? app/src/androidTest/java/com/example/oylfrutossecos/StorePresentationUiTest.kt
?? app/src/main/java/com/example/oylfrutossecos/domain/StorePresentation.kt
?? app/src/test/java/com/example/oylfrutossecos/CartViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/CatalogViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/InventoryRepositoryTest.kt
?? app/src/test/java/com/example/oylfrutossecos/InventoryViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/LocalRepositoriesTest.kt
?? app/src/test/java/com/example/oylfrutossecos/LogicTestSupport.kt
?? app/src/test/java/com/example/oylfrutossecos/MicroserviceRetrofitFailureTest.kt
?? app/src/test/java/com/example/oylfrutossecos/NoticeHistoryPersistenceTest.kt
?? app/src/test/java/com/example/oylfrutossecos/NotificationViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/OrderRepositoryTest.kt
?? app/src/test/java/com/example/oylfrutossecos/OrderViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/ProductRepositoryTest.kt
?? app/src/test/java/com/example/oylfrutossecos/RemoteCatalogViewModelTest.kt
?? app/src/test/java/com/example/oylfrutossecos/RepositorySqliteSupport.kt
?? app/src/test/java/com/example/oylfrutossecos/StorePresentationTest.kt
?? app/src/test/java/com/example/oylfrutossecos/WeatherConditionsTest.kt
?? notification-service/src/test/java/com/oyl/notifications/NotificationPersistenceTest.java
?? pickup-service/src/test/java/com/oyl/pickup/PickupPersistenceTest.java
?? scripts/tests/
?? server/src/test/java/com/oyl/frutossecos/api/ServiceCommunicationTest.java
```

## 8. Evidencias y limitaciones

- [JaCoCo HTML](app/build/reports/jacoco/jacocoDebugLogicReport/html/index.html) y [XML](app/build/reports/jacoco/jacocoDebugLogicReport/jacocoDebugLogicReport.xml).
- [JUnit Android](app/build/reports/tests/testDebugUnitTest/index.html); reportes JUnit/HTML bajo `build/test-results/test` y `build/reports/tests/test` de cada servicio.
- [Instrumentación Compose/SQLite](app/build/reports/androidTests/connected/debug/index.html).
- Logs y contraste de clases/archivos: `build/final-polish/`; el informe de cobertura mantiene el historial de la revisión anterior.
- Las pruebas generaron capturas en las cachés `final-qa`, `redesign-qa` y `map-overview-qa`, pero no se exportaron antes de la limpieza del runner. No están disponibles después de la desinstalación y no se entregan como evidencias existentes. Deben capturarse nuevamente en un AVD seguro.

No se encontró la pauta oficial en el repositorio. No se certifica el cumplimiento completo de la evaluación ni se inventan integrantes, RUT, commits, tablero Trello o capturas de firma. Se debe contrastar con la pauta original el alcance de cobertura, requisitos grupales, formato de entrega y condiciones de publicación.

La aplicación se prepara para evaluación académica, **no para producción comercial**: autenticación local, clave administrativa compartida, consultas de pedidos por UUID sin identidad remota del cliente, HTTP limitado al emulador y ubicaciones/horarios de ejemplo siguen siendo limitaciones conocidas. Los teléfonos físicos requieren configuración y acceso seguro adicionales. No hay cobros, despachos ni reservas reales.

El catálogo conserva creación, consulta y edición, sin endpoint ni botón de eliminación de productos. Los puntos de retiro tienen CRUD por API; el mapa no incorpora formularios nuevos para administrarlos. No se agregaron esas funciones secundarias ni se afirma CRUD completo de todos los recursos desde la interfaz.

La APK debug se compiló para validación; la release anterior y el keystore no se reemplazaron. Una firma válida de un archivo anterior no acredita este código actualizado. La APK definitiva, ZIP y documentación privada de entrega deben actualizarse después de revisar los resultados.

## 9. Checklist de entrega y pasos manuales

- [x] Preservar rama y cambios locales; revisar UI y normalización sin migraciones destructivas.
- [ ] Resolver el incidente de limpieza del AVD: falta una copia válida para recuperar los datos Android anteriores; no confundirlo con la persistencia verificada en bases aisladas.
- [x] Comprobar contratos, autorización, compras, stock, reintentos y separación de usuarios con pruebas aisladas.
- [x] Verificar servicios activos, UTF-8, delegación y cuatro eventos del pedido indicado mediante GET.
- [x] Probar reinicios de cada H2 en archivos independientes y recuperación outbox.
- [x] Conservar alcance JaCoCo, medir instrucciones/líneas y ejecutar compilación debug.
- [x] Revisar scripts, `.gitignore` y documentación técnica sin secretos reales.
- [ ] Contrastar requisitos exactos con la pauta oficial.
- [ ] Revisar visualmente la app final con sus datos y registrar capturas reales de la demostración.
- [ ] Completar integrantes, distribución de tareas, enlace/captura real de Trello y datos personales solo en el documento privado.
- [ ] Autorizar por separado commit/push y generación de la APK definitiva.
- [ ] Generar release con el mismo keystore/alias originales, verificar con apksigner y actualizar ZIP/documentos/evidencias.

Procedimiento de cierre:

1. Si los servicios ya funcionan, no reiniciarlos. Ejecutar `powershell -ExecutionPolicy Bypass -File .\scripts\comprobar-microservicios.ps1`. Si están apagados, ejecutar `iniciar-microservicios.ps1 -VisibleConsoles`, introducir las mismas claves acordadas y esperar su arranque.
2. Antes de volver a usar el AVD afectado, identificar una copia válida o decidir explícitamente que era un entorno descartable de pruebas. No cargar imágenes antiguas encima ni presentar una instalación nueva como recuperación. Para la demostración posterior, abrir esta rama y ejecutar `app` en un AVD seguro. No desinstalar una app con datos para resolver diferencias de firma; usar otro AVD si corresponde.
3. Cliente: revisar Inicio, catálogo, detalle, una bolsa, carrito y confirmación; crear un pedido académico y comprobar que aparece como recibido/pendiente, no falsamente confirmado. Revisar número, importe CLP y disponibilidad.
4. ADMIN: Cuenta → Configuración administrativa → introducir la misma clave `OYL_ADMIN_KEY`, sin capturarla. Revisar pedidos, stock y motivos. Avanzar el pedido nuevo por Confirmado, Preparado y Entregado; elegir el pedido por cliente, fecha y productos cuando su referencia administrativa sea breve.
5. Cliente: actualizar Mis pedidos y expandir Seguimiento; revisar avisos nuevos y antiguos, lectura y ausencia de duplicados. No esperar que todos los eventos remotos se inserten en Notificaciones locales. Para el script, obtener el UUID completo mediante la consulta administrativa de la API, no a partir del número corto de Android.
6. Mapa: primera apertura Ver todos, elegir Centro y Ñuñoa, comprobar marcadores/centrado/clima, mover el mapa y volver a Ver todos. Revisar que “Ñuñoa” y el aviso de ubicaciones de ejemplo se lean completos.
7. QR: probar código OYL válido, texto incompatible, código de producto inexistente, cancelación y reintento. La lectura óptica real requiere cámara/Google Play; las pruebas simuladas no sustituyen esa captura manual.
8. Perfil: editar nombre/teléfono/dirección, volver a Cuenta, salir y entrar; comprobar persistencia y que otra cuenta no cambie. Cerrar sesión ADMIN y confirmar que se solicita validar nuevamente su acceso.
9. Copiar reportes, resultados y capturas reales a la carpeta privada de entrega antes de limpiar builds. Completar Word/Trello con datos reales, sin publicar RUT, claves ni keystores.
10. Después de la revisión conjunta, generar **APK release** con el keystore y alias originales hacia una ubicación nueva, conservando el APK anterior. Ejecutar `apksigner verify --verbose --print-certs RUTA_APK_NUEVA` y calcular su SHA-256. Verificar instalación en un AVD apropiado y actualizar los paquetes de Android y los tres servicios, con wrappers/archivos Gradle raíz. No sustituirla por debug ni usar otra identidad de firma.
