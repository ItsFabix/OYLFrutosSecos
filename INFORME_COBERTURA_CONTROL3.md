> **Actualización 25/11/2026:** el bloque del 24 de noviembre se conserva como registro histórico. La medición vigente aparece en la actualización al final y en [el informe final](INFORME_VERSION_FINAL_CONTROL3.md).

> **Reejecución de cierre del 25/11:** las cuatro tareas Test se forzaron sin caché (221 Android JVM, 20 ventas, 4 retiro y 4 notificaciones), todas aprobadas. JaCoCo se regeneró: instrucciones 91,57 %, líneas 93,07 %, ramas 83,32 %, métodos 91,59 %, 115 clases. Se mantienen los 80 resultados instrumentados anteriores; no se reejecutaron sobre AVD con datos. Las 15 comprobaciones PowerShell y las 20 comprobaciones HTTP/H2 aisladas se contabilizan aparte.

# Control 3: pruebas y cobertura de lógica Android

Fecha: 24 de noviembre de 2026. Rama: `control3-tres-microservicios`.

## Resultado verificado

Se agregaron **82 casos JUnit Android**: 52 métodos de prueba, uno de ellos parametrizado con 31 combinaciones de códigos meteorológicos y etiquetas. Se conservan las 127 pruebas anteriores. Las **209 pruebas Android y 23 pruebas Spring Boot pasan**, sin fallos, errores ni pruebas omitidas.

El objetivo de al menos 80 % de instrucciones y líneas se cumple, incluso por separado en los tres paquetes evaluados. También se supera el margen deseado de 85 %.

| Métrica JaCoCo | Antes | Después | Elementos cubiertos / total final |
| --- | ---: | ---: | ---: |
| Instrucciones | 48,49 % | **91,27 %** | 10.199 / 11.175 |
| Líneas | 46,85 % | **92,64 %** | 1.133 / 1.223 |
| Ramas | 55,53 % | **82,79 %** | 741 / 895 |
| Métodos | 45,43 % | **91,36 %** | 370 / 405 |
| Complejidad | 45,45 % | 82,17 % | 696 / 847 |
| Clases | 39,47 % | 92,98 % | 106 / 114 |

Porcentaje = cubiertos / (cubiertos + no cubiertos), calculado desde los contadores XML; no se deduce del número de pruebas aprobadas.

| Paquete | Instrucciones antes → después | Líneas antes → después | Ramas después | Métodos después |
| --- | ---: | ---: | ---: | ---: |
| `domain` | 90,50 % → **93,40 %** | 84,53 % → **98,90 %** | 90,48 % | 92,31 % |
| `ui.viewmodel` | 43,43 % → **89,62 %** | 40,92 % → **89,93 %** | 77,08 % | 90,15 % |
| `data.repository` | 32,32 % → **94,29 %** | 38,24 % → **97,06 %** | 85,86 % | 94,38 % |

## Alcance y fiabilidad de la medición

Se mantienen los mismos filtros de clases: `**/domain/**`, `**/data/repository/**` y `**/ui/viewmodel/**`, en las mismas carpetas de clases Kotlin y Java. Se compararon ambos XML: coinciden los nombres de las 114 clases y los totales de instrucciones, líneas, ramas, métodos y complejidad. No se eliminó ninguna clase del denominador.

Esta cobertura corresponde a esos paquetes de lógica, **no al conjunto de toda la aplicación**, las pantallas Compose, todos los métodos de SQLite ni el código Java de los microservicios.

Se conservaron las correcciones locales de Gradle: JaCoCo 0.8.13, filtros explícitos en lugar de `logicalPackages` y lectura de datos desde `outputs/unit_test_code_coverage/debugUnitTest`.

Se habilitaron recursos Android para Robolectric y se añadieron dependencias exclusivas de pruebas: MockK 1.13.13, Robolectric 4.14.1 y kotlinx-coroutines-test 1.8.1. Además, se habilitó `isIncludeNoLocationClasses`: sin ello, las pruebas SQLite pasaban pero las clases cargadas por el sandbox de Robolectric no aportaban cobertura al informe. La exclusión de instrumentación `jdk.internal.*` corresponde únicamente a infraestructura del JDK, fuera de los paquetes evaluados. No excluye lógica OYL ni altera el alcance del reporte.

Referencias técnicas: [opción de instrumentación en Gradle](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.testing.jacoco.plugins/-jacoco-task-extension/is-include-no-location-classes.html), [pruebas con Robolectric](https://robolectric.org/getting-started/), [control de corrutinas](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/).

## Pruebas incorporadas

Los siguientes archivos son nuevos, bajo `app/src/test/java/com/example/oylfrutossecos/`:

| Archivo | Casos | Comprobaciones principales |
| --- | ---: | --- |
| `ProductRepositoryTest.kt` | 4 | Catálogo remoto y caché; separación de productos locales; alta/edición; fotografía privada local; stock del servidor; validaciones, autorización, HTTP y cancelación. |
| `OrderViewModelTest.kt` | 6 | Historial, carga y recuperación; doble confirmación; callbacks; errores de compra; identidad/rol; contactos actualizados; avance administrativo y frescura de datos. |
| `OrderRepositoryTest.kt` | 6 | Pedido y descuento simulado remoto; SQLite real; aislamiento por usuario; historial local/remoto separado; estados; respuesta perdida y recuperación con JSON idéntico después de reabrir la base; fallos REST. |
| `RemoteCatalogViewModelTest.kt` | 5 | Salud y catálogo; carga; errores y cancelación; UUID persistido antes de enviar; recuperación entre instancias; fallo de almacenamiento; bloqueo durante envío; compra confirmada con fallo posterior de actualización de stock. |
| `InventoryViewModelTest.kt` | 4 | Productos/movimientos; errores de autorización y red; ajustes; actualización de stock; doble envío; cancelación y limpieza administrativa. |
| `InventoryRepositoryTest.kt` | 4 | Mapeo de IDs locales/remotos; filtro de movimientos; entrada/salida; límites y notas; clave obligatoria; errores HTTP; escritura confirmada aunque falle la recarga. |
| `NotificationViewModelTest.kt` | 5 | Avisos por propietario; no leídos; marcar uno/todos; selección por tipo y fecha; errores; permiso Android/callback; doble envío y cancelación. |
| `LocalRepositoriesTest.kt` | 6 | AuthRepository, CartRepository y NotificationRepository con SQLite: validaciones, alias duplicado, rol, sesión, logout, perfil tras reabrir la base, usuarios separados, cantidades y bloqueo de checkout pendiente. |
| `CartViewModelTest.kt` | 5 | Carga y totales; bloqueo pendiente; cantidades; altas; carrito vacío; conflictos, validaciones, excepciones y cancelación. |
| `CatalogViewModelTest.kt` | 5 | Filtros, carga, caché y advertencia; validación de formulario; guardado y doble envío; HTTP 403 y cancelación. |
| `WeatherConditionsTest.kt` | 31 | Etiqueta española exacta para cada código WMO admitido y dos códigos desconocidos. |
| `MicroserviceRetrofitFailureTest.kt` | 1 | HTTP 503 de puntos de retiro; HTTP 404 de eventos; respuesta vacía válida; rutas/métodos correctos sin enviar secretos internos al cliente. |
| **Total nuevo** | **82** | Casos con comprobaciones de comportamiento y estado. |

Archivos auxiliares nuevos: `LogicTestSupport.kt` (dispatcher Main controlado, fixtures y liberación de ViewModels) y `RepositorySqliteSupport.kt` (base SQLite temporal exclusiva de cada prueba y doble REST). La base usa rutas temporales cortas para evitar el límite de rutas de SQLite en Windows; nunca accede al singleton ni a datos del usuario o del emulador. Para probar reinicios se cierra y vuelve a abrir el archivo de prueba con otro helper.

Otros archivos modificados por esta tarea: `app/build.gradle.kts` y este informe. No se modificaron archivos de producción Kotlin/Java, migraciones, credenciales ni firma. Los archivos de release y directorios `build` ya presentes fuera de seguimiento no forman parte de estos cambios.

## Ejecución final

Se ejecutó iterativamente el comando solicitado:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugLogicReport --console=plain
```

Tras alcanzar el objetivo se ejecutó la suite completa, forzando la ejecución de cada tarea de pruebas para evitar reutilizar resultados anteriores:

```powershell
.\gradlew.bat :server:test --rerun :pickup-service:test --rerun :notification-service:test --rerun :app:testDebugUnitTest --rerun --console=plain
```

Resultado: `BUILD SUCCESSFUL`. Después se regeneró el reporte con el primer comando sobre los datos de esa última ejecución; Android figuró `UP-TO-DATE` porque sus pruebas acababan de ejecutarse.

| Módulo | Casos ejecutados | Fallos | Errores | Omitidos |
| --- | ---: | ---: | ---: | ---: |
| Android `app` | 209 | 0 | 0 | 0 |
| Ventas `server` | 17 | 0 | 0 | 0 |
| `pickup-service` | 3 | 0 | 0 | 0 |
| `notification-service` | 3 | 0 | 0 | 0 |
| **Total** | **232** | **0** | **0** | **0** |

Las pruebas existentes de los servicios verifican SQL/H2, productos, pedidos, stock y recuperación; outbox atómico y no duplicado; continuidad del checkout sin servicios auxiliares; CRUD/autorización/validación de puntos de retiro; y autorización, idempotencia y CRUD de eventos. `RetrofitContractTest` conserva las comprobaciones exitosas de las nuevas rutas Android; se añadieron sus casos de error sin modificar las pruebas anteriores.

También se ejecutó `git diff --check` sin incidencias. No se hizo commit, push, merge ni cambio de rama.

## Evidencias locales

- [Reporte HTML JaCoCo](app/build/reports/jacoco/jacocoDebugLogicReport/html/index.html).
- [Reporte XML JaCoCo](app/build/reports/jacoco/jacocoDebugLogicReport/jacocoDebugLogicReport.xml).
- [Pruebas Android](app/build/reports/tests/testDebugUnitTest/index.html).
- [Pruebas ventas](server/build/reports/tests/test/index.html).
- [Pruebas puntos de retiro](pickup-service/build/reports/tests/test/index.html).
- [Pruebas notificaciones](notification-service/build/reports/tests/test/index.html).
- Resultados JUnit XML: `app/build/test-results/testDebugUnitTest/` y `<servicio>/build/test-results/test/`.
- Respaldo XML inicial: `build/coverage-control3/before.xml`; contraste de contadores y clases: `build/coverage-control3/final-evidence.json`.
- Logs de ejecución final: `build/coverage-control3/final-suite.log` y `build/coverage-control3/final-coverage.log`.

Los archivos bajo `build` son evidencias generadas locales; un `clean` puede eliminarlos. Para entregar evidencias, conservar una copia de los reportes y logs antes de limpiar. Las rutas estándar de reportes se regeneran con los comandos anteriores.

## Alcance pendiente y pauta

No se encontró la pauta oficial en el repositorio. `MICROSERVICIOS_CONTROL3.md` menciona 80 % de lógica Android, pero es documentación del proyecto, no una copia acreditada de la pauta. Se verificó el objetivo explícito del usuario: al menos 80 % tanto en instrucciones como en líneas del alcance JaCoCo actual. Confirmar con la pauta original si el docente exige otra métrica o un alcance adicional.

Quedan 976 instrucciones, 90 líneas, 154 ramas y 35 métodos sin cubrir. Entre los puntos restantes están varias factories/constructores, partes de AuthViewModel (incluido su flujo de logout), `ProductFormViewModel.cameraFailed`, y variantes de errores/condiciones del checkout. La cobertura de ramas de ViewModels es 77,08 %; no se presenta como superior a 80 %. El objetivo solicitado para instrucciones y líneas sí se cumple con margen.

Esta ejecución verifica pruebas JVM Android (incluido SQLite con Robolectric), contratos HTTP simulados y pruebas Spring Boot/MockMvc. No sustituye pruebas instrumentadas en un dispositivo, una demostración manual ni un ensayo de comunicación de los tres procesos desplegados simultáneamente. No se recompiló ni se firmó una APK durante esta tarea de pruebas.


## Actualización de presentación y verificación — 25/11/2026

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

Instrumentación final: **80 pruebas aprobadas**, 0 fallidas, 0 errores y 0 omitidas. Incluye la consulta real a Open-Meteo desde el emulador, la sonda GET/403 a 8080 y la integración Retrofit → Spring Boot → SQLite con respuesta perdida/recuperación y administración sobre H2 temporal. Se corrigieron las expectativas antiguas de textos y títulos de marcadores; la comprobación nueva del recibo busca la etiqueta completa «Estado: Pendiente».

Compilación `:app:assembleDebug`: **BUILD SUCCESSFUL**. La APK generada es `app/build/outputs/apk/debug/app-debug.apk`. La release `app/release/app-release.apk` coincide con su contenido original versionado. **Incidente:** aunque las firmas debug coincidían, la limpieza automática de AGP desinstaló la app y eliminó los datos del AVD. La nueva propiedad de retención no recupera esos datos; se requiere una copia válida y no se afirma recuperación. Consultar el apartado inicial del informe final. `app/build.gradle.kts` se conserva; el cambio adicional está en `gradle.properties`.

`git diff --check`: **código 0**, sin errores; Git puede emitir avisos informativos de conversión LF/CRLF.

Se conservan las 114 clases evaluadas anteriormente y se agrega `StorePresentation`, para 115 clases en el contador CLASS. El XML conserva todas las entradas de clase anteriores; las entradas sintéticas sin contadores ya existentes no alteran ese recuento. El alcance y Gradle permanecen intactos. El nuevo denominador refleja lógica de presentación legítima, además de cambios de mensajes: 11.583 instrucciones, 1.284 líneas, 989 ramas y 416 métodos.

Hay 977 instrucciones, 89 líneas, 165 ramas y 35 métodos sin cubrir. Las ramas de ViewModels quedan en 77,10 %; no se afirma cobertura de ramas superior al 80 % en cada paquete. Todos los paquetes superan el 85 % tanto en instrucciones como en líneas.

Las pruebas y la cobertura no acreditan por sí solas la totalidad de la pauta académica, que no está disponible en el repositorio. Ver archivos, comandos, límites y evidencias en el informe final.
