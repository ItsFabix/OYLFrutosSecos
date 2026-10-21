# OYL Frutos Secos

Proyecto universitario de **Desarrollo de Aplicaciones Móviles, DSY1105**. Aplicación Android para explorar un catálogo ficticio de frutos secos, gestionar un carrito y demostrar compras mediante una API REST local. Incluye administración local de productos e inventario. No procesa pagos reales.

## Integrantes

- Integrante 1: Fabian Parrao
- Integrante 2: Benjamin Rojas
- Docente: Cristian Gomez

## Tecnologías y estructura

- Kotlin 2.0.21, Jetpack Compose, Material 3, Navigation Compose y ViewModel/StateFlow; arquitectura MVVM con repositorios y validaciones de dominio.
- SQLite mediante `SQLiteOpenHelper` para usuarios, sesión, productos, carrito por usuario, historial, movimientos, notificaciones e intentos de compra pendientes. La base actual tiene versión 7 y migraciones existentes.
- Retrofit 2.11.0 y Gson para consultar catálogo y confirmar compras REST.
- Cámara externa mediante `TakePicture`, `FileProvider` y fotografías en almacenamiento privado de la app.
- Google Code Scanner 16.1.0 para QR; osmdroid 6.1.20 y OpenStreetMap para mapas.
- Backend Java con Spring Boot 3.5.7, Spring JDBC y H2 persistente; JUnit 4 en Android, JUnit 5/Spring Boot Test en servidor y pruebas de interfaz con Compose.

Directorios: `app/` (Android), `server/` (API), `app/src/test/` (pruebas JVM), `app/src/androidTest/` (pruebas en dispositivo) y `server/src/test/` (pruebas del backend).

## Funcionalidades implementadas

### Cliente

- Bienvenida animada, registro con validaciones por campo e inicio de sesión local según rol.
- Inicio con saludo, productos destacados disponibles, resumen del carrito y accesos a catálogo, pedidos, notificaciones y mapa.
- Catálogo con búsqueda, categorías, disponibilidad y detalle del producto.
- Escaneo QR para abrir productos; entrada manual alternativa, por ejemplo `OYL:1` para el identificador 1.
- Carrito persistente por usuario y confirmación del pedido contra Spring Boot. El intento conserva en SQLite su identificador y JSON; una compra interrumpida se puede recuperar desde el carrito reutilizando la solicitud original.
- Historial local de pedidos, consulta de datos de la cuenta y cierre de sesión.

### Administrador

- Inicio con total de productos locales, agotados, stock bajo (1 a 5 bolsas) y pedidos pendientes del historial local, con identificación explícita de datos de demostración.
- Creación y edición local de productos con validaciones independientes, captura de fotografías y conservación de la imagen existente al cancelar la cámara.
- Inventario local con entradas, salidas, validación de cantidades y motivo, e historial de movimientos.
- Consulta del historial de pedidos del dispositivo. Los pedidos históricos de demostración locales permiten avanzar su estado; las copias de pedidos REST se distinguen por su identificador remoto.

### Recursos nativos

- **Cámara:** utiliza la aplicación de cámara instalada y un URI de `FileProvider`. Las fotos permanecen privadas en el dispositivo y no se suben a Spring Boot.
- **QR:** Google Code Scanner abre su interfaz de cámara; necesita Google Play Services y puede requerir conexión para descargar su módulo. La entrada manual no sustituye la demostración del escaneo.
- **Mapa:** mapa nativo con dos puntos ficticios de retiro y acceso a indicaciones externas. No obtiene la ubicación GPS del usuario.
- **Notificaciones:** historial SQLite y notificaciones locales de demostración. En Android 13 o superior se solicita permiso para mostrarlas en el sistema; no se utiliza Firebase ni push remoto.

## Requisitos comprobados en Gradle

| Componente | Configuración del proyecto |
| --- | --- |
| Android Studio | Versión compatible con Android Gradle Plugin **8.12.3** y Gradle **8.13** |
| Kotlin / Compose BOM | **2.0.21** / **2024.11.00** |
| Android SDK | `compileSdk = 36`, `targetSdk = 36`; instalar la plataforma API 36 y herramientas del SDK |
| Dispositivo mínimo | `minSdk = 26` (Android 8.0); para la demostración se recomienda un emulador API 36 con Google Play Services y cámara configurada |
| Java | JDK 17 o compatible para AGP/Spring Boot; la validación de esta entrega utiliza **JDK 21**. Android genera bytecode Java/Kotlin 11 y el servidor Java 17 |

Las versiones están en `app/build.gradle.kts`, `server/build.gradle.kts`, `gradle/libs.versions.toml` y `gradle/wrapper/gradle-wrapper.properties`. No es necesario instalar Gradle globalmente: se utiliza el wrapper incluido. Configurar el SDK desde Android Studio y el JDK de Gradle; `local.properties` contiene una ruta propia de cada equipo y no debe subirse al repositorio.

## Ejecución en Windows y emulador

1. Abrir la raíz del repositorio en Android Studio, configurar el SDK/JDK y sincronizar Gradle. La primera sincronización requiere acceso a los repositorios de dependencias.
2. En una terminal PowerShell situada en la raíz, iniciar el backend y mantener esa terminal abierta:

   ```powershell
   .\gradlew.bat :server:bootRun
   ```

3. Comprobar desde el PC `http://127.0.0.1:8080/api/health` y `http://127.0.0.1:8080/api/products`. El servidor escucha solamente en `127.0.0.1:8080`. H2 conserva sus datos en `server/oyl-data.mv.db` al ejecutar `bootRun` desde este proyecto.
4. Iniciar un emulador Android con Google Play Services. Seleccionar la configuración **app** y pulsar **Run**. La app utiliza `http://10.0.2.2:8080/`, que apunta al PC anfitrión desde el emulador. La configuración de red admite HTTP local para ese host; no es una configuración lista para un teléfono físico o producción.
5. Crear un cliente con el formulario de registro y seleccionar **Cliente** al ingresar. En una instalación nueva se crea la cuenta ficticia de administrador: alias `admin`, contraseña `Admin1234`, selector **Administrador**. Las cuentas existentes se conservan; no borrar datos para repetir la demostración.
6. Para demostrar una compra REST, usar productos existentes y con stock en el servidor; agregar al carrito, confirmar y revisar el identificador remoto en el historial. Se necesita Spring Boot disponible para confirmar o recuperar pedidos.
7. Para demostrar la administración **local** sin que una recarga del catálogo remoto reemplace valores de productos compartidos, detener previamente el servidor con `Ctrl+C`. Las pantallas locales siguen utilizando SQLite; no borrar H2 ni la base del emulador.

El backend dispone además de operaciones administrativas protegidas por `OYL_ADMIN_KEY` (al menos 16 caracteres); la app no integra dichas operaciones ni necesita esa clave para la demostración cliente. No incrustar claves en el APK ni publicar archivos `.env`.

## Pruebas y APK

Desde la raíz:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :server:test
```

Reportes HTML:

- Android JVM: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Spring Boot: `server/build/reports/tests/test/index.html`.

Las pruebas del servidor utilizan configuración de prueba y una base H2 en memoria; no necesitan iniciar el servidor de demostración.

Para compilar las pruebas de dispositivo:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
```

En un emulador exclusivo de pruebas, con ADB autorizado, se puede ejecutar `:app:connectedDebugAndroidTest`. Para conservar una instalación de demostración, preferir un emulador separado o instalación de actualización (`adb install -r`) y ejecución directa de las clases aisladas mediante `am instrument`, sin desinstalar, usar `pm clear` ni restablecer el AVD. Las pruebas nuevas de Inicio usan datos ficticios y una base temporal independiente.

Ejemplo para ejecutar únicamente las ocho pruebas de Inicio, después de compilar ambos APK (ajustar la ruta del SDK y el identificador del emulador según `adb devices`):

```powershell
$oylAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $oylAdb devices
& $oylAdb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
& $oylAdb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $oylAdb -s emulator-5556 shell am instrument -w -r -e class 'com.example.oylfrutossecos.HomeScreenTest,com.example.oylfrutossecos.HomeNavigationTest' 'com.example.oylfrutossecos.test/androidx.test.runner.AndroidJUnitRunner'
```

Validación realizada el **21 de octubre de 2026**, en la rama `entrega-final-dsy1105`: **84 pruebas JVM Android, 7 Spring Boot y 8 instrumentadas de Inicio aprobadas**, compilación debug y APK de pruebas correctos. Las pruebas de navegación utilizan el `NavHost` real con SQLite aislado y verifican los accesos de cliente y administrador y el retorno a Inicio. Esto no sustituye la revisión visual y demostración manual en Android Studio.

| Archivo | Uso |
| --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | Aplicación **debug**, firmada automáticamente con la clave de depuración del equipo; permite probar y presentar esta compilación |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Paquete de pruebas instrumentadas; no es la aplicación para entregar al usuario |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | Salida esperada de `:app:assembleRelease` sin configuración de firma; solo existe si se ejecuta esa tarea y **no es un release firmado** |

Para un APK release firmado, utilizar **Build > Generate Signed Bundle / APK > APK**, crear o seleccionar una clave propia, elegir `release` y verificar el archivo generado. El proyecto no incluye una configuración de firma release ni una clave de entrega. No compartir claves o contraseñas; no declarar firmado un APK por su nombre de archivo.

## Alcance y limitaciones actuales

- Es una demostración académica con productos y puntos de retiro ficticios; no hay pasarela de pago.
- Autenticación y roles son locales. No existe autenticación remota de clientes para producción.
- La integración remota es parcial: consulta de productos y creación idempotente de pedidos con recuperación. SQLite conserva una copia local; el historial no actualiza automáticamente cambios de estado efectuados en el backend.
- La administración de productos, fotografías e inventario sigue siendo local. Sus cambios no se envían al backend; una actualización remota puede sobrescribir valores locales de productos con el mismo identificador. Los indicadores de Inicio no representan estadísticas del servidor.
- Los estados de pedidos remotos no se administran desde Android. El avance local se limita a pedidos históricos de demostración.
- El mapa no aplica una selección de punto de retiro al pedido. Los datos de la cuenta se consultan; no hay edición de perfil ni configuración avanzada.
- Las notificaciones son locales, no push. La app no recibe eventos remotos en segundo plano.
- Sin el servidor se puede consultar la información local, pero una compra REST requiere reconexión. Ante un intento pendiente, recuperar la solicitud existente desde el carrito.

## Preparación de entrega DSY1105

Completar integrantes y revisar los requisitos concretos del AVA. Preparar el código fuente con este README y el APK solicitado por la pauta; si se exige release firmado, generarlo y verificarlo previamente. Conservar enlaces y evidencias reales de colaboración en GitHub/Trello y de las demostraciones realizadas.

No incluir en Git ni en el archivo de código fuente bases `.mv.db`/`.trace.db`, datos privados del emulador, fotos de usuarios, claves, `.env`, `local.properties`, cachés o carpetas `build/`. Entregar los APK por separado cuando corresponda.
