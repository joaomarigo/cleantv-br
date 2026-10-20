# Auditoria técnica — CleanYourTV (launcher) para a versão brasileira

Data: 2026-10-20. Base auditada: `main` do repositório oficial no commit `69c93b6`
("launcher: version 2.4 (versionCode 5)"), 542 commits. As alterações deste trabalho estão na
branch `br/launcher-pt-br`.

Tudo o que está marcado como **verificado** foi lido no código ou executado. **Medido** indica
uma execução real na TV de referência.

---

## 1. Estrutura do repositório

| Pasta | Conteúdo |
|---|---|
| `launcher/` | O app Android (único código que roda na TV) |
| `tools/` | Cliente ADB em Python (`tv.py`, `b64push.py`) e scripts `disable`/`restore`/`theme` (sh e ps1) |
| `examples/` | ~50 listas `debloat.txt` por modelo de TV |
| `themes/` | Temas (`config.json` + papel de parede opcional) enviados por `tools/theme.*` |
| `docs/` | Especificação de cartões de módulo, solução de problemas, dispositivos |
| `tests/` | Teste Python dos caminhos das listas |

## 2. Tecnologia (verificado em `launcher/`)

| Item | Valor |
|---|---|
| Linguagem | Java 17 (sem Kotlin) |
| UI | Views clássicas + `RecyclerView` (sem Compose, sem Leanback) |
| Gradle / AGP | wrapper Gradle 8.9 / Android Gradle Plugin 8.5.2 |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |
| Dependências | `androidx.recyclerview:1.3.2`, `androidx.dynamicanimation:1.0.0`; teste: `junit:4.13.2` |
| Pacote | `com.gathin.tvhome`, versionCode 5, versionName 2.4 |
| Release | R8 com `minifyEnabled` e `shrinkResources`, sem ofuscação, **assinado com a chave de debug local** |
| Testes | 10 classes JUnit de lógica pura, **97 testes, 0 falhas** (executado) |
| Lint (`lintRelease`) | **0 erros**, cerca de 52 avisos e 61 informativos de tradução ausente (executado) |
| APK release | **379.029 bytes** (executado) |

## 3. Arquitetura

Um único processo, sem serviços em segundo plano permanentes.

- **`MainActivity`** (2.075 linhas): a Home. Cabeçalho (saudação, relógio, data), pílulas
  (Entradas, Wi-Fi, Reordenar, Configurações), grade `RecyclerView` em que a 1ª linha é o
  *dock*, destaque de vidro (`GlassView`) com mola (`SpringAnimation`), "prateleira" com o nome do
  app e brilho ambiente na cor do banner, menu de entradas (TV Input Framework), modo de
  reordenação, cartões de módulo e linha opcional de CPU/memória/armazenamento.
- **`AppAdapter`**: descobre os apps (`queryIntentActivities` com `LEANBACK_LAUNCHER`, mais os
  apps comuns adicionados pela biblioteca), carrega banner/ícone, ordena (ordem salva →
  `dock_apps` → cartões → alfabética) e cria os blocos.
- **`Config`**: lê `res/values/config.xml`, sobrepõe `config.json` de um tema
  (`/sdcard/Android/data/com.gathin.tvhome/files/`) e depois as escolhas feitas na TV
  (`SharedPreferences "launcher_settings"`: só colunas, animações e efeitos avançados).
- **`LauncherSettingsActivity`**: colunas (4–7), animações, efeitos avançados, "seguir o tema",
  tela inicial padrão e configurações da TV.
- **`QuickSettingsActivity`**: painel aberto pelo botão de configurações do controle (opcional).
- **`AppLibraryDialog` + `LibraryApps`**: biblioteca opcional (`app_library`, desligada por padrão)
  em uma janela horizontal; adiciona/remove atalhos da Home, persistidos em arquivos `AtomicFile`.
- **`Order`**: ordem personalizada dos blocos (arquivo JSON).
- **`Cards`/`CardFeature`**: cartões de módulo de outros apps, com consentimento por chave de
  assinatura.
- **Serviços opcionais**: `HomeKeyService` e `InputKeyService` (acessibilidade, só se ativados por
  ADB) e `ClockDream` (protetor de tela).
- **Extras visuais**: `SkyView` (céu por horário), `Festival` + drawables (tema festivo de Diwali).

### Como inicia e como lista apps

`MainActivity` declara `MAIN` + `HOME` + `DEFAULT` (pode ser Home) e `MAIN` + `LEANBACK_LAUNCHER`
(aparece na lista de apps da TV). A visibilidade de pacotes do Android 11+ é resolvida com
`<queries>` por *intent* (`LAUNCHER`, `LEANBACK_LAUNCHER`, categoria de cartão): **não** usa
`QUERY_ALL_PACKAGES`. Correto e com o menor privilégio.

### Permissões

| Permissão | Uso | Avaliação |
|---|---|---|
| `ACCESS_NETWORK_STATE` | estado da rede na pílula | necessária |
| `ACCESS_WIFI_STATE` | sinal/SSID | necessária |
| `ACCESS_FINE_LOCATION` | ler o nome da rede Wi-Fi (Android 10+) | opcional; nunca pedida em tempo de execução, só concedida por ADB. Sem ela a pílula mostra "Wi-Fi" |

Nenhuma permissão de internet: **o launcher não acessa a rede**.

## 4. Inventário de funcionalidades (antes das alterações)

Ver `docs/FEATURES.md` (coluna "Original").

## 5. Problemas encontrados

### 5.1 Desempenho (com evidência)

| # | Problema | Evidência |
|---|---|---|
| D1 | **A grade inteira é reconstruída na thread principal toda vez que se volta à Home** (`onStop` marca `rebuildOnResume`; `onResume` chama `reloadApps` → `PackageManager.queryIntentActivities`, `loadLabel`, `loadBanner` e `loadIcon` de cada app), mesmo sem nenhuma instalação ou remoção. | Código: `MainActivity.onStop/onResume`, `AppAdapter.reload`. **Medido** na TV (v2.2): abertura "HOT" do launcher em **1.375 ms**. |
| D2 | A biblioteca de apps carrega ícone, rótulo e data de instalação de todos os apps na thread principal ao abrir. | `AppLibraryDialog.refresh` |
| D3 | A TV de referência roda com ~350 MB em swap logo após o boot; cada MB conta. | Medido (relatório de diagnóstico) |

### 5.2 Compatibilidade

| # | Problema | Evidência |
|---|---|---|
| C1 | O atalho "Display" do painel rápido usa `Settings.ACTION_DISPLAY_SETTINGS`, que **não existe** nesta TV (resolve para "No activity found"). Resultado: toast de erro. | Verificado na TV com `cmd package resolve-activity` |
| C2 | O "launcher original" com `HOME` **priority=2** (`com.google.android.tvlauncher`) vence filtros de terceiros (limitados a prioridade 0). Instalar o APK não troca a Home. | Verificado na TV |
| C3 | `Settings.ACTION_HOME_SETTINGS` existe nesta TV (`permissioncontroller/.role.ui.HomeSettingsActivity`), mas não se sabe se a escolha vence a prioridade 2 sem desativar o original. | Verificado (resolução); comportamento **não testado** |

### 5.3 Segurança

| # | Problema | Avaliação |
|---|---|---|
| S1 | `MainActivity` é exportada (obrigatório para ser Home) e aceita a ação `com.gathin.tvhome.action.LAUNCHER_SETTINGS`. Qualquer app instalado pode abrir as configurações do launcher por cima da Home. | Baixo impacto (só abre a tela), mas desnecessário: a ação deve ser aceita só de dentro do próprio app. |
| S2 | Nenhuma rede, nenhuma telemetria, `allowBackup=false`, serviços exportados protegidos por permissões de sistema (`BIND_ACCESSIBILITY_SERVICE`, `BIND_DREAM_SERVICE`), receptor de pacotes registrado como `RECEIVER_NOT_EXPORTED`. | Adequado |
| S3 | Logs: apenas avisos de leitura de `config.json`/atalhos, sem dados pessoais. | Adequado |

### 5.4 Usabilidade e acessibilidade

| # | Problema |
|---|---|
| U1 | **Português do Brasil incompleto**: existe `values-pt` com ~45 textos e mistura de português europeu ("pega numa app", "guardar"). Faltam ~20 textos (configurações do launcher, painel rápido, efeitos, colunas), que aparecem em inglês. |
| U2 | **Sem busca** de apps. |
| U3 | **Sem "Recentes" nem "Mais usados"**: não há registro de uso. |
| U4 | Segurar OK num bloco abre direto "Informações do app"; não há como remover um app da Home a partir da Home. |
| U5 | Os blocos não têm `contentDescription`: o TalkBack não lê o nome do app. |
| U6 | A biblioteca de apps vem desligada por padrão e é uma faixa horizontal; com muitos apps, achar um item exige rolar muito. |
| U7 | Personalização na TV limitada a colunas/animações; o resto só por tema via ADB. |
| U8 | Não há como "restaurar padrões" além de "seguir o tema". |

### 5.5 Limitações do sistema (não contornáveis sem privilégios)

- **Reiniciar/desligar a TV**: exige `REBOOT`/`SHUTDOWN`, permissões de sistema. Um app comum não pode.
- **Tempo de uso de outros apps**: `UsageStatsManager` exige `PACKAGE_USAGE_STATS` (acesso especial). Optamos
  por registrar apenas as aberturas feitas pelo próprio launcher.
- **Desativar o launcher original**: exige ADB (`pm disable-user`). Nenhuma API pública faz isso.
- **Tema claro**: toda a identidade visual (pílulas, vidro, brilho, textos semitransparentes) foi
  desenhada para fundo escuro. Um tema claro exigiria refazer cores em todos os layouts e drawables.
  Fica fora desta etapa (registrado no roadmap).

## 6. Qualidade do código

- Código maduro e bem comentado, com decisões justificadas (desempenho em GPUs fracas, foco
  determinístico, robustez contra arquivos corrompidos).
- `MainActivity` é grande (2.075 linhas), mas está organizado por seções. Não há motivo para
  reescrever: as melhorias serão incrementais, em classes novas e pequenas.
- Testes cobrem a lógica pura (ordem, dock, navegação, cartões, biblioteca). A UI não tem testes
  instrumentados; a validação de UI é feita na TV real.
- Avisos de lint relevantes: `NotifyDataSetChanged`, `RtlHardcoded` (intencional, documentado no layout),
  `MissingQuantity` em traduções.

## 7. Conclusão

A base é adequada para a versão brasileira: leve, sem dependências pesadas, sem rede e com
menor privilégio. Prioridades:

1. Não reconstruir a grade sem necessidade (D1).
2. pt-BR completo (U1).
3. Busca, todos os apps, recentes e mais usados (U2, U3, U6).
4. Menu de ações no bloco e acessibilidade (U4, U5).
5. Configurações reais na TV (U7, U8).
6. Atalhos do sistema apenas quando existem (C1); fluxo de tela inicial padrão documentado (C2, C3).
7. Restringir a ação interna de configurações (S1).
