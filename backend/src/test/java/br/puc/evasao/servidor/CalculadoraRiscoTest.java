// Autor: Cadu Spadari
// Alterações: Lucas Canto — nova regra (+4 para frequência < 75%) e testes de engajamento (atrasos/atividades)
package br.puc.evasao.servidor;

/*
 * Testes da regra do score de risco. Não precisam de banco: montamos o histórico
 * na mão (método registro lá embaixo) e conferimos pontuação, nível e sinais.
 */

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraRiscoTest {

    private final CalculadoraRisco calculadora = new CalculadoraRisco();

    @Test
    void historicoVazioFicaBaixo() {
        Document risco = calculadora.calcular(Collections.emptyList());
        assertEquals(CalculadoraRisco.BAIXO, risco.getString("nivel"));
        assertEquals(0, risco.getInteger("pontuacao"));
    }

    @Test
    void frequenciaAbaixoDe75SomaQuatroENotaAbaixoDe5SomaDois() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 70, 4.0)));
        assertEquals(6, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
        assertTrue(sinais(risco).stream().anyMatch(s -> s.contains("75%")));
        assertTrue(sinais(risco).stream().anyMatch(s -> s.contains("5,0")));
    }

    @Test
    void frequenciaAbaixoDe75SozinhaJaEAltoENaoSomaOPontoDe85() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 70, 8.0)));
        assertEquals(4, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
    }

    @Test
    void frequenciaAbaixoDe85ENotaAbaixoDe7FicamMedio() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 80, 6.0)));
        assertEquals(2, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.MEDIO, risco.getString("nivel"));
    }

    @Test
    void limitesSeguemARegraEstritaDeMenorQue() {
        Document seguro = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 85, 7.0)));
        assertEquals(0, seguro.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.BAIXO, seguro.getString("nivel"));

        Document noLimite = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 75, 5.0)));
        assertEquals(2, noLimite.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.MEDIO, noLimite.getString("nivel"));
    }

    @Test
    void quedaNaJanelaDeTresRegistrosSomaUmPontoCada() {
        List<Document> historico = Arrays.asList(
                registro("2026-08-01", 95, 9.0),
                registro("2026-09-01", 90, 8.0),
                registro("2026-10-01", 90, 8.0),
                registro("2026-11-01", 78, 6.5));
        Document risco = calculadora.calcular(historico);
        assertEquals(4, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
    }

    @Test
    void quatroPontosOuMaisSaoAltoEDoisOuTresSaoMedio() {
        assertEquals(CalculadoraRisco.ALTO, calculadora.calcular(
                Collections.singletonList(registro("2026-11-01", 74, 6.9))).getString("nivel"));
        assertEquals(CalculadoraRisco.MEDIO, calculadora.calcular(
                Collections.singletonList(registro("2026-11-01", 84, 6.0))).getString("nivel"));
    }

    @Test
    void pesoColocaAltoAntesDeMedioEBaixo() {
        assertTrue(CalculadoraRisco.peso(CalculadoraRisco.ALTO) < CalculadoraRisco.peso(CalculadoraRisco.MEDIO));
        assertTrue(CalculadoraRisco.peso(CalculadoraRisco.MEDIO) < CalculadoraRisco.peso(CalculadoraRisco.BAIXO));
    }

    @Test
    void pontuacaoMaximaAtingeOitoPontosComTodosOsCriterios() {
        List<Document> historico = Arrays.asList(
                registro("2026-09-01", 90, 9.0),
                registro("2026-10-01", 85, 8.5),
                registro("2026-11-01", 70, 4.5));
        Document risco = calculadora.calcular(historico);
        assertEquals(8, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
    }

    @Test
    void quedaExataNoLimiteDeUmPontoENaFrequenciaDezPontosSomaPontos() {
        List<Document> historico = Arrays.asList(
                registro("2026-09-01", 95, 8.0),
                registro("2026-10-01", 90, 7.5),
                registro("2026-11-01", 85, 7.0));
        Document risco = calculadora.calcular(historico);
        assertEquals(2, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.MEDIO, risco.getString("nivel"));
    }

    @Test
    void semColunasDeEngajamentoOCalculoNaoMuda() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 90, 8.0)));
        assertEquals(0, risco.getInteger("pontuacao"));
        assertTrue(!risco.containsKey("atrasosAtual") && !risco.containsKey("atividadesAtual"));
    }

    @Test
    void atrasosRecorrentesSomamUmPontoAPartirDeCinco() {
        Document quatro = calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 8.0).append("atrasos", 4)));
        assertEquals(0, quatro.getInteger("pontuacao"));

        Document cinco = calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 8.0).append("atrasos", 5)));
        assertEquals(1, cinco.getInteger("pontuacao"));
        assertEquals(5, cinco.getInteger("atrasosAtual"));
        assertTrue(sinais(cinco).get(0).contains("atrasado"));
    }

    @Test
    void poucasAtividadesEntreguesSomamUmOuDoisPontos() {
        assertEquals(0, calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 8.0).append("atividades", 75.0))).getInteger("pontuacao"));
        assertEquals(1, calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 8.0).append("atividades", 74.0))).getInteger("pontuacao"));
        Document metade = calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 8.0).append("atividades", 49.0)));
        assertEquals(2, metade.getInteger("pontuacao"));
        assertTrue(sinais(metade).get(0).contains("atividades"));
    }

    @Test
    void engajamentoRuimSomadoANotaBaixaLevaAAlto() {
        Document risco = calculadora.calcular(Collections.singletonList(
                registro("2026-11-01", 90, 6.0).append("atrasos", 6).append("atividades", 40.0)));
        assertEquals(4, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
    }

    private static Document registro(String data, double frequencia, double nota) {
        return new Document("data", data).append("frequencia", frequencia).append("nota", nota);
    }

    private static List<String> sinais(Document risco) {
        return risco.getList("sinais", String.class);
    }
}