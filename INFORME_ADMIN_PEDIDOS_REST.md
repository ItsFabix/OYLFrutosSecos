# Integracion de pedidos REST: informe de entrega

Rama de trabajo: `admin-pedidos-rest`, creada desde el `main` local porque no existia localmente ni en `origin` al comenzar. Sin commit, push ni merge.

## Comportamiento implementado

- `GET /api/admin/orders` devuelve ID, requestId, cliente, productos, cantidades, precios, total, modalidad, contactos y estado. Reutiliza la validacion de `OYL_ADMIN_KEY` / `X-OYL-ADMIN-KEY` del controlador administrativo. Sin clave, clave incorrecta o configuracion menor de 16 caracteres: HTTP 403.
- Android utiliza ese listado para el administrador y el endpoint existente `POST /api/admin/orders/{id}/advance` para PENDING -> CONFIRMED -> PREPARED -> DELIVERED. Entregado no ofrece otro avance.
- El administrador introduce la clave en un campo oculto y pulsa **Usar clave y consultar**. El campo se vacia al enviar; la clave queda exclusivamente en memoria del ViewModel. **Olvidar clave**, cerrar sesion o salir de la composicion de la sesion administrativa limpian esa referencia y cancelan sus operaciones. No se usa SQLite, SharedPreferences, SavedStateHandle ni archivos para la clave.
- Los pedidos se actualizan al entrar a la pantalla, al pulsar **Actualizar pedidos** y despues de avanzar. Los filtros existentes permiten ver los pendientes.
- Para el cliente, cada pedido REST vinculado al historial SQLite se consulta mediante `GET /api/orders/{id}`. Sus datos y su estado visibles provienen de la respuesta del servidor.
- El historial combina las filas puramente locales con una sola fila por ID REST. Las copias SQLite de pedidos remotos se reemplazan visualmente por la respuesta REST. Las claves de filas Compose incluyen el origen para evitar colisiones con IDs locales.
- Ante fallos HTTP o de conexion se muestra un mensaje y se conserva el historial SQLite, con advertencia de que los estados REST no estan actualizados. Se bloquea el avance REST hasta consultar otra vez con exito. Los pedidos puramente locales conservan su avance y sus notificaciones sin requerir clave REST.

## Archivos de implementacion modificados

| Archivo | Cambio |
| --- | --- |
| `server/src/main/java/com/oyl/frutossecos/api/AdminController.java` | Listado administrativo protegido; conserva el endpoint de avance y su autorizacion. |
| `server/src/main/java/com/oyl/frutossecos/api/OrderService.java` | Lectura de todos los pedidos y sus lineas; sin cambios en creacion, locks ni descuento de stock. |
| `app/src/main/java/com/example/oylfrutossecos/data/remote/OylRestService.kt` | Retrofit para listado y avance con cabecera por llamada; modalidad y contacto en respuesta. |
| `app/src/main/java/com/example/oylfrutossecos/data/repository/OrderRepository.kt` | Consulta REST, combinacion sin duplicados, avance por origen y mensajes de error. |
| `app/src/main/java/com/example/oylfrutossecos/ui/viewmodel/OrderViewModel.kt` | Clave en memoria, limpieza, carga y avance con refresco, estado de disponibilidad REST. |
| `app/src/main/java/com/example/oylfrutossecos/ui/screens/OrdersScreen.kt` | Entrada de clave, actualizacion manual y botones de avance REST/local. |
| `app/src/main/java/com/example/oylfrutossecos/ui/navigation/StoreNavigation.kt` | Limpieza al cerrar sesion/salir y consulta al entrar al historial. |

Se reviso `OylDatabase.kt` y se conservaron sus metodos y esquema sin modificaciones. Tampoco se modificaron `CheckoutCoordinator`, `RemoteCheckoutCache`, la recuperacion de solicitudes pendientes, el carrito, los movimientos de inventario ni la autenticacion local.

## Pruebas agregadas o adaptadas

- `server/src/test/java/com/oyl/frutossecos/api/AdminOrdersTest.java`: listado y contenido, ausencia de clave, clave incorrecta, configuracion vacia/corta, transiciones, lectura del estado por el cliente, ultimo estado sin avance, ID inexistente y stock constante durante el avance. La clave de estas pruebas es un valor ficticio exclusivo de tests.
- `app/src/test/java/com/example/oylfrutossecos/OrderHistoryTest.kt`: deduplicacion, conservacion de pedidos locales y autoridad del estado REST en todas las transiciones.
- `app/src/androidTest/java/com/example/oylfrutossecos/AdminOrdersUiTest.kt`: botones Compose, recorrido de estados, bloqueo REST sin consulta vigente, avance local y vaciado del campo de clave.
- `app/src/androidTest/java/com/example/oylfrutossecos/CheckoutSqliteRecoveryTest.kt`: nuevas pruebas con SQLite real de avance local y notificaciones, integracion Repository/ViewModel con servicio simulado, deduplicacion, refresco del cliente, caida de servidor y limpieza de clave. Se adapto el servicio simulado a la interfaz ampliada.
- `app/src/test/java/com/example/oylfrutossecos/CheckoutRecoveryTest.kt`: adaptacion del servicio simulado a la interfaz ampliada; mantiene sus comprobaciones de recuperacion.

## Ejecucion de validaciones

El Java predeterminado del equipo es Java 25 y fallo al arrancar Gradle. Se utilizo el JDK 17 ya instalado:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :server:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
git diff --check
```

- Spring Boot: 10 pruebas, 0 fallos (incluidas 3 pruebas administrativas nuevas).
- Android JVM: 87 pruebas, 0 fallos (incluidas 3 pruebas nuevas de historial); repetidas tras los ultimos ajustes.
- APK debug y APK instrumentado: compilacion correcta con JDK 17; regenerados tras los ultimos ajustes.
- Suite instrumentada completa: 35 pruebas, 0 fallos en el AVD Pixel_7, iniciado sin ventana para esta validacion.
- Repeticion final de las dos clases instrumentadas afectadas: 11 pruebas, 0 fallos; incluye limpieza de clave en ViewModel y vaciado del campo oculto.
- `git diff --check`: sin errores.

La repeticion de las pruebas afectadas utiliza:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.oylfrutossecos.AdminOrdersUiTest,com.example.oylfrutossecos.CheckoutSqliteRecoveryTest' --console=plain
```

El informe HTML instrumentado refleja la ultima ejecucion (la repeticion de las clases afectadas).

Artefactos:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- Informes: `app/build/reports/tests/testDebugUnitTest/index.html`, `server/build/reports/tests/test/index.html` y `app/build/reports/androidTests/connected/debug/index.html`.

## Pasos exactos en Android Studio

1. Abrir este directorio como proyecto y comprobar que la rama seleccionada es `admin-pedidos-rest`. En Settings > Build, Execution, Deployment > Build Tools > Gradle, seleccionar JDK 17 para Gradle. Sincronizar el proyecto.
2. En la terminal PowerShell de Android Studio, desde la raiz, iniciar Spring Boot con una clave temporal elegida para esta demostracion. Usar al menos 16 caracteres ASCII, sin espacios iniciales/finales. El comando pide la clave oculta y no la escribe en un archivo ni como literal del historial:

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
   $claveDemo = Read-Host 'Clave administrativa temporal (minimo 16 caracteres ASCII)' -AsSecureString
   $env:OYL_ADMIN_KEY = [System.Net.NetworkCredential]::new('', $claveDemo).Password
   try {
       .\gradlew.bat :server:bootRun --console=plain
   } finally {
       Remove-Item Env:OYL_ADMIN_KEY -ErrorAction SilentlyContinue
       $claveDemo.Dispose()
   }
   ```

3. Esperar que el servidor escuche en `127.0.0.1:8080`. Abrir Device Manager, iniciar un emulador y ejecutar la configuracion **app**. Retrofit ya apunta a `http://10.0.2.2:8080/`, que representa el PC anfitrion desde el emulador.
4. Registrarse o iniciar sesion como cliente local. Agregar un producto con stock al carrito, revisar las cantidades, seleccionar retiro o entrega y pulsar **Confirmar pedido**. Anotar el ID del servidor mostrado al finalizar. Si existe una solicitud pendiente, usar **Recuperar pedido pendiente**; no borrar datos para intentar recrearla.
5. Ir a **Cuenta > Mis pedidos**. Verificar que el ID REST aparece una sola vez y su estado inicial es **Pendiente de validacion**.
6. Cerrar sesion del cliente. Iniciar sesion con el administrador local de demostracion: alias `admin`, contrasena `Admin1234`. Ir a **Cuenta > Gestionar pedidos**.
7. Introducir la misma clave temporal del servidor y pulsar **Usar clave y consultar**. Filtrar pendientes si se desea. Buscar el ID anotado y comprobar cliente, productos, cantidades, total y modalidad.
8. Pulsar **Marcar como Confirmado**. Esperar el refresco y comprobar el nuevo estado. Continuar con **Marcar como Preparado** y **Marcar como Entregado**. El ultimo estado ya no ofrece boton. Si se mantiene un filtro de estado anterior, el pedido sale de ese filtro; seleccionar **Todos** para verlo.
9. Cerrar sesion del administrador, volver al cliente original y abrir **Mis pedidos**. Pulsar **Actualizar pedidos** y comprobar **Entregado**, con una sola fila para ese ID. Para observar cada estado desde ambas cuentas, alternar sesiones despues de cada avance; introducir nuevamente la clave al volver al administrador.
10. Verificar errores: introducir una clave incorrecta y comprobar el mensaje de autorizacion; detener Spring Boot con Ctrl+C y pulsar **Actualizar pedidos** para ver el mensaje de conexion y el historial local. Reiniciar con la misma clave, introducirla otra vez si es necesario y reintentar. Los pedidos REST sin consulta vigente no permiten avanzar.
11. Si existen pedidos puramente locales del historial anterior, comprobar que mantienen su boton sin clave REST. La prueba instrumentada tambien crea y avanza un pedido local en una base aislada y comprueba sus notificaciones.
12. Cerrar sesion del administrador y volver a entrar: sera necesario introducir de nuevo la clave. No guardar la clave en codigo, configuraciones de ejecucion versionadas ni archivos del proyecto.

## Limitaciones

- Es una demostracion local. La clave compartida administrativa y el inicio de sesion SQLite **no constituyen autenticacion segura de produccion**. Se conserva la proteccion administrativa existente; no se incorporaron secretos al APK ni al repositorio.
- No hay push, sondeo automatico ni sincronizacion continua entre dispositivos. El cliente consulta los IDs REST vinculados a su historial local; reinstalar/borrar datos o cambiar de dispositivo no recupera automaticamente ese historial. El administrador si lista todos los pedidos del servidor.
- Si falla una consulta REST, se muestra la copia SQLite con advertencia; su estado puede ser antiguo. Los pedidos que solo existen en el servidor requieren una consulta administrativa correcta para aparecer.
- La clave se pierde al destruir la sesion/composicion administrativa o el proceso, y puede requerir reingreso tras recrear la actividad.
- La configuracion de red actual corresponde al emulador. Un telefono fisico requiere otra URL y configuracion de acceso al servidor; no se cambio esa configuracion.
- Las pruebas HTTP usan Spring MockMvc con H2 aislado. Las pruebas del Repository usan SQLite real y un servicio simulado; las de Compose verifican interacciones y estados. No equivalen a un recorrido manual completo con Android conectado al Spring Boot real.
