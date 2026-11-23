// Autor: Lucas Canto
// (regra +4 para frequência < 75%, engajamento, textos em pt-BR e lista de ações: Lucas Canto)
package br.puc.evasao.servidor;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Cálculo do score de risco de evasão (regra simples do MVP):
 * faltas (frequência) + nota + queda de desempenho -> ALTO / MEDIO / BAIXO.
 *
 * Pontuação:
 *   frequência atual  < 75%  -> +4   (abaixo do mínimo legal: sozinha já leva a ALTO)
 *   frequência atual  < 85%  -> +1
 *   nota atual        < 5,0  -> +2
 *   nota atual        < 7,0  -> +1
 *   queda de nota     >= 1,0 ponto na janela analisada -> +1
 *   queda de frequência >= 10 p.p. na janela analisada -> +1
 *   engajamento (opcional, quando a planilha traz):
 *     atrasos no período >= 5 -> +1   (atrasos recorrentes)
 *     atividades entregues < 50% -> +2 | < 75% -> +1   (ausência em atividades)
 *
 *   pontuação >= 4 -> ALTO | >= 2 -> MEDIO | senão BAIXO
 */
public class CalculadoraRisco {

    public static final String ALTO = "ALTO";
    public static final String MEDIO = "MEDIO";
    public static final String BAIXO = "BAIXO";

    /** Quantos registros mais recentes do histórico entram na análise de queda. */
    private static final int JANELA = 3;

    static final int LIMITE_ATRASOS = 5;

    static final List<String> ACOES_ALTO = List.of(
            "Acionar assistente social",
            "Contatar a família com urgência",
            "Agendar conversa individual com o aluno");
    static final List<String> ACOES_MEDIO = List.of(
            "Conversar com os professores da turma",
            "Comunicar a família sobre a situação");
    static final List<String> ACOES_BAIXO = List.of(
            "Manter acompanhamento regular");

    /** Todas as ações que o sistema pode recomendar (as providências só aceitam estas). */
    public static final List<String> TODAS_AS_ACOES = juntar(ACOES_ALTO, ACOES_MEDIO, ACOES_BAIXO);

    /**
     * Recebe o histórico do aluno (um registro por data) e devolve o documento "risco":
     * nível, pontuação, valores atuais, sinais (motivos em texto) e ações recomendadas.
     */
    public Document calcular(List<Document> historico) {
        // Copia antes de ordenar para não bagunçar a lista de quem chamou.
        // As datas estão em AAAA-MM-DD, então ordem alfabética = ordem cronológica.
        List<Document> ordenado = new ArrayList<>(historico);
        ordenado.sort(Comparator.comparing(r -> r.getString("data")));

        List<String> sinais = new ArrayList<>();
        int pontos = 0;
        double freqAtual = 0;
        double notaAtual = 0;
        Document engajamento = new Document();

        if (!ordenado.isEmpty()) {
            // "atual" = registro mais recente; "inicioJanela" = o mais antigo dos últimos 3,
            // usado para medir se a nota/frequência caiu
            Document atual = ordenado.get(ordenado.size() - 1);
            Document inicioJanela = ordenado.get(Math.max(0, ordenado.size() - JANELA));
            freqAtual = numero(atual, "frequencia");
            notaAtual = numero(atual, "nota");

            if (freqAtual < 75) {
                pontos += 4;
                sinais.add(texto("Frequência de %.0f%%, abaixo do mínimo de 75%%", freqAtual));
            } else if (freqAtual < 85) {
                pontos += 1;
                sinais.add(texto("Frequência de %.0f%%, perto do limite", freqAtual));
            }

            if (notaAtual < 5) {
                pontos += 2;
                sinais.add(texto("Nota %.1f, abaixo de 5,0", notaAtual));
            } else if (notaAtual < 7) {
                pontos += 1;
                sinais.add(texto("Nota %.1f, abaixo de 7,0", notaAtual));
            }

            double quedaNota = numero(inicioJanela, "nota") - notaAtual;
            if (quedaNota >= 1.0) {
                pontos += 1;
                sinais.add(texto("A nota caiu %.1f ponto(s) nos últimos meses", quedaNota));
            }

            double quedaFreq = numero(inicioJanela, "frequencia") - freqAtual;
            if (quedaFreq >= 10) {
                pontos += 1;
                sinais.add(texto("A frequência caiu %.0f%% nos últimos meses", quedaFreq));
            }

            // Engajamento: só conta quando a planilha trouxe essas colunas
            Object atrasos = atual.get(ImportadorCsv.ATRASOS);
            if (atrasos instanceof Number) engajamento.append("atrasosAtual", atrasos);
            if (atrasos instanceof Number && ((Number) atrasos).intValue() >= LIMITE_ATRASOS) {
                pontos += 1;
                sinais.add(texto("Chegou atrasado %.0f vezes no último período", ((Number) atrasos).doubleValue()));
            }
            Object atividades = atual.get(ImportadorCsv.ATIVIDADES);
            if (atividades instanceof Number) {
                engajamento.append("atividadesAtual", atividades);
                double entregues = ((Number) atividades).doubleValue();
                if (entregues < 50) {
                    pontos += 2;
                    sinais.add(texto("Entregou só %.0f%% das atividades", entregues));
                } else if (entregues < 75) {
                    pontos += 1;
                    sinais.add(texto("Entregou %.0f%% das atividades, abaixo do esperado", entregues));
                }
            }
        }

        // Classificação final: 4+ pontos = ALTO, 2 ou 3 = MÉDIO, 0 ou 1 = BAIXO
        String nivel = pontos >= 4 ? ALTO : pontos >= 2 ? MEDIO : BAIXO;

        Document risco = new Document("nivel", nivel)
                .append("pontuacao", pontos)
                .append("frequenciaAtual", freqAtual)
                .append("notaAtual", notaAtual);
        risco.putAll(engajamento); // só entra se a planilha trouxe atrasos/atividades
        return risco
                .append("sinais", sinais)
                .append("acoes", acoesRecomendadas(nivel))
                .append("calculadoEm", new Date());
    }

    private List<String> acoesRecomendadas(String nivel) {
        switch (nivel) {
            case ALTO:
                return new ArrayList<>(ACOES_ALTO);
            case MEDIO:
                return new ArrayList<>(ACOES_MEDIO);
            default:
                return new ArrayList<>(ACOES_BAIXO);
        }
    }

    @SafeVarargs
    private static List<String> juntar(List<String>... listas) {
        List<String> todas = new ArrayList<>();
        for (List<String> l : listas) todas.addAll(l);
        return List.copyOf(todas);
    }

    /** Texto com número no formato brasileiro (vírgula decimal). */
    private static String texto(String modelo, double valor) {
        return String.format(new Locale("pt", "BR"), modelo, valor);
    }

    /** Lê um campo numérico do registro (Integer ou Double); se não existir, vale 0. */
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
