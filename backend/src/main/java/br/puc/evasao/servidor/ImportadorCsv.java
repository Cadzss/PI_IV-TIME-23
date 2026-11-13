package br.puc.evasao.servidor;

import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importa a planilha de frequência e notas (CSV exportado do Excel/Google Planilhas).
 *
 * Cabeçalho esperado (separador "," ou ";"):
 *   matricula,nome,turma,data,frequencia,nota
 *   2026001,João P.,8ºA,2026-08-01,80,6.5
 *
 * - data: AAAA-MM-DD, e a data precisa existir no calendário
 * - frequencia: porcentagem de 0 a 100
 * - nota: 0 a 10 (aceita vírgula decimal: 6,5)
 * - nomes com vírgula precisam ir entre aspas: "Silva, João"
 */
public class ImportadorCsv {

    private static final String[] COLUNAS = {"matricula", "nome", "turma", "data", "frequencia", "nota"};

    private final AlunoRepositorio alunos;
    private final AlertaRepositorio alertas;

    public ImportadorCsv(AlunoRepositorio alunos, AlertaRepositorio alertas) {
        this.alunos = alunos;
        this.alertas = alertas;
    }

    public Document importar(String conteudo) {
        Analise analise = analisar(conteudo);
        for (Document a : analise.porAluno.values()) {
            alunos.salvarRegistros(a.getString("matricula"), a.getString("nome"), a.getString("turma"),
                    a.getList("registros", Document.class), alertas);
        }

        return new Document("alunos", analise.porAluno.size())
                .append("registros", analise.registros)
                .append("erros", analise.erros);
    }

    /** Lê e valida a planilha sem gravar. Usado pelo importador e pelos testes. */
    static Analise analisar(String conteudo) {
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("Planilha vazia ou sem cabeçalho");
        }
        String[] linhas = conteudo.replace("\uFEFF", "").split("\\r?\\n");
        if (linhas.length < 2) {
            throw new IllegalArgumentException("Planilha vazia ou sem cabeçalho");
        }

        char sep = linhas[0].indexOf(';') >= 0 ? ';' : ',';
        Map<String, Integer> indice = lerCabecalho(separar(linhas[0], sep));

        Map<String, Document> porAluno = new LinkedHashMap<>();
        List<String> erros = new ArrayList<>();
        int registros = 0;

        for (int i = 1; i < linhas.length; i++) {
            if (linhas[i].trim().isEmpty()) {
                continue;
            }
            List<String> c = separar(linhas[i], sep);
            try {
                String matricula = campo(c, indice, "matricula");
                double frequencia = decimal(campo(c, indice, "frequencia"));
                double nota = decimal(campo(c, indice, "nota"));
                String data = campo(c, indice, "data");
                validarFaixa(frequencia, nota, data);

                Document aluno = porAluno.computeIfAbsent(matricula, m -> new Document("matricula", m)
                        .append("nome", campo(c, indice, "nome"))
                        .append("turma", campo(c, indice, "turma"))
                        .append("registros", new ArrayList<Document>()));
                aluno.getList("registros", Document.class).add(new Document("data", data)
                        .append("frequencia", frequencia)
                        .append("nota", nota));
                registros++;
            } catch (RuntimeException e) {
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }

        return new Analise(porAluno, registros, erros);
    }

    private static void validarFaixa(double frequencia, double nota, String data) {
        if (frequencia < 0 || frequencia > 100) {
            throw new IllegalArgumentException("frequência fora de 0 a 100: " + frequencia);
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota fora de 0 a 10: " + nota);
        }
        try {
            LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data inválida (use AAAA-MM-DD): " + data);
        }
    }

    private static Map<String, Integer> lerCabecalho(List<String> cabecalho) {
        Map<String, Integer> indice = new LinkedHashMap<>();
        for (int i = 0; i < cabecalho.size(); i++) {
            indice.put(cabecalho.get(i).trim().toLowerCase(), i);
        }
        for (String col : COLUNAS) {
            if (!indice.containsKey(col)) {
                throw new IllegalArgumentException("Coluna obrigatória ausente no cabeçalho: " + col);
            }
        }
        return indice;
    }

    private static String campo(List<String> c, Map<String, Integer> indice, String nome) {
        int i = indice.get(nome);
        String v = i < c.size() ? c.get(i).trim() : "";
        if (v.isEmpty()) {
            throw new IllegalArgumentException("campo '" + nome + "' vazio");
        }
        return v;
    }

    private static double decimal(String v) {
        try {
            return Double.parseDouble(v.replace("%", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor numérico inválido: " + v);
        }
    }

    /** Separa uma linha de CSV respeitando campos entre aspas. */
    static List<String> separar(String linha, char sep) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (aspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    aspas = !aspas;
                }
            } else if (c == sep && !aspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    static final class Analise {
        final Map<String, Document> porAluno;
        final int registros;
        final List<String> erros;

        Analise(Map<String, Document> porAluno, int registros, List<String> erros) {
            this.porAluno = porAluno;
            this.registros = registros;
            this.erros = erros;
        }
    }
}
