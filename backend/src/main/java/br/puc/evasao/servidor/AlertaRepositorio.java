// Autor: Cadu Spadari
package br.puc.evasao.servidor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Coleção MongoDB "alertas". */
public class AlertaRepositorio {

    public static final String NOVO_RISCO_ALTO = "NOVO_RISCO_ALTO";
    public static final String MUDANCA_NIVEL = "MUDANCA_NIVEL";

    private final MongoCollection<Document> alertas;

    public AlertaRepositorio(Banco banco) {
        this.alertas = banco.getDatabase().getCollection("alertas");
    }

    public void registrarMudanca(Document aluno, String nivelAnterior, String nivelNovo) {
        if (nivelNovo.equals(nivelAnterior)) {
            return;
        }
        if (nivelAnterior == null && !CalculadoraRisco.ALTO.equals(nivelNovo)) {
            return;
        }

        String nome = aluno.getString("nome");
        String turma = aluno.getString("turma");
        String tipo;
        String titulo;
        if (CalculadoraRisco.ALTO.equals(nivelNovo)) {
            tipo = NOVO_RISCO_ALTO;
            titulo = nivelAnterior == null
                    ? "Novo aluno em risco ALTO: " + nome + " (" + turma + ")"
                    : nome + " (" + turma + ") entrou em risco ALTO (antes: " + rotulo(nivelAnterior) + ")";
        } else {
            tipo = MUDANCA_NIVEL;
            titulo = nome + " (" + turma + ") mudou de risco " + rotulo(nivelAnterior) + " para " + rotulo(nivelNovo);
        }

        Document documento = new Document("tipo", tipo)
                .append("matricula", aluno.getString("matricula"))
                .append("nomeAluno", nome)
                .append("turma", turma)
                .append("nivelAnterior", nivelAnterior)
                .append("nivelNovo", nivelNovo)
                .append("titulo", titulo)
                .append("lido", false)
                .append("criadoEm", new Date());

        if (NOVO_RISCO_ALTO.equals(tipo)) {
            documento.append("email", new NotificadorEmail().notificar(titulo, corpoEmail(aluno, nivelAnterior)));
        }
        alertas.insertOne(documento);
    }

    private static String corpoEmail(Document aluno, String nivelAnterior) {
        StringBuilder texto = new StringBuilder();
        texto.append("Aluno: ").append(aluno.getString("nome"))
                .append(" (").append(aluno.getString("turma")).append(")\n");
        texto.append("Matrícula: ").append(aluno.getString("matricula")).append('\n');
        texto.append("Nível anterior: ")
                .append(nivelAnterior == null ? "novo aluno" : rotulo(nivelAnterior)).append('\n');
        texto.append("Nível atual: ALTO\n");

        Document risco = aluno.get("risco", Document.class);
        if (risco != null) {
            List<String> sinais = risco.getList("sinais", String.class, new ArrayList<>());
            if (!sinais.isEmpty()) {
                texto.append("\nSinais:\n");
                for (String sinal : sinais) {
                    texto.append("- ").append(sinal).append('\n');
                }
            }
            List<String> acoes = risco.getList("acoes", String.class, new ArrayList<>());
            if (!acoes.isEmpty()) {
                texto.append("\nAções recomendadas:\n");
                for (String acao : acoes) {
                    texto.append("- ").append(acao).append('\n');
                }
            }
        }
        return texto.toString();
    }

    public List<Document> listar() {
        List<Document> lista = new ArrayList<>();
        for (Document a : alertas.find().sort(Sorts.descending("criadoEm")).limit(100)) {
            a.put("id", a.getObjectId("_id").toHexString());
            a.remove("_id");
            lista.add(a);
        }
        return lista;
    }

    public boolean marcarLido(String id) {
        if (!ObjectId.isValid(id)) {
            return false;
        }
        return alertas.updateOne(Filters.eq("_id", new ObjectId(id)), Updates.set("lido", true))
                .getMatchedCount() > 0;
    }

    private static String rotulo(String nivel) {
        return CalculadoraRisco.MEDIO.equals(nivel) ? "MÉDIO" : String.valueOf(nivel);
    }
}