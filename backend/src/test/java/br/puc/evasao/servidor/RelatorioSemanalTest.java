// Autor: Lucas Canto
package br.puc.evasao.servidor;

/*
 * Testes do resumo semanal: contagem por nível e quem piorou/melhorou.
 */

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioSemanalTest {

    private static Document aluno(String nivel) {
        return new Document("risco", new Document("nivel", nivel));
    }

    private static Document mudanca(String antes, String depois) {
        return new Document("nivelAnterior", antes).append("nivelNovo", depois);
    }

    @Test
    void contaAlunosPorNivel() {
        List<Document> alunos = Arrays.asList(aluno("ALTO"), aluno("ALTO"), aluno("MEDIO"), aluno("BAIXO"));
        Document r = RelatorioSemanal.resumir(alunos, Collections.emptyList());
        assertEquals(4, r.getInteger("total"));
        assertEquals(2, r.getInteger("alto"));
        assertEquals(1, r.getInteger("medio"));
        assertEquals(1, r.getInteger("baixo"));
    }

    @Test
    void separaQuemPiorouMelhorouEEntrouEmAlto() {
        List<Document> mudancas = Arrays.asList(
                mudanca(null, "ALTO"),      // aluno novo já em alto
                mudanca("MEDIO", "ALTO"),   // piorou e entrou em alto
                mudanca("BAIXO", "MEDIO"),  // piorou
                mudanca("ALTO", "MEDIO"));  // melhorou
        Document r = RelatorioSemanal.resumir(Collections.singletonList(aluno("ALTO")), mudancas);
        assertEquals(2, r.getInteger("entraramEmAlto"));
        assertEquals(2, r.getInteger("pioraram"));
        assertEquals(1, r.getInteger("melhoraram"));
    }

    @Test
    void descricaoEmTextoSimples() {
        Document r = RelatorioSemanal.resumir(
                Arrays.asList(aluno("ALTO"), aluno("BAIXO")), Collections.singletonList(mudanca("MEDIO", "ALTO")));
        String texto = RelatorioSemanal.descrever(r);
        assertTrue(texto.startsWith("2 alunos acompanhados: 1 em risco alto"));
        assertTrue(texto.contains("1 entraram em risco alto"));
    }
}
