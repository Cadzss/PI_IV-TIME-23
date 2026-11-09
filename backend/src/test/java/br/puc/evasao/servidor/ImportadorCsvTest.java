package br.puc.evasao.servidor;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportadorCsvTest {

    private static final String CABECALHO = "matricula,nome,turma,data,frequencia,nota\n";

    @Test
    void importaLinhaValida() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO + "2026001,João P.,8ºA,2026-11-01,80,6.5\n");
        assertEquals(1, analise.registros);
        assertEquals(1, analise.porAluno.size());
        assertTrue(analise.erros.isEmpty());
        Document registro = analise.porAluno.get("2026001").getList("registros", Document.class).get(0);
        assertEquals(80.0, registro.getDouble("frequencia"));
        assertEquals(6.5, registro.getDouble("nota"));
    }

    @Test
    void aceitaPontoEVirgulaEVirgulaDecimal() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                "matricula;nome;turma;data;frequencia;nota\n"
                        + "2026001;Ana;8ºA;2026-11-01;80%;6,5\n");
        Document registro = analise.porAluno.get("2026001").getList("registros", Document.class).get(0);
        assertEquals(80.0, registro.getDouble("frequencia"));
        assertEquals(6.5, registro.getDouble("nota"));
    }

    @Test
    void nomeEntreAspasPodeTerVirgula() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO + "2026001,\"Silva, João\",8ºA,2026-11-01,90,8\n");
        assertEquals("Silva, João", analise.porAluno.get("2026001").getString("nome"));
        assertTrue(analise.erros.isEmpty());
    }

    @Test
    void rejeitaFrequenciaENotaForaDaFaixaSemDescartarAPlanilha() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO
                        + "2026001,Ana,8ºA,2026-11-01,120,6\n"
                        + "2026002,Bia,8ºA,2026-11-01,80,11\n"
                        + "2026003,Caio,8ºA,2026-11-01,90,7\n");
        assertEquals(1, analise.registros);
        assertEquals(2, analise.erros.size());
        assertTrue(analise.erros.get(0).contains("frequência"));
        assertTrue(analise.erros.get(1).contains("nota"));
        assertTrue(analise.porAluno.containsKey("2026003"));
    }

    @Test
    void rejeitaDataInvalidaEPlanilhaVazia() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO + "2026001,Ana,8ºA,01/11/2026,80,6\n");
        assertEquals(0, analise.registros);
        assertTrue(analise.erros.get(0).contains("data"));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ImportadorCsv.analisar(""));
        assertTrue(erro.getMessage().contains("vazia"));
    }

    @Test
    void exigeColunasDoCabecalho() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ImportadorCsv.analisar("matricula,nome\n1,Ana\n"));
        assertTrue(erro.getMessage().contains("turma"));
    }

    @Test
    void separarRespeitaAspas() {
        List<String> campos = ImportadorCsv.separar("1,\"Silva, João\",8ºA", ',');
        assertEquals(ArraysOf("1", "Silva, João", "8ºA"), campos);
    }

    private static List<String> ArraysOf(String... valores) {
        return java.util.Arrays.asList(valores);
    }
}
