# Compatibilidade

## Requisitos

| Item | Valor |
|---|---|
| Sistema | Android TV ou Google TV, Android 8.0 (API 26) ou mais novo |
| Compilado para | API 34 (`compileSdk`/`targetSdk` 34) |
| Arquitetura | qualquer (Java puro, sem bibliotecas nativas): funciona em sistemas 32 bits (`armeabi-v7a`) e 64 bits |
| Tela | sem toque; controle remoto com setas, OK e Voltar |
| Memória | projetado para TVs de 1 GB |
| Permissões obrigatórias | nenhuma além de estado de rede/Wi-Fi (normais, concedidas na instalação) |
| Internet | não usa |

Não funciona em Samsung (Tizen), LG (webOS) nem como tela inicial em Fire TV (o Fire OS não entrega o botão
Início a terceiros), como no projeto original.

## TV de referência (testada)

| Propriedade | Valor (lido por ADB) |
|---|---|
| Marca/fabricante | Philco · `ro.product.manufacturer=Expressluck` · `ro.product.brand=Caixun` |
| Modelo / placa | `AiPlus2K` / `martin` (`Caixun_2K_ATV2`) |
| SoC | MediaTek `mt5867`, 4× Cortex-A55, **sistema 32 bits** |
| Sistema | Android TV 14 (API 34), build `UKNV.260502.001`, patch 2026-05-01, `ro.config.low_ram=true` |
| RAM | 935 MB (`MemTotal`) |
| Tela | 1920×1080 físico, interface em 1280×720 (213 dpi) |
| Tela inicial de fábrica | `com.google.android.tvlauncher/.MainActivity`, **HOME priority 2** |
| Outros candidatos a HOME | `com.android.tv.settings/.system.FallbackHome` (priority −1000) |
| `setupwraith` | instalado, mas **não** disputa o HOME |
| TV ao vivo / entradas | `com.mediatek.wwtv.tvcenter`; HDMI 1–3 em `com.mediatek.tvinput/.hdmi.HDMIInputService/HW4–HW6`; sintonizador `HW0`/`HW7` |
| Telas do sistema disponíveis | Configurações, Imagem e som (`SOUND_SETTINGS`), Rede, Apps, Sobre, escolha de tela inicial (`HOME_SETTINGS` no controlador de permissões); **sem** `DISPLAY_SETTINGS` |

Resultados de instalação e uso nesta TV: ver `docs/CHECKLIST.md` (seção *Validação em TV física*) e
`docs/PERFORMANCE.md`.

## Limitações conhecidas

| Limitação | Origem | Contorno |
|---|---|---|
| Instalar o APK não troca a tela inicial | sistema: o launcher de fábrica tem prioridade 2, apps instalados no máximo 0 | pedido oficial pela tela de configurações; se não bastar, ADB (`docs/INSTALACAO.md`, 5.2) |
| Desativar o launcher de fábrica sem computador | não há API pública | ADB |
| Reiniciar/desligar a TV pelo launcher | exige permissão de sistema (`REBOOT`) | usar o menu da TV |
| "Mais usados"/"Recentes" só contam aberturas feitas pelo launcher | ler o uso de outros apps exige `PACKAGE_USAGE_STATS` | escolha deliberada (privacidade e menor privilégio) |
| Nome da rede Wi-Fi | Android 10+ exige localização | permissão opcional por ADB |
| Capturas de tela pretas com HDMI/TV ao vivo | camada de vídeo protegida | não afeta o uso |
| Firmwares que bloqueiam apps de fora (`INSTALL_FAILED_VERIFICATION_FAILURE`) | fabricante | nenhum: usar o launcher de fábrica |
| Google TV: `setupwraith` assume o HOME | sistema | desativar junto (ver `docs/TROUBLESHOOTING.md`) |

## Idiomas

Português do Brasil completo (`values-pt-rBR`, 126/126 textos) com padrões brasileiros (dock com Netflix,
YouTube, Globoplay e Prime Video; botão Apps ligado). Os outros 17 idiomas do projeto original continuam; os textos
novos aparecem em inglês neles até alguém traduzir. Android 13+ permite escolher o idioma só do launcher:
`adb shell cmd locale set-app-locales com.gathin.tvhome --locales pt-BR`.
