# Roadmap — versão brasileira do launcher

Prioridades: **P0** = fundação/estabilidade, **P1** = funcionalidade essencial, **P2** = desejável.
O estado de cada item fica em `docs/CHECKLIST.md`.

## P0 — Fundação

| Item | O quê | Por quê |
|---|---|---|
| F1 | Reconstruir a grade só quando um pacote mudou (`PackageManager.getChangedPackages`) | Remove trabalho pesado da thread principal a cada volta à Home (auditoria D1) |
| F2 | Ação interna `LAUNCHER_SETTINGS` só com *token* do próprio processo | Segurança (S1) |
| F3 | Atalhos do sistema exibidos apenas se o sistema os resolve; "Imagem e som" via `SOUND_SETTINGS` quando `DISPLAY_SETTINGS` não existe | Compatibilidade (C1) |
| F4 | `contentDescription` nos blocos | Acessibilidade (U5) |

## P0 — Localização

| Item | O quê |
|---|---|
| L1 | `values-pt-rBR/strings.xml` completo, revisado para o português do Brasil |
| L2 | Padrões brasileiros em `values-pt-rBR/config_br.xml` (dock com apps populares no Brasil, "Todos os apps" ligado) |
| L3 | `locales_config` com `pt-BR` |
| L4 | Todos os textos novos em `strings.xml` (inglês) + `pt-BR`; nada fixo no código |

## P1 — Home

| Item | O quê |
|---|---|
| H1 | Tela **Todos os apps**: grade, busca sem acentos, ordenação (A–Z, Mais usados, Recentes), estados vazios, atualização ao instalar/remover |
| H2 | **Registro local de aberturas** (`LaunchStats`): contagem e última abertura por app, sem permissões |
| H3 | Ordem da grade da Home: alfabética ou mais usados |
| H4 | **Menu do bloco** (segurar OK ou tecla Menu): Abrir, Mover, Remover da tela inicial, Informações do app |
| H5 | Retorno visual ao abrir um app; mensagem clara se falhar |
| H6 | Pílula **Apps** no topo para abrir Todos os apps |

## P1 — Personalização (persistente)

Apps por linha, linhas abaixo do dock, dock separado, nome em destaque, ordem da grade, botão
Todos os apps, saudação, informações do sistema, cor de fundo, céu por horário, animações,
movimento reduzido, efeitos avançados, **restaurar padrões**.

## P1 — Tela inicial padrão

| Item | O quê |
|---|---|
| T1 | Botão "Definir como tela inicial": `RoleManager.ROLE_HOME` → `ACTION_HOME_SETTINGS` → instruções |
| T2 | Mostrar qual app é a tela inicial atual |
| T3 | `docs/INSTALACAO.md` com o caminho manual (ADB) e a volta segura |

## P2 — Depois desta entrega

- Tema claro (exige redesenho das cores; ver auditoria 5.5).
- Linhas por categoria na Home (exige reestruturar a navegação da grade única).
- Carregar banners em segundo plano.
- Testes instrumentados (Espresso) — exige emulador Android TV.
- Assinatura release própria (hoje: chave de debug local, como no projeto original).

## Fora do escopo (limitações do sistema)

Reiniciar/desligar a TV, ler o uso de outros apps sem permissão especial, desativar o launcher
original sem ADB, recomendações de conteúdo de serviços de streaming.
