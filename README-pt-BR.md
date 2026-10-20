# CleanTV Início — edição brasileira do CleanYourTV

Uma tela inicial leve, sem anúncios e **totalmente em português do Brasil** para Android TV, feita a partir do
projeto [CleanYourTV](https://github.com/Gathin23/cleanyourtv) de [@Gathin23](https://github.com/Gathin23).
Todo o crédito pela base (launcher, ferramentas, listas de TVs) é do projeto original; esta edição adiciona a
tradução, funções novas e testes numa TV brasileira de entrada.

> Licença MIT, a mesma do original (ver `LICENSE`). Sem root, sem desbloqueio, sem firmware modificado. Tudo é
> reversível.

## O que esta edição adiciona

- **Português do Brasil completo** — todos os textos do launcher, data em pt-BR, dock com Netflix, YouTube,
  Globoplay e Prime Video.
- **Todos os apps** — busca sem acentos ("globo" acha Globoplay), ordens A–Z / Mais usados / Recentes,
  adicionar ou remover da tela inicial.
- **Menu do bloco** — segure OK (ou aperte Menu): Abrir, Mover, Remover da tela inicial, Informações do app.
- **Configurações na própria TV** — apps por linha, linhas abaixo do dock, cor de fundo, céu por horário,
  movimento reduzido, saudação e mais, com "Restaurar padrões".
- **Tela inicial padrão** — pede a troca pelo método oficial do Android e explica quando a TV não deixa;
  script para trocar e voltar pelo computador.
- **Correções** de foco do botão Início encontradas nos testes (também presentes no original).
- **Privacidade** — sem internet, sem telemetria, sem permissões novas; "Mais usados" conta só o que você abre
  pelo próprio launcher.

## Testado em

Philco (Caixun/Expressluck `martin`), MediaTek MT5867, **1 GB de RAM**, Android TV 14 — uma TV de entrada comum
no Brasil. Resultados, inclusive o que **não** melhorou, em [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) e
[`docs/CHECKLIST.md`](docs/CHECKLIST.md).

Deve funcionar em outras TVs Android TV / Google TV com Android 8 ou mais novo, mas cada fabricante é diferente:
teste primeiro como app comum.

## Como instalar (Windows)

Passo a passo completo em [`docs/INSTALACAO.md`](docs/INSTALACAO.md). Resumo:

1. Na TV: Configurações → Sistema → Sobre → aperte OK em **Build** 7 vezes → Opções do desenvolvedor →
   **Depuração USB**.
2. No PC: JDK 17, Android SDK 34 e o ADB (Platform-Tools).
3. Instale como app comum (não mexe na tela inicial):
   ```powershell
   powershell -ExecutionPolicy Bypass -File launcher\instalar-app.ps1 -TvHost <IP-da-TV> -Adb C:\caminho\adb.exe
   ```
4. Use pelo menu de apps da TV por uns dias. Gostou? Torne-o a tela inicial:
   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\tela-inicial.ps1 -TvHost <IP-da-TV> -Adb C:\caminho\adb.exe -Ativar
   ```
   Para voltar: o mesmo comando com `-Restaurar`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/INSTALACAO.md`](docs/INSTALACAO.md) | compilar, instalar, tela inicial, voltar, desinstalar, diagnóstico |
| [`docs/FEATURES.md`](docs/FEATURES.md) | funções do original e desta edição |
| [`docs/COMPATIBILIDADE.md`](docs/COMPATIBILIDADE.md) | requisitos e limitações conhecidas |
| [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) | medições reais e método |
| [`docs/AUDITORIA.md`](docs/AUDITORIA.md) | auditoria técnica do código original |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | o que mudou |

O README original (em inglês), com as listas de *debloat* de mais de 50 TVs, continua em [`README.md`](README.md).

## Créditos

- Projeto original, launcher e ferramentas: [CleanYourTV](https://github.com/Gathin23/cleanyourtv) por
  [@Gathin23](https://github.com/Gathin23) e contribuidores — MIT.
- Fonte Outfit: SIL Open Font License (`launcher/FONT-LICENSE-OFL.txt`).
- Ícone de busca: Material Icons (Apache 2.0).
