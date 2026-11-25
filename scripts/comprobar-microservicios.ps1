# Ejecutar desde la raiz, con los tres servidores en marcha:
# powershell -ExecutionPolicy Bypass -File .\scripts\comprobar-microservicios.ps1
param([string]$OrderId = "")
$ErrorActionPreference = "Stop"
function Check($name, $url) {
    Write-Host ("Consultando " + $name + ": " + $url)
    try { return Invoke-RestMethod -Uri $url -TimeoutSec 10 }
    catch { throw ("No responde " + $name + ". " + $_.Exception.Message) }
}
$ventas = Check "ventas 8080" "http://127.0.0.1:8080/api/health"
$puntos = @(Check "retiro 8081" "http://127.0.0.1:8081/api/pickup-points")
$avisos = Check "notificaciones 8082" "http://127.0.0.1:8082/api/notifications/health"
$proxy = @(Check "delegacion 8080 a 8081" "http://127.0.0.1:8080/api/pickup-points")
$productos = @(Check "catalogo 8080" "http://127.0.0.1:8080/api/products")
if ($ventas.status -ne "ok" -or $avisos.status -ne "ok") { throw "Falla el health de un microservicio" }
if ($puntos.Count -lt 2 -or $puntos.Count -ne $proxy.Count) {
    throw "Puntos de retiro distintos entre la API directa y la delegada"
}
if ($productos.Count -lt 8) { throw "Catalogo incompleto" }
Write-Host ("OK: 3 servicios activos, " + $puntos.Count + " puntos y " + $productos.Count + " productos.")
if ($OrderId) {
    $events = @(Check "eventos remotos de pedido" ("http://127.0.0.1:8080/api/orders/" + $OrderId + "/notifications"))
    Write-Host ("Eventos remotos para " + $OrderId + ": " + $events.Count)
    if ($events.Count -lt 1) { throw "Todavia no se entregaron eventos a notificaciones" }
}
Write-Host "Las pruebas de compra, persistencia, reintento y APK requieren una demostracion adicional."
