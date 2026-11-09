package br.puc.evasao.servidor;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Cálculo do score de risco de evasão (regra simples do MVP):
 * faltas (frequência) + nota + queda de desempenho -> ALTO / MEDIO / BAIXO.
 *
 * Pontuação:
 *   frequência atual  < 75%  -> +3   (abaixo do mínimo legal)
 *   frequência atual  < 85%  -> +1
 *   nota atual        < 5,0  -> +2
 *   nota atual        < 7,0  -> +1
 *   queda de nota     >= 1,0 ponto na janela analisada -> +1
 *   queda de frequência >= 10 p.p. na janela analisada -> +1
 *
 *   pontuação >= 4 -> ALTO | >= 2 -> MEDIO | senão BAIXO
 */
public class CalculadoraRisco {

    public static final String ALTO = "ALTO";
    public static final String MEDIO = "MEDIO";
    public static final String BAIXO = "BAIXO";

    /** Quantos registros mais recentes do histórico entram na análise de queda. */
    private static final int JANELA = 3;

    public Document calcular(List<Document> historico) {
        List<Document> ordenado = new ArrayList<>(historico);
        ordenado.sort(Comparator.comparing(r -> r.getString("data")));

        List<String> sinais = new ArrayList<>();
        int pontos = 0;
        double freqAtual = 0;
        double notaAtual = 0;

        if (!ordenado.isEmpty()) {
            Document atual = ordenado.get(ordenado.size() - 1);
            Document inicioJanela = ordenado.get(Math.max(0, ordenado.size() - JANELA));
            freqAtual = numero(atual, "frequencia");
            notaAtual = numero(atual, "nota");

            if (freqAtual < 75) {
                pontos += 3;
                sinais.add(String.format("Frequência de %.0f%%, abaixo do mínimo de 75%%", freqAtual));
            } else if (freqAtual < 85) {
                pontos += 1;
                sinais.add(String.format("Frequência de %.0f%%, em zona de atenção", freqAtual));
            }

            if (notaAtual < 5) {
                pontos += 2;
                sinais.add(String.format("Nota média de %.1f, abaixo de 5,0", notaAtual));
            } else if (notaAtual < 7) {
                pontos += 1;
                sinais.add(String.format("Nota média de %.1f, abaixo de 7,0", notaAtual));
            }

            double quedaNota = numero(inicioJanela, "nota") - notaAtual;
            if (quedaNota >= 1.0) {
                pontos += 1;
                sinais.add(String.format("Queda de %.1f ponto(s) na nota nos últimos registros", quedaNota));
            }

            double quedaFreq = numero(inicioJanela, "frequencia") - freqAtual;
            if (quedaFreq >= 10) {
                pontos += 1;
                sinais.add(String.format("Queda de %.0f p.p. na frequência nos últimos registros", quedaFreq));
            }
        }

        String nivel = pontos >= 4 ? ALTO : pontos >= 2 ? MEDIO : BAIXO;

        return new Document("nivel", nivel)
                .append("pontuacao", pontos)
                .append("frequenciaAtual", freqAtual)
                .append("notaAtual", notaAtual)
                .append("sinais", sinais)
                .append("acoes", acoesRecomendadas(nivel))
                .append("calculadoEm", new Date());
    }

    private List<String> acoesRecomendadas(String nivel) {
        List<String> acoes = new ArrayList<>();
        switch (nivel) {
            case ALTO:
                acoes.add("Acionar assistente social");
                acoes.add("Contatar a família com urgência");
                acoes.add("Agendar conversa individual com o aluno");
                break;
            case MEDIO:
                acoes.add("Conversar com os professores da turma");
                acoes.add("Comunicar a família sobre a situação");
                break;
            default:
                acoes.add("Manter acompanhamento regular");
        }
        return acoes;
    }

    private static double numero(Document d, String campo) {
        Object v = d.get(campo);
        return v instanceof Number ? ((Number) v).doubleValue() : 0;
    }

    /** Ordem usada para ordenar o dashboard (ALTO primeiro). */
    public static int peso(String nivel) {
        if (ALTO.equals(nivel)) return 0;
        if (MEDIO.equals(nivel)) return 1;
        return 2;
    }
}
