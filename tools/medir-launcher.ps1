# Mede o launcher na TV com o mesmo roteiro para cada APK: instala, abre a frio 3 vezes (TotalTime do am start)
# e volta à Home 5 vezes depois de abrir outro app (Configurações), medindo o onResume pelo log de eventos;
# por fim lê a memória (PSS). Só toca no pacote com.gathin.tvhome:
# não desativa nada e não muda qual app é a tela inicial.
#
# Uso: powershell -ExecutionPolicy Bypass -File tools\medir-launcher.ps1 -TvHost 192.168.1.50 -Adb <adb.exe> -Apk a.apk,b.apk
#      (-Apk: um ou mais caminhos separados por virgula)
param(
    [Parameter(Mandatory = $true)][string]$TvHost,
    [Parameter(Mandatory = $true)][string]$Adb,
    [Parameter(Mandatory = $true)][string]$Apk,
    [int]$Frio = 3,
    [int]$Volta = 5
)
$ErrorActionPreference = 'Continue'
$s = "${TvHost}:5555"
$pkg = 'com.gathin.tvhome'
$main = "$pkg/.MainActivity"
$outro = 'com.android.tv.settings/.MainSettings'

function Sh([string]$cmd) { & $Adb -s $s shell $cmd 2>&1 | ForEach-Object { "$_" } }
# Acorda a tela antes de cada passo: com o protetor de tela ativo, am start -W fica esperando.
function Acordar { Sh 'input keyevent KEYCODE_WAKEUP' | Out-Null }
function Tempo([string]$saida) {
    $m = [regex]::Match(($saida -join "`n"), 'TotalTime: (\d+)')
    if ($m.Success) { [int]$m.Groups[1].Value } else { -1 }
}

& $Adb connect $s | Out-Null
$resultado = @()
foreach ($arquivo in ($Apk -split ',')) {
    $nome = Split-Path $arquivo -Leaf
    & $Adb -s $s push $arquivo /data/local/tmp/medir.apk | Out-Null
    $inst = Sh 'pm install -r /data/local/tmp/medir.apk'
    Sh 'rm -f /data/local/tmp/medir.apk' | Out-Null
    if (($inst -join ' ') -notmatch 'Success') { throw "instalacao falhou: $inst" }
    $versao = (Sh "dumpsys package $pkg | grep versionName | head -1").Trim()

    $temposFrio = @()
    for ($i = 0; $i -lt $Frio; $i++) {
        Acordar
        Sh "am force-stop $pkg" | Out-Null
        Start-Sleep 3
        $temposFrio += Tempo (Sh "am start -W -n $main")
        Start-Sleep 4
    }
    $temposVolta = @()
    for ($i = 0; $i -lt $Volta; $i++) {
        Acordar
        Sh "am start -W -n $outro" | Out-Null
        Start-Sleep 3
        Sh "am start -W -n $main" | Out-Null
        Start-Sleep 2
        # Android 12+: o log de eventos anota quanto o onResume levou (último campo de wm_on_resume_called).
        $ev = Sh "logcat -b events -d | grep wm_on_resume_called | grep $pkg.MainActivity | tail -1"
        $m = [regex]::Match(($ev -join ' '), ',(\d+)\]\s*$')
        $temposVolta += $(if ($m.Success) { [int]$m.Groups[1].Value } else { -1 })
        Start-Sleep 3
    }
    $pss = (Sh "dumpsys meminfo $pkg | grep 'TOTAL PSS'") -join ' '
    $pssKb = [regex]::Match($pss, 'TOTAL PSS:\s+(\d+)').Groups[1].Value
    $mediana = { param($v) $o = $v | Sort-Object; $o[[int][math]::Floor($o.Count / 2)] }
    $resultado += [pscustomobject]@{
        Apk = $nome; Versao = $versao
        FrioMs = ($temposFrio -join ' / '); FrioMediana = (& $mediana $temposFrio)
        VoltaMs = ($temposVolta -join ' / '); VoltaMediana = (& $mediana $temposVolta)
        PssKB = $pssKb
    }
}
$resultado | Format-List
