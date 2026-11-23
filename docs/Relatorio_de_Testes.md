# Relatório de Testes — Early-Warning de Evasão Escolar

**Projeto:** PI IV — Time 23  
**Autor dos testes unitários:** Cadu Spadari  
**Framework:** JUnit 5 (Maven Surefire)  
**Como executar:** na pasta `backend`, com JDK 11+:

```bash
./mvnw test
```

Os testes **não** exigem MongoDB nem servidor ligados — cobrem regras puras (score e CSV).

---

## 1. Escopo

| Camada | O que foi testado | Arquivo |
|---|---|---|
| Regra de negócio | Pontuação e níveis ALTO / MÉDIO / BAIXO | `CalculadoraRiscoTest.java` |
| Importação | Parser, validação e agrupamento do CSV | `ImportadorCsvTest.java` |

**Fora do escopo unitário (MVP):** integração socket ponta a ponta, SMTP real, UI React (validados manualmente na demo).

---

## 2. Ambiente de teste

| Item | Valor |
|---|---|
| Linguagem | Java 11+ |
| Build | Maven Wrapper (`./mvnw`) |
| Dependências de teste | JUnit Jupiter |
| Dados | Históricos e CSVs montados no próprio teste (sem fixture externa) |

---

## 3. Suíte `CalculadoraRiscoTest` (10 casos)

Critérios da regra (MVP):

| Condição | Pontos |
|---|---|
| Frequência atual &lt; 75% | +3 |
| Frequência atual &lt; 85% (e ≥ 75%) | +1 |
| Nota atual &lt; 5,0 | +2 |
| Nota atual &lt; 7,0 (e ≥ 5,0) | +1 |
| Queda de nota ≥ 1,0 na janela de 3 registros | +1 |
| Queda de frequência ≥ 10 p.p. na janela de 3 | +1 |

Classificação: **≥ 4 → ALTO · 2–3 → MÉDIO · 0–1 → BAIXO**

| ID | Método de teste | Entrada (resumo) | Resultado esperado | Status |
|---|---|---|---|---|
| CR-01 | `historicoVazioFicaBaixo` | Histórico vazio | nível BAIXO, pontuação 0 | OK |
| CR-02 | `frequenciaAbaixoDe75SomaTresENotaAbaixoDe5SomaDois` | freq 70, nota 4,0 | pontuação 5, ALTO; sinais com 75% e 5,0 | OK |
| CR-03 | `frequenciaAbaixoDe75NaoSomaTambemOPontoDe85` | freq 70, nota 8,0 | pontuação 3 (só +3), MÉDIO | OK |
| CR-04 | `frequenciaAbaixoDe85ENotaAbaixoDe7FicamMedio` | freq 80, nota 6,0 | pontuação 2, MÉDIO | OK |
| CR-05 | `limitesSeguemARegraEstritaDeMenorQue` | (85,7,0) e (75,5,0) | 0→BAIXO; 2→MÉDIO (limites inclusivos) | OK |
| CR-06 | `quedaNaJanelaDeTresRegistrosSomaUmPontoCada` | 4 registros com queda nota e freq | pontuação 4, ALTO | OK |
| CR-07 | `quatroPontosOuMaisSaoAltoEDoisOuTresSaoMedio` | (74,6,9) e (84,6,0) | ALTO e MÉDIO | OK |
| CR-08 | `pesoColocaAltoAntesDeMedioEBaixo` | pesos dos níveis | ALTO &lt; MÉDIO &lt; BAIXO na ordenação | OK |
| CR-09 | `pontuacaoMaximaAtingeSetePontosComTodosOsCriterios` | queda + freq&lt;75 + nota&lt;5 | pontuação 7, ALTO | OK |
| CR-10 | `quedaExataNoLimiteDeUmPontoENaFrequenciaDezPontosSomaPontos` | queda exata 1,0 e 10 p.p. | pontuação 2, MÉDIO | OK |

---

## 4. Suíte `ImportadorCsvTest` (10 casos)

Cabeçalho obrigatório: `matricula,nome,turma,data,frequencia,nota` (`,` ou `;`).

| ID | Método de teste | Cenário | Resultado esperado | Status |
|---|---|---|---|---|
| IC-01 | `importaLinhaValida` | Uma linha correta | 1 aluno, 1 registro, sem erros | OK |
| IC-02 | `aceitaPontoEVirgulaEVirgulaDecimal` | `;` e `6,5` / `80%` | frequencia 80,0 e nota 6,5 | OK |
| IC-03 | `nomeEntreAspasPodeTerVirgula` | `"Silva, João"` | nome preservado com vírgula | OK |
| IC-04 | `rejeitaFrequenciaENotaForaDaFaixaSemDescartarAPlanilha` | 120%, nota 11 e linha boa | 2 erros + 1 registro válido | OK |
| IC-05 | `rejeitaDataInvalidaEPlanilhaVazia` | `01/11/2026` e CSV vazio | erro de data; vazio lança exceção | OK |
| IC-06 | `rejeitaDiaQueNaoExisteEAceita29DeFevereiroEmAnoBissexto` | 31/02, 31/04, 29/02/2024 | rejeita inexistentes; aceita bissexto | OK |
| IC-07 | `exigeColunasDoCabecalho` | cabeçalho incompleto | exceção citando coluna ausente | OK |
| IC-08 | `separarRespeitaAspas` | campos com aspas | lista de 3 campos correta | OK |
| IC-09 | `agrupaMultiplosRegistrosDoMesmoAluno` | 2 linhas mesma matrícula | 1 aluno, 2 registros | OK |
| IC-10 | `rejeitaFrequenciaENotaNegativas` | −5 e −2,0 | 0 registros, 2 erros | OK |

---

## 5. Resumo quantitativo

| Suíte | Casos | Passou | Falhou | Cobertura funcional |
|---|---|---|---|---|
| CalculadoraRiscoTest | 10 | 10 | 0 | Score, limites, quedas, pesos |
| ImportadorCsvTest | 10 | 10 | 0 | Separador, faixas, datas, agrupamento |
| **Total** | **20** | **20** | **0** | Regras centrais do MVP |

---

## 6. Testes manuais (aceitação da demo)

| ID | Fluxo | Passos | Esperado |
|---|---|---|---|
| MN-01 | Login | Abrir `/login`, autenticar conta demo | Entra no Dashboard |
| MN-02 | Listagem | Filtrar turma / nível | Lista ordenada ALTO → MÉDIO → BAIXO |
| MN-03 | Detalhe | Abrir um aluno | Histórico + gráfico + sinais |
| MN-04 | Alertas | Ver alertas e marcar lido | Status atualiza |
| MN-05 | Importar CSV | Enviar `backend/dados/exemplo.csv` | Contadores alunos/registros/erros |
| MN-06 | Recalcular | Botão recalcular riscos | Riscos e alertas atualizam |
| MN-07 | E-mail ALTO | Aluno entra em ALTO sem SMTP | Alerta com `email.status = copia_local` e cópia em `logs/emails.log` |

---

## 7. Conclusão

Os **20 testes unitários** validam as duas regras mais críticas do MVP (score de evasão e importação da planilha), incluindo bordas (limites 75/85/5/7, datas inexistentes, bissexto, valores negativos e agrupamento por matrícula). Os fluxos de interface e integração foram conferidos manualmente para a banca.

*Documento gerado para entrega do PI IV — Time 23.*
