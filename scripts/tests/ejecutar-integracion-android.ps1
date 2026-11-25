<# Ejecuta instrumentacion completa con un backend de ventas exclusivo de pruebas.
No usa ni detiene 8080/8081/8082. La base H2 es de memoria y la clave se genera
para esta ejecucion; no se escribe en archivos ni se pasa en argumentos Gradle.
Requiere un emulador activo, Java y Android SDK configurados.
#>
param([string]$DeviceSerial = "")
$ErrorActionPreference = 'Stop'
if ($DeviceSerial -notmatch '^emulator-[0-9]+$') {
    throw 'Indica -DeviceSerial emulator-NNNN de un AVD dedicado con datos respaldados o descartables. No se inicia instrumentacion sin seleccionar el destino.'
}
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if (-not (Test-Path $adb)) { $adb = (Get-Command adb).Source }
$deviceState = & $adb -s $DeviceSerial get-state
if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') { throw 'El emulador seleccionado no esta disponible.' }
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$gradle = Join-Path $root 'gradlew.bat'
& $gradle :server:bootJar --console=plain
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el JAR de pruebas.' }
$output = Join-Path $root 'build/final-polish'
New-Item -ItemType Directory -Force $output | Out-Null
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$testKey = 'isolated-' + [guid]::NewGuid().ToString('N')
$argumentName = 'ORG_GRADLE_PROJECT_android.testInstrumentationRunnerArguments.oylIntegrationAdminKey'
$previousArgument = [Environment]::GetEnvironmentVariable($argumentName, 'Process')
$previousAdmin = $env:OYL_ADMIN_KEY
$previousToken = $env:OYL_SERVICE_TOKEN
$previousSerial = $env:ANDROID_SERIAL
$testProcess = $null
$testExit = 1
try {
    $env:OYL_ADMIN_KEY = $testKey
    $env:OYL_SERVICE_TOKEN = ''
    $jar = Join-Path $root 'server/build/libs/server-0.0.1-SNAPSHOT.jar'
    $testProcess = Start-Process -FilePath (Get-Command java).Source -WorkingDirectory $root -WindowStyle Hidden -PassThru `
        -ArgumentList @('-jar', ('"' + $jar + '"'), "--server.port=$port", '--server.address=127.0.0.1',
            '--spring.datasource.url=jdbc:h2:mem:android_integration;DB_CLOSE_DELAY=-1', '--spring.main.banner-mode=off') `
        -RedirectStandardOutput (Join-Path $output 'isolated-server.log') `
        -RedirectStandardError (Join-Path $output 'isolated-server-error.log')
    $env:OYL_ADMIN_KEY = $previousAdmin
    $env:OYL_SERVICE_TOKEN = $previousToken
    $ready = $false
    for ($attempt = 0; $attempt -lt 50; $attempt++) {
        if ($testProcess.HasExited) { throw 'El backend aislado termino antes de iniciar. Revisa isolated-server.log.' }
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/health" -TimeoutSec 1
            if ($health.status -eq 'ok') { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 200
    }
    if (-not $ready) { throw 'No inicio el backend H2 aislado dentro del plazo.' }
    [Environment]::SetEnvironmentVariable($argumentName, $testKey, 'Process')
    $env:ANDROID_SERIAL = $DeviceSerial
    Write-Host "Backend de prueba listo en puerto $port con H2 de memoria. Servicios existentes intactos."
    & $gradle :app:connectedDebugAndroidTest --console=plain `
        '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true' `
        "-Pandroid.testInstrumentationRunnerArguments.oylIntegrationBaseUrl=http://10.0.2.2:$port/" `
        '-Pandroid.testInstrumentationRunnerArguments.oylReadOnlyProbeUrl=http://10.0.2.2:8080/' `
        '-Pandroid.testInstrumentationRunnerArguments.oylWeatherLiveProbe=true'
    $testExit = $LASTEXITCODE
}
finally {
    [Environment]::SetEnvironmentVariable($argumentName, $previousArgument, 'Process')
    $env:OYL_ADMIN_KEY = $previousAdmin
    $env:OYL_SERVICE_TOKEN = $previousToken
    $env:ANDROID_SERIAL = $previousSerial
    $testKey = $null
    if ($null -ne $testProcess -and -not $testProcess.HasExited) {
        # Solo el proceso creado aqui: nunca buscar/matar procesos por nombre o puerto.
        Stop-Process -Id $testProcess.Id
        $testProcess.WaitForExit()
    }
}
exit $testExit
