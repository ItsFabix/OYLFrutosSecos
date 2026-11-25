# Self-contained Windows PowerShell 5.1 checks; no Pester dependency or live service writes.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../comprobar-microservicios.ps1')
$script:checks = 0
$script:payload = '[]'
$script:failure = $null
$script:responses = @{}
function Assert-Check([bool]$condition, [string]$message) {
    if (-not $condition) { throw "FALLO: $message" }
    $script:checks++
}
function Assert-Failure([scriptblock]$action, [string]$text) {
    $caught = $null
    try { & $action | Out-Null } catch { $caught = $_.Exception.Message }
    Assert-Check ($null -ne $caught -and $caught.Contains($text)) "Se esperaba un error que incluyera '$text'; recibido '$caught'."
}
function Invoke-WebRequest([string]$Uri, [switch]$UseBasicParsing, [int]$TimeoutSec) {
    if ($script:failure) { throw $script:failure }
    $body = if ($script:responses.ContainsKey($Uri)) { $script:responses[$Uri] } else { $script:payload }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    return [pscustomobject]@{ RawContentStream = [System.IO.MemoryStream]::new($bytes, $false) }
}

# UTF-8 literals built from code points so the test file itself also works without a BOM in PS 5.1.
$nunoa = ([string][char]0x00D1) + 'u' + ([char]0x00F1) + 'oa'
$script:payload = '[{"id":"nunoa","title":"' + $nunoa + '","reference":"Plaza","schedule":"L-V","latitude":-33.4554,"longitude":-70.5982}]'
$points = @(Check 'UTF8' 'http://isolated/points' -ExpectArray)
Assert-Check ($points.Count -eq 1 -and $points[0].title -ceq $nunoa) 'Arreglo unitario y caracteres UTF-8.'
$script:payload = '[]'
Assert-Check (@(Check 'empty' 'http://isolated/empty' -ExpectArray).Count -eq 0) 'Arreglo vacio valido.'
$script:payload = '{"status":"ok"}'
Assert-Failure { Check 'shape' 'http://isolated/shape' -ExpectArray } 'se esperaba un arreglo JSON'
$script:payload = '{invalid'
Assert-Failure { Check 'json' 'http://isolated/json' } 'JSON o UTF-8 invalida'
Assert-Failure { Assert-RemoteOrderId '#5' } 'UUID remoto completo'
Assert-RemoteOrderId 'a9209d94-cc09-43fb-ad38-0fa9788eca72'
Assert-Check $true 'UUID remoto aceptado.'
$script:failure = [System.Net.WebException]::new('isolated', [System.Net.WebExceptionStatus]::ConnectFailure)
Assert-Failure { Check 'refused' 'http://isolated/refused' } 'Conexion rechazada'
$script:failure = [System.Net.WebException]::new('isolated', [System.Net.WebExceptionStatus]::Timeout)
Assert-Failure { Check 'timeout' 'http://isolated/timeout' } 'Tiempo de espera'
$script:failure = [System.Exception]::new('isolated')
$script:failure | Add-Member NoteProperty Response ([pscustomobject]@{StatusCode=404})
Assert-Failure { Check 'missing' 'http://isolated/missing' } 'HTTP 404'
$script:failure = [System.Exception]::new('isolated')
$script:failure | Add-Member NoteProperty Response ([pscustomobject]@{StatusCode=503})
Assert-Failure { Check 'unavailable' 'http://isolated/unavailable' } 'HTTP 503'
$script:failure = $null
$otherOrder = ConvertFrom-Json ('[{"longitude":-70.5982,"latitude":-33.4554,"schedule":"L-V","reference":"Plaza","title":"' + $nunoa + '","id":"nunoa"}]')
Assert-Check ((Canonical-Points $points) -ceq (Canonical-Points $otherOrder)) 'Comparacion independiente del orden de propiedades.'
$otherOrder[0].reference = 'Otra direccion'
Assert-Check ((Canonical-Points $points) -cne (Canonical-Points $otherOrder)) 'Diferencias de contenido detectadas.'
Assert-Failure { Canonical-Points @([pscustomobject]@{id='incomplete'}) } 'falta el campo'

$base = 'http://127.0.0.1:'
$p = '[{"id":"centro","title":"Centro","reference":"Plaza","schedule":"L-V","latitude":-33.4,"longitude":-70.6},' +
    '{"id":"nunoa","title":"' + $nunoa + '","reference":"Plaza","schedule":"L-V","latitude":-33.4554,"longitude":-70.5982}]'
$script:responses["${base}8080/api/health"] = '{"status":"ok"}'
$script:responses["${base}8082/api/notifications/health"] = '{"status":"ok"}'
$script:responses["${base}8081/api/pickup-points"] = $p
$script:responses["${base}8080/api/pickup-points"] = $p
$script:responses["${base}8080/api/products"] = '[1,2,3,4,5,6,7,8]'
Invoke-MicroserviceChecks
Assert-Check $true 'Tres servicios, ocho productos, dos puntos.'
$script:responses["${base}8080/api/pickup-points"] = $p.Replace('Centro','Otro')
Assert-Failure { Invoke-MicroserviceChecks } 'Puntos de retiro distintos'
Write-Host ("OK: " + $script:checks + " comprobaciones PowerShell aisladas.") -ForegroundColor Green
