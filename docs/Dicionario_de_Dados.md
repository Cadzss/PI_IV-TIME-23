# Dicionário de Dados — Early-Warning de Evasão Escolar

**Projeto:** PI IV — Time 23  
**SGBD:** MongoDB  
**Banco:** `early_warning_evasao` (variável `MONGO_DB`)  
**URI padrão:** `mongodb://localhost:27017` (variável `MONGO_URI`)  
**Modelo:** documentos (NoSQL) — não há tabelas relacionais; cada **coleção** agrupa documentos JSON.

---

## Visão geral das coleções

| Coleção | Responsabilidade | Índice único |
|---|---|---|
| `alunos` | Cadastro do aluno, histórico de frequência/nota e risco calculado | `matricula` |
| `alertas` | Eventos de mudança de risco (e e-mail em risco ALTO) | `_id` (ObjectId) |
| `usuarios` | Contas de acesso (coordenador, professor, aluno) | `email` |

---

## 1. Coleção `alunos`

Documento raiz do aluno. O histórico fica embutido (array). O risco é recalculado a cada importação/atualização.

### Campos do documento

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `_id` | ObjectId | sim (auto) | Identificador interno do MongoDB |
| `matricula` | String | sim | RA / matrícula do aluno (única) |
| `nome` | String | sim | Nome do aluno |
| `turma` | String | sim | Turma (ex.: `8ºA`) |
| `historico` | Array de Document | sim | Lista de registros data/frequência/nota |
| `risco` | Document | sim | Resultado da `CalculadoraRisco` |

### Subdocumento `historico[]`

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `data` | String (ISO `AAAA-MM-DD`) | sim | Data do registro; precisa existir no calendário |
| `frequencia` | Number (Double) | sim | Frequência em % — faixa **0 a 100** |
| `nota` | Number (Double) | sim | Nota — faixa **0 a 10** |

Registros com a **mesma data** na mesma matrícula são substituídos (upsert por data).

### Subdocumento `risco`

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `nivel` | String | sim | `ALTO` \| `MEDIO` \| `BAIXO` |
| `pontuacao` | Integer | sim | Soma dos critérios (0–7 no MVP) |
| `frequenciaAtual` | Number | sim | Frequência do registro mais recente |
| `notaAtual` | Number | sim | Nota do registro mais recente |
| `sinais` | Array de String | sim | Motivos textuais do score |
| `acoes` | Array de String | sim | Ações recomendadas conforme o nível |
| `calculadoEm` | Date | sim | Momento do cálculo |

### Exemplo

```json
{
  "matricula": "2026001",
  "nome": "João P.",
  "turma": "8ºA",
  "historico": [
    { "data": "2026-08-01", "frequencia": 80.0, "nota": 6.5 },
    { "data": "2026-11-01", "frequencia": 62.0, "nota": 5.2 }
  ],
  "risco": {
    "nivel": "ALTO",
    "pontuacao": 5,
    "frequenciaAtual": 62.0,
    "notaAtual": 5.2,
    "sinais": ["Frequência de 62%, abaixo do mínimo de 75%"],
    "acoes": ["Acionar assistente social", "Contatar a família com urgência"],
    "calculadoEm": { "$date": "2026-11-22T00:00:00.000Z" }
  }
}
```

---

## 2. Coleção `alertas`

Gerados quando o nível de risco **muda**. Em `NOVO_RISCO_ALTO`, o campo `email` guarda o resultado do `NotificadorEmail`.

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `_id` | ObjectId | sim (auto) | ID interno |
| `id` | String | só na API | Hex do `_id` (exposto na listagem) |
| `tipo` | String | sim | `NOVO_RISCO_ALTO` \| `MUDANCA_NIVEL` |
| `matricula` | String | sim | Matrícula do aluno |
| `nomeAluno` | String | sim | Nome no momento do alerta |
| `turma` | String | sim | Turma no momento do alerta |
| `nivelAnterior` | String \| null | não | Nível anterior (`null` = aluno novo) |
| `nivelNovo` | String | sim | `ALTO` \| `MEDIO` \| `BAIXO` |
| `titulo` | String | sim | Texto resumido do evento |
| `lido` | Boolean | sim | `false` até marcar como lido |
| `criadoEm` | Date | sim | Data/hora de criação |
| `email` | Document | só em ALTO | Status do envio (ver abaixo) |

### Subdocumento `email` (quando tipo = `NOVO_RISCO_ALTO`)

| Campo | Tipo | Descrição |
|---|---|---|
| `destino` | String | Destinatário (ex.: `EMAIL_COORDENADOR`) |
| `assunto` | String | Assunto da mensagem |
| `corpo` | String | Corpo com sinais e ações |
| `criadoEm` | Date | Momento da tentativa |
| `status` | String | `enviado` \| `copia_local` \| `falha` |
| `motivo` | String | Detalhe (SMTP ausente, erro, etc.) |

---

## 3. Coleção `usuarios`

Autenticação do painel. A senha **nunca** é devolvida pela API — só o hash fica no banco.

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `_id` | ObjectId | sim (auto) | ID interno |
| `nome` | String | sim | Nome do usuário |
| `email` | String | sim | E-mail normalizado (minúsculo), **único** |
| `senhaHash` | String | sim | Hash da senha (`SenhaHash`) |
| `funcao` | String | sim | `coordenador` \| `professor` \| `aluno` |
| `criadoEm` | String (ISO Instant) | sim | Momento do cadastro |

### Resposta pública (sem `senhaHash`)

```json
{ "nome": "Coordenador", "email": "coordenador@escola.local", "funcao": "coordenador" }
```

---

## 4. Regras de negócio ligadas aos dados

| Regra | Onde |
|---|---|
| Frequência 0–100; nota 0–10; data `AAAA-MM-DD` válida | `ImportadorCsv` |
| Score: ≥4 ALTO, 2–3 MÉDIO, 0–1 BAIXO | `CalculadoraRisco` |
| Alerta + e-mail só em transição para ALTO (ou aluno novo já ALTO) | `AlertaRepositorio` |
| E-mail institucional para cadastro de coordenador/professor | `RegrasUsuario` |

---

## 5. Relacionamentos (lógicos)

MongoDB não tem FK, mas o vínculo é:

```
usuarios  (autenticação — independente)
alunos.matricula  <──  alertas.matricula
```

Um aluno tem N registros em `historico` e 0..N documentos em `alertas`.

---

*Documento gerado para entrega do PI IV — Time 23.*
