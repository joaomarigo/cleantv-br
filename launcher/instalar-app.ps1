# Compila o launcher e o instala na TV COMO APP COMUM: não desativa nada e não muda a tela inicial.
# (O build-and-install.ps1 original desativa o launcher da TV; este não.) Para torná-lo a tela inicial,
# siga docs/INSTALACAO.md.
#
# Uso: powershell -ExecutionPolicy Bypass -File launcher\instalar-app.ps1 -TvHost 192.168.1.50 [-Adb C:\...\adb.exe] [-SemCompilar]
# Requer: JDK 17 (JAVA_HOME) e Android SDK com platforms;android-34 e build-tools;34.0.0 (ANDROID_HOME ou local.properties).
param(
    [Parameter(Mandatory = $true)][string]$TvHost,
    [string]$Adb = 'adb',
    [switch]$SemCompilar
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$s = "${TvHost}:5555"

if (-not $SemCompilar) {
    if (-not $env:JAVA_HOME) { throw 'Defina JAVA_HOME para um JDK 17 (o Android Studio traz um na pasta jbr).' }
    .\gradlew.bat assembleRelease -q --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
}
$apk = 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $apk)) { throw "APK não encontrado: $apk" }

& $Adb connect $s | Out-Null
$estado = (& $Adb -s $s get-state 2>&1) -join ' '
if ($estado -notmatch 'device') { throw "TV não conectada ($estado). Ligue a depuração e aceite 'Permitir depuração' na TV." }

& $Adb -s $s push $apk /data/local/tmp/cleantv.apk | Out-Null
$saida = (& $Adb -s $s shell pm install -r /data/local/tmp/cleantv.apk 2>&1) -join ' '
& $Adb -s $s shell rm -f /data/local/tmp/cleantv.apk | Out-Null
Write-Host $saida
if ($saida -notmatch 'Success') { throw 'A TV recusou a instalação. Nada mais foi alterado.' }

$inicioAtual = (& $Adb -s $s shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1 | Select-Object -Last 1)
Write-Host "Instalado. A tela inicial continua sendo: $inicioAtual"
Write-Host 'Abra "CleanTV Início" pela lista de apps da TV para testar.'
