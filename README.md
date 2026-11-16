# PI_IV-TIME-23 — Early-Warning de Evasão Escolar

Projeto Integrador IV - Time 23. Sistema que identifica antecipadamente alunos em risco de evasão escolar.

## Arquitetura

```
React (navegador) --HTTP--> PonteHttp (Java, cliente Socket) --Socket TCP--> Servidor (Java) --> MongoDB
   :5173                        :8080                                          :12345            :27017
```

- **backend/** (Java 11 + Maven)
  - `protocolo/Protocolo` — formato das mensagens trocadas via Socket (1 linha JSON por mensagem)
  - `servidor/Servidor` — `ServerSocket` que atende cada cliente em uma thread (`AtendenteCliente`)
  - `servidor/ProcessadorPedidos` — executa cada ação do protocolo
  - `servidor/CalculadoraRisco` — regra do score de risco (ALTO / MÉDIO / BAIXO)
  - `servidor/AlunoRepositorio`, `AlertaRepositorio` — acesso ao MongoDB
  - `servidor/ImportadorCsv` — importação da planilha de frequência e notas
  - `ponte/ClienteSocket` + `ponte/PonteHttp` — cliente Socket Java exposto em HTTP para o React
- **frontend/** (React + Vite + Tailwind + Recharts) — Dashboard, Detalhe do aluno, Alertas, Importar planilha

O navegador não consegue abrir Sockets TCP diretamente, por isso a **PonteHttp** existe:
ela é o cliente Socket em Java e o React só conversa com ela.

## Como rodar

### 1. Instale (uma vez só)

| Programa | Versão | Download |
|---|---|---|
| Java (JDK) | 11 ou superior | https://adoptium.net |
| Node.js | 18 ou superior | https://nodejs.org |
| MongoDB | qualquer recente | https://www.mongodb.com/try/download/community |

O **Maven não precisa ser instalado**: o projeto já vem com o `mvnw` (Maven Wrapper), que baixa tudo sozinho.

Sem vontade de instalar o MongoDB? Se você tiver Docker, basta rodar `docker compose up -d` na raiz do projeto.

### 2. Rode

**Mac / Linux**
```bash
./rodar.sh --popular   # primeira vez (cria dados de exemplo)
./rodar.sh             # das próximas vezes
```

**Windows** (Prompt de Comando, na pasta do projeto)
```bat
rodar.bat --popular
rodar.bat
```

Depois abra **http://localhost:5173** no navegador.
Na primeira execução as dependências são baixadas, então demora um pouco mais.

### Rodando manualmente (passo a passo)

Se preferir subir cada parte em um terminal (no Windows use `mvnw.cmd` no lugar de `./mvnw`):

```bash
# terminal 1 - servidor Socket
cd backend
./mvnw -q compile exec:java@popular   # só na primeira vez
./mvnw -q compile exec:java@servidor

# terminal 2 - ponte HTTP -> Socket
cd backend
./mvnw -q exec:java@ponte

# terminal 3 - frontend
cd frontend
npm install
npm run dev
```

### Problemas comuns

- **"Não foi possível conectar à ponte HTTP"**: a ponte (terminal 2) não está rodando.
- **Erro de conexão com o MongoDB**: confira se o MongoDB está ativo na porta 27017.
  Para usar outro endereço (ex.: MongoDB Atlas), defina a variável `MONGO_URI`.
- **Porta em uso (12345, 8080 ou 5173)**: feche a execução anterior e rode de novo.

### Testes

Na pasta `backend`, sem MongoDB e sem o servidor ligados:

```bash
./mvnw test
```

Os testes JUnit cobrem a regra do score e a leitura da planilha, inclusive data que não existe no calendário.

### E-mail de risco alto

Quando o aluno entra em ALTO, o sistema prepara um e-mail para o coordenador. Sem SMTP configurado, a mensagem não sai da máquina: a cópia fica em `logs/emails.log` e o alerta aparece com status `copia_local`.

| Variável | Função |
|---|---|
| `EMAIL_COORDENADOR` | quem recebe (padrão `coordenador@escola.local`) |
| `EMAIL_REMETENTE` | quem envia |
| `SMTP_HOST` | servidor SMTP. Sem ele, só grava a cópia local |
| `SMTP_PORT` | porta (padrão 587) |
| `SMTP_USUARIO` | usuário, se o servidor pedir senha |
| `SMTP_SENHA` | senha |

## Ações do protocolo

| Ação | Dados | Retorno |
|---|---|---|
| `LISTAR_ALUNOS` | `{turma?, nivel?}` | alunos ordenados por risco |
| `DETALHAR_ALUNO` | `{matricula}` | aluno com histórico e risco |
| `LISTAR_TURMAS` | — | lista de turmas |
| `LISTAR_ALERTAS` | — | alertas (mais recentes primeiro) |
| `MARCAR_ALERTA_LIDO` | `{id}` | — |
| `IMPORTAR_CSV` | `{conteudo}` | `{alunos, registros, erros}` |
| `RECALCULAR_RISCOS` | — | `{recalculados}` |

## Regra do score de risco (MVP)

| Condição | Pontos |
|---|---|
| Frequência atual < 75% | +3 |
| Frequência atual < 85% | +1 |
| Nota atual < 5,0 | +2 |
| Nota atual < 7,0 | +1 |
| Queda de nota ≥ 1,0 nos últimos 3 registros | +1 |
| Queda de frequência ≥ 10 p.p. nos últimos 3 registros | +1 |

**≥ 4 → ALTO · 2–3 → MÉDIO · 0–1 → BAIXO**

## Planilha (CSV)

```
matricula,nome,turma,data,frequencia,nota
2026001,João P.,8ºA,2026-11-01,62,5.2
```

A data precisa existir. `2026-02-31` e `2026-04-31` são recusados; `2024-02-29` entra porque 2024 é bissexto. Exemplo completo em `backend/dados/exemplo.csv`.
