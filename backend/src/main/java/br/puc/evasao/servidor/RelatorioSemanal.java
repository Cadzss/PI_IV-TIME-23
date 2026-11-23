// Autor: Lucas Canto
package br.puc.evasao.servidor;

import org.bson.Document;

import java.util.Date;
import java.util.List;

/**
 * Resumo semanal de risco (aviso "Relatório semanal de risco disponível", Tela 3 do protótipo).
 * É gerado quando a central de avisos é aberta e ainda não existe um resumo nos últimos 7 dias.
 */
public class RelatorioSemanal {

    static final long SEMANA_MS = 7L * 24 * 60 * 60 * 1000;

    private final AlunoRepositorio alunos;
    private final AlertaRepositorio alertas;

    public RelatorioSemanal(AlunoRepositorio alunos, AlertaRepositorio alertas) {
        this.alunos = alunos;
        this.alertas = alertas;
    }

    // synchronized: se duas telas pedirem ao mesmo tempo, só uma gera o relatório (não duplica)
    public synchronized void gerarSeNecessario() {
        Date semanaPassada = new Date(System.currentTimeMillis() - SEMANA_MS);
        if (alertas.existeRelatorioDesde(semanaPassada)) {
            return;
        }
        List<Document> lista = alunos.listar(null, null);
        if (lista.isEmpty()) {
            return; // nada para resumir ainda
        }
        Document resumo = resumir(lista, alertas.mudancasDesde(semanaPassada));
        alertas.registrarRelatorioSemanal(resumo, descrever(resumo));
    }

    /** Conta alunos por nível e as mudanças da semana. Sem banco, para poder testar. */
    static Document resumir(List<Document> alunos, List<Document> mudancas) {
        int alto = 0;
        int medio = 0;
        int baixo = 0;
        for (Document aluno : alunos) {
            Document risco = aluno.get("risco", Document.class);
            String nivel = risco == null ? null : risco.getString("nivel");
            if (CalculadoraRisco.ALTO.equals(nivel)) alto++;
            else if (CalculadoraRisco.MEDIO.equals(nivel)) medio++;
            else baixo++;
        }

        int entraramEmAlto = 0;
        int pioraram = 0;
        int melhoraram = 0;
        for (Document m : mudancas) {
            String antes = m.getString("nivelAnterior");
            String depois = m.getString("nivelNovo");
            if (CalculadoraRisco.ALTO.equals(depois)) entraramEmAlto++;
            if (antes == null) continue;
            // peso: ALTO=0, MÉDIO=1, BAIXO=2. Diferença negativa = foi para um nível pior.
            int diferenca = CalculadoraRisco.peso(depois) - CalculadoraRisco.peso(antes);
            if (diferenca < 0) pioraram++;
            else if (diferenca > 0) melhoraram++;
        }

        return new Document("total", alunos.size())
                .append("alto", alto)
                .append("medio", medio)
                .append("baixo", baixo)
                .append("entraramEmAlto", entraramEmAlto)
                .append("pioraram", pioraram)
                .append("melhoraram", melhoraram);
    }

    static String descrever(Document r) {
        return r.getInteger("total") + " alunos acompanhados: "
                + r.getInteger("alto") + " em risco alto, "
                + r.getInteger("medio") + " em risco médio e "
                + r.getInteger("baixo") + " em risco baixo. Nos últimos 7 dias, "
                + r.getInteger("entraramEmAlto") + " entraram em risco alto, "
                + r.getInteger("pioraram") + " pioraram e "
                + r.getInteger("melhoraram") + " melhoraram.";
    }
}
