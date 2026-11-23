// Autor: Cadu Spadari
// Alterações: Lucas Canto — e-mail em segundo plano (fila), títulos iguais ao protótipo,
//             relatório semanal, contagem de não lidos e limpeza dos avisos de aluno removido
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Coleção MongoDB "alertas". */
public class AlertaRepositorio {

    public static final String NOVO_RISCO_ALTO = "NOVO_RISCO_ALTO";
    public static final String MUDANCA_NIVEL = "MUDANCA_NIVEL";
    public static final String RELATORIO_SEMANAL = "RELATORIO_SEMANAL";

    private final MongoCollection<Document> alertas;

    /**
     * O e-mail sai em segundo plano: a importação não espera o SMTP (que pode levar vários segundos
     * por mensagem ou estar fora do ar). O alerta é gravado na hora com status "pendente".
     */
    private final ExecutorService filaEmail = Executors.newSingleThreadExecutor(tarefa -> {
        Thread t = new Thread(tarefa, "envio-email");
        t.setDaemon(true);
        return t;
    });

    public AlertaRepositorio(Banco banco) {
        this.alertas = banco.getDatabase().getCollection("alertas");
    }

    /**
     * Chamado sempre que o risco de um aluno é recalculado. Só gera aviso quando o nível MUDA
     * (ou quando um aluno novo já chega em ALTO). Se entrou em ALTO, também manda e-mail.
     */
    public void registrarMudanca(Document aluno, String nivelAnterior, String nivelNovo) {
        if (nivelNovo.equals(nivelAnterior)) {
            return; // não mudou nada, não precisa avisar
        }
        if (nivelAnterior == null && !CalculadoraRisco.ALTO.equals(nivelNovo)) {
            return; // aluno novo em MÉDIO/BAIXO não gera aviso (senão a primeira planilha geraria um monte)
        }

        String nome = aluno.getString("nome");
        String turma = aluno.getString("turma");
        String tipo;
        String titulo;
        if (CalculadoraRisco.ALTO.equals(nivelNovo)) {
            tipo = NOVO_RISCO_ALTO;
            // Mesmo texto do protótipo (Tela 3)
            titulo = nivelAnterior == null
                    ? "Novo aluno em risco ALTO: " + nome + " (" + turma + ")"
                    : nome + " (" + turma + ") mudou de risco " + rotulo(nivelAnterior) + " para ALTO";
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

        String corpo = null;
        if (NOVO_RISCO_ALTO.equals(tipo)) {
            corpo = corpoEmail(aluno, nivelAnterior);
            documento.append("email", new Document("destino", NotificadorEmail.destino())
                    .append("assunto", titulo)
                    .append("status", "pendente"));
        }
        alertas.insertOne(documento);

        // Primeiro grava o aviso (com e-mail "pendente"), depois manda o e-mail sem travar quem chamou
        if (corpo != null) {
            enviarEmSegundoPlano(documento.getObjectId("_id"), titulo, corpo);
        }
    }

    private void enviarEmSegundoPlano(ObjectId idAlerta, String assunto, String corpo) {
        filaEmail.submit(() -> {
            try {
                Document email = new NotificadorEmail().notificar(assunto, corpo);
                alertas.updateOne(Filters.eq("_id", idAlerta), Updates.set("email", email));
            } catch (RuntimeException e) {
                System.out.println("[E-mail] Não foi possível concluir o envio: " + e.getMessage());
            }
        });
    }

    /** Espera os e-mails que ainda estão na fila (chamar antes de fechar o banco). */
    public void aguardarEmails() {
        filaEmail.shutdown();
        try {
            if (!filaEmail.awaitTermination(2, TimeUnit.MINUTES)) {
                System.out.println("[E-mail] Tempo esgotado; alguns e-mails ficaram como pendentes.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    /** Grava o aviso "Relatório semanal de risco disponível" (Tela 3 do protótipo). */
    public void registrarRelatorioSemanal(Document resumo, String descricao) {
        alertas.insertOne(new Document("tipo", RELATORIO_SEMANAL)
                .append("titulo", "Relatório semanal de risco disponível")
                .append("descricao", descricao)
                .append("resumo", resumo)
                .append("lido", false)
                .append("criadoEm", new Date()));
    }

    public boolean existeRelatorioDesde(Date desde) {
        return alertas.find(Filters.and(Filters.eq("tipo", RELATORIO_SEMANAL), Filters.gte("criadoEm", desde)))
                .first() != null;
    }

    /** Mudanças de nível (inclusive aluno novo em ALTO) desde a data. */
    public List<Document> mudancasDesde(Date desde) {
        List<Document> lista = new ArrayList<>();
        alertas.find(Filters.and(Filters.in("tipo", NOVO_RISCO_ALTO, MUDANCA_NIVEL), Filters.gte("criadoEm", desde)))
                .into(lista);
        return lista;
    }

    /** Apaga os avisos de um aluno removido (o link "Ver aluno" deixaria de funcionar). */
    public void removerDoAluno(String matricula) {
        alertas.deleteMany(Filters.eq("matricula", matricula));
    }

    public long contarNaoLidos() {
        return alertas.countDocuments(Filters.eq("lido", false));
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