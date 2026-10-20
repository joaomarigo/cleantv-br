# Checklist

`[x]` concluído e verificado · `[ ]` pendente · `[!]` bloqueado ou dependente de validação externa.
TV = Philco/Caixun `martin`, Android TV 14, `192.168.1.50`, testes de 2026-10-20 com teclas simuladas por ADB e
capturas de tela (`launcher/app/build/tv-teste/`, fora do git).

## Auditoria
- [x] Estrutura, tecnologia, dependências e arquitetura levantadas — `docs/AUDITORIA.md`
- [x] Linha de base: 97 testes / 0 falhas, `lintRelease` 0 erros, APK 379.029 B (executado no commit `69c93b6`)
- [x] Telas do sistema disponíveis na TV verificadas com `cmd package resolve-activity`
- [x] Inventário de funcionalidades — `docs/FEATURES.md`

## Tradução
- [x] `values-pt-rBR/strings.xml` com 126/126 textos (script que compara os nomes com `values/strings.xml`)
- [x] Nenhum texto fixo em layouts ou `setText`/`Toast` com literal (busca no código; só números 4–7 nas colunas)
- [x] Plurais (`one`/`many`/`other`), data em pt-BR em duas linhas — captura `22-apos-reiniciar-app.png`
- [x] `pt-BR` em `locales_config.xml`
- [x] Concordância Sim/Não nos interruptores — verificado na TV ("Saudação: Sim")
- [!] Revisão por um segundo falante nativo

## Interface
- [x] Home em pt-BR com dock brasileiro — captura `01-home.png`
- [x] Botão Apps no topo — captura `03-foco-apps.png`
- [x] Todos os apps: lista, selos, contagem — `04-todos-os-apps.png`, `14-todos-v2.png`
- [x] Menu do bloco (segurar OK e tecla Menu) — `02-menu-bloco.png`, foco no `PopupWindow` confirmado
- [x] Soltar OK depois de segurar não ativa o 1º item — `02-menu-bloco.png`
- [x] Configurações do launcher com todas as seções — `18/19/20-config-*.png`
- [x] Diálogo Restaurar começa em Cancelar — verificado na TV
- [x] Menu Entradas lista HDMI 1–3 e TV ao vivo — `25-entradas.png`
- [ ] Tema claro (não implementado; ROADMAP P2)

## Funcionalidades
- [x] Busca sem acentos — teste de unidade + TV ("globo" → Globoplay, `05-busca-globo.png`)
- [x] Estado vazio da busca — `06-busca-vazia.png`
- [x] Voltar limpa a busca e devolve o foco ao 1º app — `07-apos-voltar.png`
- [x] Abrir app pela lista fecha a tela Todos os apps — app em primeiro plano, `AllAppsActivity` encerrada
- [x] Recentes — Manual aberto por último aparece em 1º (`09-recentes-manual.png`)
- [x] Mais usados e contagem persistem após `am force-stop` — `16-ordem-lembrada.png`
- [x] Ordenação escolhida é lembrada — `16-ordem-lembrada.png`
- [x] Remover da tela inicial — selo some, aviso, Home sem o app (lista completa por acessibilidade)
- [x] Adicionar à tela inicial — selo volta (`15-adicionado.png`)
- [x] Componentes de sistema só de celular escondidos — 19 → 17 apps
- [x] Início com foco no rodapé volta ao 1º app — 2/2 após correção; **1 falha** na 1ª tentativa logo após reinstalar
- [x] Início com foco no meio da grade volta ao 1º app — verificado
- [ ] Ordem "Mais usados" na grade da Home — implementada, não exercitada na TV
- [ ] Trocar de entrada HDMI pelo menu — não executado para não interromper o uso da TV
- [ ] Mover bloco pelo menu (modo reorganizar) — implementado, não exercitado na TV

## Desempenho
- [x] Medição comparativa original × BR — `docs/PERFORMANCE.md`, `tools/medir-launcher.ps1`
- [x] Resultado registrado sem exagero: diferença dentro do ruído; hipótese de 1,4 s corrigida
- [!] Memória/swap com o CleanTV como tela inicial de verdade — depende da troca (ADB)

## Segurança
- [x] Ação `LAUNCHER_SETTINGS` só com token interno — código; abrir pelo painel rápido continua funcionando no código, **não exercitado na TV** (painel só aparece com `settings_key_panel`)
- [x] Sem permissões novas; `<queries>` só por intent; sem rede; sem telemetria — manifesto revisado
- [x] Componentes novos não exportados (`AllAppsActivity`) — manifesto
- [x] Nenhum crash do launcher no `logcat -b crash` durante os testes

## Compatibilidade
- [x] Instala no Android TV 14 32 bits sem bloqueio — `pm install` → `Success`
- [x] Atalhos do sistema só quando existem; "Imagem e som" no lugar de "Imagem" — `20-config-3.png`
- [x] Pedido oficial de tela inicial (`RoleManager`) — diálogo do sistema exibido e aceito
- [!] **Botão Início continua indo para a tela de fábrica** mesmo com o papel concedido (prioridade 2) — `docs/COMPATIBILIDADE.md`

## Testes
- [x] 107 testes de unidade, 0 falhas (`testDebugUnitTest`)
- [x] `lintRelease` 0 erros
- [ ] Testes instrumentados (Espresso) — sem emulador Android TV configurado

## Build
- [x] `assembleDebug` — `launcher/app/build/outputs/apk/debug/app-debug.apk`
- [x] `assembleRelease` — `launcher/app/build/outputs/apk/release/app-release.apk` (~397 KB)
- [!] Assinatura release própria — usa a chave de debug local, como o original (`docs/INSTALACAO.md`, seção 1)

## Instalação
- [x] `launcher/instalar-app.ps1` instala sem mexer na tela inicial — executado 4 vezes na TV
- [x] `docs/INSTALACAO.md`: compilar, instalar, permissões, tela inicial, testar, voltar, diagnosticar, desinstalar
- [!] Caminho por ADB (desativar `tvlauncher`) e volta — documentado, **não executado** (exige sua autorização)

## Validação em TV física
- [x] Todos os itens marcados acima como verificados na TV
- [!] Uso como tela inicial após reinicialização da TV — depende do caminho por ADB
- [!] Uso por vários dias (estabilidade em sessão longa)
