# Instalação — CleanTV Início (edição brasileira do CleanYourTV)

Pacote: `com.gathin.tvhome` (o mesmo do projeto original, para instalar por cima de uma versão
anterior). Nome na TV em português: **CleanTV Início**. Não há APK pronto para baixar: você compila
e sabe exatamente o que vai para a TV, como no projeto original.

> Nada aqui exige root, desbloqueio ou troca de firmware. Tudo é reversível.

## 1. Compilar

Requisitos no computador (Windows):

| Item | Versão | Observação |
|---|---|---|
| JDK | 17 | Android Studio traz um em `jbr`; ou Eclipse Temurin 17 (zip) |
| Android SDK | `platforms;android-34`, `build-tools;34.0.0` | via Android Studio ou `cmdline-tools` (`sdkmanager`) |
| Gradle | 8.9 | baixado automaticamente pelo `gradlew` |
| ADB | qualquer recente | Android SDK Platform-Tools |

```powershell
cd launcher
$env:JAVA_HOME = 'C:\caminho\do\jdk-17'
# local.properties com: sdk.dir=C\:\\caminho\\do\\Android\\Sdk
.\gradlew.bat assembleRelease      # APK: launcher\app\build\outputs\apk\release\app-release.apk
.\gradlew.bat assembleDebug        # APK de teste: launcher\app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat testDebugUnitTest    # testes de unidade
```

**Assinatura:** o release é assinado com a chave de *debug* do seu computador (`%USERPROFILE%\.android\debug.keystore`),
como no projeto original. Para atualizar o app na TV depois, compile **no mesmo computador** (ou copie esse
arquivo); com outra chave a TV recusa a atualização e é preciso desinstalar antes. Para um release com chave
própria, crie um keystore (`keytool -genkeypair ...`) e configure um `signingConfigs.release` no
`app/build.gradle` — sem colocar senhas no repositório.

## 2. Preparar a TV (uma vez)

1. Configurações → Sistema (ou Preferências do dispositivo) → Sobre → aperte OK em **Build** 7 vezes.
2. Volte → Opções do desenvolvedor → ligue **Depuração USB** (e "Depuração por rede", se existir).
3. Anote o IP: Configurações → Rede e internet → sua rede. A TV e o PC precisam estar na **mesma rede**.

## 3. Instalar como app comum (sem mudar a tela inicial)

```powershell
powershell -ExecutionPolicy Bypass -File launcher\instalar-app.ps1 -TvHost 192.168.1.50 -Adb C:\caminho\platform-tools\adb.exe
```

Na primeira conexão, aceite **"Permitir depuração USB?"** na TV (marque "Sempre permitir").
O script só instala: **não** desativa nada e **não** mexe na tela inicial. Abra **CleanTV Início** pela
lista de apps da TV e teste à vontade. Se a TV recusar a instalação, nada mais foi alterado.

Equivalente manual:

```bash
adb connect 192.168.1.50:5555
adb push app-release.apk /data/local/tmp/cleantv.apk
adb shell pm install -r /data/local/tmp/cleantv.apk
adb shell rm /data/local/tmp/cleantv.apk
```

## 4. Permissões

Nenhuma permissão precisa ser concedida para o launcher funcionar. Opcional:

| Permissão | Para quê | Como |
|---|---|---|
| `ACCESS_FINE_LOCATION` | mostrar o **nome** da rede Wi-Fi no botão de rede (Android 10+ exige) | `adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION` |

Sem ela, o botão mostra apenas "Wi-Fi". O launcher não usa localização para mais nada e não acessa a internet.

## 5. Usar como tela inicial padrão

### 5.1 Caminho oficial (tente primeiro)

No launcher: segure OK no botão **Configurações** → **Configurações do launcher** → seção *Tela inicial padrão*
→ **Usar como tela inicial padrão**. O launcher pede ao Android, por API pública:

1. o pedido de papel de tela inicial (`RoleManager.ROLE_HOME`, Android 10+), ou
2. a lista de telas iniciais do sistema (`Settings.ACTION_HOME_SETTINGS`).

A mesma tela mostra qual app o botão Início abre agora.

**Limitação comum em Android TV:** a tela inicial de fábrica (`com.google.android.tvlauncher`) declara
**prioridade 2** no filtro HOME; apps instalados ficam limitados à prioridade 0. Em muitos firmwares isso faz o
botão Início continuar indo para a tela de fábrica mesmo depois da escolha. O resultado na TV de referência
está em `docs/COMPATIBILIDADE.md`.

### 5.2 Caminho manual (ADB), quando o oficial não basta

Só faça isto depois de ter usado o CleanTV Início como app comum e confirmado que ele funciona.

```bash
# 1) Abra o novo launcher e defina a preferência
adb shell am start -n com.gathin.tvhome/.MainActivity
adb shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
# 2) Desative (não desinstale) a tela inicial de fábrica
adb shell pm disable-user --user 0 com.google.android.tvlauncher
# 3) Defina a preferência de novo (o Android a esquece quando o conjunto de telas iniciais muda)
adb shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
# 4) Confira: deve responder com.gathin.tvhome/.MainActivity
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Se o passo 4 mostrar outro app, **volte imediatamente** (seção 7). Em Google TV, o assistente de configuração
`com.google.android.tungsten.setupwraith` também disputa a tela inicial; veja `docs/TROUBLESHOOTING.md`.
Na TV de referência (Android TV 14, não Google TV) ele **não** aparece entre os candidatos.

## 6. Testar o botão Início

1. Aperte **Início** no controle: deve abrir o CleanTV Início.
2. Abra um app (ex.: YouTube), aperte **Início**: volta ao CleanTV Início.
3. Reinicie a TV (Configurações → Sistema → Reiniciar). Depois do boot, a tela inicial deve ser o CleanTV Início.
4. Confira **Entradas** (HDMI e TV ao vivo) e o teclado na busca de **Apps**.

## 7. Voltar à tela inicial original

Pelo computador (sempre funciona enquanto a depuração estiver ligada):

```bash
adb shell pm enable com.google.android.tvlauncher
adb shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Sem computador: Configurações → Apps → Ver todos os apps → *Apps do sistema* → **Android TV Home** →
**Ativar**. Depois, se a TV perguntar qual tela inicial usar, escolha a original.

## 8. Diagnóstico

| Sintoma | Verificação / solução |
|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | APK assinado com outra chave: desinstale (seção 9) e instale de novo |
| `INSTALL_FAILED_VERIFICATION_FAILURE` | firmware que bloqueia apps de fora; use apenas o launcher original |
| Início ainda abre a tela de fábrica | prioridade 2 do launcher de fábrica: seção 5.2 |
| Botão Wi-Fi sem nome da rede | permissão opcional da seção 4 |
| Lista de apps vazia | `adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER` |
| Travou / fecha sozinho | `adb logcat -b crash -d` e `adb logcat -d -s AndroidRuntime` |
| Sem teclado na busca | `adb shell ime list -s`; selecione o Gboard em Configurações → Sistema → Teclado |

## 9. Desinstalar sem ficar sem tela inicial

**Primeiro** devolva a tela inicial original (seção 7) e confira que o botão Início abre ela. Só então:

```bash
adb shell pm uninstall com.gathin.tvhome
```

ou Configurações → Apps → CleanTV Início → Desinstalar. Se a tela de fábrica estiver desativada no momento da
desinstalação, a TV fica só com a tela de emergência do sistema (`FallbackHome`, sem apps) até ela ser reativada
pela seção 7.
