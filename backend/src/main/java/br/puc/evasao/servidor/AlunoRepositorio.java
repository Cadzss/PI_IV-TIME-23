package br.puc.evasao.servidor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coleção "alunos". Formato de cada documento:
 * {
 *   matricula: "2026001", nome: "João P.", turma: "8ºA",
 *   historico: [ { data: "2026-08-01", frequencia: 80.0, nota: 6.5 }, ... ],
 *   risco: { nivel, pontuacao, frequenciaAtual, notaAtual, sinais: [...], acoes: [...], calculadoEm }
 * }
 */
public class AlunoRepositorio {

    private final MongoCollection<Document> alunos;
    private final CalculadoraRisco calculadora = new CalculadoraRisco();

    public AlunoRepositorio(Banco banco) {
        this.alunos = banco.getDatabase().getCollection("alunos");
        alunos.createIndex(Indexes.ascending("matricula"), new IndexOptions().unique(true));
    }

    /** Lista alunos (sem o histórico completo) ordenados por risco: ALTO, MÉDIO, BAIXO. */
    public List<Document> listar(String turma, String nivel) {
        List<Bson> filtros = new ArrayList<>();
        if (turma != null && !turma.isEmpty()) filtros.add(Filters.eq("turma", turma));
        if (nivel != null && !nivel.isEmpty()) filtros.add(Filters.eq("risco.nivel", nivel));
        Bson filtro = filtros.isEmpty() ? new Document() : Filters.and(filtros);

        List<Document> lista = new ArrayList<>();
        alunos.find(filtro)
                .projection(Projections.exclude("_id", "historico"))
                .into(lista);

        lista.sort(Comparator
                .comparingInt((Document a) -> CalculadoraRisco.peso(risco(a).getString("nivel")))
                .thenComparing((Document a) -> -risco(a).getInteger("pontuacao", 0))
                .thenComparing(a -> a.getString("nome")));
        return lista;
    }

    public Document detalhar(String matricula) {
        Document aluno = alunos.find(Filters.eq("matricula", matricula))
                .projection(Projections.exclude("_id"))
                .first();
        if (aluno != null) {
            List<Document> historico = new ArrayList<>(aluno.getList("historico", Document.class, new ArrayList<>()));
            historico.sort(Comparator.comparing(r -> r.getString("data")));
            aluno.put("historico", historico);
        }
        return aluno;
    }

    public List<String> turmas() {
        List<String> turmas = new ArrayList<>();
        alunos.distinct("turma", String.class).into(turmas);
        turmas.sort(String::compareTo);
        return turmas;
    }

    /**
     * Insere/atualiza um aluno com novos registros de frequência e nota, recalcula o risco
     * e gera alerta se o nível mudou. Registros com a mesma data são substituídos.
     */
    public void salvarRegistros(String matricula, String nome, String turma,
                                List<Document> novosRegistros, AlertaRepositorio alertas) {
        Document existente = alunos.find(Filters.eq("matricula", matricula)).first();

        Map<String, Document> porData = new LinkedHashMap<>();
        String nivelAnterior = null;
        if (existente != null) {
            for (Document r : existente.getList("historico", Document.class, new ArrayList<>())) {
                porData.put(r.getString("data"), r);
            }
            nivelAnterior = risco(existente).getString("nivel");
        }
        for (Document r : novosRegistros) {
            porData.put(r.getString("data"), r);
        }

        List<Document> historico = new ArrayList<>(porData.values());
        Document risco = calculadora.calcular(historico);

        Document aluno = new Document("matricula", matricula)
                .append("nome", nome)
                .append("turma", turma)
                .append("historico", historico)
                .append("risco", risco);

        alunos.replaceOne(Filters.eq("matricula", matricula), aluno, new ReplaceOptions().upsert(true));
        alertas.registrarMudanca(aluno, nivelAnterior, risco.getString("nivel"));
    }

    /** Recalcula o risco de todos os alunos (útil se a regra de cálculo mudar). */
    public int recalcularTodos(AlertaRepositorio alertas) {
        int total = 0;
        for (Document aluno : alunos.find()) {
            String nivelAnterior = risco(aluno).getString("nivel");
            Document risco = calculadora.calcular(aluno.getList("historico", Document.class, new ArrayList<>()));
            alunos.updateOne(Filters.eq("_id", aluno.get("_id")), new Document("$set", new Document("risco", risco)));
            aluno.put("risco", risco);
            alertas.registrarMudanca(aluno, nivelAnterior, risco.getString("nivel"));
            total++;
        }
        return total;
    }

    private static Document risco(Document aluno) {
        Document r = aluno.get("risco", Document.class);
        return r == null ? new Document() : r;
    }
}
