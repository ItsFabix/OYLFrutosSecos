<# Sign the existing release build using the original private keystore.
Passwords are entered interactively and kept only in this process environment.
Never run with a PowerShell transcript recording the interactive session.
#>
param([string]$Keystore = (Join-Path $env:USERPROFILE 'Documents/oyl-release.jks'))
$ErrorActionPreference = 'Stop'
$Host.UI.RawUI.WindowTitle = 'OYL - Firma APK release'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ((& git -C $root branch --show-current) -ne 'control3-tres-microservicios') { throw 'Rama incorrecta.' }
$sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$apksigner = Join-Path $sdk 'build-tools/36.0.0/apksigner.bat'
$keytool = Join-Path 'C:/Program Files/Android/Android Studio/jbr/bin' 'keytool.exe'
$unsigned = Join-Path $root 'app/build/outputs/apk/release/app-release-unsigned.apk'
$original = Join-Path $root 'app/release/app-release.apk'
$destination = Join-Path $root 'build/presentacion-hoy/release'
$expected = '9d1c01df179ec7901200ce8d8f373cb1dcfb2146404a20dffe9711b313131547'
foreach ($file in @($Keystore, $apksigner, $keytool, $unsigned, $original)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Falta: $file" }
}
$oldVerification = & $apksigner verify --verbose --print-certs $original
if ($LASTEXITCODE -ne 0 -or -not ($oldVerification -match $expected)) { throw 'La APK original no coincide con el certificado esperado.' }
New-Item -ItemType Directory -Force $destination | Out-Null
$output = Join-Path $destination ('OYL-Control3-release-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.apk')
if (Test-Path -LiteralPath $output) { throw 'El destino ya existe.' }
$previousStore = $env:OYL_SIGN_STORE_PASS
$previousKey = $env:OYL_SIGN_KEY_PASS
try {
    Write-Host 'Firma OYL: no crear otro keystore. La APK anterior se conserva.'
    $storeSecret = Read-Host 'Contrasena del keystore original (entrada oculta)' -AsSecureString
    $env:OYL_SIGN_STORE_PASS = [System.Net.NetworkCredential]::new('', $storeSecret).Password
    & $keytool -list -keystore $Keystore '-storepass:env' 'OYL_SIGN_STORE_PASS'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir el keystore con esa contrasena.' }
    $alias = Read-Host 'Alias ORIGINAL mostrado en la lista'
    if ([string]::IsNullOrWhiteSpace($alias)) { throw 'Se requiere el alias original.' }
    $keySecret = Read-Host 'Contrasena de la clave (Enter si coincide con la del keystore)' -AsSecureString
    $env:OYL_SIGN_KEY_PASS = if ($keySecret.Length -eq 0) { $env:OYL_SIGN_STORE_PASS } else { [System.Net.NetworkCredential]::new('', $keySecret).Password }
    & $apksigner sign --ks $Keystore --ks-key-alias $alias --ks-pass env:OYL_SIGN_STORE_PASS --key-pass env:OYL_SIGN_KEY_PASS --out $output $unsigned
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la firma. La APK original permanece intacta.' }
    $verification = & $apksigner verify --verbose --print-certs $output
    if ($LASTEXITCODE -ne 0 -or -not ($verification -match $expected)) { throw 'No entregar: la firma no es valida o el certificado es diferente.' }
    $verification | Set-Content -LiteralPath (Join-Path $destination 'apksigner-verificacion.txt') -Encoding UTF8
    $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
    @{ apk = $output; certificateSha256 = $expected; apkSha256 = $hash; verified = $true } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $destination 'release-verificada.json') -Encoding UTF8
    Write-Host "APK firmada y certificado original verificado: $output" -ForegroundColor Green
}
finally {
    $env:OYL_SIGN_STORE_PASS = $previousStore
    $env:OYL_SIGN_KEY_PASS = $previousKey
    if ($storeSecret) { $storeSecret.Dispose() }
    if ($keySecret) { $keySecret.Dispose() }
}
