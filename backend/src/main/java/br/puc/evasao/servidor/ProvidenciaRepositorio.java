// Autor: Lucas Canto
package br.puc.evasao.servidor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coleção "providencias": o que a escola fez a partir das ações recomendadas.
 * { matricula, acao, observacao?, status: EM_ANDAMENTO|CONCLUIDA, criadaPor {nome,email}, criadaEm,
 *   resultado?, concluidaPor?, concluidaEm? }
 */
public class ProvidenciaRepositorio {

    private final MongoCollection<Document> providencias;

    public ProvidenciaRepositorio(Banco banco) {
        this.providencias = banco.getDatabase().getCollection("providencias");
        // Índice para as buscas mais comuns (providências de um aluno / em andamento)
        providencias.createIndex(Indexes.ascending("matricula", "status"));
    }

    public void registrar(String matricula, String acao, String observacao, Document usuario) {
        // Não deixa abrir duas vezes a mesma ação para o mesmo aluno enquanto a primeira não terminou
        boolean jaAberta = providencias.find(Filters.and(
                Filters.eq("matricula", matricula),
                Filters.eq("acao", acao),
                Filters.eq("status", RegrasProvidencia.EM_ANDAMENTO))).first() != null;
        if (jaAberta) {
            throw new IllegalArgumentException("Essa providência já está em andamento para este aluno.");
        }
        Document nova = new Document("matricula", matricula)
                .append("acao", acao)
                .append("status", RegrasProvidencia.EM_ANDAMENTO)
                .append("criadaPor", autor(usuario))
                .append("criadaEm", new Date());
        if (observacao != null) {
            nova.append("observacao", observacao);
        }
        providencias.insertOne(nova);
    }

    /** Conclui e devolve a matrícula do aluno da providência. */
    public String concluir(String id, String resultado, Document usuario) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Não encontramos essa providência.");
        }
        Document existente = providencias.find(Filters.eq("_id", new ObjectId(id))).first();
        if (existente == null) {
            throw new IllegalArgumentException("Não encontramos essa providência.");
        }
        if (RegrasProvidencia.CONCLUIDA.equals(existente.getString("status"))) {
            throw new IllegalArgumentException("Essa providência já foi concluída.");
        }
        providencias.updateOne(Filters.eq("_id", existente.getObjectId("_id")), Updates.combine(
                Updates.set("status", RegrasProvidencia.CONCLUIDA),
                Updates.set("resultado", resultado),
                Updates.set("concluidaPor", autor(usuario)),
                Updates.set("concluidaEm", new Date())));
        return existente.getString("matricula");
    }

    /** Providências do aluno, mais recentes primeiro. */
    public List<Document> listar(String matricula) {
        List<Document> lista = new ArrayList<>();
        for (Document p : providencias.find(Filters.eq("matricula", matricula)).sort(Sorts.descending("criadaEm"))) {
            p.put("id", p.getObjectId("_id").toHexString());
            p.remove("_id");
            lista.add(p);
        }
        return lista;
    }

    /** Matrículas que têm pelo menos uma providência em andamento. */
    public Set<String> matriculasEmAcompanhamento() {
        Set<String> matriculas = new HashSet<>();
        for (Document p : providencias.find(Filters.eq("status", RegrasProvidencia.EM_ANDAMENTO))
                .projection(Projections.include("matricula"))) {
            matriculas.add(p.getString("matricula"));
        }
        return matriculas;
    }

    public void removerDoAluno(String matricula) {
        providencias.deleteMany(Filters.eq("matricula", matricula));
    }

    private static Document autor(Document usuario) {
        return new Document("nome", usuario.getString("nome")).append("email", usuario.getString("email"));
    }
}
