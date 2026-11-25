# Ejecutar desde la raiz: powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-microservicios.ps1
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jbr = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path (Join-Path $jbr "bin\java.exe")) {
    $env:JAVA_HOME = $jbr
    $env:Path = "$jbr\bin;$env:Path"
}
& java -version
$secureToken = Read-Host "Token interno para los tres servicios (minimo 16 caracteres)" -AsSecureString
$token = [System.Net.NetworkCredential]::new('', $secureToken).Password
if ($token.Length -lt 16) { throw "El token interno debe tener al menos 16 caracteres" }
$env:OYL_SERVICE_TOKEN = $token
$secureAdmin = Read-Host "Clave de administrador de la app (minimo 16 caracteres)" -AsSecureString
$admin = [System.Net.NetworkCredential]::new('', $secureAdmin).Password
if ($admin.Length -lt 16) { throw "La clave de administrador debe tener al menos 16 caracteres" }
$env:OYL_ADMIN_KEY = $admin
foreach ($module in @("pickup-service", "notification-service", "server")) {
    $task = ':' + $module + ':bootRun'
    Start-Process -FilePath "powershell.exe" -WorkingDirectory $root -ArgumentList @(
        "-NoExit", "-Command", (".\gradlew.bat " + $task + " --console=plain")
    )
}
Write-Host "Abiertas tres consolas: retiro 8081, notificaciones 8082 y ventas 8080."
Write-Host "No cierres esas consolas durante la demostracion."
