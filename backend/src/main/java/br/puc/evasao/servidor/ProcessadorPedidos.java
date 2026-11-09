package br.puc.evasao.servidor;

import br.puc.evasao.protocolo.Protocolo;
import org.bson.Document;

/** Recebe um pedido do protocolo, executa a ação correspondente e monta a resposta. */
public class ProcessadorPedidos {

    private final AlunoRepositorio alunos;
    private final AlertaRepositorio alertas;
    private final ImportadorCsv importador;

    public ProcessadorPedidos(AlunoRepositorio alunos, AlertaRepositorio alertas) {
        this.alunos = alunos;
        this.alertas = alertas;
        this.importador = new ImportadorCsv(alunos, alertas);
    }

    public Document processar(Document pedido) {
        String acao = pedido.getString("acao");
        Document dados = pedido.get("dados", Document.class);
        if (dados == null) dados = new Document();

        try {
            if (acao == null) {
                return Protocolo.erro("Pedido sem 'acao'");
            }
            switch (acao) {
                case Protocolo.LISTAR_ALUNOS:
                    return Protocolo.sucesso(alunos.listar(dados.getString("turma"), dados.getString("nivel")));

                case Protocolo.DETALHAR_ALUNO: {
                    Document aluno = alunos.detalhar(dados.getString("matricula"));
                    return aluno == null ? Protocolo.erro("Aluno não encontrado") : Protocolo.sucesso(aluno);
                }

                case Protocolo.LISTAR_TURMAS:
                    return Protocolo.sucesso(alunos.turmas());

                case Protocolo.LISTAR_ALERTAS:
                    return Protocolo.sucesso(alertas.listar());

                case Protocolo.MARCAR_ALERTA_LIDO:
                    return alertas.marcarLido(dados.getString("id"))
                            ? Protocolo.sucesso(null)
                            : Protocolo.erro("Alerta não encontrado");

                case Protocolo.IMPORTAR_CSV:
                    return Protocolo.sucesso(importador.importar(dados.getString("conteudo")));

                case Protocolo.RECALCULAR_RISCOS:
                    return Protocolo.sucesso(new Document("recalculados", alunos.recalcularTodos(alertas)));

                default:
                    return Protocolo.erro("Ação desconhecida: " + acao);
            }
        } catch (IllegalArgumentException e) {
            return Protocolo.erro(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Protocolo.erro("Erro interno no servidor: " + e.getMessage());
        }
    }
}
