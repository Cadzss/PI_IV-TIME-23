# PI_IV-TIME-23 — Early-Warning de Evasão Escolar

Projeto Integrador IV · Time 23 · PUC-Campinas — Engenharia de Software · 2026

Sistema que identifica **antecipadamente** os alunos em risco de evasão escolar. Ele cruza **frequência, notas e
engajamento** (atrasos e atividades entregues), calcula um nível de risco (ALTO / MÉDIO / BAIXO), avisa a coordenação
quando um aluno piora e acompanha as providências tomadas — para a escola agir **antes** do abandono, e não depois.

**Usuário (persona):** Coordenador Pedagógico / Gestor Escolar (e professores). O aluno é o **beneficiário**, não usuário
(relatório de ideação, seção 2.1.3) — por isso não existe conta de aluno.

---

## Sumário

1. [O que o sistema faz](#o-que-o-sistema-faz)
2. [Fluxo de uso](#fluxo-de-uso)
3. [Como rodar](#como-rodar)
4. [Arquitetura](#arquitetura)
5. [Login, permissões e contas](#login-permissões-e-contas)
6. [Planilha (Excel ou CSV)](#planilha-excel-ou-csv)
7. [Regra do score de risco](#regra-do-score-de-risco)
8. [Providências](#providências-ações-recomendadas-da-tela-2)
9. [Avisos e e-mail](#avisos-tela-3-e-e-mail)
10. [Ações do protocolo](#ações-do-protocolo-socket)
11. [Testes](#testes)
12. [Estrutura de pastas e documentos](#estrutura-de-pastas-e-documentos)
13. [Autoria](#autoria)

---

## O que o sistema faz

| Tela | O que tem |
|---|---|
| **Entrar / Criar conta** | Login por e-mail e senha (com o "olho" para ver a senha). Criar conta pede o **código da escola**. |
| **Painel** (Tela 1 do protótipo) | Alunos ordenados do maior para o menor risco; filtros por turma, risco e providência; busca; indicadores (risco alto, risco médio, **risco alto sem providência**, médias). No primeiro acesso mostra as **boas-vindas** com o passo a passo. |
| **Detalhe do aluno** (Tela 2) | Selo de risco, **linha do tempo Frequência x Notas** (últimos 3 meses), sinais identificados, **ações recomendadas** que viram providências, histórico completo com **adicionar/corrigir/excluir lançamento** e remover aluno (coordenador). |
| **Avisos** (Tela 3) | Mudanças de nível ("Maria S. mudou de risco BAIXO para MÉDIO"), **relatório semanal de risco** e situação do e-mail. O menu mostra quantos avisos não foram lidos. |
| **Enviar planilha** | Excel (.xlsx) ou CSV, **planilha modelo** para baixar e **prévia** antes de gravar. |
| **Equipe** (coordenador) | Quem tem acesso; redefinir senha (senha temporária) e remover acesso. |
| **Minha conta** | Trocar o nome e alterar a senha. |

## Fluxo de uso

1. **Primeiro acesso** — o coordenador entra e o painel mostra as boas-vindas: *baixar a planilha modelo → preencher → enviar*.
2. **Enviar planilha** — escolhe o arquivo do Excel, confere a **prévia** (alunos novos, já cadastrados, linhas com problema) e confirma.
3. **Painel** — o servidor calcula o risco de cada aluno; os de risco alto aparecem primeiro.
4. **Detalhe do aluno** — vê por que o aluno está em risco e registra uma **providência** (ex.: "Acionar assistente social").
5. **Avisos** — quando um aluno muda de nível, aparece um aviso (e um e-mail se entrou em risco alto); toda semana sai um resumo.
6. **Acompanhamento** — ao concluir a providência, registra o resultado; o indicador "risco alto sem providência" mostra quem ainda não foi atendido.

O usuário **nunca mexe no banco**: tudo entra pela planilha ou pelas telas.

---

## Como rodar

### 1. Instale (uma vez só)

| Programa | Versão | Download |
|---|---|---|
| Java (JDK) | 11 ou superior | https://adoptium.net |
| Node.js | 18 ou superior | https://nodejs.org |
| MongoDB | qualquer recente | https://www.mongodb.com/try/download/community |

O **Maven não precisa ser instalado**: o projeto já vem com o `mvnw` (Maven Wrapper), que baixa tudo sozinho.
Sem vontade de instalar o MongoDB? Com Docker, basta `docker compose up -d` na raiz do projeto.

### 2. Rode

**Mac / Linux**
```bash
./rodar.sh             # sobe tudo (banco vazio = tela de boas-vindas no painel)
./rodar.sh --popular   # APAGA alunos/avisos/providências e carrega 12 alunos de exemplo
```

**Windows** (Prompt de Comando, na pasta do projeto)
```bat
rodar.bat
rodar.bat --popular
```

Abra **http://localhost:5173**. Na primeira execução as dependências são baixadas, então demora um pouco mais.

**Contas de demonstração** (criadas sempre que o servidor sobe, se não existirem):
`coordenador@escola.local` / `coordenador` e `professor@escola.local` / `professor`.
Código da escola para criar conta: `escola-2026` (troque com a variável `SENHA_INSTITUCIONAL`).

### Rodando manualmente (passo a passo)

Cada parte em um terminal (no Windows use `mvnw.cmd` no lugar de `./mvnw`):

```bash
# terminal 1 - servidor Socket
cd backend
./mvnw -q compile exec:java@popular   # opcional: dados de exemplo
./mvnw -q compile exec:java@servidor  # para desligar: digite "desativar"

# terminal 2 - ponte HTTP -> Socket
cd backend
./mvnw -q exec:java@ponte

# terminal 3 - frontend
cd frontend
npm install
npm run dev
```

### Problemas comuns

- **"O sistema está fora do ar no momento"**: a ponte (terminal 2) ou o servidor (terminal 1) não está rodando.
- **Erro de conexão com o MongoDB**: confira se ele está ativo na porta 27017. Outro endereço (ex.: MongoDB Atlas): variável `MONGO_URI`.
- **Porta em uso (12345, 8080 ou 5173)**: feche a execução anterior e rode de novo.
- **Voltou para a tela de login sozinho**: o servidor foi reiniciado (as sessões ficam em memória). Entre de novo.

---

## Arquitetura

```
React (navegador) --HTTP--> PonteHttp (Java, cliente Socket) --Socket TCP--> Servidor (Java) --> MongoDB
   :5173                        :8080                                          :12345            :27017
```

**Toda a regra de negócio fica no Servidor Java**: cálculo do risco, validação da planilha, avisos, relatório semanal,
e-mail, login, permissões e acesso ao banco. A ponte só traduz HTTP ⇄ Socket (o navegador não abre Socket TCP), e o
React só mostra as telas.

- **backend/** (Java 11 + Maven, pacote `br.puc.evasao`)
  - `protocolo/` — o que viaja pelo Socket, **no padrão passado pelo professor** (Fazedor de Continhas): `Comunicado`
    (serializável), `PedidoDeOperacao`, `PedidoDeResultado`, `Resultado`, `PedidoParaSair`, `ComunicadoDeDesligamento`
    e o `Parceiro` (`receba`, `envie`, `espie`, `adeus`) sobre `ObjectOutputStream` / `ObjectInputStream`.
    `Protocolo` guarda os nomes das ações e o filtro de segurança da desserialização.
  - `servidor/Servidor` — sobe a `AceitadoraDeConexao` e lê o comando `desativar` no console
  - `servidor/AceitadoraDeConexao` — thread do `ServerSocket`; cada cliente ganha uma `SupervisoraDeConexao` (outra thread)
  - `servidor/ProcessadorPedidos` — confere a sessão e a permissão e executa cada ação
  - `servidor/Sessoes`, `Permissoes` — token de login e o que cada função pode pedir
  - `servidor/CalculadoraRisco` — regra do score (frequência, nota, quedas e engajamento)
  - `servidor/ImportadorCsv` — leitura/validação da planilha e prévia
  - `servidor/RelatorioSemanal` — resumo semanal de risco
  - `servidor/RegrasUsuario`, `RegrasProvidencia`, `SenhaHash` — validações e hash de senha (PBKDF2)
  - `servidor/AlunoRepositorio`, `AlertaRepositorio`, `UsuarioRepositorio`, `ProvidenciaRepositorio`, `Banco` — MongoDB
  - `servidor/NotificadorEmail` — e-mail de risco alto (em segundo plano)
  - `ponte/Cliente` + `ponte/TratadoraDeComunicadoDeDesligamento` — cliente Socket; reconecta sozinho se a conexão cair
  - `ponte/PonteHttp` — expõe o `Cliente` em HTTP para o React
- **frontend/** (React 18 + Vite + Tailwind 3 + Recharts)
  - `src/api.js` (única porta para o servidor), `src/auth.js` (sessão), `src/planilha.js` (Excel → CSV e planilha modelo),
    `src/formatos.js` (datas, notas, textos)
  - `src/pages/` — Login, Cadastro, Dashboard, DetalheAluno, Alertas, Importar, Equipe, MinhaConta
  - `src/components/` — BadgeRisco, CampoSenha, Estado, Icones

**Segurança:** servidor (:12345) e ponte (:8080) escutam **só em localhost** (o navegador chega pela ponte via proxy do
Vite); pelo Socket só passam objetos do protocolo (`Protocolo.FILTRO_DESSERIALIZACAO`); senhas guardadas com hash e salt.

---

## Login, permissões e contas

No login (`AUTENTICAR`) o servidor devolve um **token**; todo pedido seguinte leva esse token dentro dos `dados`. Sem
token válido a resposta é `{"ok": false, "codigo": "SESSAO_EXPIRADA"}` e a tela volta para o login. As sessões ficam em
memória e duram 8 horas.

| Função | Cadastro | Pode |
|---|---|---|
| Coordenador | código da escola | painel, detalhe, **avisos**, enviar planilha, corrigir lançamentos, providências, **remover aluno**, recalcular, **Equipe**, Minha conta |
| Professor | código da escola | painel, detalhe, enviar planilha, corrigir lançamentos, providências, recalcular, Minha conta |

- **Minha conta** — trocar o nome e alterar a senha (pede a senha atual; desconecta as outras sessões da pessoa).
- **Equipe** — o coordenador **redefine a senha** de alguém (senha temporária mostrada uma única vez; a pessoa é lembrada
  de trocar ao entrar) e **remove o acesso** de quem saiu da escola (desconecta na hora). Não dá para remover a si mesmo.
- Contas antigas com função `aluno` são recusadas no login.

---

## Planilha (Excel ou CSV)

1. **Enviar planilha** — arquivo do **Excel (.xlsx)** ou **.csv**. O `.xlsx` é lido no navegador (`read-excel-file`) e
   convertido para o mesmo CSV que o servidor valida. Botão **"Baixar planilha modelo"** (`write-excel-file`).
2. **Prévia** — mostra alunos novos/já cadastrados, linhas com problema e os primeiros lançamentos. Só grava em
   **"Confirmar e salvar"**.
3. **Correções pontuais** — na tela do aluno: adicionar, corrigir ou excluir lançamento; o risco é recalculado na hora.

Formato (primeira linha = nomes das colunas; acentos e maiúsculas são aceitos, ex.: `Matrícula`, `Frequência`):

```
matricula,nome,turma,data,frequencia,nota,atrasos,atividades
2026001,João P.,8ºA,01/11/2026,62,5.2,6,47
```

- `atrasos` (quantidade no período) e `atividades` (% entregues, 0 a 100) são **opcionais**.
- Data em `01/11/2026` ou `2026-11-01`, e precisa existir (`31/02/2026` é recusado; `29/02/2024` entra, ano bissexto).
- No Excel, frequência e atividades podem estar em formato de porcentagem (62% é lido como 62).
- Mesma matrícula e mesma data: o lançamento novo substitui o antigo. Exemplo completo: `backend/dados/exemplo.csv`.

---

## Regra do score de risco

| Condição | Pontos |
|---|---|
| Frequência atual < 75% (abaixo do mínimo — sozinha já dá ALTO) | +4 |
| Frequência atual < 85% | +1 |
| Nota atual < 5,0 | +2 |
| Nota atual < 7,0 | +1 |
| Queda de nota ≥ 1,0 nos últimos 3 registros | +1 |
| Queda de frequência ≥ 10 pontos percentuais nos últimos 3 registros | +1 |
| Engajamento (se a planilha trouxer): atrasos no período ≥ 5 | +1 |
| Engajamento: atividades entregues < 75% / < 50% | +1 / +2 |

**≥ 4 → ALTO · 2–3 → MÉDIO · 0–1 → BAIXO.** Cobre os quatro sinais do mapa mental: queda de frequência, redução de
desempenho, atrasos recorrentes e ausência em atividades.

---

## Providências (ações recomendadas da Tela 2)

Clicar numa ação recomendada (ex.: **"Acionar assistente social"**) registra uma **providência** — quem registrou,
quando e uma observação. Ela fica **em andamento** até alguém **concluir** contando o resultado. O painel mostra
**"Risco alto sem providência"**, que mede o objetivo 2.2.2 do relatório ("aumentar a proporção de alunos em risco que
recebem alguma intervenção"). Ao concluir, se o aluno continua em risco alto, ele volta a contar como "sem providência".
Prazo, aviso de atraso e encaminhamento automático para a assistente social ficam para a evolução "Fluxo de encaminhamento".

## Avisos (Tela 3) e e-mail

- **Mudança de nível** — mesmo texto do protótipo: `Novo aluno em risco ALTO: Ana C. (7ºC)`,
  `Maria S. (9ºB) mudou de risco BAIXO para MÉDIO`.
- **Relatório semanal de risco disponível** — quantos alunos em cada nível e quantos pioraram/melhoraram nos últimos
  7 dias (gerado ao abrir os avisos, se ainda não houver um da semana).
- **E-mail de risco alto** — enviado **em segundo plano** (a importação não espera). Status: `pendente` → `enviado`,
  `falha` ou, sem SMTP, `copia_local` (cópia em `logs/emails.log`, na pasta onde o servidor roda — normalmente `backend/`).

| Variável | Função |
|---|---|
| `EMAIL_COORDENADOR` | quem recebe (padrão `coordenador@escola.local`) |
| `EMAIL_REMETENTE` | quem envia |
| `SMTP_HOST` / `SMTP_PORT` | servidor SMTP (sem ele, só grava a cópia local) / porta (padrão 587) |
| `SMTP_USUARIO` / `SMTP_SENHA` | login no servidor SMTP, se ele pedir |
| `SENHA_INSTITUCIONAL` | código da escola para criar conta (padrão `escola-2026`) |
| `MONGO_URI` / `MONGO_DB` | endereço e nome do banco (padrão `mongodb://localhost:27017` / `early_warning_evasao`) |

---

## Ações do protocolo (Socket)

O cliente manda um `PedidoDeOperacao` (ação + dados em JSON) e depois um `PedidoDeResultado`; o servidor responde com
um `Resultado` (`{"ok": true, "dados": ...}` ou `{"ok": false, "erro": "..."}`). Menos `AUTENTICAR`,
`CADASTRAR_USUARIO` e `ENCERRAR_SESSAO`, toda ação exige `token` e a permissão da função.

| Ação | Dados | Retorno |
|---|---|---|
| `AUTENTICAR` | `{email, senha}` | `{nome, email, funcao, token, senhaTemporaria?}` |
| `CADASTRAR_USUARIO` | `{nome, email, senha, funcao, senhaInstitucional}` | igual ao `AUTENTICAR` |
| `ENCERRAR_SESSAO` | `{token}` | — |
| `LISTAR_ALUNOS` | `{turma?, nivel?}` | alunos ordenados por risco, com `emAcompanhamento` |
| `DETALHAR_ALUNO` | `{matricula}` | aluno com histórico, risco e providências |
| `LISTAR_TURMAS` | — | lista de turmas |
| `PREVISUALIZAR_CSV` | `{conteudo}` | confere sem gravar: `{alunos, novos, existentes, registros, erros, avisos, amostra}` |
| `IMPORTAR_CSV` | `{conteudo}` | `{alunos, registros, erros, avisos}` |
| `RECALCULAR_RISCOS` | — | `{recalculados}` |
| `SALVAR_LANCAMENTO` | `{matricula, data, frequencia, nota, atrasos?, atividades?, dataOriginal?}` | aluno atualizado |
| `EXCLUIR_LANCAMENTO` | `{matricula, data}` | aluno atualizado (não apaga o último lançamento) |
| `REMOVER_ALUNO` | `{matricula}` | — (coordenador; apaga também avisos e providências) |
| `REGISTRAR_PROVIDENCIA` | `{matricula, acao, observacao?}` | aluno atualizado |
| `CONCLUIR_PROVIDENCIA` | `{id, resultado}` | aluno atualizado |
| `LISTAR_ALERTAS` | — | avisos (mais recentes primeiro) |
| `MARCAR_ALERTA_LIDO` | `{id}` | — |
| `CONTAR_AVISOS_NAO_LIDOS` | — | `{naoLidos}` (coordenador) |
| `ATUALIZAR_PERFIL` | `{nome}` | `{nome, email, funcao}` |
| `ALTERAR_SENHA` | `{senhaAtual, novaSenha}` | — |
| `LISTAR_EQUIPE` | — | `[{nome, email, funcao, criadoEm, voce}]` (coordenador) |
| `REMOVER_USUARIO` | `{email}` | — (coordenador) |
| `REDEFINIR_SENHA` | `{email}` | `{senhaTemporaria}` (coordenador) |

---

## Testes

Na pasta `backend`, sem MongoDB e sem servidor ligados:

```bash
./mvnw test
```

**56 testes JUnit** — regra do score (inclusive engajamento), leitura da planilha (datas, faixas, colunas opcionais,
cabeçalho com acento), cadastro e troca de senha, hash de senha, permissões e sessões, relatório semanal, providências
e o `Parceiro` sobre um Socket de verdade (filtro de classes e reconexão). Detalhes em `docs/Relatorio_de_Testes.md`.

No frontend: `npm run lint` e `npm run build`.

---

## Estrutura de pastas e documentos

```
backend/      Java 11 + Maven (protocolo, servidor, ponte) e dados/exemplo.csv
frontend/     React + Vite + Tailwind + Recharts
docs/         Dicionário de dados e Relatório de testes (.md e .pdf)
prototipos/   Relatório de ideação, Protótipo/Funcionalidades/Cronograma/MVP e Matriz SWOT (PDF)
rodar.sh      sobe tudo no Mac/Linux (rodar.bat no Windows)
```

| Documento | Arquivo |
|---|---|
| Dicionário de dados (MongoDB) | [`docs/Dicionario_de_Dados.md`](docs/Dicionario_de_Dados.md) · [PDF](docs/Dicionario_de_Dados.pdf) |
| Relatório de testes (JUnit) | [`docs/Relatorio_de_Testes.md`](docs/Relatorio_de_Testes.md) · [PDF](docs/Relatorio_de_Testes.pdf) |
| Relatório de ideação, protótipo e SWOT | pasta [`prototipos/`](prototipos/) |

---

## Autoria

Cada arquivo de código traz no cabeçalho quem é o **autor** e, quando outra pessoa mexeu nele, uma linha
**"Alterações:"** com o resumo do que mudou. O código também está comentado explicando o porquê de cada parte.