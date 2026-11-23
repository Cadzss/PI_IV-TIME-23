// Autor: Cadu Spadari
package br.puc.evasao.servidor;

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
    void frequenciaAbaixoDe75SomaTresENotaAbaixoDe5SomaDois() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 70, 4.0)));
        assertEquals(5, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.ALTO, risco.getString("nivel"));
        assertTrue(sinais(risco).stream().anyMatch(s -> s.contains("75%")));
        assertTrue(sinais(risco).stream().anyMatch(s -> s.contains("5,0")));
    }

    @Test
    void frequenciaAbaixoDe75NaoSomaTambemOPontoDe85() {
        Document risco = calculadora.calcular(Collections.singletonList(registro("2026-11-01", 70, 8.0)));
        assertEquals(3, risco.getInteger("pontuacao"));
        assertEquals(CalculadoraRisco.MEDIO, risco.getString("nivel"));
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
    void pontuacaoMaximaAtingeSetePontosComTodosOsCriterios() {
        List<Document> historico = Arrays.asList(
                registro("2026-09-01", 90, 9.0),
                registro("2026-10-01", 85, 8.5),
                registro("2026-11-01", 70, 4.5));
        Document risco = calculadora.calcular(historico);
        assertEquals(7, risco.getInteger("pontuacao"));
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

    private static Document registro(String data, double frequencia, double nota) {
        return new Document("data", data).append("frequencia", frequencia).append("nota", nota);
    }

    private static List<String> sinais(Document risco) {
        return risco.getList("sinais", String.class);
    }
}