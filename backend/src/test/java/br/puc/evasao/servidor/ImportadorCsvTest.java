// Autor: Cadu Spadari
// Alterações: Lucas Canto — avisos de nome/turma, colunas opcionais, data dd/mm/aaaa, cabeçalho com acento
//             e validação do lançamento digitado na tela
package br.puc.evasao.servidor;

/*
 * Testes da leitura da planilha. Usamos o método analisar(), que só lê e valida (não grava
 * no MongoDB), então dá para testar com textos CSV montados aqui mesmo.
 */

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
                CABECALHO + "2026001,Ana,8ºA,2026/11/01,80,6\n");
        assertEquals(0, analise.registros);
        assertTrue(analise.erros.get(0).contains("data"));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ImportadorCsv.analisar(""));
        assertTrue(erro.getMessage().contains("vazia"));
    }

    @Test
    void rejeitaDiaQueNaoExisteEAceita29DeFevereiroEmAnoBissexto() {
        ImportadorCsv.Analise fevereiro = ImportadorCsv.analisar(
                CABECALHO + "2026001,Ana,8ºA,2026-02-31,80,6\n");
        assertEquals(0, fevereiro.registros);
        assertTrue(fevereiro.erros.get(0).contains("data"));

        ImportadorCsv.Analise abril = ImportadorCsv.analisar(
                CABECALHO + "2026001,Ana,8ºA,2026-04-31,80,6\n");
        assertEquals(0, abril.registros);
        assertTrue(abril.erros.get(0).contains("data"));

        ImportadorCsv.Analise bissexto = ImportadorCsv.analisar(
                CABECALHO + "2026001,Ana,8ºA,2024-02-29,80,6\n");
        assertEquals(1, bissexto.registros);
        assertTrue(bissexto.erros.isEmpty());
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
        assertEquals(Arrays.asList("1", "Silva, João", "8ºA"), campos);
    }

    @Test
    void agrupaMultiplosRegistrosDoMesmoAluno() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO
                        + "2026001,Carlos,8A,2026-10-01,90,8.5\n"
                        + "2026001,Carlos,8A,2026-11-01,75,6.0\n");
        assertEquals(2, analise.registros);
        assertEquals(1, analise.porAluno.size());
        assertTrue(analise.erros.isEmpty());
        List<Document> registros = analise.porAluno.get("2026001").getList("registros", Document.class);
        assertEquals(2, registros.size());
    }

    @Test
    void avisaQuandoAMesmaMatriculaApareceComNomeOuTurmaDiferente() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO
                        + "2026001,Carlos,8A,2026-10-01,90,8.5\n"
                        + "2026001,Carla,8B,2026-11-01,75,6.0\n");
        assertEquals(2, analise.registros);
        assertTrue(analise.erros.isEmpty());
        assertEquals(2, analise.avisos.size());
        assertTrue(analise.avisos.get(0).contains("nome"));
        assertTrue(analise.avisos.get(1).contains("turma"));
        assertEquals("Carlos", analise.porAluno.get("2026001").getString("nome"));
    }

    @Test
    void lePlanilhaComColunasDeEngajamentoOpcionais() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                "matricula,nome,turma,data,frequencia,nota,atrasos,atividades\n"
                        + "2026001,Ana,8A,2026-11-01,80,6,3,70%\n"
                        + "2026002,Bia,8A,2026-11-01,90,8,,\n");
        assertTrue(analise.erros.isEmpty());
        Document ana = analise.porAluno.get("2026001").getList("registros", Document.class).get(0);
        assertEquals(3, ana.getInteger("atrasos"));
        assertEquals(70.0, ana.getDouble("atividades"));
        Document bia = analise.porAluno.get("2026002").getList("registros", Document.class).get(0);
        assertTrue(!bia.containsKey("atrasos") && !bia.containsKey("atividades"));
    }

    @Test
    void recusaAtrasosEAtividadesInvalidos() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                "matricula,nome,turma,data,frequencia,nota,atrasos,atividades\n"
                        + "2026001,Ana,8A,2026-11-01,80,6,-1,70\n"
                        + "2026002,Bia,8A,2026-11-01,80,6,2.5,70\n"
                        + "2026003,Caio,8A,2026-11-01,80,6,1,120\n");
        assertEquals(0, analise.registros);
        assertEquals(3, analise.erros.size());
        assertTrue(analise.erros.get(0).contains("atrasos"));
        assertTrue(analise.erros.get(2).contains("atividades"));
    }

    @Test
    void aceitaDataNoFormatoBrasileiroEGuardaComoAnoMesDia() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO + "2026001,Ana,8A,01/11/2026,80,6\n" + "2026002,Bia,8A,1/2/2026,80,6\n"
                        + "2026003,Caio,8A,31/02/2026,80,6\n");
        assertEquals(2, analise.registros);
        assertEquals("2026-11-01", analise.porAluno.get("2026001").getList("registros", Document.class).get(0).getString("data"));
        assertEquals("2026-02-01", analise.porAluno.get("2026002").getList("registros", Document.class).get(0).getString("data"));
        assertEquals(1, analise.erros.size());
        assertTrue(analise.erros.get(0).contains("31/02/2026"));
    }

    @Test
    void cabecalhoAceitaAcentosEMaiusculas() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                "Matrícula;Nome;Turma;Data;Frequência;Nota;Atrasos;Atividades\n"
                        + "2026001;Ana;8A;01/11/2026;80;6,5;2;90\n");
        assertTrue(analise.erros.isEmpty());
        assertEquals(1, analise.registros);
    }

    @Test
    void montarRegistroValidaOLancamentoDaTela() {
        Document r = ImportadorCsv.montarRegistro("05/11/2026", 88, 7.5, 2.0, null);
        assertEquals("2026-11-05", r.getString("data"));
        assertEquals(2, r.getInteger("atrasos"));
        assertTrue(!r.containsKey("atividades"));
        assertThrows(IllegalArgumentException.class, () -> ImportadorCsv.montarRegistro("2026-11-05", 101, 7, null, null));
        assertThrows(IllegalArgumentException.class, () -> ImportadorCsv.montarRegistro("2026-11-05", 90, 7, 1.5, null));
        assertThrows(IllegalArgumentException.class, () -> ImportadorCsv.montarRegistro("ontem", 90, 7, null, null));
    }

    @Test
    void rejeitaFrequenciaENotaNegativas() {
        ImportadorCsv.Analise analise = ImportadorCsv.analisar(
                CABECALHO
                        + "2026001,Lucas,8A,2026-11-01,-5,7.0\n"
                        + "2026002,Mariana,8A,2026-11-01,85,-2.0\n");
        assertEquals(0, analise.registros);
        assertEquals(2, analise.erros.size());
    }
}