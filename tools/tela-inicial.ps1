# Troca a tela inicial da TV entre o CleanTV Início e a de fábrica, pelo ADB (docs/INSTALACAO.md, seções 5.2 e 7).
# Nada é desinstalado: a tela de fábrica é só desativada, e -Restaurar a reativa.
#
#   powershell -ExecutionPolicy Bypass -File tools\tela-inicial.ps1 -TvHost 192.168.1.50 -Adb <adb.exe> -Ativar
#   powershell -ExecutionPolicy Bypass -File tools\tela-inicial.ps1 -TvHost 192.168.1.50 -Adb <adb.exe> -Restaurar
#   powershell -ExecutionPolicy Bypass -File tools\tela-inicial.ps1 -TvHost 192.168.1.50 -Adb <adb.exe>          (só mostra o estado)
param(
    [Parameter(Mandatory = $true)][string]$TvHost,
    [string]$Adb = 'adb',
    [switch]$Ativar,
    [switch]$Restaurar,
    [string]$Fabrica = 'com.google.android.tvlauncher'
)
$ErrorActionPreference = 'Continue'
$s = "${TvHost}:5555"
$novo = 'com.gathin.tvhome/.MainActivity'

function Sh([string]$cmd) { ((& $Adb -s $s shell $cmd 2>&1) -join ' ').Trim() }
function Resolve { ((& $Adb -s $s shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1) | Select-Object -Last 1).Trim() }

& $Adb connect $s | Out-Null
if (((& $Adb -s $s get-state 2>&1) -join '') -notmatch 'device') { throw "TV não conectada em $s." }

if ($Ativar -and $Restaurar) { throw 'Use -Ativar ou -Restaurar, não os dois.' }

if ($Ativar) {
    if ((Sh 'pm path com.gathin.tvhome') -notmatch 'package:') { throw 'O CleanTV Início não está instalado (launcher\instalar-app.ps1).' }
    # Ordem do projeto original: abrir o novo, preferir, desativar o de fábrica, preferir de novo, conferir.
    Sh "am start -W -n $novo" | Out-Null
    Sh "cmd package set-home-activity --user 0 $novo" | Out-Null
    Write-Host (Sh "pm disable-user --user 0 $Fabrica")
    Start-Sleep 2
    Sh "cmd package set-home-activity --user 0 $novo" | Out-Null
    $r = Resolve
    if ($r -notlike 'com.gathin.tvhome/*') {
        Write-Host "O HOME foi para '$r', não para o CleanTV. Reativando a tela de fábrica..."
        Write-Host (Sh "pm enable $Fabrica")
        throw 'Troca desfeita. Veja docs/INSTALACAO.md (Google TV: setupwraith).'
    }
}
if ($Restaurar) {
    Write-Host (Sh "pm enable $Fabrica")
    # Com as duas telas ativas, a de fábrica (prioridade 2) volta a responder; a preferência confirma.
    Sh "cmd package set-home-activity --user 0 $Fabrica/.MainActivity" | Out-Null
}

Write-Host "Botão Início abre: $(Resolve)"
Write-Host "Tela de fábrica ($Fabrica): $(if ((Sh 'pm list packages -d') -match [regex]::Escape("package:$Fabrica")) { 'desativada' } else { 'ativa' })"
