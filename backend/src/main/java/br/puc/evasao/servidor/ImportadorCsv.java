package br.puc.evasao.servidor;

import org.bson.Document;

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
 * - data: AAAA-MM-DD (período de referência do registro)
 * - frequencia: porcentagem de 0 a 100
 * - nota: 0 a 10 (aceita vírgula decimal: 6,5)
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
        String[] linhas = conteudo.replace("﻿", "").split("\\r?\\n");
        if (linhas.length < 2) {
            throw new IllegalArgumentException("Planilha vazia ou sem cabeçalho");
        }

        String sep = linhas[0].contains(";") ? ";" : ",";
        Map<String, Integer> indice = lerCabecalho(linhas[0].split(sep, -1));

        // agrupa os registros por aluno antes de salvar (um recálculo de risco por aluno)
        Map<String, Document> porAluno = new LinkedHashMap<>();
        List<String> erros = new ArrayList<>();
        int registros = 0;

        for (int i = 1; i < linhas.length; i++) {
            if (linhas[i].trim().isEmpty()) continue;
            String[] c = linhas[i].split(sep, -1);
            try {
                String matricula = campo(c, indice, "matricula");
                Document aluno = porAluno.computeIfAbsent(matricula, m -> new Document("matricula", m)
                        .append("nome", campo(c, indice, "nome"))
                        .append("turma", campo(c, indice, "turma"))
                        .append("registros", new ArrayList<Document>()));
                aluno.getList("registros", Document.class).add(new Document("data", campo(c, indice, "data"))
                        .append("frequencia", decimal(campo(c, indice, "frequencia")))
                        .append("nota", decimal(campo(c, indice, "nota"))));
                registros++;
            } catch (RuntimeException e) {
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }

        for (Document a : porAluno.values()) {
            alunos.salvarRegistros(a.getString("matricula"), a.getString("nome"), a.getString("turma"),
                    a.getList("registros", Document.class), alertas);
        }

        return new Document("alunos", porAluno.size())
                .append("registros", registros)
                .append("erros", erros);
    }

    private Map<String, Integer> lerCabecalho(String[] cabecalho) {
        Map<String, Integer> indice = new LinkedHashMap<>();
        for (int i = 0; i < cabecalho.length; i++) {
            indice.put(cabecalho[i].trim().toLowerCase(), i);
        }
        for (String col : COLUNAS) {
            if (!indice.containsKey(col)) {
                throw new IllegalArgumentException("Coluna obrigatória ausente no cabeçalho: " + col);
            }
        }
        return indice;
    }

    private static String campo(String[] c, Map<String, Integer> indice, String nome) {
        int i = indice.get(nome);
        String v = i < c.length ? c[i].trim() : "";
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
}
