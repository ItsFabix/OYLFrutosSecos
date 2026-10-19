# Paso 11B — Backend persistente y pedidos transaccionales (en pruebas)

Esta rama contiene **únicamente** los cambios de servidor necesarios para preparar la fuente oficial de productos, stock y pedidos. **La aplicación Android todavía no envía pedidos al servidor**: sus pedidos e inventario siguen locales en SQLite.

## 1. Primera comprobación: pruebas automatizadas

En Android Studio:

1. Detén el servidor anterior con `Ctrl+C` en su terminal (si está ejecutándose).
2. En la terminal del proyecto, cambia a `paso-11b-backend-inventario-pedidos`.
3. Ejecuta:

```powershell
.\gradlew.bat :server:test
```

**No avances al siguiente paso si falla.** Envía la primera excepción o captura de la ventana Build.

Los tests comprueban:
- GET `/api/health` y `/api/products`;
- POST `/api/orders` descuenta stock en transacción;
- repetir un `requestId` no genera un segundo descuento;
- falta de stock devuelve HTTP 409 sin modificar existencias;
- movimientos de inventario requieren clave administrativa y quedan auditados.

## 2. Iniciar el servidor

```powershell
.\gradlew.bat :server:bootRun
```

Deja la terminal abierta. En el navegador del PC:
- http://localhost:8080/api/health
- http://localhost:8080/api/products

Esta vez el catálogo se lee desde H2 persistente (archivo local `server/oyl-data.mv.db`, ignorado por Git). **No borres este archivo** si quieres conservar stock y pedidos de prueba después de reiniciar.

## 3. Crear un pedido ficticio en PowerShell (solo cuando los tests pasen)

Abre una segunda terminal PowerShell y ejecuta:

```powershell
$solicitud = @{
  requestId = [guid]::NewGuid().ToString()
  customerAlias = "cliente-demo"
  items = @(@{ productId = 1; quantity = 2 })
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -ContentType "application/json" -Body $solicitud
```

El servidor devuelve un pedido con `status=PENDING` y `totalClp=6980`. Al volver a visitar `/api/products`, las almendras tendrán dos bolsas menos de stock que antes de enviar el pedido. Enviar la **misma** `$solicitud` otra vez devuelve el mismo pedido sin otro descuento.

Para demostrar stock insuficiente, utiliza el ID 4 (castañas de cajú, stock inicial 0); deberías recibir HTTP 409.

## 4. Administrar existencias y avanzar pedidos (demo)

Los endpoints `/api/admin/**` requieren una clave temporal en la variable `OYL_ADMIN_KEY` del proceso del servidor (mínimo 16 caracteres). No hay clave predeterminada y **no debes incrustarla en Android ni subirla al repositorio**.

Antes de arrancar el servidor puedes introducir una clave temporal:

```powershell
$env:OYL_ADMIN_KEY = Read-Host "Clave temporal de administrador (16 caracteres o mas)"
.\gradlew.bat :server:bootRun
```

API de desarrollo:
- `POST /api/admin/products/{id}/stock` — JSON `{"delta":5,"note":"Ingreso de prueba"}`;
- `POST /api/admin/orders/{id}/advance` — avanza PENDING → CONFIRMED → PREPARED → DELIVERED.

En ambos, enviar header `X-OYL-ADMIN-KEY`. Sin clave válida el servidor responde 403.

## Alcance y límites de seguridad

El servidor escucha solo `127.0.0.1:8080` en el PC. Es una demo con datos ficticios, sin autenticación de clientes y solo HTTP local. No abrir el puerto a Internet ni almacenar datos reales. Falta incorporar autorización por usuario, HTTPS y adaptar Android a estas rutas, incluyendo el servidor como única fuente de estado de pedidos y stock. La clave de administrador de demostración no sustituye una autenticación segura.

## Flujo de Git

Las ramas de Paso 11A (conexión Retrofit) ya se fusionaron en `main`. **No fusiones este Paso 11B hasta que se ejecuten los tests y la prueba manual**.
