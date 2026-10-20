# Changelog — edição brasileira

Base: CleanYourTV `main` @ `69c93b6` (launcher 2.4, versionCode 5). Branch: `br/launcher-pt-br`.

## 2.4-br.1 (versionCode 6) — 2026-10-20

### Desempenho
- A Home **não reconstrói mais a grade a cada volta**: só quando um app foi instalado, atualizado ou removido
  (`PackageManager.getChangedPackages`) ou quando um atalho da tela inicial mudou (`LibraryApps.revision`).
  Antes, cada volta relia rótulo, banner e ícone de todos os apps na thread principal.
- Registro de aberturas gravado em segundo plano (thread de baixa prioridade), arquivo pequeno com limite.
- Todos os apps: lista lida fora da thread principal; ícones carregados só quando o bloco aparece; a tela é
  encerrada ao abrir um app, liberando memória.

### Funcionalidades
- **Todos os apps** (nova tela): busca sem acentos, ordens A–Z / Mais usados / Recentes, selos "Na tela inicial"
  e "Novo", menu Abrir / Adicionar ou Remover da tela inicial / Informações do app, estados vazios, atualização ao
  instalar/remover, letras de teclado vão direto para a busca.
- Botão **Apps** no topo da Home (com `app_library`, padrão em pt-BR).
- **Menu do bloco** na Home (segurar OK ou tecla Menu): Abrir, Mover, Remover da tela inicial, Informações do app.
- Retorno visual ao abrir um app.
- Ordem opcional "Mais usados" para a grade da Home (`grid_sort`).
- **Configurações do launcher** refeitas: linhas abaixo do dock, dock separado, nome grande, ordem da grade,
  botão Apps, saudação, linha de sistema, cor de fundo, céu, movimento reduzido, mais as opções originais;
  tudo persistido e com **Restaurar configurações padrão**.
- Tela inicial padrão: mostra qual app o botão Início abre e pede a troca por API oficial
  (`RoleManager.ROLE_HOME` → `Settings.ACTION_HOME_SETTINGS` → explicação do caminho por ADB).
- Atalhos do sistema só aparecem se a TV tiver a tela; "Imagem e som" quando não há tela de Imagem.
- Novas chaves de configuração: `show_greeting`, `grid_sort` (também em `config.json` de temas).

### Correções encontradas nos testes na TV
- Início com o foco no botão "Todos os apps" do rodapé voltava ao rodapé; agora vai ao 1º app (defeito do original).
- `focusTile` esperava para sempre uma rolagem que não começava quando o bloco ainda não estava montado
  (o foco caía num bloco qualquer); agora aplica o foco assim que o bloco aparece (defeito do original).
- Todos os apps esconde componentes de fábrica que só têm entrada de celular (ex.: "HotKeyService").
- Todos os apps lembra a ordenação escolhida.
- Data da Home em duas linhas em pt-BR (cortava ao lado dos 4 botões).
- Interruptores em pt-BR com Sim/Não (concordância).
- Diálogo Restaurar começa em Cancelar.
- Quando o Android concede o papel de tela inicial mas a tela de fábrica continua com o botão Início, as
  configurações explicam isso e não reabrem o pedido.

### Localização
- `values-pt-rBR/strings.xml` completo (126/126 textos), revisado para o português do Brasil.
- `values-pt-rBR/config_br.xml`: dock com Netflix, YouTube, Globoplay e Prime Video; botão Apps ligado.
- `pt-BR` em `locales_config`.
- Nome do app em pt-BR: **CleanTV Início**.

### Segurança e acessibilidade
- A ação interna `LAUNCHER_SETTINGS` só é aceita com um token que existe apenas no processo do launcher
  (a `MainActivity` é exportada por ser Home; antes qualquer app podia abrir as configurações por cima dela).
- `<queries>` só para as intents usadas (nenhum `QUERY_ALL_PACKAGES`).
- Blocos da Home e de Todos os apps com descrição para leitores de tela; cabeçalhos de seção marcados como títulos.

### Removido
- `AppLibraryDialog` (biblioteca horizontal), substituída por Todos os apps. Os atalhos salvos continuam valendo:
  mesmo arquivo (`LibraryApps`). Removidas as funções que só ela usava (`comparePriority`, `nextFocus`) e os
  5 testes delas, o desenho `library_check` e os textos `library_recent`, `library_hint_add`, `follow_theme`,
  `follow_theme_hint` (em todos os idiomas).

### Ferramentas e documentação
- `launcher/instalar-app.ps1`: compila e instala **sem** desativar nada nem mudar a tela inicial.
- `tools/medir-launcher.ps1`: roteiro de medição reproduzível.
- `docs/`: AUDITORIA, ROADMAP, FEATURES, COMPATIBILIDADE, PERFORMANCE, INSTALACAO, CHECKLIST, este CHANGELOG.

### Testes
- Novos: `AppSearchTest`, `LaunchStatsTest`, `ConfigChoicesTest` (15 testes). Total: 107 testes, 0 falhas.
