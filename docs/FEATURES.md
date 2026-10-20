# Funcionalidades — original × edição brasileira

Legenda: **Mantida** = preservada sem mudança de comportamento; **Alterada** = continua existindo, com a
mudança descrita; **Nova** = adicionada nesta edição; **Removida** = substituída (motivo ao lado).

## Tela inicial (Home)

| Funcionalidade | Estado | Detalhe |
|---|---|---|
| Fundo escuro, dock com 4+ apps numa faixa, grade abaixo | Mantida | |
| Destaque de vidro com mola entre os blocos | Mantida | |
| Nome grande do app e brilho na cor do banner (prateleira) | Mantida | agora pode ser desligado na TV |
| Saudação, relógio e data | Alterada | saudação pode ser desligada; data em `pt-BR` ("terça-feira, 20 de outubro") |
| Botões Entradas, Wi-Fi/rede, Configurações | Mantida | |
| Botão **Apps** (todos os apps e busca) | Nova | aparece com `app_library` ligado (padrão em `pt-BR`) |
| Botão Reorganizar (opcional) | Mantida | |
| Navegação determinística por D-pad | Mantida | |
| Lembrar o último bloco focado | Mantida | |
| Atualizar a grade ao instalar/remover apps | Alterada | por evento de pacote (como antes) e, ao voltar à Home, **só se algum pacote mudou** |
| Segurar OK no bloco | Alterada | antes abria "Informações do app"; agora abre um menu: Abrir, Mover, Remover da tela inicial, Informações do app |
| Tecla Menu do controle no bloco | Nova | abre o mesmo menu |
| Retorno visual ao abrir um app | Nova | leve escurecimento do bloco (sem custo de layout) |
| Nome do app para leitores de tela | Nova | `contentDescription` em cada bloco |
| Ordem da grade por "mais usados" | Nova | opcional; conta só aberturas feitas por este launcher |
| Rodapé "Biblioteca de apps" | Alterada | agora abre a tela Todos os apps |
| Cartões de módulo (outros apps propõem um bloco) | Mantida | |
| Linha de CPU/memória/armazenamento | Mantida | agora também liga/desliga na TV |
| Céu por horário | Mantida | agora também liga na TV (Suave/Intenso) |
| Tema festivo (Diwali) | Mantida | só por tema |
| Papel de parede por tema (`config.json` + `wallpaper.jpg`) | Mantida | |
| Protetor de tela Relógio | Mantida | |
| Serviços de acessibilidade opcionais (tecla Início/Input) | Mantida | |

## Todos os apps (nova tela)

| Funcionalidade | Estado | Detalhe |
|---|---|---|
| Lista de todos os apps que abrem (TV e Android comuns) | Nova | uma entrada por pacote, preferindo a de TV |
| Busca local sem acentos e sem diferença de maiúsculas | Nova | "globo" acha "Globoplay"; cada palavra precisa aparecer no nome ou no pacote |
| Ordem A–Z, Mais usados, Recentes | Nova | "Recentes" = última abertura por este launcher |
| Selo "Na tela inicial" e "Novo" (instalado há ≤ 7 dias) | Nova | |
| Abrir, Adicionar/Remover da tela inicial, Informações do app | Nova | segurar OK ou tecla Menu |
| Estados vazios ("Nenhum app encontrado para …") | Nova | |
| Atualiza ao instalar/remover apps com a tela aberta | Nova | |
| Teclado de controle com letras vai direto para a busca | Nova | |
| Voltar limpa a busca; Voltar de novo volta à Home | Nova | |
| Biblioteca horizontal antiga (`AppLibraryDialog`) | Removida | substituída por esta tela; os atalhos salvos continuam valendo (mesmo arquivo) |

## Configurações do launcher

| Opção | Estado | Persistência |
|---|---|---|
| Apps por linha (4–7) | Mantida | `SharedPreferences` |
| Linhas visíveis abaixo do dock (0–3) | Nova na TV | idem |
| Dock em faixa separada | Nova na TV | idem |
| Nome grande acima do dock | Nova na TV | idem |
| Ordem dos outros apps (A–Z / Mais usados) | Nova | idem |
| Botão Apps | Nova na TV | idem |
| Saudação | Nova | idem |
| Linha de CPU/memória/armazenamento | Nova na TV | idem |
| Cor de fundo (Preto, Grafite, Azul-noite) | Nova | idem |
| Céu por horário (Desligado, Suave, Intenso) | Nova na TV | idem |
| Animações | Mantida | idem |
| Movimento reduzido | Nova na TV | idem |
| Efeitos avançados (com aviso) | Mantida | idem |
| Tela inicial padrão: estado atual + pedido oficial | Alterada | mostra qual app o botão Início abre; usa `RoleManager` quando existe |
| Atalhos: Configurações da TV, Imagem (ou Imagem e som), Rede, Apps, Sobre | Alterada | só aparecem se a TV tiver a tela |
| Restaurar configurações padrão (com confirmação) | Alterada | substitui "Seguir o tema"; apaga todas as escolhas da TV |

Todas as escolhas valem por cima de `config.xml` e do tema, sobrevivem a reinicializações e são apagadas
quando um tema é enviado ou redefinido (comportamento original, estendido a todas as chaves).

## Painel rápido (botão Configurações do controle, opcional)

| Item | Estado |
|---|---|
| Configurações da TV, Rede, Apps (reativar), Configurações do launcher | Mantida |
| Imagem | Alterada: some quando a TV não tem a tela; usa "Imagem e som" quando é o que existe |

## Planejado (não implementado)

| Funcionalidade | Motivo |
|---|---|
| Tema claro / automático | toda a identidade visual foi feita para fundo escuro; exige redesenho (ROADMAP P2) |
| Seções/categorias em linhas na Home, reordenar seções | a Home é uma grade única com navegação própria; exige reestruturação (ROADMAP P2) |
| Escolher papel de parede pela TV | muitas TVs não têm seletor de arquivos; continua por tema |
| Reiniciar/desligar a TV | exige permissão de sistema |
| Uso de apps abertos fora do launcher | exige acesso especial `PACKAGE_USAGE_STATS` |
| Recomendações de conteúdo | exige integrações com serviços; fora do escopo |
