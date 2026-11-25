# OYL Frutos Secos — recuperación y cierre seguro

> **Actualización posterior, cierre para presentación:** por instrucción del usuario se detuvo la búsqueda de datos Android antiguos, conservando AVD y respaldos. Los cuatro eventos del pedido conocido sí se recuperaron desde el outbox original de ventas: IDs, estados y timestamps intactos, copias H2 antes y después, consulta HTTP con cuatro resultados y lectura de la copia persistida. No se inventaron eventos. Ambas rutas H2 y los tokens efectivos coincidían; la tabla destino estaba vacía y no hay auditoría suficiente para atribuir la causa del vaciado. Aprobaron veinte comprobaciones aisladas adicionales de compra, estados, caída, reentrega e independencia/persistencia de los tres servicios. Las observaciones anteriores de cero eventos conservadas abajo son históricas.

Se comprobó además una compra nueva desde Android (`1607936c-06fc-4623-b190-33b8924a4ea1`), sus cuatro estados/eventos, actualización del historial y reposición desde administración. La copia H2 final contiene doce eventos: cuatro históricos recuperados del outbox y cuatro por cada una de las dos compras nuevas de demostración. El estado de lectura previo de los cuatro eventos históricos no estaba en el outbox y no se acredita como recuperado. El AVD original permanece intacto; la demostración actual usa `OYL_Demostracion_API36`, con datos nuevos y respaldo privado. La configuración de pantalla preparada es 720×1600 a 270 dpi: en la prueba inicial de 360×640 dp, la tarjeta informativa ocupaba el área del mapa; no se cambió el diseño para esa altura reducida. El manifiesto actual contiene 77 archivos, incluidos dos scripts nuevos de firma y comprobación aislada. La release definitiva está firmada con el certificado original, instalada sin reemplazos en `OYL_Release_Control3` y copiada a `%USERPROFILE%\Documents\OYL-Control3\OYL-Control3-release-20261125.apk`. Se verificó su código contra la compilación actual y su hash contra la copia instalada. La compra desde esa APK es `ec121ba3-466c-4ebc-9d26-bfeece0143bc`; administración avanzó los cuatro estados y el seguimiento muestra los cuatro eventos en español. La sesión y el historial persistieron después de reiniciar la app. Al cierre se reejecutaron las 249 pruebas JVM, todas aprobadas, y se regeneró JaCoCo sin cambiar alcance, clases ni porcentajes. Se conservan los 80 resultados instrumentados anteriores; no se reejecutaron sobre AVD con datos. Los avisos Android locales y los eventos remotos se verificaron por separado.

Revisión del 25/11/2026, desde las 09:03 de Chile. Rama: `control3-tres-microservicios`.

## Resultado principal

**No se recuperaron los datos Android anteriores al incidente.** No se encontró una copia SQLite acreditada. Hay imágenes antiguas de AVD que justifican una investigación sobre una copia aislada, pero no se ha comprobado que contengan OYL ni sus datos recientes. No se restauró, arrancó, cerró ni modificó ningún AVD durante esta revisión; tampoco se reinstaló la app.

El AVD afectado tiene ahora una instalación nueva, posterior al incidente. Además, la consulta actual de eventos del pedido conocido devuelve cero elementos. Por ambas razones no se presenta la entrega como cerrada ni se confunden los resultados automatizados aprobados con una demostración actual completa.

Solo se añadieron documentación y el manifiesto de archivos para revisión. El código funcional, pruebas, cobertura, configuración de firma y cambios locales anteriores se conservaron.

## 1. AVD identificado y estado actual

- Nombre lógico: **OYL**; serial **emulator-5554**; Android 17/API 37, x86_64.
- Archivo de registro: `%USERPROFILE%\.android\avd\OYL.ini`.
- Directorio efectivo: `%USERPROFILE%\.android\avd\OYL_SQLite_QA.avd`.
- Proceso identificado: `qemu-system-x86_64.exe`, PID 33872, con `-avd OYL -no-snapstorage`. No se cambió su estado.
- Los reportes de instrumentación corresponden al dispositivo `OYL(AVD) - 17`.
- Package Manager muestra una instalación nueva de `com.example.oylfrutossecos` a las **12:02:47 UTC / 09:02:47 de Chile** del 25/11, anterior al comienzo de esta revisión y posterior a la pérdida. No se atribuye esa instalación a una recuperación.

Se leyó la base actual dos veces, con bytes idénticos, y se inspeccionó una copia privada en el host con SQLite de solo lectura. Esquema 8, 98.304 bytes y `PRAGMA quick_check = ok`:

| Tabla actual | Filas |
| --- | ---: |
| Usuarios | 1, exclusivamente ADMIN |
| Sesión | 1 |
| Productos | 8 |
| Carrito | 0 |
| Pedidos / líneas | 0 / 0 |
| Checkout pendiente | 0 |
| Avisos | 3 |
| Movimientos locales | 0 |

No hay copia SQLite del pedido remoto conocido ni fotografías de productos en el directorio privado observado. Estos datos actuales no prueban recuperación de usuarios, perfiles o historial anteriores. La copia de inspección está bajo `build/recovery-closeout/`, ignorada por Git; no se entrega ni se incorpora al commit.

Sin una copia anterior íntegra no se puede cuantificar exactamente cuántos usuarios, fotos, carritos o registros locales desaparecieron. La ausencia actual se documenta; no se inventa un inventario previo ni se deduce que esos datos puedan reconstruirse desde H2.

## 2. Respaldos buscados y candidatos

Se revisaron nombres de archivos y metadatos de `.android`, el proyecto y sus carpetas antiguas `build`, AndroidStudioProjects, Documents, Downloads y OneDrive. Se buscaron `.db`, `.sqlite`, `.sqlite3`, `.ab`, `.tar`, `.tgz`, `.bak`, imágenes y snapshots. No se abrieron bases ajenas al proyecto. Algunos temporales del sistema no eran accesibles; no se forzaron permisos.

| Candidato | Evidencia observada | Conclusión |
| --- | --- | --- |
| `OYL_SQLite_QA.avd/snapshots/default_boot` | Solo `ram.img` de 4 GiB, fechado 02/11. Falta `snapshot.pb`; los discos del AVD están bloqueados por el proceso activo. | No hay snapshot completo acreditado. No se forzó el bloqueo ni se cerró el AVD. |
| `Pixel_9_Pro.avd` | AVD inactivo; discos userdata, encryptionkey, cache y sdcard; snapshot interno `default_boot` del **06/11/2026 12:35**, coincidente entre esos componentes. Hay `snapshot.pb`, `hardware.ini`, RAM y texturas. | **Candidato para clonación autorizada**, no recuperación confirmada. |
| Disco actual de `Pixel_9_Pro` | Userdata y encryptionkey modificados por última vez el **11/11**. Cadena QCOW2 legible, sin indicador de corrupción ni dirty flag según `qemu-img info`. | Puede ser un candidato más reciente que el snapshot del 06/11. No se montó ni arrancó; contenido Android sin validar. |
| `build/admin-access-qa/avd` | Imagen completa userdata de pruebas y overlay QCOW2 del **23/11**, sin snapshots internos; carpeta snapshot solo con RAM. | Candidato de un entorno de pruebas, sin demostrar que contenga datos personales o historial perdidos. No presentarlo como respaldo del usuario. |
| ZIP Android/Spring Boot del 23/11 | 171 y 25 entradas; sin bases, exportaciones ni imágenes de AVD. | Código fuente antiguo; no respaldo de datos. |
| `OYL_Paso_9_Notificaciones.zip` | 12 entradas, sin datos persistentes. | Código, no recuperación. |
| Otros cuatro comprimidos revisados | Sin archivos de datos OYL; siete comprimidos inspeccionados en total, sin extraerlos. | No se encontró respaldo SQLite. |
| Historial local Git, todas las ramas leídas | Ninguna ruta histórica `.db`, `.sqlite`, `.sqlite3`, `.ab` o `.qcow2`. No se cambió de rama. | Git no ofrece una copia de la base del usuario. |
| Backup Manager del AVD | Deshabilitado; consulta de conjuntos de restauración: `No restore sets`. | No hay restauración cloud disponible acreditada desde ese perfil. `allowBackup=true` en el manifest no demuestra que se haya hecho una copia. |

No se investigaron cuentas cloud externas, discos externos no identificados ni historiales remotos de OneDrive. No se afirma que sea imposible toda recuperación; se afirma que **no hay una copia válida comprobada y no se recuperaron los datos en esta sesión**.

## 3. Procedimiento de recuperación propuesto, pendiente de autorización

1. Mantener OYL abierto tal como está; no restaurar snapshots ni ejecutar pruebas sobre él. No cargar imágenes antiguas encima de su directorio ni sobrescribir datos de la instalación nueva.
2. Priorizar el AVD **Pixel_9_Pro inactivo**. Con autorización, copiar su directorio completo y registro a una ubicación privada nueva, conservando userdata y todos sus backing files, encryptionkey, sdcard, snapshots y configuración. Registrar hashes del origen y de las copias. No copiar solo `ram.img` ni solo el overlay QCOW2.
3. Crear un nombre de AVD distinto, por ejemplo `OYL_Recuperacion_Aislada`, con paths que apunten únicamente a las copias. Revisar también la cadena de backing files para que ninguna escritura pueda dirigirse a los originales. No modificar el registro `OYL.ini` existente.
4. Arrancar únicamente la copia, en otro puerto, sin guardar snapshots ni conectar OYL a datos reales. Examinar primero el estado de disco del 11/11; si se necesita, ensayar `default_boot` del 06/11 en **otra copia**. No instalar OYL antes de verificar si el paquete y sus datos ya existen.
5. Si aparece `oyl_auth.db`, exportar a un destino privado consistente la base y cualquier WAL/journal necesario, junto con las fotos privadas. Verificar integridad, esquema, propietarios, fechas y referencias. Datos del 06/11 o 11/11 no pueden acreditarse como recuperación de pedidos creados el 25/11.
6. Solo después de validar contenido y con aprobación adicional, definir qué datos reales pueden trasladarse. Conservar la instalación nueva por separado; no hacer mezclas de usuarios, IDs, carritos ni solicitudes pendientes por coincidencias de nombre.
7. Si Pixel no contiene OYL, valorar la imagen de pruebas del 23/11 bajo la misma separación. Cualquier fixture o dato académico encontrado se identificará como tal, nunca como dato recuperado del usuario.

Si esas inspecciones no encuentran una SQLite/fotos válidas, la conclusión seguirá siendo **datos locales no recuperados**. No se reconstruirán usuarios, contraseñas, fotos, carritos o checkout pendientes a partir de ejemplos.

## 4. Qué permanece disponible en los servicios

Observación GET independiente del 25/11 a las 09:07 de Chile:

| Consulta | Resultado actual |
| --- | --- |
| Ventas 8080, salud | HTTP 200, `ok`. |
| Catálogo | HTTP 200, **8 productos**. |
| Retiro 8081 y delegación por 8080 | HTTP 200, **2 puntos**, contenido idéntico. |
| Notificaciones 8082, salud | HTTP 200, `ok`. |
| Pedido `a9209d94-cc09-43fb-ad38-0fa9788eca72` | HTTP 200, **DELIVERED**, una línea. |
| Eventos de ese pedido | HTTP 200, **lista vacía**. |

Los tres procesos siguen siendo los iniciados alrededor de las 00:17; sus directorios de ejecución son `server`, `pickup-service` y `notification-service`. Existen sus tres H2 independientes (`oyl-data.mv.db`, `oyl-pickup-data.mv.db`, `oyl-notifications-data.mv.db`). No se detuvieron, reiniciaron ni abrieron mediante JDBC durante esta revisión.

Los archivos H2 activos están bloqueados incluso para copia; se respetó ese bloqueo. No se puede certificar por inspección de tablas que todos los movimientos, pedidos o eventos estén intactos. El script de comprobación con ese UUID **falla actualmente** porque recibe cero eventos; no se presenta como aprobado. Las cuatro respuestas documentadas en la revisión anterior son evidencia histórica, no el estado actual.

No se ha establecido la causa de esa diferencia. No se atribuye a la desinstalación Android, que afecta almacenamiento distinto. Queda pendiente una inspección autorizada del servicio de notificaciones y del outbox original. Si sus registros originales aún existen, podría plantearse reentregar exactamente esos eventos e IDs sobre una copia aislada; no se inventaron eventos ni se ejecutó una reentrega.

H2 no conserva las credenciales/sesiones locales, carritos sin enviar, fotos privadas ni todas las relaciones del historial cliente SQLite. El catálogo y el pedido remoto siguen disponibles, pero volver a registrarse no restaura automáticamente el historial local anterior.

## 5. Protección de futuras pruebas y resultados conservados

Verificación estática realizada contra el **source JAR local de AGP 8.12.3**:

- AGP reconoce `android.injected.androidTest.leaveApksInstalledAfterRun`.
- `gradle.properties` la fija en `true`.
- `scripts/tests/ejecutar-integracion-android.ps1` la pasa explícitamente, exige `-DeviceSerial emulator-NNNN` y limita `ANDROID_SERIAL`, restaurando el entorno al terminar.
- No se ejecutó instrumentación ni se probó dinámicamente la retención sobre el AVD actual. Esa validación sigue pendiente en un AVD dedicado, con respaldo y autorización. No basta comparar certificados de APK.

Esta opción evita la desinstalación de limpieza de AGP; no sustituye un respaldo ni protege frente a una desinstalación manual, borrado de datos o pruebas que limpien su propia base. Las siguientes pruebas deben dirigirse a un AVD dedicado y descartable.

Se recomputaron los conteos desde los XML ya existentes, sin reejecutar pruebas que escriban datos:

| Suite anterior conservada | Aprobadas | Fallidas / errores / omitidas |
| --- | ---: | ---: |
| Android JVM | 221 | 0 / 0 / 0 |
| Ventas | 20 | 0 / 0 / 0 |
| Retiro | 4 | 0 / 0 / 0 |
| Notificaciones | 4 | 0 / 0 / 0 |
| Android instrumentadas | 80 | 0 / 0 / 0 |
| **Total** | **329** | **0 / 0 / 0** |

Las **15 comprobaciones PowerShell aisladas sí se ejecutaron otra vez en esta revisión y aprobaron**. No incluyen como éxito la consulta real fallida de eventos.

JaCoCo conservado: instrucciones **91,57 %**, líneas **93,07 %**, ramas **83,32 %**, métodos **91,59 %**, **115 clases**. Alcance: `domain`, `data/repository`, `ui/viewmodel`; no toda la aplicación ni los servicios. No se modificaron filtros, clases o pruebas para alterar la medición.

La revisión estática de textos de pantallas/componentes encontró nueve coincidencias técnicas, todas en eliminación de sufijos `demo`, nombres internos de archivos o traducción de mensajes heredados. No se detectó un nuevo texto técnico innecesario que requiera cambiar código. No se hizo una revisión visual nueva del AVD ni se modificó su estado para obtener capturas.

## 6. Próximo commit y revisión de seguridad

El archivo [MANIFIESTO_COMMIT_CONTROL3.txt](MANIFIESTO_COMMIT_CONTROL3.txt) enumera exactamente los archivos propuestos: cambios funcionales ya probados, pruebas nuevas/existentes adaptadas, Gradle/retención, scripts, `.gitignore` y documentación. No se agregó nada al índice ni se hizo commit.

Son **75 archivos propuestos**, incluidos el informe y el propio manifiesto. La simulación `git add --dry-run --pathspec-from-file=MANIFIESTO_COMMIT_CONTROL3.txt` terminó con código 0; el hash del índice permaneció idéntico y siguen existiendo cero archivos staged. La lista debe volver a revisarse si aparecen cambios posteriores.

Comprobaciones de preparación:

- `git diff --check`: **0**, sin errores de whitespace.
- Sin archivos privados de firma, `.env`, `local.properties`, bases de datos ni exportaciones en el manifiesto. Los hallazgos/copia privada actuales quedan bajo `build`, ignorado por Git.
- Escaneo textual de archivos públicos sin claves privadas, patrones de tokens GitHub/AWS ni RUT con formato detectados. Se mantienen fixtures y cuentas académicas ya existentes; no se añadieron credenciales privadas reales. Este escaneo no se presenta como una garantía absoluta de ausencia de secretos en cualquier binario histórico.
- `.gitignore` conserva exclusiones de builds/cachés, metadatos release, H2, logs, keystores y configuración privada; no se borraron archivos ni se ignoraron fuentes necesarias.
- La APK release antigua ya versionada no cambió y no se añade una APK nueva al próximo commit. No se cambió de rama ni se hizo push/merge.

Después de revisar el manifiesto y **solo con autorización para preparar el índice**, podrán usarse:

```powershell
git add --pathspec-from-file=MANIFIESTO_COMMIT_CONTROL3.txt
git diff --cached --check
git diff --cached --stat
git status --short
```

No se ejecutaron esos comandos de staging; el único `git add` de esta revisión es su simulación `--dry-run`. Un commit/push requiere autorización posterior. No usar `git add .` sin revisar nuevamente archivos y secretos.

## 7. APK release con la identidad original, todavía pendiente

La release original conserva firma v2 válida. Su certificado SHA-256 es:

```text
9d1c01df179ec7901200ce8d8f373cb1dcfb2146404a20dffe9711b313131547
```

Existe `%USERPROFILE%\Documents\oyl-release.jks`, fuera del repositorio. No se abrió ni modificó, y no se conocen ni se inventan su alias o contraseñas. La huella se obtuvo del certificado público de la APK, no de la clave privada.

Pasos posteriores a la revisión y autorización:

1. Confirmar que se está en `control3-tres-microservicios`, revisar el manifiesto, resultados y pendientes. No generar una release para dar por resuelta la recuperación.
2. Abrir el proyecto raíz en Android Studio. **Build → Generate Signed App Bundle / APK → APK → app**.
3. Elegir el **keystore existente**, no crear otro. Introducir sus contraseñas y seleccionar el alias original. Si se desconoce, identificarlo privadamente con `keytool -list -keystore "$env:USERPROFILE\Documents\oyl-release.jks"`; la contraseña se introduce en el prompt, nunca en argumentos o archivos públicos.
4. Seleccionar `release` y un destino nuevo, separado de `app/release/app-release.apk`, para conservar el archivo anterior. No modificar la identidad de firma para resolver problemas de instalación.
5. Verificar la APK nueva sin instalarla en el AVD afectado:

```powershell
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat"
& $apksigner verify --verbose --print-certs 'RUTA_APK_NUEVA'
Get-FileHash -LiteralPath 'RUTA_APK_NUEVA' -Algorithm SHA256
```

6. Exigir verificación correcta y **la misma huella del certificado** indicada arriba. El hash del archivo APK puede cambiar porque el código cambió; al copiar la nueva APK a la entrega, el hash de la copia sí debe coincidir con el de ese mismo archivo nuevo.
7. Probar la release en un AVD dedicado con respaldo. Si la firma de una instalación existente es distinta, no desinstalar para forzar la prueba. No generar ni entregar debug como si fuera release firmada.

## 8. Evidencias y checklist de entrega

La carpeta privada anterior contiene ZIP/APK/Word del 23/11: no acreditan esta versión. El Word es legible como OOXML, pero todavía menciona un backend modular, no los dos módulos nuevos ni los conteos/cobertura actuales. No se modificó ni se publicó ese documento privado.

| Evidencia privada encontrada | Estado para el cierre |
| --- | --- |
| Configuración de firma | Cero imágenes; preparar captura real sin contraseñas. |
| Verificación apksigner | Archivos de texto, cero capturas; preparar verificación de la APK definitiva. |
| Trello | Cero imágenes; falta tablero/captura real y reparto de tareas. |
| Pruebas | 87 archivos antiguos; incorporar los reportes actuales de 329 pruebas y JaCoCo con fecha y alcance. |
| Aplicación | Ocho imágenes anteriores; validar vigencia y recapturar los cambios actuales. No se presentan como evidencia nueva. |

Capturas a preparar en un entorno autorizado: catálogo/precio CLP, carrito y pedido pendiente, estados y seguimiento, avisos nuevos/antiguos normalizados, administración e inventario, mapa Ver todos/Centro/Ñuñoa y clima, resultados QR, perfil persistente, tres servicios activos y eventos del pedido. No capturar claves, tokens, RUT o datos personales para GitHub. Los datos de una nueva demostración deben identificarse como nuevos, no recuperados.

Checklist para finalizar:

- [x] Rama y cambios locales conservados; sin código funcional nuevo en esta revisión.
- [x] AVD identificado; búsqueda de respaldos y candidatos documentada.
- [x] Ninguna restauración, reinstalación, cierre de AVD o escritura en sus datos.
- [x] 329 resultados aprobados y 115 clases/cobertura conservados; 15 comprobaciones PowerShell reaprobadas.
- [x] Configuración de retención comprobada estáticamente; manifiesto y revisión de seguridad preparados.
- [ ] Autorizar o descartar la inspección de candidatos sobre clones; recuperación local todavía no acreditada.
- [ ] Resolver por qué el pedido remoto devuelve cero eventos; no afirmar que los cuatro eventos sigan disponibles.
- [ ] Validar retención de APK/datos sobre un AVD de pruebas con copia previa.
- [ ] Contrastar pauta oficial del Control 3; no se acredita cumplimiento íntegro de requisitos no aportados.
- [ ] Actualizar Word privado, integrantes/RUT, GitHub/Trello y capturas reales.
- [ ] Autorizar commit/push por separado; revisar el índice antes de publicar.
- [ ] Autorizar y generar release con la clave original, verificar certificado y hash, probarla y regenerar los ZIP con wrappers/Gradle raíz y los tres servicios.

La prioridad de cierre es decidir la investigación aislada del respaldo y aclarar los eventos remotos; después se revisan los artefactos y se autoriza la entrega definitiva. No se generaron APK, ZIP ni firmas nuevos durante esta revisión.
