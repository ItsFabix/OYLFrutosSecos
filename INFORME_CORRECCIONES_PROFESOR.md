# Control 3 — Correcciones del profesor

Fecha: 23 de noviembre de 2026. Rama: `control3-integracion`.

Esta intervención incorpora las tres correcciones solicitadas. El árbol de trabajo ya contenía cambios de la integración anterior; la lista siguiente identifica exclusivamente los archivos intervenidos para estas correcciones. No se hicieron commits, push, merge ni cambios de rama.

## Comportamiento implementado

### Puntos de retiro

- Selector Material 3 **Centro / Ñuñoa**, siempre visible, con estado seleccionado y marca de verificación. Elegir un local limpia el filtro anterior y centra automáticamente el mapa en sus coordenadas existentes.
- Marcador seleccionado verde, mayor y opaco; los restantes tienen otro tamaño, relleno y opacidad. Nombre, referencia geográfica y horario siguen en la tarjeta del local.
- El mapa ocupa el espacio entre selector y tarjeta; se conservan gestos, controles, búsqueda, centrado e indicaciones. Se detiene cualquier animación de centrado anterior al cambiar de local para impedir que restaure la selección previa.
- Se conserva `onWeatherPoint` y el flujo Retrofit independiente de Open-Meteo: selección por coordenadas, carga/éxito/error y caché de 10 minutos. La respuesta del clima no bloquea el mapa.
- Coordenadas originales: Centro/Plaza de Armas `-33.4372, -70.6506`; Ñuñoa/Plaza Ñuñoa `-33.4554, -70.5982`. Son puntos de demostración en referencias geográficas existentes, no sucursales reales verificadas. Se conserva esa advertencia y no se inventan direcciones.

### Escáner QR

El resultado se muestra mediante `AlertDialog` Material 3, tanto para cámara como para búsqueda manual:

| Situación | Respuesta |
| --- | --- |
| Formato aceptado y producto presente | «¡Producto OYL reconocido!» y botón **Ver producto**. La navegación usa el ID local vinculado al ID REST. |
| QR legible pero formato incompatible | «QR escaneado, pero no compatible». Ofrece **Volver a escanear**. |
| Formato reconocido y catálogo disponible, producto ausente | «Producto no encontrado», con explicación específica. Permite reescanear o actualizar catálogo tras cerrar. |
| Catálogo cargando o con error | «Catálogo no disponible». Permite actualizar sin afirmar que el producto no existe. |
| Cancelación voluntaria | Sin diálogo ni mensaje de error; el botón queda disponible. |
| Fallo de cámara/servicio | Mensaje sobre cámara/Google Play y opción de reintentar. No muestra excepciones internas. |

Se conserva el contrato existente: `OYL:1`, `OYL:ID:1` y el ID numérico positivo sin prefijo. Solo se reconocen productos presentes en el catálogo, no texto arbitrario ni URLs. No se muestran los contenidos desconocidos ni se abren enlaces automáticamente. La cámara sigue usando Google Code Scanner, sin añadir permisos. Referencia del SDK: [CommonStatusCodes](https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes).

### Perfil

- **Cuenta → Editar perfil** abre nombre, teléfono y dirección precargados. No hay campos para rol, alias, correo, contraseña ni ID.
- Validación en ViewModel y almacenamiento: nombre de 2–80 caracteres; móvil chileno de nueve dígitos comenzando en 9, con prefijo `+56` opcional, espacios/guiones permitidos; dirección de 5–160 caracteres. Se recortan espacios exteriores al guardar.
- `AuthRepository` realiza la escritura en SQLite fuera del hilo principal. La transacción comprueba que la sesión activa sea del dueño del formulario; el `UPDATE` también exige esa relación. Una sesión distinta o cerrada rechaza la operación.
- Solo cambian las columnas `name`, `phone`, `address`. Se conservan ID, alias, correo, rol, salt/hash de contraseña, sesión, carrito, historial y solicitudes de checkout guardadas. No se necesita migración: esas tres columnas ya existían; el esquema sigue en versión 8.
- Tras guardar, se actualiza el usuario del `AuthViewModel`, se vuelve a Cuenta y aparece **Perfil actualizado**. Los nuevos checkout reciben el contacto actualizado; un intento pendiente conserva exactamente su solicitud original. Los pedidos anteriores mantienen su información histórica.
- Guardar bloquea dobles pulsaciones y la salida durante la escritura. Los errores conservan el formulario para reintentar. La edición funciona sin Spring Boot ni Internet.

## Archivos de esta intervención

Rutas de producción relativas a `app/src/main/java/com/example/oylfrutossecos/`:

| Archivo | Cambio |
| --- | --- |
| `MainActivity.kt` | Propaga el perfil actualizado a la sesión visible. |
| `data/local/OylDatabase.kt` | Actualización transaccional del perfil de la sesión activa. |
| `data/model/ProfileDraft.kt` — nuevo | Datos personales editables, sin identificadores ni credenciales. |
| `data/repository/AuthRepository.kt` | Persistencia del perfil en IO. |
| `domain/ProfileValidator.kt` — nuevo | Validaciones de datos personales. |
| `domain/QrFeedback.kt` — nuevo | Clasificación de lectura/cancelación/fallo y mensajes seguros. |
| `ui/navigation/StoreNavigation.kt` | Ruta del formulario, bloqueo durante guardado y actualización del usuario usado en futuros pedidos. |
| `ui/screens/AccountScreen.kt` | Acción Editar perfil y confirmación de guardado. |
| `ui/screens/EditProfileScreen.kt` — nuevo | Formulario con validaciones y estados de guardado/error. |
| `ui/screens/PickupMapScreen.kt` | Selector, marcadores, distribución y centrado. |
| `ui/screens/QrScannerScreen.kt` | Diálogos, reintento y cancelación silenciosa. |
| `ui/viewmodel/AuthViewModel.kt` | Actualiza el perfil de la misma identidad activa. |
| `ui/viewmodel/OrderViewModel.kt` | Actualiza los datos personales usados por futuros checkout sin recrear el historial. |
| `ui/viewmodel/ProfileViewModel.kt` — nuevo | Estado del formulario y guardado. |

Pruebas, en los paquetes equivalentes `app/src/test/java/com/example/oylfrutossecos/` y `app/src/androidTest/java/com/example/oylfrutossecos/`:

- Unitarias nuevas: `ProfileViewModelTest.kt`, `QrFeedbackTest.kt`.
- Instrumentadas nuevas: `QrFeedbackUiTest.kt`.
- Instrumentadas ampliadas: `AuthSqliteTest.kt`, `HomeNavigationTest.kt`, `PickupWeatherUiTest.kt`, `RestRepositoryIntegrationTest.kt`.
- Documentación: `README.md` y este informe.

No se modificaron archivos Spring Boot para esta intervención, ni los contratos Retrofit de pedidos o Open-Meteo. Tampoco se modificó el parser de formatos QR existente.

## Verificación

- `:app:testDebugUnitTest`: **122 pruebas, cero fallos/errores**. Incluye 11 nuevas de clasificación QR y validación/guardado del perfil.
- `:app:assembleDebug` y `:app:assembleDebugAndroidTest`: compilaciones satisfactorias.
- `:server:test --rerun-tasks`: **15 pruebas, cero fallos/errores**, ejecutadas nuevamente.
- Instrumentación final: **25 pruebas, cero fallos y ninguna omitida**, en `emulator-5554`. Incluye 5 de SQLite, 5 del QR, 4 de clima/mapa, 3 de pantallas pequeñas, 7 de navegación/perfil/administración y 1 de Retrofit real contra Spring Boot aislado.
- `git diff --check`: sin errores de formato. Los avisos LF/CRLF corresponden a la configuración Windows de Git.

La primera ejecución instrumentada detectó una carrera de centrado con una animación anterior. Se corrigió y la repetición de las cuatro pruebas del clima/mapa más la integración REST real pasó (**5/5**). La ejecución conjunta final posterior pasó **25/25**. No se eliminó ninguna prueba.

Las pruebas SQLite usan bases temporales independientes. La integración REST usa otro Spring Boot en el puerto `18080`, H2 **en memoria** y una clave pública exclusiva del fixture de pruebas; no usa datos ni clave administrativa del servidor habitual `8080`. Se instaló mediante `adb install -r` y se ejecutó instrumentación directamente para conservar los datos de la instalación del usuario.

Registros y artefactos locales:

- `build/professor-corrections/android-build.log` y `server-tests.log`.
- `build/professor-corrections/instrumentation.log`: primer intento, conserva el fallo encontrado.
- `build/professor-corrections/map-rest-recheck.log`: repetición mapa + Retrofit real, 5/5.
- `build/professor-corrections/instrumentation-final.log`: ejecución conjunta final.
- `build/professor-corrections/mapa-320dp.png` y `cuenta-320dp.png`: capturas de la interfaz revisadas visualmente.
- `app/build/reports/tests/testDebugUnitTest/index.html` y `server/build/reports/tests/test/index.html`.
- `app/build/outputs/apk/debug/app-debug.apk`: APK debug, no APK release de entrega.

Comandos de compilación/pruebas desde la raíz del repositorio en PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :server:test --rerun-tasks --console=plain
git diff --check
```

Las clases instrumentadas de cierre son `AuthSqliteTest`, `QrFeedbackUiTest`, `PickupWeatherUiTest`, `SmallScreenUiTest`, `HomeNavigationTest` y `RestRepositoryIntegrationTest`. La sonda de Open-Meteo se activa con `oylWeatherLiveProbe=true`. La prueba REST necesita `oylIntegrationBaseUrl=http://10.0.2.2:18080/` y `oylIntegrationAdminKey` del servidor aislado. Nunca ejecutar esa prueba de escritura contra una base de datos con pedidos del usuario.

## Prueba manual

Instalar/ejecutar la variante debug desde Android Studio, o instalar conservando los datos:

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

1. **Mapa:** iniciar sesión → Inicio o Cuenta → Puntos de retiro. Comprobar Centro seleccionado y su referencia. Pulsar Ñuñoa: debe cambiar selección, marcador, centro y referencia a Plaza Ñuñoa. El clima pasa a carga/dato de ese punto o muestra un error recuperable. Arrastrar, usar zoom y Centrar mapa; inmediatamente cambiar al otro local para comprobar que ninguna animación lo devuelve al anterior. Verificar también búsqueda y Cómo llegar. Al volver entre puntos, se reutiliza el clima cacheado vigente.
2. **QR:** con catálogo disponible, ir a QR. Escanear un QR con `OYL:<ID REST existente>`: debe aparecer la confirmación y abrir detalle solo al pulsar Ver producto. Probar un QR con texto/URL: diálogo de incompatibilidad, sin abrir navegador. Probar `OYL:<ID positivo inexistente>`: Producto no encontrado. Pulsar Volver a escanear y luego cancelar: no debe quedar un error. La búsqueda manual permite comprobar los mismos resultados escribiendo esos contenidos. Ante fallo real del servicio debe aparecer el mensaje de cámara/Google Play con reintento.
3. **Perfil:** Cuenta → Editar perfil. Probar un nombre de un carácter y un teléfono inválido: no se guardan y aparecen errores por campo. Ingresar datos válidos y Guardar cambios: vuelve a Cuenta con confirmación y nombre actualizado; Datos de la cuenta muestra teléfono/dirección nuevos. Cerrar sesión y volver a entrar con el mismo alias/contraseña; reiniciar el proceso sin borrar datos y comprobar persistencia. Entrar con otro usuario y confirmar que conserva sus propios datos. Carrito y pedidos del usuario anterior permanecen asociados a su ID. Si había un checkout pendiente, su recuperación conserva el contacto de la solicitud original.

## Límites

- Las pruebas del lector simulan los eventos de Google Code Scanner: verifican formato, resolución, diálogo, navegación, reintento y cancelación; queda la comprobación óptica con la cámara real y Google Play del dispositivo. No se afirma haber leído físicamente un QR con la cámara del emulador.
- El mapa usa puntos y horarios demostrativos ya existentes. Los mosaicos y datos meteorológicos nuevos necesitan Internet; la caché no garantiza disponibilidad offline de todas las teselas.
- La edición del perfil es local al dispositivo; no crea una autenticación remota ni sincronización de usuarios entre instalaciones.
- Los resultados de pruebas son conteos de ejecución, no un porcentaje de cobertura. Esta corrección no prepara ZIP final ni APK release firmada, ni cambia la arquitectura de microservicios pendiente de aclaración en la pauta.

## Actualización adicional — Vista general del mapa

Esta sección actualiza el comportamiento inicial del mapa descrito arriba. Al abrir o volver a entrar en **Puntos de retiro**, no hay un local seleccionado: **Ver todos** está activo, ambos marcadores tienen igual énfasis y la tarjeta muestra los nombres y referencias de Centro y Ñuñoa. Elegir un establecimiento resalta su marcador y muestra dirección, horario y clima; **Ver todos** regresa al conjunto completo y limpia la búsqueda anterior.

El encuadre usa `BoundingBox.fromGeoPoints` con todas las coordenadas existentes y `MapView.zoomToBoundingBox`, con un margen de 32 dp limitado por el tamaño disponible. No hay coordenadas centrales ni zoom constante para simular la vista general. El ajuste espera a que el `MapView` nativo tenga layout, dimensiones positivas y un tamaño que coincida con Compose durante dos frames: así no usa la altura intermedia anterior a la medición de la tarjeta. La documentación de [osmdroid 6.1.20](https://github.com/osmdroid/osmdroid/blob/osmdroid-parent-6.1.20/osmdroid-android/src/main/java/org/osmdroid/views/MapView.java) exige conocer ese tamaño antes de calcular el zoom.

Las órdenes de encuadre están separadas de la actualización de marcadores y solo se ejecutan al abrir, seleccionar un local, pulsar Ver todos o Centrar mapa, o usar la búsqueda. Las recomposiciones y respuestas del clima no emiten otra orden de encuadre. No se consulta clima al abrir la vista general; una respuesta pendiente del local anterior tampoco selecciona un establecimiento ni aparece como si fuera el clima de todos.

**Cómo llegar** y **Abrir mapa en el navegador** requieren seleccionar un establecimiento y se muestran deshabilitados en la vista general. **Centrar mapa** sigue disponible y vuelve a encuadrar el conjunto cuando Ver todos está activo. Se conservan gestos, zoom, búsqueda, coordenadas y colores. La modificación de producción se limita a `PickupMapScreen.kt`; no modifica REST, SQLite, QR, perfil, repositorios meteorológicos ni Spring Boot.

Archivos de esta actualización:

- `app/src/main/java/com/example/oylfrutossecos/ui/screens/PickupMapScreen.kt`.
- Nuevo `app/src/androidTest/java/com/example/oylfrutossecos/PickupMapOverviewUiTest.kt`: visibilidad de ambos marcadores con márgenes en píxeles, selección/destacado/centrado, vuelta al conjunto, tamaño real del viewport y conservación de cámara ante respuestas del clima y recomposiciones.
- `app/src/androidTest/java/com/example/oylfrutossecos/PickupWeatherUiTest.kt`: selecciona explícitamente el local para las pruebas de carga, éxito y error.
- `app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt`: salir y volver mediante el NavHost real recupera Ver todos.
- `README.md` y este informe.

Verificación final de esta actualización:

- `:app:testDebugUnitTest`: **122/122**, sin fallos ni errores.
- `:app:assembleDebug` y `:app:assembleDebugAndroidTest`: correctas.
- Instrumentación en `emulator-5554`: **11/11**, sin fallos ni omisiones. Incluye 3 pruebas nuevas de vista general, 4 de clima (con sonda Retrofit real Open-Meteo), 3 de pantallas pequeñas/teclado y 1 de salida y reentrada mediante el NavHost real.
- Las pruebas verifican posiciones de ambos marcadores dentro del viewport con margen, Centro y Ñuñoa centrados/destacados, Ver todos repetido tras mover la cámara, distintos tamaños del mapa y conservación del centro/zoom manual ante clima y recomposiciones.
- `git diff --check`: correcto.

Registros finales: `build/map-overview-qa/build-final.log`, `instrumentation-final.log` y `diff-check.log`. Capturas revisadas: `vista-general-320dp.png` y `nunoa-320dp.png` en ese directorio. Los primeros intentos instrumentados detectaron el uso de una altura intermedia del mapa; se conservan sus registros de diagnóstico y la ejecución final satisfactoria los reemplaza como resultado vigente. No se eliminó ninguna prueba existente ni se borraron datos de la app. La instalación se conservó con `adb install -r`.

Los resultados 122/25/15 de las secciones anteriores corresponden a la intervención anterior y se conservan como registro histórico. En esta actualización no se repitieron pruebas Spring Boot ni se modificó su código: el cambio de producción se limita al mapa Android.

Prueba manual de la vista general:

1. Ejecutar la APK debug en el emulador e iniciar sesión. Abrir Puntos de retiro desde Inicio o Cuenta: **Ver todos** debe aparecer seleccionado y ambos marcadores deben verse completos, con margen. La tarjeta identifica los dos puntos; no muestra el clima de un local elegido automáticamente.
2. Pulsar Centro y luego Ñuñoa: cada selección centra y destaca el marcador correspondiente y muestra sus datos y clima. El clima puede cargar o fallar sin impedir usar el mapa.
3. Pulsar Ver todos: reaparecen ambos marcadores dentro del encuadre general. Arrastrar/usar zoom y pulsar otra vez Ver todos o Centrar mapa para recuperar el conjunto.
4. Seleccionar un local, mover manualmente el mapa y esperar el resultado del clima: no debe reiniciarse el encuadre. En vista general tampoco debe cambiar la selección por una respuesta meteorológica anterior.
5. Volver a Inicio/Cuenta mediante Volver, entrar otra vez al mapa y comprobar que recupera la vista general. Probar búsqueda, zoom y los enlaces de indicaciones después de elegir un local.

## Última actualización — Privacidad y presentación del acceso administrativo

Se retiró el panel permanente de Inicio y las secciones principales. Solo ADMIN ve **Cuenta → Configuración administrativa**, con un formulario enmascarado para conectar, cambiar o desconectar el acceso. Tras conectar se oculta el campo y se muestra la confirmación; cambiar la clave requiere una acción explícita. CLIENTE no tiene la entrada ni la ruta de configuración.

Pedidos administrativos, inventario, edición de productos y movimientos ofrecen **Configurar acceso** únicamente cuando se intenta entrar sin autorización. Después de configurar, Volver retorna a la operación original y los datos se refrescan automáticamente. En Inicio los indicadores públicos siguen disponibles; los protegidos quedan sin valor hasta conectar, sin avisos técnicos permanentes.

La clave continúa solo en memoria, sin estado guardable ni cambios SQLite, preferencias, archivos o logs. Se limpia antes de invocar el cierre de sesión y también al desconectar o terminar el proceso. La navegación y la recreación de actividad conservan la autorización activa. Al reiniciar el proceso, Cuenta permite configurarla de nuevo conservando la sesión local.

`AdminAuthorizedService` delega al cliente REST existente, conserva las cabeceras HTTP y detecta exclusivamente 401/403 de operaciones administrativas. Invalida la credencial rechazada sin reintentar escrituras; errores de red/servidor no borran una sesión autorizada. Una respuesta tardía de una clave anterior no desconecta una clave de reemplazo. La validación inicial sigue consultando pedidos y movimientos antes de aceptar la clave. Spring Boot y su autorización HTTP no se modificaron.

Archivos de esta intervención:

- Producción: `ui/navigation/StoreNavigation.kt`, `ui/screens/AccountScreen.kt`, `ui/components/AdminAccessPanel.kt`, `ui/viewmodel/AdminSessionViewModel.kt` y nuevo `data/remote/AdminAuthorizedService.kt` (rutas relativas a `app/src/main/java/com/example/oylfrutossecos/`).
- Pruebas: nuevo `app/src/test/java/com/example/oylfrutossecos/AdminAuthorizedServiceTest.kt`; ampliados `AdminSessionViewModelTest.kt`, `app/src/androidTest/java/com/example/oylfrutossecos/HomeNavigationTest.kt` y `NavigationTestApi.kt`.
- Documentación: `README.md`, `INFORME_CONTROL3_INTEGRACION.md` y este informe, con los pasos actualizados.

No se modificaron Spring Boot, SQLite, Open-Meteo, mapas, QR ni el perfil; Cuenta solo incorpora la acción administrativa según el rol.

Verificación de esta intervención:

- **126/126 pruebas unitarias Android**, sin fallos/errores. Incluyen el paso de cabeceras HTTP, los seis endpoints administrativos, ausencia de reintentos automáticos y protección frente a respuestas tardías de una clave reemplazada.
- **16/16 pruebas instrumentadas de UI/navegación**: `HomeNavigationTest` (10) y `HomeScreenTest` (6), con repositorios/SQLite reales sobre archivos temporales y un servidor simulado protegido. Se verifican CLIENTE, ADMIN sin acceso, configuración contextual y desde Cuenta, clave incorrecta/reemplazo/desconexión, refresco de pedidos/inventario, navegación y recreación sin pedir otra clave, rechazo 403 y limpieza de la clave antes del callback de logout. Se conserva también el flujo cliente → pedido → administrador → cliente.
- **1/1 sonda Retrofit real de solo lectura** contra el servidor existente `8080`: salud/catálogo disponibles y HTTP 403 en pedidos/movimientos sin credencial. La sonda opcional se ejecutó separadamente con `oylReadOnlyProbeUrl=http://10.0.2.2:8080/`.
- APK debug y APK de instrumentación compiladas; debug instalada conservando datos con `adb install -r`.
- `git diff --check`: correcto. Capturas de Inicio y Cuenta revisadas a 320 dp.

Registros: `build/admin-settings-qa/build.log`, `instrumentation.log`, `server-read-only.log`, `diff-check.log`; capturas `inicio-admin-320dp.png` y `cuenta-admin-320dp.png` en ese directorio. El servidor real solo recibió GET sin clave; no se leyó su clave ni se alteraron pedidos, existencias o base persistente. Las operaciones administrativas de UI se comprobaron contra el doble protegido de pruebas. No hubo commit, push ni merge.

Prueba manual: entrar como CLIENTE y comprobar que Cuenta no ofrece configuración; entrar como ADMIN y comprobar Inicio limpio; abrir Inventario/Pedidos sin acceso y usar Configurar acceso, o entrar desde Cuenta; introducir la clave del servidor y conectar; volver a Inventario/Pedidos para comprobar datos sin otra solicitud de clave; cambiar la clave desde Cuenta si corresponde; cerrar sesión y volver a entrar para comprobar que requiere configurar nuevamente el acceso. Reiniciar solo el proceso conserva SQLite y elimina la clave en memoria. No borrar datos de la aplicación.
