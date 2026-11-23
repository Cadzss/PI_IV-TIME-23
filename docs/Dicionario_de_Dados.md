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
| `usuarios` | Contas de acesso (coordenador, professor) | `email` |
| `providencias` | Providências tomadas a partir das ações recomendadas | `matricula` + `status` |

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
| `atrasos` | Integer | não | Atrasos no período (inteiro ≥ 0). Só existe se a planilha trouxer a coluna |
| `atividades` | Number (Double) | não | % de atividades entregues — faixa **0 a 100**. Só existe se a planilha trouxer a coluna |

Registros com a **mesma data** na mesma matrícula são substituídos (upsert por data).

### Subdocumento `risco`

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `nivel` | String | sim | `ALTO` \| `MEDIO` \| `BAIXO` |
| `pontuacao` | Integer | sim | Soma dos critérios (0–8 sem engajamento; até 11 com engajamento) |
| `frequenciaAtual` | Number | sim | Frequência do registro mais recente |
| `notaAtual` | Number | sim | Nota do registro mais recente |
| `atrasosAtual` | Integer | não | Atrasos do registro mais recente (se houver) |
| `atividadesAtual` | Number | não | % de atividades entregues no registro mais recente (se houver) |
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
    { "data": "2026-08-01", "frequencia": 84.0, "nota": 6.9, "atrasos": 3, "atividades": 79.0 },
    { "data": "2026-11-01", "frequencia": 62.0, "nota": 5.2, "atrasos": 6, "atividades": 47.0 }
  ],
  "risco": {
    "nivel": "ALTO",
    "pontuacao": 10,
    "frequenciaAtual": 62.0,
    "notaAtual": 5.2,
    "atrasosAtual": 6,
    "atividadesAtual": 47.0,
    "sinais": ["Frequência de 62%, abaixo do mínimo de 75%", "Entregou só 47% das atividades"],
    "acoes": ["Acionar assistente social", "Contatar a família com urgência"],
    "calculadoEm": { "$date": "2026-11-22T00:00:00.000Z" }
  }
}
```

---

## 2. Coleção `alertas`

Gerados quando o nível de risco **muda** e, uma vez por semana, com o resumo de risco (`RELATORIO_SEMANAL`).
Em `NOVO_RISCO_ALTO`, o campo `email` guarda o resultado do `NotificadorEmail`.

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `_id` | ObjectId | sim (auto) | ID interno |
| `id` | String | só na API | Hex do `_id` (exposto na listagem) |
| `tipo` | String | sim | `NOVO_RISCO_ALTO` \| `MUDANCA_NIVEL` \| `RELATORIO_SEMANAL` |
| `matricula` | String | exceto relatório | Matrícula do aluno |
| `nomeAluno` | String | exceto relatório | Nome no momento do alerta |
| `turma` | String | exceto relatório | Turma no momento do alerta |
| `nivelAnterior` | String \| null | não | Nível anterior (`null` = aluno novo) |
| `nivelNovo` | String | exceto relatório | `ALTO` \| `MEDIO` \| `BAIXO` |
| `titulo` | String | sim | Texto do evento, igual ao protótipo (ex.: `Maria S. (9ºB) mudou de risco BAIXO para MÉDIO`) |
| `descricao` | String | só relatório | Resumo em texto do relatório semanal |
| `resumo` | Document | só relatório | `{total, alto, medio, baixo, entraramEmAlto, pioraram, melhoraram}` |
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
| `status` | String | `pendente` (na fila de envio) \| `enviado` \| `copia_local` \| `falha` |
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
| `funcao` | String | sim | `coordenador` \| `professor` (não há conta de aluno: ele é beneficiário, não usuário) |
| `criadoEm` | String (ISO Instant) | sim | Momento do cadastro |
| `senhaTemporaria` | Boolean | não | `true` depois que o coordenador redefine a senha; some quando a pessoa cria uma nova |

### Resposta pública (sem `senhaHash`)

```json
{ "nome": "Coordenador", "email": "coordenador@escola.local", "funcao": "coordenador" }
```

No login e no cadastro a resposta também traz o `token` da sessão (guardado só na memória do servidor, não no banco).

---

## 4. Coleção `providencias`

O que a escola fez a partir das ações recomendadas (Tela 2). Removida junto com o aluno.

| Campo | Tipo | Obrigatório | Descrição / domínio |
|---|---|---|---|
| `_id` | ObjectId | sim (auto) | ID interno (exposto como `id` na API) |
| `matricula` | String | sim | Aluno da providência |
| `acao` | String | sim | Uma das ações recomendadas (ex.: `Acionar assistente social`) |
| `observacao` | String | não | Observação de quem registrou (até 500 caracteres) |
| `status` | String | sim | `EM_ANDAMENTO` \| `CONCLUIDA` |
| `criadaPor` | Document | sim | `{nome, email}` de quem registrou |
| `criadaEm` | Date | sim | Momento do registro |
| `resultado` | String | ao concluir | O que aconteceu (obrigatório para concluir; até 500 caracteres) |
| `concluidaPor` | Document | ao concluir | `{nome, email}` de quem concluiu |
| `concluidaEm` | Date | ao concluir | Momento da conclusão |

---

## 5. Regras de negócio ligadas aos dados

| Regra | Onde |
|---|---|
| Frequência 0–100; nota 0–10; data `AAAA-MM-DD` válida | `ImportadorCsv` |
| Score: ≥4 ALTO, 2–3 MÉDIO, 0–1 BAIXO | `CalculadoraRisco` |
| Alerta + e-mail só em transição para ALTO (ou aluno novo já ALTO) | `AlertaRepositorio` |
| Código da escola (senha institucional) para cadastro de coordenador/professor | `RegrasUsuario` |
| Frequência < 75% vale +4 (sozinha já dá ALTO) | `CalculadoraRisco` |
| Professor não vê avisos; só coordenador e professor têm conta | `Permissoes`, `RegrasUsuario` |
| Atrasos ≥ 5 (+1); atividades < 75% (+1) ou < 50% (+2) — só quando a planilha traz | `CalculadoraRisco` |
| Relatório semanal criado se não houver um nos últimos 7 dias | `RelatorioSemanal` |
| Providência só para ações recomendadas; não duplica uma em andamento; concluir exige resultado | `RegrasProvidencia`, `ProvidenciaRepositorio` |

---

## 6. Relacionamentos (lógicos)

MongoDB não tem FK, mas o vínculo é:

```
usuarios  (autenticação — independente)
alunos.matricula  <──  alertas.matricula   (alertas do tipo RELATORIO_SEMANAL não têm matrícula)
alunos.matricula  <──  providencias.matricula
```

Um aluno tem N registros em `historico` e 0..N documentos em `alertas`.

---

*Documento gerado para entrega do PI IV — Time 23.*
