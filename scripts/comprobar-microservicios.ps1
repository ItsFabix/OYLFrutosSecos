<#
.SYNOPSIS
Comprueba los tres servicios mediante consultas GET, sin modificar datos.
.PARAMETER OrderId
UUID remoto del pedido (id de GET /api/orders/{id}), no el numero #5 de Android.
#>
param([string]$OrderId = "")
$ErrorActionPreference = "Stop"

function Check($name, $url, [switch]$ExpectArray) {
    Write-Host ("Consultando " + $name + ": " + $url)
    try { $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10 }
    catch {
        $webResponse = $_.Exception.Response
        if ($null -ne $webResponse) {
            $code = [int]$webResponse.StatusCode
            if ($code -eq 404) { throw "HTTP 404 en ${name}: ruta o pedido no encontrado. Comprueba la URL y el UUID remoto." }
            throw "HTTP $code en ${name}. El servicio respondio, pero no pudo completar la consulta."
        }
        if ($_.Exception.Status -eq [System.Net.WebExceptionStatus]::Timeout) {
            throw "Tiempo de espera agotado en ${name}. Comprueba la disponibilidad del servicio."
        }
        throw "Conexion rechazada o no disponible en ${name}. Comprueba que el servicio este iniciado y el puerto sea correcto."
    }
    try {
        # Windows PowerShell 5.1 puede interpretar Content con una codificacion incorrecta.
        $utf8 = New-Object System.Text.UTF8Encoding($false, $true)
        $json = $utf8.GetString($response.RawContentStream.ToArray()).TrimStart([char]0xFEFF)
        $parsed = ConvertFrom-Json -InputObject $json -ErrorAction Stop
    }
    catch { throw "Respuesta JSON o UTF-8 invalida en ${name}. No se pudo interpretar el contenido recibido." }
    if ($ExpectArray -and -not $json.TrimStart().StartsWith('[')) {
        throw "Respuesta invalida en ${name}: se esperaba un arreglo JSON."
    }
    return $parsed
}

function Assert-RemoteOrderId([string]$Value) {
    $parsedId = [guid]::Empty
    if ($Value -and -not [guid]::TryParseExact($Value, "D", [ref]$parsedId)) {
        throw "-OrderId requiere el UUID remoto completo del pedido; no admite el numero #5 mostrado en Android."
    }
}

function Canonical-Points($Points) {
    foreach ($point in $Points) {
        foreach ($field in @('id', 'title', 'reference', 'schedule', 'latitude', 'longitude')) {
            if ($null -eq $point.PSObject.Properties[$field] -or $null -eq $point.$field) {
                throw "Respuesta de puntos invalida: falta el campo $field."
            }
        }
    }
    # Comparar contenido con un orden estable de filas y propiedades JSON.
    return ConvertTo-Json -InputObject @($Points | Sort-Object id |
        Select-Object id, title, reference, schedule, latitude, longitude) -Depth 10 -Compress
}

function Invoke-MicroserviceChecks([string]$RemoteOrderId = "") {
    Assert-RemoteOrderId $RemoteOrderId
    $ventas = Check "ventas 8080" "http://127.0.0.1:8080/api/health"
    $puntos = @(Check "retiro 8081" "http://127.0.0.1:8081/api/pickup-points" -ExpectArray)
    $avisos = Check "notificaciones 8082" "http://127.0.0.1:8082/api/notifications/health"
    $proxy = @(Check "delegacion 8080 a 8081" "http://127.0.0.1:8080/api/pickup-points" -ExpectArray)
    $productos = @(Check "catalogo 8080" "http://127.0.0.1:8080/api/products" -ExpectArray)
    if ($ventas.status -ne "ok" -or $avisos.status -ne "ok") { throw "Estado de salud inesperado en un microservicio." }
    if ($puntos.Count -lt 2 -or $puntos.Count -ne $proxy.Count) { throw "Cantidad incorrecta de puntos de retiro." }
    if ((Canonical-Points $puntos) -cne (Canonical-Points $proxy)) {
        throw "Puntos de retiro distintos entre la consulta directa y la delegada."
    }
    # Se admiten productos legitimos agregados despues de la carga inicial de ocho.
    if ($productos.Count -lt 8) { throw "Catalogo incompleto: se esperaban al menos ocho productos." }
    Write-Host "OK: 3 microservicios activos." -ForegroundColor Green
    Write-Host ("Puntos de retiro: " + $puntos.Count)
    Write-Host ("Productos: " + $productos.Count)
    Write-Host "Respuestas directa y delegada coinciden." -ForegroundColor Green
    if ($RemoteOrderId) {
        $events = @(Check "eventos del pedido" ("http://127.0.0.1:8080/api/orders/" + $RemoteOrderId + "/notifications") -ExpectArray)
        Write-Host ("Eventos remotos para " + $RemoteOrderId + ": " + $events.Count)
        if ($events.Count -lt 1) { throw "Todavia no hay eventos entregados. Comprueba el outbox y reintenta en unos segundos." }
        if (@($events | Select-Object -ExpandProperty eventId -Unique).Count -ne $events.Count) { throw "Se recibieron identificadores de evento duplicados." }
        foreach ($event in $events) {
            if ($event.orderId -ne $RemoteOrderId) { throw "Se recibio un evento de otro pedido." }
            Write-Host ("  " + $event.kind + " / " + $event.status)
        }
    }
    Write-Host "Esta comprobacion solo realiza lecturas. Las compras, reinicios y la APK se verifican por separado."
}

# Las pruebas pueden importar las funciones sin consultar los servicios del usuario.
if ($MyInvocation.InvocationName -ne '.') { Invoke-MicroserviceChecks $OrderId.Trim() }
