# Desempenho

Todos os números abaixo foram **medidos** na TV de referência (Philco/Caixun `martin`, MT5867, 935 MB, Android TV
14, `low_ram=true`), salvo quando marcados como **meta** ou **estimativa**. Nada aqui foi medido em emulador.

## Contexto da TV (medido)

Logo após o boot, sem nenhuma alteração, a TV já tem ~350 MB em swap comprimido (ZRAM) e mata apps em segundo
plano continuamente (30 mortes pelo LMK nos primeiros 6 minutos). Durante as medições desta etapa, o swap estava
entre 434 MB e 540 MB. É um ambiente com muita variação: abrir qualquer app pode levar o dobro do tempo de
uma tentativa para outra.

## Método

Script reproduzível: `tools/medir-launcher.ps1`. Para cada APK, na mesma sessão e com a TV no mesmo estado:

1. instala com `pm install -r` (mesmo `versionCode` nas duas versões, para que uma substitua a outra);
2. **abertura a frio** ×3: `am force-stop com.gathin.tvhome` → `am start -W` → `TotalTime`;
3. **volta à Home** ×5: abre as Configurações da TV, volta ao launcher, e lê a duração do `onResume` no log de
   eventos do sistema (`wm_on_resume_called`, último campo, Android 12+). O `TotalTime` do `am start` não serve
   aqui: com a atividade já existente ele não é informado;
4. **memória**: `TOTAL PSS` de `dumpsys meminfo com.gathin.tvhome` no fim.

Antes de cada passo o script acorda a tela (`KEYCODE_WAKEUP`): na primeira tentativa o protetor de tela entrou no
meio da medição e travou o `am start -W`.

## Resultados (2026-10-20, 10:03)

| Métrica | Original 2.4 (`69c93b6`) | Edição BR 2.4-br.1 | Leitura |
|---|---|---|---|
| Abertura a frio, ms | 2.697 / 4.311 / 1.255 | 2.613 / 1.181 / 1.164 | variação maior que a diferença; **sem conclusão** |
| `onResume` ao voltar, ms | 23 / 17 / 13 / 7 / 11 (mediana 13) | 8 / 7 / 19 / 15 / 5 (mediana 8) | ambos baixos; diferença dentro do ruído |
| PSS no fim, MB | 57,4 | 58,4 | **equivalentes** (+1 MB) |
| Tamanho do APK release | 379.029 B | 397.077 B | +18 KB (nova tela, textos) |

Uma rodada anterior, interrompida pelo protetor de tela, deu PSS de 68,0 MB (original) e 57,0 MB (BR). A rodada
completa acima mostrou valores iguais, então a primeira diferença era ruído.

## O que mudou e o efeito real

| Otimização | Efeito medido |
|---|---|
| Não reconstruir a grade ao voltar se nenhum pacote mudou (`getChangedPackages`) | **Pequeno.** Nesta TV a reconstrução custa só ~5–15 ms. A hipótese inicial (de que ela causava ~1,4 s) estava **errada**: o 1.375 ms de ontem era a troca de tarefa completa até o primeiro quadro, uma medida diferente. A mudança fica porque elimina trabalho repetido sem custo, mas não é um ganho perceptível. |
| Todos os apps: lista lida em segundo plano, ícones sob demanda, tela encerrada ao abrir um app | Não comparável com a biblioteca antiga (a tela é nova). Garante que a memória da tela não fica presa depois do uso. |
| Registro de aberturas em thread de baixa prioridade | Sem custo visível na thread principal (gravação assíncrona). |

## Comparação com a tela inicial de fábrica (medido no diagnóstico)

| Processo | PSS |
|---|---|
| `com.google.android.tvlauncher` (de fábrica), após o boot | 117 MB |
| `com.google.android.tvrecommendations` (fileiras de fábrica) | 12–19 MB |
| CleanTV Início (esta edição), em uso | 57–61 MB |

## Com o CleanTV como tela inicial (medido, 1 amostra)

Tela de fábrica desativada por ADB, TV reiniciada, medição aos 3 minutos de uptime — o mesmo ponto da linha de
base do diagnóstico (19/10, tela de fábrica):

| Métrica aos ~3 min após o boot | Tela de fábrica (19/10) | CleanTV Início (20/10) |
|---|---|---|
| Swap em ZRAM | 347 MB | 319 MB |
| Used RAM | 924 MB | 939 MB |
| Free RAM | 260 MB | 220 MB |
| Mortes pelo LMK desde o boot | 30 (em ~6 min) | 32 (em ~3 min) |
| `tvlauncher` rodando | sim (117 MB PSS logo depois) | **não** |
| Primeiro `onResume` da tela inicial no boot | — | 3.667 ms |

**Leitura honesta:** com uma amostra de cada lado, em dias e estados diferentes, **não dá para afirmar ganho de
memória**. O swap ficou 28 MB menor, mas a RAM usada e as mortes pelo LMK ficaram no mesmo patamar: outros
processos ocupam o espaço liberado (`tvrecommendations` continua ativo, 16 MB). O que é certo é que o processo de
117 MB da tela de fábrica não roda mais. Uma comparação válida exige várias reinicializações de cada lado.

## Limitações dos testes

- Uma única TV; 3 aberturas a frio e 5 voltas por versão. Poucas amostras para uma TV com tanta variação.
- O launcher não era a tela inicial durante as medições (o botão Início ainda ia para a de fábrica). Os números
  medem o app aberto como app comum.
- Fluidez da navegação (quadros perdidos) não foi comparada entre as versões; o diagnóstico mediu só a tela de
  fábrica (p99 de 600 ms).
- Sessões longas (dias) não foram testadas.

## Metas ainda não validadas

- Volta à Home sem travadas perceptíveis com o launcher como tela inicial.
- Swap pós-boot menor que os ~350 MB medidos com a tela de fábrica.
