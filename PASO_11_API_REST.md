# OYL Paso 11A — API REST de prueba (Spring Boot + Retrofit)

## Objetivo de esta primera fase
Comprobar una conexión real entre el emulador Android y un backend local Spring Boot.
La API es **solo lectura**, usa 8 productos ficticios y no modifica los pedidos ni el inventario.
Esto NO equivale todavía a la integración final del backend exigida en el caso.

## Requisitos
- Android Studio y el proyecto OYL ya sincronizado.
- Conexión a Internet la primera vez para descargar dependencias Gradle.
- Gradle JDK 17 o superior compatible.
- El emulador de Android (no un iPhone).

## A) Iniciar servidor desde Android Studio
1. Abre la terminal en la raíz del proyecto (donde está `gradlew.bat`).
2. Ejecuta:
   ```powershell
   .\gradlew.bat :server:bootRun
   ```
3. Mantén esa terminal **abierta**. Espera el mensaje `Started OylApiApplication`.
4. En Chrome/Edge del PC abre:
   - http://localhost:8080/api/health
   - http://localhost:8080/api/products

El primero responde JSON con `"status":"ok"`.
El segundo responde un arreglo JSON con ocho productos de ejemplo.

Si el puerto 8080 está ocupado, cierra el proceso que lo usa y repite. Este tutorial asume 8080.

## B) Abrir Android
1. Con el servidor iniciado, ejecuta la app con Run.
2. Ingresa a OYL y entra a **Cuenta → Probar conexión REST (Paso 11)**.
3. Debe aparecer **Conexión establecida** y **8 productos recibidos por Retrofit**.
4. Prueba el botón **Consultar servidor** para actualizar.

## C) Aclaraciones
- El emulador llama a `http://10.0.2.2:8080/` para llegar a `localhost` del PC.
- El AndroidManifest solo permite HTTP sin cifrado a `10.0.2.2`. No usar este endpoint en producción.
- El catálogo actual, las fotos, el carrito, el inventario y los pedidos mantienen SQLite en esta fase.
- Las fotos tomadas en el emulador están privadas en el dispositivo y no se suben al servidor.
- Para detener Spring Boot presiona `Ctrl+C` en la terminal.
- **No borres los datos del emulador**: eso borraría las fotos y los datos locales.

## Fase 11B (siguiente, aún no implementada)
- Persistencia real en el servidor (productos, pedidos e inventario).
- Descuento transaccional de stock en el servidor, validaciones e idempotencia.
- Sincronización de catálogo en caché SQLite y estados de pedido.
- Autorización real para endpoints administrativos.
- Reemplazar HTTP local por HTTPS remoto al publicar.
