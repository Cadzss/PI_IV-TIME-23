// Autor: Cadu Spadari
// Alterações: Lucas Canto — colunas opcionais de engajamento, data dd/mm/aaaa, cabeçalho com acento,
//             avisos de nome/turma divergente, prévia sem gravar e validação reaproveitada pela tela
package br.puc.evasao.servidor;

import org.bson.Document;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa CSV matricula,nome,turma,data,frequencia,nota (separador , ou ;).
 * Colunas opcionais de engajamento: atrasos (quantidade no período) e atividades (% entregues).
 * Cabeçalho aceita acentos e maiúsculas ("Matrícula", "Frequência"); data em AAAA-MM-DD ou DD/MM/AAAA.
 * (Arquivos .xlsx são convertidos para este formato no navegador antes de chegar aqui.)
 */
public class ImportadorCsv {

    private static final String[] COLUNAS = {"matricula", "nome", "turma", "data", "frequencia", "nota"};
    static final String ATRASOS = "atrasos";
    static final String ATIVIDADES = "atividades";

    private static final Pattern DATA_BR = Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$");
    private static final int LIMITE_AMOSTRA = 10;

    private final AlunoRepositorio alunos;
    private final AlertaRepositorio alertas;

    public ImportadorCsv(AlunoRepositorio alunos, AlertaRepositorio alertas) {
        this.alunos = alunos;
        this.alertas = alertas;
    }

    /** Lê a planilha e grava tudo (usado no "Confirmar e salvar"). */
    public Document importar(String conteudo) {
        Analise analise = analisar(conteudo);
        for (Document a : analise.porAluno.values()) {
            alunos.salvarRegistros(
                    a.getString("matricula"),
                    a.getString("nome"),
                    a.getString("turma"),
                    a.getList("registros", Document.class),
                    alertas);
        }
        return new Document("alunos", analise.porAluno.size())
                .append("registros", analise.registros)
                .append("erros", analise.erros)
                .append("avisos", analise.avisos);
    }

    /** Confere a planilha sem gravar nada: o que vai entrar, o que tem problema, quem é novo. */
    public Document previsualizar(String conteudo) {
        Analise analise = analisar(conteudo);
        Set<String> existentes = alunos.matriculasExistentes(analise.porAluno.keySet());

        List<Document> amostra = new ArrayList<>();
        for (Document a : analise.porAluno.values()) {
            for (Document r : a.getList("registros", Document.class)) {
                if (amostra.size() >= LIMITE_AMOSTRA) break;
                amostra.add(new Document("matricula", a.getString("matricula"))
                        .append("nome", a.getString("nome"))
                        .append("turma", a.getString("turma"))
                        .append("novo", !existentes.contains(a.getString("matricula")))
                        .append("registro", r));
            }
        }
        return new Document("alunos", analise.porAluno.size())
                .append("novos", analise.porAluno.size() - existentes.size())
                .append("existentes", existentes.size())
                .append("registros", analise.registros)
                .append("erros", analise.erros)
                .append("avisos", analise.avisos)
                .append("amostra", amostra);
    }

    static Analise analisar(String conteudo) {
        if (conteudo == null || conteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("A planilha está vazia ou sem a linha com os nomes das colunas");
        }
        // \uFEFF é o BOM que o Excel coloca no começo do CSV; \r?\n aceita quebra de linha do Windows e do Mac
        String[] linhas = conteudo.replace("\uFEFF", "").split("\\r?\\n");
        if (linhas.length < 2) {
            throw new IllegalArgumentException("A planilha está vazia ou sem a linha com os nomes das colunas");
        }

        // Excel em português costuma salvar com ";" — se o cabeçalho tiver ";", usa ele como separador
        char sep = linhas[0].indexOf(';') >= 0 ? ';' : ',';
        Map<String, Integer> indice = lerCabecalho(separar(linhas[0], sep));

        Map<String, Document> porAluno = new LinkedHashMap<>();
        List<String> erros = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        int registros = 0;

        for (int i = 1; i < linhas.length; i++) {
            if (linhas[i].trim().isEmpty()) {
                continue;
            }
            List<String> c = separar(linhas[i], sep);
            try {
                String matricula = campo(c, indice, "matricula");
                Document registro = montarRegistro(
                        campo(c, indice, "data"),
                        decimal(campo(c, indice, "frequencia")),
                        decimal(campo(c, indice, "nota")),
                        numeroOpcional(c, indice, ATRASOS),
                        numeroOpcional(c, indice, ATIVIDADES));

                Document aluno = porAluno.computeIfAbsent(matricula, m -> new Document("matricula", m)
                        .append("nome", campo(c, indice, "nome"))
                        .append("turma", campo(c, indice, "turma"))
                        .append("registros", new ArrayList<Document>()));
                avisarSeDiferente(avisos, i + 1, matricula, "nome", aluno.getString("nome"), c, indice);
                avisarSeDiferente(avisos, i + 1, matricula, "turma", aluno.getString("turma"), c, indice);
                aluno.getList("registros", Document.class).add(registro);
                registros++;
            } catch (RuntimeException e) {
                // Uma linha ruim não derruba a planilha inteira: anota o erro e segue para a próxima.
                // (i + 1 porque a linha 1 da planilha é o cabeçalho)
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new Analise(porAluno, registros, erros, avisos);
    }

    /** A mesma matrícula com nome/turma diferente na planilha: vale o primeiro, e a pessoa fica sabendo. */
    private static void avisarSeDiferente(List<String> avisos, int linha, String matricula, String coluna,
                                          String primeiro, List<String> c, Map<String, Integer> indice) {
        String valor = campo(c, indice, coluna);
        if (!valor.equals(primeiro)) {
            avisos.add("Linha " + linha + ": a matrícula " + matricula + " aparece com " + coluna + " \""
                    + valor + "\", mas antes estava \"" + primeiro + "\". Mantivemos \"" + primeiro + "\".");
        }
    }

    /**
     * Valida e monta um lançamento (usado pela planilha e pela correção na tela do aluno).
     * Atrasos e atividades são opcionais (null = não informado).
     */
    static Document montarRegistro(String data, double frequencia, double nota, Double atrasos, Double atividades) {
        String dataIso = normalizarData(data);
        if (frequencia < 0 || frequencia > 100) {
            throw new IllegalArgumentException("a frequência precisa ficar entre 0 e 100");
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("a nota precisa ficar entre 0 e 10");
        }
        Document registro = new Document("data", dataIso).append("frequencia", frequencia).append("nota", nota);
        if (atrasos != null) {
            if (atrasos < 0 || atrasos != Math.floor(atrasos)) {
                throw new IllegalArgumentException("os atrasos precisam ser um número inteiro, 0 ou mais");
            }
            registro.append(ATRASOS, atrasos.intValue());
        }
        if (atividades != null) {
            if (atividades < 0 || atividades > 100) {
                throw new IllegalArgumentException("as atividades entregues precisam ficar entre 0 e 100");
            }
            registro.append(ATIVIDADES, atividades);
        }
        return registro;
    }

    /** Aceita AAAA-MM-DD ou DD/MM/AAAA e devolve sempre AAAA-MM-DD. Recusa dias que não existem. */
    static String normalizarData(String data) {
        String texto = data == null ? "" : data.trim();
        try {
            Matcher br = DATA_BR.matcher(texto);
            if (br.matches()) {
                return LocalDate.of(Integer.parseInt(br.group(3)), Integer.parseInt(br.group(2)),
                        Integer.parseInt(br.group(1))).toString();
            }
            return LocalDate.parse(texto).toString();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("a data \"" + texto
                    + "\" não é válida (use dia/mês/ano, ex.: 01/11/2026, ou ano-mês-dia, ex.: 2026-11-01)");
        }
    }

    /** Coluna opcional: vazia ou ausente devolve null. */
    private static String opcional(List<String> c, Map<String, Integer> indice, String nome) {
        Integer i = indice.get(nome);
        if (i == null || i >= c.size()) return null;
        String v = c.get(i).trim();
        return v.isEmpty() ? null : v;
    }

    private static Double numeroOpcional(List<String> c, Map<String, Integer> indice, String nome) {
        String v = opcional(c, indice, nome);
        return v == null ? null : decimal(v);
    }

    private static Map<String, Integer> lerCabecalho(List<String> cabecalho) {
        Map<String, Integer> indice = new LinkedHashMap<>();
        for (int i = 0; i < cabecalho.size(); i++) {
            indice.put(normalizarCabecalho(cabecalho.get(i)), i);
        }
        for (String col : COLUNAS) {
            if (!indice.containsKey(col)) {
                throw new IllegalArgumentException("Falta a coluna \"" + col + "\" na primeira linha da planilha");
            }
        }
        return indice;
    }

    /** "Matrícula " → "matricula", "Frequência" → "frequencia". */
    static String normalizarCabecalho(String nome) {
        return Normalizer.normalize(nome.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private static String campo(List<String> c, Map<String, Integer> indice, String nome) {
        int i = indice.get(nome);
        String v = i < c.size() ? c.get(i).trim() : "";
        if (v.isEmpty()) {
            throw new IllegalArgumentException("a coluna \"" + nome + "\" está vazia");
        }
        return v;
    }

    private static double decimal(String v) {
        try {
            return Double.parseDouble(v.replace("%", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + v + "\" não é um número");
        }
    }

    /**
     * Divide uma linha do CSV em campos, respeitando aspas: "Silva, João" é um campo só,
     * e "" dentro de aspas vira uma aspa literal (é o padrão do CSV).
     */
    static List<String> separar(String linha, char sep) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char ch = linha.charAt(i);
            if (ch == '"') {
                if (aspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    aspas = !aspas;
                }
            } else if (ch == sep && !aspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    /** Resultado da leitura: alunos agrupados por matrícula, quantos registros entraram, erros e avisos. */
    static final class Analise {
        final Map<String, Document> porAluno;
        final int registros;
        final List<String> erros;
        final List<String> avisos;

        Analise(Map<String, Document> porAluno, int registros, List<String> erros, List<String> avisos) {
            this.porAluno = porAluno;
            this.registros = registros;
            this.erros = erros;
            this.avisos = avisos;
        }
    }
}