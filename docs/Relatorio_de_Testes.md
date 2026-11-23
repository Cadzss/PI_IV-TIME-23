# Relatório de Testes — Early-Warning de Evasão Escolar

**Projeto:** PI IV — Time 23  
**Autor dos testes unitários:** Cadu Spadari  
**Framework:** JUnit 5 (Maven Surefire)  
**Como executar:** na pasta `backend`, com JDK 11+:

```bash
./mvnw test
```

Os testes **não** exigem MongoDB nem servidor ligados — cobrem regras puras (score, CSV, cadastro, senha, permissões, relatório semanal) e o protocolo sobre um Socket local.

---

## 1. Escopo

| Camada | O que foi testado | Arquivo |
|---|---|---|
| Regra de negócio | Pontuação e níveis ALTO / MÉDIO / BAIXO | `CalculadoraRiscoTest.java` |
| Importação | Parser, validação e agrupamento do CSV | `ImportadorCsvTest.java` |
| Cadastro e senha | Validação do cadastro e hash PBKDF2 | `RegrasUsuarioTest.java`, `SenhaHashTest.java` |
| Acesso | Permissões por função e sessões (token) | `PermissoesESessoesTest.java` |
| Avisos | Resumo do relatório semanal | `RelatorioSemanalTest.java` |
| Providências | Ação válida, observação, conclusão com resultado | `RegrasProvidenciaTest.java` |
| Protocolo | `Parceiro` sobre Socket real, filtro de classes, `TratadoraDeComunicadoDeDesligamento` | `ParceiroSocketTest.java` |

**Fora do escopo unitário (MVP):** servidor completo com MongoDB, SMTP real, UI React (validados manualmente na demo).

---

## 2. Ambiente de teste

| Item | Valor |
|---|---|
| Linguagem | Java 11+ |
| Build | Maven Wrapper (`./mvnw`) |
| Dependências de teste | JUnit Jupiter |
| Dados | Históricos e CSVs montados no próprio teste (sem fixture externa) |

---

## 3. Suíte `CalculadoraRiscoTest` (14 casos)

Critérios da regra (MVP):

| Condição | Pontos |
|---|---|
| Frequência atual &lt; 75% | +4 (sozinha já dá ALTO) |
| Frequência atual &lt; 85% (e ≥ 75%) | +1 |
| Nota atual &lt; 5,0 | +2 |
| Nota atual &lt; 7,0 (e ≥ 5,0) | +1 |
| Queda de nota ≥ 1,0 na janela de 3 registros | +1 |
| Queda de frequência ≥ 10 p.p. na janela de 3 | +1 |
| Atrasos no período ≥ 5 (engajamento, opcional) | +1 |
| Atividades entregues &lt; 75% / &lt; 50% (engajamento, opcional) | +1 / +2 |

Classificação: **≥ 4 → ALTO · 2–3 → MÉDIO · 0–1 → BAIXO**

| ID | Método de teste | Entrada (resumo) | Resultado esperado | Status |
|---|---|---|---|---|
| CR-01 | `historicoVazioFicaBaixo` | Histórico vazio | nível BAIXO, pontuação 0 | OK |
| CR-02 | `frequenciaAbaixoDe75SomaQuatroENotaAbaixoDe5SomaDois` | freq 70, nota 4,0 | pontuação 6, ALTO; sinais com 75% e 5,0 | OK |
| CR-03 | `frequenciaAbaixoDe75SozinhaJaEAltoENaoSomaOPontoDe85` | freq 70, nota 8,0 | pontuação 4 (só +4), ALTO | OK |
| CR-04 | `frequenciaAbaixoDe85ENotaAbaixoDe7FicamMedio` | freq 80, nota 6,0 | pontuação 2, MÉDIO | OK |
| CR-05 | `limitesSeguemARegraEstritaDeMenorQue` | (85,7,0) e (75,5,0) | 0→BAIXO; 2→MÉDIO (limites inclusivos) | OK |
| CR-06 | `quedaNaJanelaDeTresRegistrosSomaUmPontoCada` | 4 registros com queda nota e freq | pontuação 4, ALTO | OK |
| CR-07 | `quatroPontosOuMaisSaoAltoEDoisOuTresSaoMedio` | (74,6,9) e (84,6,0) | ALTO e MÉDIO | OK |
| CR-08 | `pesoColocaAltoAntesDeMedioEBaixo` | pesos dos níveis | ALTO &lt; MÉDIO &lt; BAIXO na ordenação | OK |
| CR-09 | `pontuacaoMaximaAtingeOitoPontosComTodosOsCriterios` | queda + freq&lt;75 + nota&lt;5 | pontuação 8, ALTO | OK |
| CR-10 | `quedaExataNoLimiteDeUmPontoENaFrequenciaDezPontosSomaPontos` | queda exata 1,0 e 10 p.p. | pontuação 2, MÉDIO | OK |
| CR-11 | `semColunasDeEngajamentoOCalculoNaoMuda` | freq 90, nota 8, sem atrasos/atividades | pontuação 0, sem campos de engajamento | OK |
| CR-12 | `atrasosRecorrentesSomamUmPontoAPartirDeCinco` | 4 e 5 atrasos | 0 e +1 ponto, sinal de atraso | OK |
| CR-13 | `poucasAtividadesEntreguesSomamUmOuDoisPontos` | 75%, 74% e 49% entregues | 0, +1 e +2 pontos | OK |
| CR-14 | `engajamentoRuimSomadoANotaBaixaLevaAAlto` | nota 6, 6 atrasos, 40% entregues | pontuação 4, ALTO | OK |

---

## 4. Suíte `ImportadorCsvTest` (16 casos)

Cabeçalho obrigatório: `matricula,nome,turma,data,frequencia,nota` (`,` ou `;`).

| ID | Método de teste | Cenário | Resultado esperado | Status |
|---|---|---|---|---|
| IC-01 | `importaLinhaValida` | Uma linha correta | 1 aluno, 1 registro, sem erros | OK |
| IC-02 | `aceitaPontoEVirgulaEVirgulaDecimal` | `;` e `6,5` / `80%` | frequencia 80,0 e nota 6,5 | OK |
| IC-03 | `nomeEntreAspasPodeTerVirgula` | `"Silva, João"` | nome preservado com vírgula | OK |
| IC-04 | `rejeitaFrequenciaENotaForaDaFaixaSemDescartarAPlanilha` | 120%, nota 11 e linha boa | 2 erros + 1 registro válido | OK |
| IC-05 | `rejeitaDataInvalidaEPlanilhaVazia` | `2026/11/01` e CSV vazio | erro de data; vazio lança exceção | OK |
| IC-06 | `rejeitaDiaQueNaoExisteEAceita29DeFevereiroEmAnoBissexto` | 31/02, 31/04, 29/02/2024 | rejeita inexistentes; aceita bissexto | OK |
| IC-07 | `exigeColunasDoCabecalho` | cabeçalho incompleto | exceção citando coluna ausente | OK |
| IC-08 | `separarRespeitaAspas` | campos com aspas | lista de 3 campos correta | OK |
| IC-09 | `agrupaMultiplosRegistrosDoMesmoAluno` | 2 linhas mesma matrícula | 1 aluno, 2 registros | OK |
| IC-10 | `rejeitaFrequenciaENotaNegativas` | −5 e −2,0 | 0 registros, 2 erros | OK |
| IC-11 | `avisaQuandoAMesmaMatriculaApareceComNomeOuTurmaDiferente` | mesma matrícula, nome e turma diferentes | 2 registros, 2 avisos, mantém o primeiro nome | OK |
| IC-12 | `lePlanilhaComColunasDeEngajamentoOpcionais` | colunas atrasos/atividades, uma linha com células vazias | lê 3 atrasos e 70%; linha vazia fica sem os campos | OK |
| IC-13 | `recusaAtrasosEAtividadesInvalidos` | atrasos −1 e 2,5; atividades 120 | 3 erros, nenhum registro | OK |
| IC-14 | `aceitaDataNoFormatoBrasileiroEGuardaComoAnoMesDia` | 01/11/2026, 1/2/2026 e 31/02/2026 | 2 registros em AAAA-MM-DD; 31/02 recusado | OK |
| IC-15 | `cabecalhoAceitaAcentosEMaiusculas` | `Matrícula;Frequência;...` | lê normalmente | OK |
| IC-16 | `montarRegistroValidaOLancamentoDaTela` | lançamento digitado na tela | data normalizada; recusa freq 101, atrasos 1,5 e data "ontem" | OK |

---

## 5. Resumo quantitativo

| Suíte | Casos | Passou | Falhou | Cobertura funcional |
|---|---|---|---|---|
| CalculadoraRiscoTest | 14 | 14 | 0 | Score, limites, quedas, pesos, engajamento |
| ImportadorCsvTest | 16 | 16 | 0 | Separador, faixas, datas (inclusive dd/mm/aaaa), cabeçalho com acento, agrupamento, avisos, colunas opcionais, lançamento da tela |
| RegrasUsuarioTest | 6 | 6 | 0 | E-mail, função, senha, código da escola, troca de senha, senha temporária; não existe conta de aluno |
| SenhaHashTest | 3 | 3 | 0 | PBKDF2 com salt, senha errada, formato inválido |
| PermissoesESessoesTest | 7 | 7 | 0 | O que cada função pode pedir (equipe/avisos/remover só coordenador); token; encerrar sessões de uma pessoa |
| ParceiroSocketTest | 3 | 3 | 0 | Protocolo sobre Socket real, filtro de classes, tratadora encerra quando a conexão cai |
| RelatorioSemanalTest | 3 | 3 | 0 | Contagem por nível, quem piorou/melhorou, texto do resumo |
| RegrasProvidenciaTest | 4 | 4 | 0 | Só ações recomendadas, observação opcional, resultado obrigatório, limite de texto |
| **Total** | **56** | **56** | **0** | Regras centrais do MVP, segurança e protocolo |

---

## 6. Testes manuais (aceitação da demo)

| ID | Fluxo | Passos | Esperado |
|---|---|---|---|
| MN-01 | Login | Abrir `/login`, autenticar conta demo | Entra no Dashboard |
| MN-02 | Listagem | Filtrar turma / nível | Lista ordenada ALTO → MÉDIO → BAIXO |
| MN-03 | Detalhe | Abrir um aluno | Histórico + gráfico + sinais |
| MN-04 | Alertas | Ver alertas e marcar lido | Status atualiza |
| MN-05 | Enviar planilha | Enviar `backend/dados/exemplo.csv` ou um .xlsx | Prévia (novos/existentes/problemas) e, ao confirmar, contadores |
| MN-11 | Planilha modelo | Botão "Baixar planilha modelo" | Baixa .xlsx com as colunas e exemplos |
| MN-12 | Corrigir lançamento | Na tela do aluno: adicionar, corrigir e excluir | Histórico e risco atualizam na hora; último lançamento não pode ser excluído |
| MN-13 | Remover aluno | Coordenador remove um aluno | Aluno, avisos e providências somem; professor não vê a opção |
| MN-15 | Minha conta | Trocar nome; alterar senha (atual errada, igual, válida) | Nome muda no topo; senha recusada/aceita; outros dispositivos desconectados |
| MN-16 | Equipe | Coordenador redefine a senha e remove o acesso de um professor | Senha temporária exibida uma vez; aviso de trocar senha; acesso removido desconecta na hora |
| MN-17 | Avisos no menu | Novo aviso / marcar como lido | O número ao lado de "Avisos" aumenta/diminui |
| MN-18 | Primeiro acesso | Banco sem alunos | Painel mostra boas-vindas com "Baixar planilha modelo" e "Enviar planilha" |
| MN-14 | Providências | Clicar em "Acionar assistente social", registrar e depois concluir | Aparece em andamento; o contador "Risco alto sem providência" diminui; ao concluir com resultado, volta a contar se o aluno seguir em risco alto |
| MN-06 | Recalcular | Botão recalcular riscos | Riscos e alertas atualizam |
| MN-07 | E-mail ALTO | Aluno entra em ALTO sem SMTP | Alerta com `email.status = pendente` e logo depois `copia_local`, cópia em `logs/emails.log` |
| MN-08 | Permissões | Tentar criar conta de aluno / entrar com conta antiga de aluno | Recusado (aluno é beneficiário, não usuário) |
| MN-10 | Relatório semanal | Abrir Avisos como coordenador | Aparece "Relatório semanal de risco disponível" uma vez por semana |
| MN-09 | Reconexão | Reiniciar o servidor com a ponte ligada | Ponte reconecta sozinha; tela pede login de novo |

---

## 7. Conclusão

Os **56 testes unitários** validam as regras mais críticas do MVP (score de evasão e importação da planilha), incluindo bordas (limites 75/85/5/7, datas inexistentes, bissexto, valores negativos e agrupamento por matrícula), além do engajamento (atrasos e atividades), cadastro, hash de senha, permissões por função, relatório semanal, providências e o protocolo de Socket. Os fluxos de interface e integração foram conferidos manualmente para a banca.

*Documento gerado para entrega do PI IV — Time 23.*
