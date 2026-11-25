# Agregar -VisibleConsoles para controlar las tres consolas durante la demostracion.
param([switch]$VisibleConsoles)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$occupied = @([System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
    Where-Object { $_.Port -in @(8080, 8081, 8082) } | Select-Object -ExpandProperty Port -Unique)
if ($occupied.Count -gt 0) {
    throw ("Puertos ocupados: " + ($occupied -join ', ') + ". No se inicio ni se detuvo ningun servicio. Usa el script de comprobacion si ya estan funcionando.")
}
$jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path (Join-Path $jbr "bin\java.exe")) {
    $env:JAVA_HOME = $jbr
    $env:Path = "$jbr\bin;$env:Path"
}
& java -version
$previousToken = $env:OYL_SERVICE_TOKEN
$previousAdmin = $env:OYL_ADMIN_KEY
try {
    $secureToken = Read-Host "Token interno para los tres servicios (minimo 16 caracteres)" -AsSecureString
    $token = [System.Net.NetworkCredential]::new('', $secureToken).Password
    if ($token.Length -lt 16) { throw "El token interno debe tener al menos 16 caracteres" }
    $secureAdmin = Read-Host "Clave de administrador de la app (minimo 16 caracteres)" -AsSecureString
    $admin = [System.Net.NetworkCredential]::new('', $secureAdmin).Password
    if ($admin.Length -lt 16) { throw "La clave de administrador debe tener al menos 16 caracteres" }
    $env:OYL_SERVICE_TOKEN = $token
    $env:OYL_ADMIN_KEY = $admin
    $windowStyle = if ($VisibleConsoles) { 'Normal' } else { 'Hidden' }
    foreach ($module in @("pickup-service", "notification-service", "server")) {
        $task = ':' + $module + ':bootRun'
        $process = Start-Process -FilePath "powershell.exe" -WorkingDirectory $root -WindowStyle $windowStyle -PassThru -ArgumentList @(
            "-NoExit", "-Command", (".\gradlew.bat " + $task + " --console=plain")
        )
        Write-Host ("Iniciado " + $module + "; consola PID " + $process.Id)
    }
    Write-Host "Iniciados los tres procesos. Espera el arranque y ejecuta comprobar-microservicios.ps1."
    if ($VisibleConsoles) { Write-Host "No cierres esas consolas durante la demostracion." }
}
finally {
    # Los hijos heredan las claves; no dejarlas en el entorno de esta consola.
    $env:OYL_SERVICE_TOKEN = $previousToken
    $env:OYL_ADMIN_KEY = $previousAdmin
    $token = $null
    $admin = $null
    if ($secureToken) { $secureToken.Dispose() }
    if ($secureAdmin) { $secureAdmin.Dispose() }
}
